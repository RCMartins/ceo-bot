package ceo.play

import ceo.play.Moves.generalCanTargetEnemy

sealed trait Moves

sealed trait SingleMove extends Moves {
  def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove]
}

sealed trait MultipleMoves extends Moves {
  def getValidMoves(piece: Piece, state: GameState, currentPlayer: Player): List[PlayerMove]
}

object Moves {

  @inline private final def canRangedReachEnemy(piece: Piece, target: BoardPos, state: GameState): Option[Piece] = {
    if (piece.pos.allPosUntilAreEmptyOrGhost(target, state.board))
      target.getPiece(state.board).filter(_.team != piece.team)
    else
      None
  }

  @inline private final def canRangedReachEmptyTarget(piece: Piece, target: BoardPos, state: GameState): Boolean = {
    piece.pos.allPosUntilAreEmptyOrGhost(target, state.board) && target.isValid && target.getPiece(state.board).isEmpty
  }

  @inline def generalCanTargetEnemy(piece: Piece, targetPiece: Piece): Boolean =
    (!targetPiece.data.isKing || !piece.isWeakEnchanted) &&
      (!targetPiece.data.cannotBeTargetedByMinions || !piece.data.isMinion)

  private def canMove(piece: Piece, target: BoardPos, state: GameState): Option[PlayerMove.Move] = {
    if (piece.pos.allPosUntilAreEmptyOrGhost(target, state.board) && target.isEmpty(state.board))
      Some(PlayerMove.Move(piece, target))
    else
      None
  }

  private def canMoveFromStart(piece: Piece, target: BoardPos, state: GameState): Option[PlayerMove] = {
    if (piece.pos == piece.startingPosition)
      canMove(piece, target, state)
    else
      None
  }

  private def canMoveUnblockable(piece: Piece, target: BoardPos, state: GameState): Option[PlayerMove] = {
    if (target.isEmpty(state.board))
      Some(PlayerMove.Move(piece, target))
    else
      None
  }

  private def canMoveUnblockableFromStart(piece: Piece, target: BoardPos, state: GameState): Option[PlayerMove] = {
    if (piece.pos == piece.startingPosition)
      canMoveUnblockable(piece, target, state)
    else
      None
  }

  private def canAttack(piece: Piece, target: BoardPos, state: GameState): Option[PlayerMove] = {
    canRangedReachEnemy(piece, target, state) match {
      case Some(targetPiece) if !targetPiece.isEnchanted && generalCanTargetEnemy(piece, targetPiece) =>
        if (targetPiece.canBlockFrom(piece.pos)) {
          Some(PlayerMove.AttackCanBeBlocked(piece, targetPiece))
        } else {
          Some(PlayerMove.Attack(piece, targetPiece))
        }
      case _ =>
        None
    }
  }

  private def canAttackUnblockable(piece: Piece, target: BoardPos, state: GameState): Option[PlayerMove] = {
    target.getPiece(state.board) match {
      case Some(targetPiece) if {
        targetPiece.team != piece.team &&
          !targetPiece.isEnchanted &&
          generalCanTargetEnemy(piece, targetPiece)
      } =>
        Some(PlayerMove.Attack(piece, targetPiece))
      case _ =>
        None
    }
  }

  private def canAttackUnblockableConditional(
    piece: Piece,
    target: BoardPos,
    state: GameState,
    condition: Piece => Boolean
  ): Option[PlayerMove] = {
    target.getPiece(state.board) match {
      case Some(targetPiece) if {
        targetPiece.team != piece.team &&
          !targetPiece.isEnchanted &&
          generalCanTargetEnemy(piece, targetPiece) &&
          condition(targetPiece)
      } =>
        Some(PlayerMove.Attack(piece, targetPiece))
      case _ =>
        None
    }
  }

  private def canSwapUnblockable(piece: Piece, target: BoardPos, state: GameState): Option[PlayerMove] = {
    target.getPiece(state.board) match {
      case Some(targetPiece) if targetPiece.team == piece.team && !targetPiece.data.isImmuneTo(EffectType.Displacement) =>
        Some(PlayerMove.Swap(piece, targetPiece))
      case _ =>
        None
    }
  }

