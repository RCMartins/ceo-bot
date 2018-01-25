package ceo.play

sealed trait EffectType {
  def name: String
}

object EffectType {

  case object Petrify extends EffectType {
    val name = "Petrify"
  }

  case object Poison extends EffectType {
    val name = "Poison"
  }

  case object Freeze extends EffectType {
    val name = "Freeze"
  }

  case object Enchanted extends EffectType {
    val name = "Enchanted"
  }

  case object WeakEnchanted extends EffectType {
    val name = "WeakEnchanted"
  }

  case object Displacement extends EffectType {
    val name = "Displacement"
  }

  case object Magic extends EffectType {
    val name = "Magic"
  }

  case object Ranged extends EffectType {
    val name = "Ranged"
  }

  case object Trigger extends EffectType {
    val name = "Trigger"
  }

  case object Compel extends EffectType {
    val name = "Compel"
  }

  case object BlockAttacks extends EffectType {
    val name = "BlockAttacks"
  }

  case object InstantKillPositional extends EffectType {
    val name = "InstantKillPositional"
  }

  case object PieceGrow extends EffectType {
    val name = "PieceGrow"
  }

  val allNormalEffects: List[EffectType] = List(Petrify, Poison, Freeze, Displacement, Magic, Ranged, Trigger, Compel)

  def apply(name: String): EffectType = allNormalEffects.find(_.name == name) match {
    case Some(effectStatusType) => effectStatusType
    case None => throw new Exception(s"Unknown status effect: $name")
  }
}

sealed trait EffectStatus {
  def effectType: EffectType
}

object EffectStatus {

  case class Petrified(untilTurn: Double) extends EffectStatus {
    override val effectType: EffectType = EffectType.Petrify
  }

  case class Poison(turnOfDeath: Double) extends EffectStatus {
    override val effectType: EffectType = EffectType.Poison
  }

  case class Frozen(untilTurn: Double) extends EffectStatus {
    override val effectType: EffectType = EffectType.Freeze
  }

  case class Enchanted(untilTurn: Double) extends EffectStatus {
    override val effectType: EffectType = EffectType.Enchanted
  }

  case class WeakEnchanted(untilTurn: Double) extends EffectStatus {
    override val effectType: EffectType = EffectType.WeakEnchanted
  }

  case class BlocksAttacksFrom(distances: Set[Distance]) extends EffectStatus {
    override val effectType: EffectType = EffectType.BlockAttacks
  }

  case class InstantKillPositional(distance: Distance) extends EffectStatus {
    override val effectType: EffectType = EffectType.InstantKillPositional
  }

  case class PieceGrow(moraleToPromote: Int, pieceName: String) extends EffectStatus {
    override val effectType: EffectType = EffectType.PieceGrow
  }

}
