import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

val kamonCore    = "io.kamon" %% "kamon-core"    % "1.1.3"
val kamonTestKit = "io.kamon" %% "kamon-testkit" % "1.1.3"

lazy val root = (project in file("."))
  .settings(name := "kamon-cloudwatch")
  .settings(
    libraryDependencies ++=
      compileScope(kamonCore, scalaCompact.value) ++
        testScope(scalatest, slf4jApi, slf4jnop, kamonCore, kamonTestKit)
  )

def scalaCompact = Def.setting {
  scalaBinaryVersion.value match {
    case "2.10" | "2.11" => "org.scala-lang.modules" %% "scala-java8-compat" % "0.5.0"
    case "2.12"          => "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
  }
}