  private def canRangedDestroy(piece: Piece, target: BoardPos, state: GameState): Option[PlayerMove] = {
    canRangedReachEnemy(piece, target, state) match {
      case Some(targetPiece) if !targetPiece.data.isImmuneTo(EffectType.Ranged) && generalCanTargetEnemy(piece, targetPiece) =>
        Some(PlayerMove.RangedDestroy(piece, targetPiece))
      case _ =>
        None
    }
  }

  private def canRangedPetrify(
    piece: Piece,
    target: BoardPos,
    durationTurns: Int,
    state: GameState
  ): Option[PlayerMove] = {
    canRangedReachEnemy(piece, target, state) match {
      case Some(targetPiece) if {
        !targetPiece.data.isImmuneTo(EffectType.Petrify) &&
          !targetPiece.data.isImmuneTo(EffectType.Ranged) &&
          generalCanTargetEnemy(piece, targetPiece)
      } =>
        Some(PlayerMove.RangedPetrify(piece, targetPiece, durationTurns))
      case _ =>
        None
    }
  }

  private def canMagicDestroy(piece: Piece, target: BoardPos, state: GameState): Option[PlayerMove] = {
    target.getPiece(state.board) match {
      case Some(targetPiece) if {
        targetPiece.team != piece.team &&
          !targetPiece.data.isImmuneTo(EffectType.Magic) &&
          generalCanTargetEnemy(piece, targetPiece)
      } =>
        Some(PlayerMove.MagicDestroy(piece, targetPiece))
      case _ =>
        None
    }
  }

  private def canMagicPoison(
    piece: Piece,
    target: BoardPos,
    durationTurns: Int,
    state: GameState
  ): Option[PlayerMove] = {
    target.getPiece(state.board) match {
      case Some(targetPiece) if {
        targetPiece.team != piece.team &&
          !targetPiece.data.isImmuneTo(EffectType.Poison) &&
          !targetPiece.isPoisoned &&
          generalCanTargetEnemy(piece, targetPiece)
      } =>
        Some(PlayerMove.MagicPoison(piece, targetPiece, durationTurns))
      case _ =>
        None
    }
  }

  private def canMagicPetrify(
    piece: Piece,
    target: BoardPos,
    moraleCost: Int,
    durationTurns: Int,
    state: GameState
  ): Option[PlayerMove] = {
    target.getPiece(state.board) match {
      case Some(targetPiece) if {
        targetPiece.team != piece.team &&
          !targetPiece.data.isImmuneTo(EffectType.Petrify) &&
          !targetPiece.data.isImmuneTo(EffectType.Magic) &&
          generalCanTargetEnemy(piece, targetPiece)
      } =>
        Some(PlayerMove.MagicPetrify(piece, targetPiece, moraleCost, durationTurns))
      case _ =>
        None
    }
  }

  private def canCharmMagicConditional(
    piece: Piece,
    target: BoardPos,
    state: GameState,
    condition: Piece => Boolean
  ): Option[PlayerMove] = {
    target.getPiece(state.board) match {
      case Some(targetPiece) if {
        targetPiece.team != piece.team &&
          !targetPiece.data.isImmuneTo(EffectType.Magic) &&
          generalCanTargetEnemy(piece, targetPiece) &&
          condition(targetPiece)
      } =>
        Some(PlayerMove.MagicCharm(piece, targetPiece))
      case _ =>
        None
    }
  }

  private def canTaurusRush(piece: Piece,
    target: BoardPos,
    maxDistance: Int,
    state: GameState
  ): Option[PlayerMove] = {
    canRangedReachEnemy(piece, target, state) match {
      case Some(targetPiece) if generalCanTargetEnemy(piece, targetPiece) =>
        Some(PlayerMove.TaurusRush(piece, targetPiece, maxDistance))
      case _ =>
        None
    }
  }

