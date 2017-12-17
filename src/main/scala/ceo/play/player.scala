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
  val hasKing: Boolean

  def inBaseRow(target: BoardPos): Boolean
}

object Player {

  case class PlayerWhite(morale: Int, pieces: List[Piece] = List.empty, hasKing: Boolean = false) extends Player {
    val team: PlayerTeam = PlayerTeam.White

    def changeMorale(diff: Int): PlayerWhite = copy(morale = morale + diff)

    def removePiece(piece: Piece): PlayerWhite = {
      if (piece.data.isKing)
        copy(pieces = pieces.filterNot(_ == piece), hasKing = false)
      else
        copy(pieces = pieces.filterNot(_ == piece))
    }

    def placePiece(piece: Piece): PlayerWhite = {
      if (piece.data.isKing)
        copy(pieces = piece :: pieces, hasKing = true)
      else
        copy(pieces = piece :: pieces)
    }

    override def inBaseRow(pos: BoardPos): Boolean = pos.row == 7
  }

  case class PlayerBlack(morale: Int, pieces: List[Piece] = List.empty, hasKing: Boolean = false) extends Player {
    val team: PlayerTeam = PlayerTeam.Black

    def changeMorale(diff: Int): PlayerBlack = copy(morale = morale + diff)

    def removePiece(piece: Piece): PlayerBlack = {
      if (piece.data.isKing)
        copy(pieces = pieces.filterNot(_ == piece), hasKing = false)
      else
        copy(pieces = pieces.filterNot(_ == piece))
    }

    def placePiece(piece: Piece): PlayerBlack = {
      if (piece.data.isKing)
        copy(pieces = piece :: pieces, hasKing = true)
      else
        copy(pieces = piece :: pieces)
    }

    override def inBaseRow(pos: BoardPos): Boolean = pos.row == 0
  }

}

sealed trait PlayerMove {

  def betterHumanString: String

}

object PlayerMove {

  case class Move(piece: Piece, to: BoardPos) extends PlayerMove {
    def betterHumanString: String = s"Move $piece to $to ${to - piece.pos}"
  }

  case class Attack(piece: Piece, pieceToKill: Piece) extends PlayerMove {
    def betterHumanString: String = s"$piece Attacks $pieceToKill ${pieceToKill.pos - piece.pos}"
  }

  case class RangedDestroy(piece: Piece, pieceToDestroy: Piece) extends PlayerMove {
    def betterHumanString: String = s"$piece RangedDestroy $pieceToDestroy ${pieceToDestroy.pos - piece.pos}"
  }

  case class MagicDestroy(piece: Piece, pieceToDestroy: Piece) extends PlayerMove {
    def betterHumanString: String = s"$piece MagicDestroy $pieceToDestroy ${pieceToDestroy.pos - piece.pos}"
  }

  case class RangedPetrify(piece: Piece, pieceToPetrify: Piece) extends PlayerMove {
    def betterHumanString: String = s"$piece RangedPetrify $pieceToPetrify ${pieceToPetrify.pos - piece.pos}"
  }

  case class TransformEnemyIntoAllyUnit(
    piece: Piece,
    pieceToTransform: Piece,
    moraleCost: Int,
    allyPieceData: PieceData
  ) extends PlayerMove {
    def betterHumanString: String =
      s"$piece TranformEnemyInto[cost $moraleCost] an ally ${allyPieceData.name} at $pieceToTransform ${pieceToTransform.pos - piece.pos}"
  }

  case class TaurusRush(piece: Piece, pieceToRush: Piece, maxDistance: Int) extends PlayerMove {
    def betterHumanString: String = s"$piece RushEnemy $pieceToRush ${pieceToRush.pos - piece.pos}"
  }

}
