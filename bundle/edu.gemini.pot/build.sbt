import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.pot"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.pot.spdb.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.pot",
  "edu.gemini.pot.client",
  "edu.gemini.pot.locking",
  "edu.gemini.pot.sp",
  "edu.gemini.pot.sp.memImpl",
  "edu.gemini.pot.sp.memImpl.oldtest",
  "edu.gemini.pot.sp.test",
  "edu.gemini.pot.sp.validator",
  "edu.gemini.pot.sp.version",
  "edu.gemini.pot.spdb",
  "edu.gemini.pot.spdb.fed",
  "edu.gemini.pot.spdb.fed.mr",
  "edu.gemini.pot.spdb.fed.mr.impl",
  "edu.gemini.pot.spdb.fed.mr.impl.util",
  "edu.gemini.pot.spdb.oldtest",
  "edu.gemini.pot.spdb.service",
  "edu.gemini.pot.spdb.test",
  "edu.gemini.pot.util",
  "edu.gemini.spModel.ags",
  "edu.gemini.spModel.ao",
  "edu.gemini.spModel.config",
  "edu.gemini.spModel.config.injector",
  "edu.gemini.spModel.config.injector.obswavelength",
  "edu.gemini.spModel.config.map",
  "edu.gemini.spModel.config.test",
  "edu.gemini.spModel.config2",
  "edu.gemini.spModel.config2.test",
  "edu.gemini.spModel.conflict",
  "edu.gemini.spModel.data",
  "edu.gemini.spModel.data.config",
  "edu.gemini.spModel.data.config.test",
  "edu.gemini.spModel.data.property",
  "edu.gemini.spModel.dataflow",
  "edu.gemini.spModel.dataset",
  "edu.gemini.spModel.event",
  "edu.gemini.spModel.ext",
  "edu.gemini.spModel.gemini.acqcam",
  "edu.gemini.spModel.gemini.acqcam.test",
  "edu.gemini.spModel.gemini.altair",
  "edu.gemini.spModel.gemini.altair.blueprint",
  "edu.gemini.spModel.gemini.altair.test",
  "edu.gemini.spModel.gemini.bhros",
  "edu.gemini.spModel.gemini.bhros.ech",
  "edu.gemini.spModel.gemini.bhros.test",
  "edu.gemini.spModel.gemini.calunit",
  "edu.gemini.spModel.gemini.calunit.calibration",
  "edu.gemini.spModel.gemini.calunit.smartgcal",
  "edu.gemini.spModel.gemini.calunit.smartgcal.keys",
  "edu.gemini.spModel.gemini.calunit.smartgcal.maps",
  "edu.gemini.spModel.gemini.calunit.test",
  "edu.gemini.spModel.gemini.flamingos2",
  "edu.gemini.spModel.gemini.flamingos2.blueprint",
  "edu.gemini.spModel.gemini.flamingos2.test",
  "edu.gemini.spModel.gemini.gems",
  "edu.gemini.spModel.gemini.ghost",
  "edu.gemini.spModel.gemini.ghost.blueprint",
  "edu.gemini.spModel.gemini.gmos",
  "edu.gemini.spModel.gemini.gmos.blueprint",
  "edu.gemini.spModel.gemini.gmos.test",
  "edu.gemini.spModel.gemini.gnirs",
  "edu.gemini.spModel.gemini.gnirs.blueprint",
  "edu.gemini.spModel.gemini.gnirs.test",
  "edu.gemini.spModel.gemini.gpi",
  "edu.gemini.spModel.gemini.gpi.blueprint",
  "edu.gemini.spModel.gemini.gpol",
  "edu.gemini.spModel.gemini.graces.blueprint",
  "edu.gemini.spModel.gemini.gsaoi",
  "edu.gemini.spModel.gemini.gsaoi.blueprint",
  "edu.gemini.spModel.gemini.igrins2",
  "edu.gemini.spModel.gemini.init",
  "edu.gemini.spModel.gemini.inst",
  "edu.gemini.spModel.gemini.michelle",
  "edu.gemini.spModel.gemini.michelle.blueprint",
  "edu.gemini.spModel.gemini.michelle.test",
  "edu.gemini.spModel.gemini.nici",
  "edu.gemini.spModel.gemini.nici.blueprint",
  "edu.gemini.spModel.gemini.nici.test",
  "edu.gemini.spModel.gemini.nifs",
  "edu.gemini.spModel.gemini.nifs.blueprint",
  "edu.gemini.spModel.gemini.nifs.test",
  "edu.gemini.spModel.gemini.niri",
  "edu.gemini.spModel.gemini.niri.blueprint",
  "edu.gemini.spModel.gemini.niri.test",
  "edu.gemini.spModel.gemini.obscomp",
  "edu.gemini.spModel.gemini.obscomp.test",
  "edu.gemini.spModel.gemini.obslog",
  "edu.gemini.spModel.gemini.parallacticangle",
  "edu.gemini.spModel.gemini.phase1",
  "edu.gemini.spModel.gemini.phoenix",
  "edu.gemini.spModel.gemini.phoenix.test",
  "edu.gemini.spModel.gemini.phoenix.blueprint",
  "edu.gemini.spModel.gemini.plan",
  "edu.gemini.spModel.gemini.plan.test",
  "edu.gemini.spModel.gemini.security",
  "edu.gemini.spModel.gemini.seqcomp",
  "edu.gemini.spModel.gemini.seqcomp.smartgcal",
  "edu.gemini.spModel.gemini.seqcomp.test",
  "edu.gemini.spModel.gemini.texes",
  "edu.gemini.spModel.gemini.texes.blueprint",
  "edu.gemini.spModel.gemini.texes.test",
  "edu.gemini.spModel.gemini.trecs",
  "edu.gemini.spModel.gemini.trecs.blueprint",
  "edu.gemini.spModel.gemini.trecs.test",
  "edu.gemini.spModel.gemini.visitor",
  "edu.gemini.spModel.gemini.visitor.blueprint",
  "edu.gemini.spModel.gems",
  "edu.gemini.spModel.guide",
  "edu.gemini.spModel.ictd",
  "edu.gemini.spModel.init",
  "edu.gemini.spModel.inst",
  "edu.gemini.spModel.obs",
  "edu.gemini.spModel.obs.context",
  "edu.gemini.spModel.obs.plannedtime",
  "edu.gemini.spModel.obscomp",
  "edu.gemini.spModel.obscomp.test",
  "edu.gemini.spModel.obsclass",
  "edu.gemini.spModel.obslog",
  "edu.gemini.spModel.obsrecord",
  "edu.gemini.spModel.obsseq",
  "edu.gemini.spModel.obsseq.test",
  "edu.gemini.spModel.prog",
  "edu.gemini.spModel.rich.core",
  "edu.gemini.spModel.rich.pot.sp",
  "edu.gemini.spModel.rich.pot.spdb",
  "edu.gemini.spModel.rich.shared.immutable",
  "edu.gemini.spModel.seqcomp",
  "edu.gemini.spModel.syntax",
  "edu.gemini.spModel.syntax.event",
  "edu.gemini.spModel.syntax.skycalc",
  "edu.gemini.spModel.syntax.sp",
  "edu.gemini.spModel.target",
  "edu.gemini.spModel.target.env",
  "edu.gemini.spModel.target.obsComp",
  "edu.gemini.spModel.target.offset",
  "edu.gemini.spModel.target.sync",
  "edu.gemini.spModel.target.system",
  "edu.gemini.spModel.target.system.test",
  "edu.gemini.spModel.telescope",
  "edu.gemini.spModel.template",
  "edu.gemini.spModel.test",
  "edu.gemini.spModel.time",
  "edu.gemini.spModel.timeacct",
  "edu.gemini.spModel.too",
  "edu.gemini.spModel.type",
  "edu.gemini.spModel.util",
  "edu.gemini.spModel.util.test",
  "edu.gemini.util")

OsgiKeys.additionalHeaders +=
  ("Import-Package" -> "!org.apache.regexp,!org.apache.xerces.*,*")
