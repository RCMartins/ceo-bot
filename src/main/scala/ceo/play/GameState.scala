package ceo.play

import ceo.play.Player.{PlayerBlack, PlayerWhite}
import ceo.play.PlayerTeam.{Black, White}

case class GameState(board: Board, playerWhite: PlayerWhite, playerBlack: PlayerBlack, currentTurn: Double, movesHistory: List[PlayerMove]) {

  def getPlayer(team: PlayerTeam): Player = if (team == White) playerWhite else playerBlack

  def nextTurn: GameState = copy(currentTurn = currentTurn + 0.5)

  override def toString: String = {
    val nameSize = 14
    val normalDashLine = " " + "-" * ((nameSize + 1) * 8 + 1)

    def moraleDashLine(morale: Int) = {
      val textInLine = s" morale: ${morale.toString} "
      val firstHalf = normalDashLine.length / 2 - textInLine.length / 2 - 1
      val secondHalf = normalDashLine.length - firstHalf - textInLine.length - 1
      s" ${"-" * firstHalf}$textInLine${"-" * secondHalf}\n"
    }

    board.getRows.zipWithIndex.map { case (line, rowN) => line.map { pieceOpt =>
      val formatStr = s"%${nameSize}s"
      formatStr.format(
        pieceOpt.map(
          piece => piece.data.name).getOrElse(""))
    }.mkString(s"$rowN|", "|", "|")
    }
      .mkString(moraleDashLine(playerBlack.morale), "\n" + normalDashLine + "\n", "\n" + moraleDashLine(playerWhite.morale))
  }

  /**
    * Removes piece and adds another piece and updates morale/player pieces
    * updatePiece(piece1, piece2) === removePiece(piece1).placePiece(piece2)
    */
  def updatePiece(piece: Piece, pieceNewPos: Piece): GameState = {
    removePiece(piece).placePiece(pieceNewPos)
  }

  /**
    * Places piece and updates morale/player pieces
    */
  def placePiece(piece: Piece): GameState = {
    val withNewPiece = copy(board = board.place(piece))

    if (piece.team == White) {
      withNewPiece.copy(playerWhite =
        withNewPiece.playerWhite
          .changeMorale(piece.currentMorale)
          .placePiece(piece)
      )
    } else {
      withNewPiece.copy(playerBlack =
        withNewPiece.playerBlack
          .changeMorale(piece.currentMorale)
          .placePiece(piece)
      )
    }
  }

  /**
    * Removes piece and updates morale/player pieces
    */
  def removePiece(piece: Piece): GameState = {
    val withoutPiece = copy(board = board.remove(piece.pos))

    if (piece.team == White) {
      withoutPiece.copy(playerWhite =
        withoutPiece.playerWhite
          .changeMorale(-piece.currentMorale)
          .removePiece(piece)
      )
    } else {
      withoutPiece.copy(playerBlack =
        withoutPiece.playerBlack
          .changeMorale(-piece.currentMorale)
          .removePiece(piece)
      )
    }
  }

  def changeMorale(playerTeam: PlayerTeam, moraleDiff: Int): GameState = {
    if (playerTeam == White)
      copy(playerWhite = playerWhite.changeMorale(moraleDiff))
    else
      copy(playerBlack = playerBlack.changeMorale(moraleDiff))
  }

  def getCurrentPlayerMoves: List[PlayerMove] = {
    val currentPlayer: Player = getCurrentPlayer

    currentPlayer.pieces.flatMap { piece =>
      val moves: Seq[Moves] = piece.data.moves

      moves.flatMap(_.getValidMove(piece, this, currentPlayer))
    }
  }

  def winner: Option[PlayerTeam] = {
    if (playerWhite.morale == 0) {
      Some(Black)
    } else if (playerBlack.morale == 0) {
      Some(White)
    } else {
      None
    }
  }

  def playPlayerMove(move: PlayerMove): GameState = {
    import PlayerMove._

    val newState = move match {
      case Move(piece, target) =>
        val pieceNewPos = piece.moveTo(target, this).copy(hasMoved = true)
        updatePiece(piece, pieceNewPos)
      case Attack(piece, pieceToKill) =>
        val (pieceNewPos, updatedState) = piece.killed(pieceToKill, this)
        val pieceUpdated = pieceNewPos.copy(hasMoved = true)

        updatedState
          .removePiece(piece)
          .removePiece(pieceToKill)
          .placePiece(pieceUpdated)
      case RangedDestroy(_, pieceToKill) =>
        this.removePiece(pieceToKill)
    }

    newState.copy(
      currentTurn = newState.currentTurn + 0.5,
      movesHistory = move :: newState.movesHistory
    )
  }

  def getCurrentPlayer: Player = if (currentTurn == currentTurn.toInt) playerWhite else playerBlack

}

object GameState {

  def compare(before: GameState, after: GameState, team: PlayerTeam): Int = {
    after.winner match {
      case Some(playerTeam) if playerTeam == team => Int.MaxValue
      case Some(playerTeam) if playerTeam == team.enemy => Int.MinValue
      case None =>
        val whiteDiff = after.playerWhite.morale - before.playerWhite.morale
        val blackDiff = after.playerBlack.morale - before.playerBlack.morale

        team.chooseWhiteBlack(whiteDiff - blackDiff, blackDiff - whiteDiff)
    }
  }

}