package ceo.play

import ceo.play.PlayerTeam.{Black, White}

sealed trait PlayerTeam {

  val letter: Char

  def enemy: PlayerTeam = if (this == White) Black else White

  def chooseWhiteBlack[A](whiteBranch: A, blackBranch: A): A = if (this == White) whiteBranch else blackBranch
}

object PlayerTeam {

  case object White extends PlayerTeam {
    val letter = 'W'
  }

  case object Black extends PlayerTeam {
    val letter = 'B'
  }

  def apply(str: String): PlayerTeam = if (str == "White") White else Black

}

trait Player {
  val team: PlayerTeam
  val morale: Int
  val pieces: List[Piece]

  def inBaseRow(target: BoardPos): Boolean
}

object Player {

  case class PlayerWhite(morale: Int, pieces: List[Piece] = List.empty) extends Player {
    val team: PlayerTeam = PlayerTeam.White

    def changeMorale(diff: Int): PlayerWhite = copy(morale = Math.max(0, morale + diff))

    def removePiece(piece: Piece): PlayerWhite = copy(pieces = pieces.filterNot(_ == piece))

    def placePiece(piece: Piece): PlayerWhite = copy(pieces = piece :: pieces)

    override def inBaseRow(pos: BoardPos): Boolean = pos.row == 7
  }

  case class PlayerBlack(morale: Int, pieces: List[Piece] = List.empty) extends Player {
    val team: PlayerTeam = PlayerTeam.Black

    def changeMorale(diff: Int): PlayerBlack = copy(morale = Math.max(0, morale + diff))

    def removePiece(piece: Piece): PlayerBlack = copy(pieces = pieces.filterNot(_ == piece))

    def placePiece(piece: Piece): PlayerBlack = copy(pieces = piece :: pieces)

    override def inBaseRow(pos: BoardPos): Boolean = pos.row == 0
  }

}

sealed trait PlayerMove

object PlayerMove {

  case class Move(piece: Piece, to: BoardPos) extends PlayerMove

  case class Attack(piece: Piece, pieceToKill: Piece) extends PlayerMove

  case class RangedDestroy(piece: Piece, pieceToDestroy: Piece) extends PlayerMove

}