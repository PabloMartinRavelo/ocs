package edu.gemini.model.p1.dtree.exchange

import edu.gemini.model.p1.dtree.{TextNode, SingleSelectNode}
import edu.gemini.model.p1.immutable._

object Subaru {
  def apply() = new InstrumentNode

  class InstrumentNode extends SingleSelectNode[Unit, SubaruInstrument, SubaruBlueprint](()) {
    val title       = "Subaru Instrument"
    val description = "Select the Subaru instrument."
    def choices     = SubaruInstrument.values.toList

    def apply(i: SubaruInstrument) = i match {
      case ins if ins == SubaruInstrument.forName("VISITOR") => Left(CustomNameNode(SubaruBlueprint(ins, None)))
      case ins                                               => Right(SubaruBlueprint(ins, None))
    }

    def unapply = {
      case b: SubaruInstrument => b
    }
  }

  case class CustomNameNode(fs: SubaruBlueprint) extends TextNode[SubaruBlueprint, SubaruBlueprint](fs) {
    val title       = "Instrument name"
    val description = "Enter the name of the instrument."

    def apply(n: String) = Right(fs.copy(customName = Some(n)))

    def unapply = {
      case b: SubaruBlueprint => b.customName.getOrElse("")
    }

  }

}