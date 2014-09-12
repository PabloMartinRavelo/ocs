package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosSBlueprintLongslitNs
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.spModel.gemini.gmos.{GmosSouthType, InstGmosSouth}

case class GmosSLongslitNs(blueprint:SpGmosSBlueprintLongslitNs) extends GmosSBase[SpGmosSBlueprintLongslitNs] {

  // IF SPECTROSCOPY MODE == LONGSLIT N&S
  //         INCLUDE FROM 'LONGSLIT N&S BP' IN
  //             Target group: {9} - {10}
  //             Baseline folder: {11} - {16}
  //         For spec observations: {10}, {11}, {13}-{15}
  //             SET DISPERSER FROM PI
  //             SET FILTER FROM PI
  //             SET FPU FROM PI
  //         For acquisitions: {9}, {12}
  //             if FPU!=None in the OT inst. iterators, then SET FPU FROM PI

  val targetGroup = 9 to 10
  val baselineFolder = 11 to 16
  val notes = Seq.empty

  def initialize(grp:ISPGroup, db:TemplateDb):Either[String, Unit] = {

    def forSpecObservations(o:ISPObservation):Either[String, Unit] = for {
      _ <- o.setDisperser(blueprint.disperser).right
      _ <- o.setFilter(blueprint.filter).right
      _ <- o.setFpu(blueprint.fpu).right
    } yield ()

    def forAcquisitions(o:ISPObservation):Either[String, Unit] = for {
      _ <- o.setFpu(blueprint.fpu).right
      _ <- o.ed.modifySeqAllKey(InstGmosSouth.FPUNIT_PROP.getName) {
        case GmosSouthType.FPUnitSouth.FPU_NONE => GmosSouthType.FPUnitSouth.FPU_NONE
        case _ => blueprint.fpu
      }.right
    } yield ()

    for {
      _ <- forObservations(grp, List(10, 11, 13, 14, 15), forSpecObservations).right
      _ <- forObservations(grp, List(9, 12), forAcquisitions).right
    } yield ()

  }
}