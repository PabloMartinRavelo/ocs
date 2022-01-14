package edu.gemini.pit.ui.view.problem

import edu.gemini.pit.ui.robot.ProblemRobot.Problem
import edu.gemini.pit.ui.robot.ProblemRobot.Severity._
import edu.gemini.pit.ui.util.SharedIcons
import edu.gemini.pit.ui.ShellAdvisor
import edu.gemini.pit.model.Model
import edu.gemini.pit.ui.util.gface.SimpleListViewer

import scala.swing.BorderPanel

import scalaz.{Monoid, Lens}
import edu.gemini.pit.ui.binding._
import swing._
import Swing._

// This is basically just a list of problems (which are generated by the problemHandler robot.
class ProblemView(shellAdvisor: ShellAdvisor) extends BorderPanel with BoundView[Model] {
  implicit val boolMonoid = Monoid.instance[Boolean](_ || _,  false)

  // Bound
  val lens = Lens.lensId[Model]
  override def children = List(listView)

  // Configure content, defined below
  add(listView, BorderPanel.Position.Center)

  // Our list
  object listView extends SimpleListViewer[Model, Model, Problem] {

    val lens = Lens.lensId[Model]
    private var problems: List[Problem] = Nil
    preferredSize = (preferredSize.width, 250)

    // We also need to listen to the problem handler
    shellAdvisor.problemHandler.addListener { a:Any => refresh(model) }

    onDoubleClick(_())

    override def refresh(m: Option[Model]) {
      problems = shellAdvisor.problemHandler.state
      refresh()
    }

    def elementAt(m: Model, i: Int) = problems(i)
    def size(m: Model) = problems.size

    object columns extends Enumeration { val Description, Section = Value }
    import columns._

    def columnWidth = {
      case Description => (500, Integer.MAX_VALUE)
      case Section     => (80, Integer.MAX_VALUE)
    }

    def icon(p: Problem) = {
      case Description => p.severity match {
        case Todo    => SharedIcons.CHECK_UNSELECTED
        case Error   => SharedIcons.ICON_ERROR
        case Warning => SharedIcons.ICON_WARN
        case Info    => SharedIcons.ICON_INFO
        case _       => null
      }
    }

    def text(p: Problem) = {
      case Description => p.description
      case Section     => p.section
    }

  }

}
