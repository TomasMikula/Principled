name := "principled"

organization := "org.principled"

version := "0.2-SNAPSHOT"

scalaVersion := "2.12.1"


scalastyleFailOnError := true

scalacOptions ++= Seq(
  "-Xlint",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture")

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.13.4"
)

fork := true

// A configuration which is like 'compile' except it performs additional
// static analysis. Execute static analysis via `lint:compile`
val LintTarget = config("lint").extend(Compile)

addMainSourcesToLintTarget 

addSlowScalacSwitchesToLintTarget

addWartRemoverToLintTarget

removeWartRemoverFromCompileTarget 

addFoursquareLinterToLintTarget 

removeFoursquareLinterFromCompileTarget 

def addMainSourcesToLintTarget = {
  inConfig(LintTarget) {
    Defaults.compileSettings ++ Seq(
      sources in LintTarget := {
        val lintSources = (sources in LintTarget).value
        lintSources ++ (sources in Compile).value
      }
    )
  }
}

def addSlowScalacSwitchesToLintTarget = {
  inConfig(LintTarget) {
    scalacOptions in LintTarget ++= Seq(
      "-Ywarn-unused-import",
      "-Ywarn-dead-code"
    )
  }
}

def addWartRemoverToLintTarget = {
  import wartremover._
  import Wart._
  inConfig(LintTarget) {
    wartremoverErrors ++= Seq(
      Wart.Any,
      Wart.Serializable,
      Wart.Product,
      Wart.ListOps,
      Wart.OptionPartial,
      Wart.EitherProjectionPartial,
      Wart.Any2StringAdd
    )
  }
}

def removeWartRemoverFromCompileTarget = {
  // WartRemover's sbt plugin calls addCompilerPlugin which always adds
  // directly to the Compile configuration. The bit below removes all
  // switches that could be passed to scalac about WartRemover during
  // a non-lint compile.
  scalacOptions in Compile := (scalacOptions in Compile).value filterNot { switch =>
    switch.startsWith("-P:wartremover:") ||
    "^-Xplugin:.*/org[.]brianmckenna/.*wartremover.*[.]jar$".r.pattern.matcher(switch).find
  }
}

def addFoursquareLinterToLintTarget = {
  Seq(
    resolvers += "Linter Repository" at "https://hairyfotr.github.io/linteRepo/releases",
    addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17"),
    // See https://github.com/HairyFotr/linter#list-of-implemented-checks
    // for a list of checks that foursquare linter implements.
    // By default linter enables all checks.
    // I don't mind using match on boolean variables.
    scalacOptions in LintTarget += "-P:linter:disable:PreferIfToBooleanMatch"
  )
}

def removeFoursquareLinterFromCompileTarget = {
  // We call addCompilerPlugin in project/plugins.sbt to add a depenency
  // on the foursquare linter so that sbt magically manages the JAR for us.
  // Unfortunately, addCompilerPlugin also adds a switch to scalacOptions
  // in the Compile config to load the plugin.
  // The bit below removes all switches that could be passed to scalac
  // about foursquare linter during a non-lint compile.
  scalacOptions in Compile := (scalacOptions in Compile).value filterNot { switch =>
    switch.startsWith("-P:linter:") ||
      "^-Xplugin:.*/com[.]foursquare[.]lint/.*linter.*[.]jar$".r.pattern.matcher(switch).find
  }
}
