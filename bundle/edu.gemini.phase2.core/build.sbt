import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.phase2.core"

// version set in ThisBuild


osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("edu.gemini.*")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.phase2.core.model",
  "edu.gemini.phase2.core.odb")

        
