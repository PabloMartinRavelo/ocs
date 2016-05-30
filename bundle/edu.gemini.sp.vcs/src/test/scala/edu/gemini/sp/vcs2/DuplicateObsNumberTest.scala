package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.sp.vcs2.VcsAction._
import edu.gemini.spModel.core.SPProgramID
import org.junit.Test
import org.junit.Assert

import scala.collection.JavaConverters._

/**
 * A test case that reveals the bug in REL-2797:
 *
 * "edu.gemini.pot.sp.SPTreeStateException: There is an existing observation with number:"
 *
 * when adding an observation.
 */
class DuplicateObsNumberTest {

  val key = new SPNodeKey()
  val pid = SPProgramID.toProgramID("GS-2016B-Q-1")

  @Test def testDuplicateObsNumber(): Unit = {
    val odb  = DBLocalDatabase.createTransient()
    val fact = odb.getFactory

    try {
      val progA = fact.createProgram(key, pid)
      val progB = fact.createProgram(key, pid)

      Assert.assertNotSame("have different lifespan ids", progA.getLifespanId, progB.getLifespanId)

      // Create a new observation for progA but don't add it.
      val obsA1 = fact.createObservation(progA, null)

      // Create a second observation for progA and add it.
      val obsA2 = fact.createObservation(progA, null)
      progA.addObservation(obsA2)

      // Create a new observation for progB and add it.
      val obsB1 = fact.createObservation(progB, null)
      progB.addObservation(obsB1)

      // Pull changes from progA into progB, which should renumber B1
      val diffs = ProgramDiff.compare(progA, progB.getVersions, Set.empty)
      val mc    = MergeContext(progB, diffs)
      val act   =
        for {
          prelim <- PreliminaryMerge.merge(mc).liftVcs
          plan   <- MergeCorrection(mc)(prelim, _ => VcsAction(true))
          _      <- plan.merge(fact, progB)
        } yield ()

      act.unsafeRun

      // Now progB should have obsB1 (renumbered to 3) and obsA2
      val obsKeys = progB.getObservations.asScala.map(_.getNodeKey).toSet
      Assert.assertEquals(Set(obsB1.getNodeKey, obsA2.getNodeKey), obsKeys)

      // Create a new observation for progB.  It should get number 4.
      val obsB4 = fact.createObservation(progB, null)
      progB.addObservation(obsB4)

      Assert.assertEquals(4, obsB4.getObservationNumber)

    } finally {
      odb.getDBAdmin.shutdown()
    }
  }
}
