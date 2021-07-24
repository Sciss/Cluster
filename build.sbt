lazy val baseName       = "Cluster"
lazy val baseNameL      = baseName.toLowerCase
lazy val projectVersion = "0.1.0-SNAPSHOT"

lazy val gitRepoHost    = "github.com"
lazy val gitRepoUser    = "Sciss"

ThisBuild / version       := projectVersion
ThisBuild / organization  := "de.sciss"
ThisBuild / versionScheme := Some("pvp")

lazy val root = project.in(file("."))
  .settings(publishSettings)
  .settings(
    name                := baseName,
    homepage            := Some(url(s"https://$gitRepoHost/$gitRepoUser/$baseName")),
    description         := "A data clustering library",
    licenses            := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
    scalaVersion        := "2.13.6",
    crossScalaVersions  := Seq(/*"3.0.1",*/ "2.13.6", "2.12.14"), // Breeze currently not available for Scala 3
    scalacOptions += "-deprecation",
    libraryDependencies ++= {
      val scalaMeter = if (scalaVersion.value.startsWith("2.12.")) deps.main.scalaMeter_2_12 else deps.main.scalaMeter
      Seq(
        "org.scalanlp"      %% "breeze"         % deps.main.breeze,
        "org.scalanlp"      %% "breeze-natives" % deps.main.breeze,
        "org.scalatest"     %% "scalatest"      % deps.test.scalaTest % Test,
        "com.storm-enroute" %% "scalameter"     % scalaMeter          % Test, // Benchmarks
      )
    },
    // Tests
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    logBuffered := false,
//    Test / parallelExecution := false,
  )

lazy val deps = new {
  val main = new {
    val breeze          = "1.2"
    val scalaMeter      = "0.21"
    val scalaMeter_2_12 = "0.19"
  }
  val test = new {
    val scalaTest  = "3.2.9"
  }
}

// ---- publishing ----

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },
  developers := List(
    Developer(
      id    = "armandgrillet",
      name  = "Armand Grillet",
      email = "armand.grillet@gmail.com",
      url   = url("https://armand.gr/")
    ),
    Developer(
      id    = "sciss",
      name  = "Hanns Holger Rutz",
      email = "contact@sciss.de",
      url   = url("https://www.sciss.de")
    )
  ),
  scmInfo := {
    Some(ScmInfo(
      url(s"https://$gitRepoHost/$gitRepoUser/$baseName"),
      s"scm:git@$gitRepoHost:$gitRepoUser/$baseName.git"
    ))
  },
)

