package edu.gemini.phase2.template.factory.impl.gnirs

import edu.gemini.spModel.gemini.gnirs.blueprint.SpGnirsBlueprintSpectroscopy
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.gemini.gnirs.GNIRSParams._
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.{Decker, SlitWidth => FPU}
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.altair.AltairParams.Mode._
import edu.gemini.spModel.gemini.altair.AltairParams.Mode
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.telescope.PosAngleConstraint
import edu.gemini.spModel.gemini.altair.blueprint.SpAltairNgs
import edu.gemini.spModel.gemini.altair.AltairParams.Mode

case class GnirsSpectroscopyIfu(blueprint:SpGnirsBlueprintSpectroscopy, exampleTarget: Option[SPTarget])
  extends GnirsBase[SpGnirsBlueprintSpectroscopy] {

  // Local imports

  import blueprint._
  import PixelScale.{PS_005, PS_015}
  import Disperser.D_111
  import CrossDispersed.{SXD, LXD}
  import SlitWidth.{LR_IFU, HR_IFU}
  import WellDepth.DEEP
  import MagnitudeBand.H
  import edu.gemini.spModel.obscomp.InstConstants.EXPOSURE_TIME_PROP

  // Some aliases, just to match the P-Code
  val xd = crossDisperser

  // **** IF INSTRUMENT MODE == SPECTROSCOPY (IFU) ****
  //
  // # The ordering of observations in the scheduling group should be:
  //   Notes
  //   Before standard acq
  //   Before standard spec
  //   SCI acq(s)
  //   SCI spec
  //   After standard acq
  //   After standard spec
  //
  // IF FPU == LR-IFU:
  //     INCLUDE {24}, {25}, {32}, {33}, {35}, {36} in a target-specific Scheduling Group
  // ELIF FPU == HR-IFU:
  //     INCLUDE {24}, {26}, {34}, {35}, {37} in a target-specific Scheduling Group

  if (fpu == LR_IFU)
    include(24, 25, 32, 33, 35, 36) in TargetGroup
  else if (fpu == HR_IFU)
    include(24, 26, 34, 35, 37) in TargetGroup

  // IF TARGET H-MAGNITUDE < 7 INCLUDE {27}              # ultra bright
  // IF 7 <= TARGET H-MAGNITUDE < 11.5 INCLUDE {28}      # very bright
  // IF 11.5 <= TARGET H-MAGNITUDE < 16 INCLUDE {29}     # bright
  // IF 16 <= TARGET H-MAGNITUDE < 20 INCLUDE {30}       # faint
  // IF TARGET H-MAGNITUDE >= 20 INCLUDE {31}            # blind offset
  // ELSE INCLUDE {27-31}                                # no H-magnitude so give them all

  val more =
    exampleTarget.flatMap(t => t.getMagnitude(H)).map(_.value) match {
      case Some(h) =>
        if (h < 7) Seq(27)
        else if (h < 11.5) Seq(28)
        else if (h < 16.0) Seq(29)
        else if (h < 20.0) Seq(30)
        else Seq(31)
      case None => 27 to 31
    }
  include(more:_*) in TargetGroup

  // SET CONDITIONS FROM PI
  // SET PIXEL SCALE FROM PI
  // SET DISPERSER FROM PI
  forObs(targetGroup: _*)(
    setPixelScale(pixelScale),
    setDisperser(disperser)
  )

  // # Set the FPU and Decker in the acquisitions:
  // FOR {24, 27-31, 35} SET FPU FROM PI IN ITERATORS EXCEPT WHERE FPU == acquisition
  forObs((24 +: 35 +: more): _*)(
    mutateSeq(mapStepsByKey(PARAM_FPU) {
      case FPU.ACQUISITION => FPU.ACQUISITION
      case _ => fpu
    })
  )

  // FOR {24, 27-31, 35} SET DECKER FROM PI IN ITERATORS EXCEPT WHERE DECKER == acquisition
  forObs((24 +: 35 +: more): _*)(
    ifTrue(fpu == LR_IFU)(mutateSeq(updateDecker(Decker.LR_IFU))),
    ifTrue(fpu == HR_IFU)(mutateSeq(updateDecker(Decker.HR_IFU)))
  )

  // IF PI Central Wavelength > 2.5um
  //     SET Well Depth == Deep
  forObs(targetGroup: _*)(
    ifTrue(wavelengthGe2_5)(setWellDepth(DEEP))
  )

  // # In LGS mode the science uses the mode from PI but the standards use NGS + FieldLens:
  // IF AO mode != None:
  //     ADD an Altair Adaptive Optics component to all observations
  //     IF PI AO == "Altair Natural Guidestar":  SET the Guide Star Type = Natural Guide Star
  //     IF PI AO == "Altair Natural Guidestar w/ Field Lens": SET the Guide Star Type = Natural Guide Star with Field Lens
  //     IF PI AO includes "Laser Guide Star":
  //          FOR ACQ and SCI:
  //             IF PI AO == "Altair Laser Guidestar": SET Guide Star Type = Laser Guide Star + AOWFS
  //             IF PI AO == "Altair Laser Guidestar w/ PWFS1": SET Guide Star Type = Laser Guide Star + PWFS1
  //          FOR {24-26} {35-37}:   # Standards
  //             SET Guide Star Type = Natural Guide Star with Field Lens
  altair.mode.foreach { m =>
    if (m.isNGS)
      forObs(targetGroup: _*)(addAltair(m))
    if (m.isLGS) {
      forObs(targetGroup: _*)(addAltair(m))
      val standards = targetGroup.filter(Set(24, 25, 26, 35, 36, 37))
      forObs(standards: _*)(addAltair(Mode.NGS_FL))
    }
  }

}