  private def canTransformEnemyIntoAllyPiece(
    piece: Piece,
    target: BoardPos,
    moraleCost: Int,
    AllyPieceData: PieceData,
    state: GameState
  ): Option[PlayerMove] = {
    target.getPiece(state.board) match {
      case Some(targetPiece) if targetPiece.team != piece.team && generalCanTargetEnemy(piece, targetPiece) =>
        Some(PlayerMove.TransformEnemyIntoAllyPiece(piece, targetPiece, moraleCost, AllyPieceData))
      case _ =>
        None
    }
  }

  private def canRangedPush(
    piece: Piece,
    target: BoardPos,
    moraleCost: Int,
    maxPushDistance: Int,
    pieceNameToSpawn: Option[String],
    state: GameState
  ): Option[PlayerMove] = {
    canRangedReachEnemy(piece, target, state) match {
      case Some(targetPiece) if {
        targetPiece.team != piece.team &&
          !targetPiece.data.isImmuneTo(EffectType.Displacement) &&
          generalCanTargetEnemy(piece, targetPiece)
      } =>
        val dir = (targetPiece.pos - piece.pos).toUnitVector
        if ((targetPiece.pos + dir).isEmpty(state.board)) {
          pieceNameToSpawn match {
            case None =>
              Some(PlayerMove.RangedPush(piece, targetPiece, moraleCost, maxPushDistance))
            case Some(pieceName) =>
              val pieceData = DataLoader.getPieceData(pieceName, piece.team)
              Some(PlayerMove.RangedPushSpawn(piece, targetPiece, moraleCost, maxPushDistance, pieceData))
          }
        } else
          None
      case _ =>
        None
    }
  }

  private def canMagicPushFreeze(
    piece: Piece,
    target: BoardPos,
    maxPushDistance: Int,
    freezeDuration: Int,
    state: GameState
  ): Option[PlayerMove] = {
    target.getPiece(state.board) match {
      case Some(targetPiece) if {
        targetPiece.team != piece.team &&
          !targetPiece.data.isImmuneTo(EffectType.Magic) &&
          generalCanTargetEnemy(piece, targetPiece)
      } =>
        (!targetPiece.data.isImmuneTo(EffectType.Freeze), {
          val dirDist = targetPiece.pos - piece.pos
          val absRow = Math.abs(dirDist.rowDiff)
          val absColumn = Math.abs(dirDist.columnDiff)
          val dir =
            if (absRow == absColumn)
              dirDist.toUnitVector
            else if (absRow > absColumn)
              dirDist.setColumn(0).toUnitVector
            else
              dirDist.setRow(0).toUnitVector

          (targetPiece.pos + dir).isEmpty(state.board) && !targetPiece.data.isImmuneTo(EffectType.Displacement)
        }) match {
          case (true, true) =>
            Some(PlayerMove.MagicPushFreeze(piece, targetPiece, maxPushDistance, freezeDuration))
          case (false, true) =>
            Some(PlayerMove.MagicPush(piece, targetPiece, 0, maxPushDistance))
          case (true, false) =>
            Some(PlayerMove.MagicFreeze(piece, targetPiece, freezeDuration))
          case _ =>
            None
        }
      case _ =>
        None
    }
  }

  private def canMagicFreeze(
    piece: Piece,
    target: BoardPos,
    freezeDuration: Int,
    state: GameState
  ): Option[PlayerMove] = {
    target.getPiece(state.board) match {
      case Some(targetPiece) if {
        targetPiece.team != piece.team &&
          !targetPiece.data.isImmuneTo(EffectType.Freeze) &&
          !targetPiece.data.isImmuneTo(EffectType.Magic) &&
          generalCanTargetEnemy(piece, targetPiece)
      } =>
        Some(PlayerMove.MagicFreeze(piece, targetPiece, freezeDuration))
      case _ =>
        None
    }
  }

  private def canMagicPush(
    piece: Piece,
    target: BoardPos,
    moraleCost: Int,
    maxPushDistance: Int,
    state: GameState
  ): Option[PlayerMove] = {
    target.getPiece(state.board) match {
      case Some(targetPiece) if {
        targetPiece.team != piece.team &&
          !targetPiece.data.isImmuneTo(EffectType.Displacement) &&
          !targetPiece.data.isImmuneTo(EffectType.Magic) &&
          generalCanTargetEnemy(piece, targetPiece)
      } =>
        val dir = (targetPiece.pos - piece.pos).toUnitVector
        if ((targetPiece.pos + dir).isEmpty(state.board))
          Some(PlayerMove.MagicPush(piece, targetPiece, moraleCost, maxPushDistance))
        else
          None
      case _ =>
        None
    }
  }

