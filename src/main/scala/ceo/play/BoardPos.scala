package ceo.play

import scala.language.implicitConversions

class BoardPos private(val row: Int, val column: Int) {

  import BoardPos.List1to8

  @inline final def +(other: Distance): BoardPos = BoardPos(row + other.rowDiff, column + other.columnDiff)

  @inline final def -(other: Distance): BoardPos = BoardPos(row - other.rowDiff, column - other.columnDiff)

  @inline final def -(other: BoardPos): Distance = Distance(row - other.row, column - other.column)

  override def toString: String = s"($row, $column)"

  @inline final def isValid: Boolean = row >= 0

  @inline final def getPiece(board: Board): Option[Piece] = if (isValid) board(row, column) else None

  @inline final def isEmpty(board: Board): Boolean = isValid && getPiece(board).isEmpty

  @inline final def nonEmpty(board: Board): Boolean = !isValid || getPiece(board).nonEmpty

  @inline final def allPosUntilAreEmpty(target: BoardPos, board: Board): Boolean = {
    val rowDiff = target.row - row
    val colDiff = target.column - column
    val dx = if (colDiff == 0) 0 else if (colDiff < 0) -1 else 1
    val dy = if (rowDiff == 0) 0 else if (rowDiff < 0) -1 else 1
    val maxDist = Math.max(Math.abs(colDiff), Math.abs(rowDiff)) - 1
    List1to8.view(0, maxDist).forall(distance => BoardPos(row + dy * distance, column + dx * distance).isEmpty(board))
  }

  @inline final def allPosUntilAreEmptyOrGhost(target: BoardPos, board: Board): Boolean = {
    val rowDiff = target.row - row
    val colDiff = target.column - column
    val dx = if (colDiff == 0) 0 else if (colDiff < 0) -1 else 1
    val dy = if (rowDiff == 0) 0 else if (rowDiff < 0) -1 else 1
    val maxDist = Math.max(Math.abs(colDiff), Math.abs(rowDiff)) - 1
    List1to8.view(0, maxDist).forall(distance => {
      val boardPos = BoardPos(row + dy * distance, column + dx * distance)
      boardPos.isValid && {
        boardPos.getPiece(board) match {
          case None => true
          case Some(piece) if piece.data.isGhost => true
          case _ => false
        }
      }
    })
  }

  def distanceTo(other: BoardPos): Int = Math.max(Math.abs(row - other.row), Math.abs(column - other.column))

  def canEqual(other: Any): Boolean = other.isInstanceOf[BoardPos]

  override def equals(other: Any): Boolean = other match {
    case that: BoardPos =>
      (that canEqual this) && row == that.row && column == that.column
    case _ => false
  }

  override def hashCode(): Int = row * 8 + column
}

object BoardPos {

  final val List1to8: List[Int] = (1 to 8).toList

  def unapply(boardPos: BoardPos): Option[(Int, Int)] = Some(boardPos.row, boardPos.column)

  private final val cache: Array[BoardPos] = (for {
    row <- 0 until 8
    column <- 0 until 8
  } yield new BoardPos(row, column)).toArray ++ Array(new BoardPos(-1, -1))

  def apply(row: Int, column: Int): BoardPos = {
    if (row < 0 || row >= 8 || column < 0 || column >= 8)
      cache(64)
    else
      cache(row * 8 + column)
  }

  val allBoardPositions: List[BoardPos] = (for {
    row <- 0 until 8
    column <- 0 until 8
  } yield BoardPos(row, column)).toList
}
