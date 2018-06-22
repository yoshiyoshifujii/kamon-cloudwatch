lazy val root: Project     = project.in(file(".")).dependsOn(latestSbtUmbrella)
lazy val latestSbtUmbrella = uri("git://github.com/kamon-io/kamon-sbt-umbrella.git")

addSbtPlugin("com.lucidchart"    % "sbt-scalafmt" % "1.15")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype" % "2.3")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"      % "1.1.1")
addSbtPlugin("com.github.gseitz" % "sbt-release"  % "1.0.7")
