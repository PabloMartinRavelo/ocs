package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosSBlueprintMos
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth._
import edu.gemini.spModel.gemini.gmos.InstGmosCommon

case class GmosSMosNs(blueprint:SpGmosSBlueprintMos) extends GmosSBase[SpGmosSBlueprintMos] {

  // IF SPECTROSCOPY MODE == MOS N&S
  //         INCLUDE IN
  //             Target group: {29}, {22}, {31}, {32}
  //               IF PRE-IMAGING REQ == YES
  //                 INCLUDE {17}, {27}
  //               IF PRE-IMAGING REQ == NO
  //                 INCLUDE {28}
  //             Baseline folder: {30}, {33}-{35}
  //        For spec observations: {29}, {31}, {32}, {34}, {35}
  //             SET DISPERSER FROM PI
  //             SET FILTER FROM PI
  //             For {29}, {31}
  //                 SET MOS "Slit Width" from PI
  //             For {34}, {35}
  //                 SET FPU (built-in longslit) using the width specified in PI
  //         For standard acquisition: {33}
  //            if FPU!=None in the OT inst. iterators, then SET FPU (built-in longslit) using the width specified in PI
  //         For acquisitions {27}, {28}; mask image {22}; and N&S dark {30}
  //             No actions needed

  val targetGroup = Seq(29, 22, 31, 32) ++ (if (blueprint.preImaging) Seq(17, 27) else Seq(28))
  val baselineFolder = Seq(30, 33, 34, 35)
  val all = targetGroup ++ baselineFolder
  val spec = Seq(29, 31, 32, 34, 35).filter(all.contains)

  def noneOrPiFpu(libFpu: Any) = if (libFpu == FPU_NONE) FPU_NONE else blueprint.fpu

  def forSpecObservation(o:ISPObservation):Either[String, Unit] = for {
    _ <- o.setDisperser(blueprint.disperser).right
    _ <- o.setFilter(blueprint.filter).right
  } yield ()

  def forStandardAcq(o:ISPObservation):Either[String, Unit] = for {
    _ <- o.setFpu(blueprint.fpu).right
    _ <- o.ed.modifySeqAllKey(InstGmosCommon.FPU_PROP_NAME) { case libFpu => noneOrPiFpu(libFpu) }.right
  } yield ()

  val notes = Seq.empty

  def initialize(grp:ISPGroup, db:TemplateDb):Either[String, Unit] =
    for {
      _ <- forObservations(grp, spec, forSpecObservation).right
      _ <- forObservations(grp, Seq(29, 31), _.setCustomSlitWidth(blueprint.fpu)).right
      _ <- forObservations(grp, Seq(34, 35), _.setFpu(blueprint.fpu)).right
      _ <- forObservations(grp, Seq(33), forStandardAcq).right
    } yield ()

}