  private def isAllyAt(target: BoardPos, state: GameState, currentPlayer: Player): Option[Piece] = {
    target.getPiece(state.board).filter(_.team == currentPlayer.team)
  }

  @inline private final def Or(first: Option[PlayerMove], second: => Option[PlayerMove]): Option[PlayerMove] = {
    first match {
      case None => second
      case _ => first
    }
  }

  case class MoveOrAttack(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      Or(
        canMove(piece, piece.pos + dist, state),
        canAttack(piece, piece.pos + dist, state)
      )
    }
  }

  case class MoveOrAttackUnblockable(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      Or(
        canMoveUnblockable(piece, piece.pos + dist, state),
        canAttackUnblockable(piece, piece.pos + dist, state)
      )
    }
  }

  case class MoveOrAttackOrSwapAlly(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      Or(
        canMoveUnblockable(piece, piece.pos + dist, state),
        Or(canAttackUnblockable(piece, piece.pos + dist, state),
          canSwapUnblockable(piece, piece.pos + dist, state)
        ))
    }
  }

  case class MoveOrSwapAlly(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      Or(
        canMoveUnblockable(piece, piece.pos + dist, state),
        canSwapUnblockable(piece, piece.pos + dist, state)
      )
    }
  }

  case class Move(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canMove(piece, piece.pos + dist, state)
    }
  }

  case class MoveFromStart(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canMoveFromStart(piece, piece.pos + dist, state)
    }
  }

  case class MoveUnblockable(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canMoveUnblockable(piece, piece.pos + dist, state)
    }
  }

  case class MoveUnblockableFromStart(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canMoveUnblockableFromStart(piece, piece.pos + dist, state)
    }
  }

  case class Attack(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canAttack(piece, piece.pos + dist, state)
    }
  }

  case class RangedDestroy(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canRangedDestroy(piece, piece.pos + dist, state)
    }
  }

  case class MagicDestroy(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canMagicDestroy(piece, piece.pos + dist, state)
    }
  }

  case class RangedPetrify(dist: Distance, durationTurns: Int) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canRangedPetrify(piece, piece.pos + dist, durationTurns, state)
    }
  }

  case class MagicPoison(dist: Distance, durationTurns: Int) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canMagicPoison(piece, piece.pos + dist, durationTurns, state)
    }
  }

  case class TaurusRush(dist: Distance, maxDistance: Int) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canTaurusRush(piece, piece.pos + dist, maxDistance, state)
    }
  }

  case class TransformEnemyIntoAllyPiece(dist: Distance, moraleCost: Int, allyPieceName: String) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      val allyUnitPieceData = DataLoader.getPieceData(allyPieceName, piece.team)
      canTransformEnemyIntoAllyPiece(piece, piece.pos + dist, moraleCost, allyUnitPieceData, state)
    }
  }

  case class KingCastling(posAllyPiece: Distance, posAfterKing: Distance, posAfterAllyPiece: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      // TODO check for ghost pieces in 'LONG' castling
      (canMove(piece, piece.pos + posAfterKing, state), isAllyAt(piece.pos + posAllyPiece, state, currentPlayer)) match {
        case (Some(kingMove), Some(allyPiece)) if allyPiece.data.isChampion && !allyPiece.data.isImmuneTo(EffectType.Displacement) =>
          Some(PlayerMove.KingCastling(piece, allyPiece, kingMove.to, piece.pos + posAfterAllyPiece))
        case _ =>
          None
      }
    }
  }

  case class UnstoppableTeleportTransformInto(dist: Distance, pieceName: String) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      val pieceData = DataLoader.getPieceData(pieceName, piece.team)
      val target = piece.pos + dist
      if (target.isEmpty(state.board))
        Some(PlayerMove.TeleportTransformInto(piece, target, pieceData))
      else
        None
    }
  }

  case class MagicStonePillar(dist: Distance, moraleCost: Int, durationTurns: Int) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      val target = piece.pos + dist
      if (target.isEmpty(state.board))
        Some(PlayerMove.MagicSummonPiece(piece, target, moraleCost, DataLoader.getPieceData("StonePillar", piece.team)))
      else
        canMagicPetrify(piece, target, moraleCost, durationTurns, state)
    }
  }

  case class MagicTeleportBeacon(dist: Distance, augmentedRange: Int) extends MultipleMoves {
    def getValidMoves(piece: Piece, state: GameState, currentPlayer: Player): List[PlayerMove] = {
      val target = piece.pos + dist
      if (target.isEmpty(state.board))
        currentPlayer.allPieces.filter(piece =>
          piece.pos.distanceTo(target) <= augmentedRange &&
            !piece.data.isKing && !piece.data.isImmuneTo(EffectType.Displacement)
        ).map(pieceToTeleport =>
          PlayerMove.TeleportPiece(piece, pieceToTeleport, target, piece.pos, pieceToTeleport.pos))
      else
        List.empty
    }
  }

  case class JumpMinion(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canAttackUnblockableConditional(piece, piece.pos + dist, state, _.data.isMinion)
    }
  }

  case class CharmMinion(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canCharmMagicConditional(piece, piece.pos + dist, state, _.data.isMinion)
    }
  }

  case class RangedPush(dist: Distance, moraleCost: Int, pushDistance: Int) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canRangedPush(piece, piece.pos + dist, moraleCost, pushDistance, None, state)
    }
  }

  case object TeleportToFallenAllyPosition extends MultipleMoves {
    def getValidMoves(piece: Piece, state: GameState, currentPlayer: Player): List[PlayerMove] = {
      val positions = currentPlayer.extraData.fallenPiecesPositions
      positions.flatMap(boardPos => canMoveUnblockable(piece, boardPos, state)).toList
    }
  }

  case class MagicPushFreezePiece(dist: Distance, maxPushDistance: Int, freezeDuration: Int) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canMagicPushFreeze(piece, piece.pos + dist, maxPushDistance, freezeDuration, state)
    }
  }

  /**
    * @param fromLocationMode true -> player drags teleporter piece to targetPiece,
    *                         false -> player drags teleporter piece to target empty location
    */
  case class TeleportPiece(distFrom: Distance, distTo: Distance, fromLocationMode: Boolean) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      val target = piece.pos + distTo
      (piece.pos + distFrom).getPiece(state.board) match {
        case Some(targetPiece) if {
          targetPiece.team == piece.team &&
            !targetPiece.data.isImmuneTo(EffectType.Displacement) &&
            target.isEmpty(state.board)
        } =>
          if (fromLocationMode)
            Some(PlayerMove.TeleportPiece(piece, targetPiece, target, piece.pos, targetPiece.pos))
          else
            Some(PlayerMove.TeleportPiece(piece, targetPiece, target, piece.pos, target))
        case _ =>
          None
      }
    }
  }

  case class MagicFreezePiece(dist: Distance, freezeDuration: Int) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canMagicFreeze(piece, piece.pos + dist, freezeDuration, state)
    }
  }

  case class MagicLightning(dist: Distance, moraleCost: Int, turnsToLightUpLocation: Int) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      val target = piece.pos + dist
      if (state.boardEffects.collectFirst { case BoardEffect.Lightning(pos, _) if pos == target => () }.isEmpty) {
        Some(PlayerMove.MagicLightning(piece, target, moraleCost, turnsToLightUpLocation))
      } else {
        None
      }
    }
  }

  case class TeleportKingToLocation(distances: List[Distance]) extends MultipleMoves {
    def getValidMoves(piece: Piece, state: GameState, currentPlayer: Player): List[PlayerMove] = {
      currentPlayer.pieces.find(_.data.isKing).orElse(currentPlayer.piecesAffected.find(_.data.isKing)) match {
        case Some(kingPiece) =>
          distances.flatMap { dist =>
            val target = piece.pos + dist
            if (target.isEmpty(state.board))
              Some(PlayerMove.TeleportPiece(piece, kingPiece, target, piece.pos, target))
            else
              None
          }
        case None => List.empty
      }
    }
  }

  case object TeleportToRoyalPieces extends MultipleMoves {
    def getValidMoves(piece: Piece, state: GameState, currentPlayer: Player): List[PlayerMove] = {
      val royalPieces = currentPlayer.pieces.filter(_.data.isRoyalty) ++ currentPlayer.piecesAffected.filter(_.data.isRoyalty)
      royalPieces
        .flatMap(piece => Distance.adjacentDistances.map(_ + piece.pos))
        .distinct
        .flatMap(boardPos => canMoveUnblockable(piece, boardPos, state))
    }
  }

  case class RangedSummonGeminiTwin(dist: Distance, moraleCost: Int) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      val target = piece.pos + dist
      if (canRangedReachEmptyTarget(piece, target, state)) {
        Some(PlayerMove.RangedSummonGeminiTwin(piece, target, moraleCost, DataLoader.getPieceData("GeminiTwin", piece.team)))
      } else {
        None
      }
    }
  }

  case class PatienceCannotAttackBeforeTurn(moveOrAttack: List[Distance], attack: List[Distance], untilTurn: Int) extends MultipleMoves {
    def getValidMoves(piece: Piece, state: GameState, currentPlayer: Player): List[PlayerMove] = {
      if (state.currentTurn < untilTurn)
        moveOrAttack.flatMap(dist => canMove(piece, piece.pos + dist, state))
      else {
        moveOrAttack.flatMap(dist => Or(
          canMove(piece, piece.pos + dist, state),
          canAttack(piece, piece.pos + dist, state)
        )) ++
          attack.flatMap(dist => canAttack(piece, piece.pos + dist, state))
      }
    }
  }

  case class MagicWeakEnchant(dist: Distance, durationTurns: Int) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      val target = piece.pos + dist
      target.getPiece(state.board) match {
        case Some(targetPiece) if targetPiece.team == piece.team =>
          Some(PlayerMove.MagicWeakEnchant(piece, targetPiece, durationTurns))
        case _ =>
          None
      }
    }
  }

  case class MagicEnvyClone(dist: Distance) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      val target = piece.pos + dist
      target.getPiece(state.board) match {
        case Some(targetPiece) if targetPiece.team != piece.team && generalCanTargetEnemy(piece, targetPiece) =>
          Some(PlayerMove.MagicEnvyClone(piece, targetPiece))
        case _ =>
          None
      }
    }
  }

  case class MagicMeteor(dist: Distance, moraleCost: Int, turnsToMeteor: Int, pushDistance: Int) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      val target = piece.pos + dist
      if (state.boardEffects.collectFirst { case BoardEffect.Meteor(pos, _, _) if pos == target => () }.isEmpty) {
        Some(PlayerMove.MagicMeteor(piece, target, moraleCost, turnsToMeteor, pushDistance))
      } else {
        None
      }
    }
  }

  case class MagicPush(dist: Distance, moraleCost: Int, pushDistance: Int) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canMagicPush(piece, piece.pos + dist, moraleCost, pushDistance, state)
    }
  }

  case class RangedPushSpawn(dist: Distance, moraleCost: Int, pushDistance: Int, pieceName: String) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      canRangedPush(piece, piece.pos + dist, moraleCost, pushDistance, Some(pieceName), state)
    }
  }

  case class MagicSummonPiece(dist: Distance, moraleCost: Int, pieceName: String) extends SingleMove {
    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      val target = piece.pos + dist
      if (target.isEmpty(state.board))
        Some(PlayerMove.MagicSummonPiece(piece, target, moraleCost, DataLoader.getPieceData(pieceName, piece.team)))
      else
        None
    }
  }

  case object Empty extends Moves {
    val dx = 0
    val dy = 0

    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = ???
  }

  case object DummyMove extends Moves {
    val dx = 0
    val dy = 0

    def getValidMove(piece: Piece, state: GameState, currentPlayer: Player): Option[PlayerMove] = {
      Some(PlayerMove.DummyMove(piece))
    }
  }

}
