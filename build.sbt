lazy val root = project.in(file("."))
  .settings(
    organization  := "gr.armand",
    name          := "stsc",
    version       := "1.0",
    scalaVersion  := "2.13.6",
    scalacOptions += "-deprecation",
    libraryDependencies  ++= Seq(
      "org.scalanlp"      %% "breeze"         % deps.main.breeze,
      "org.scalanlp"      %% "breeze-natives" % deps.main.breeze,
      "org.scalatest"     %% "scalatest"      % deps.test.scalaTest   % Test,
      "com.storm-enroute" %% "scalameter"     % deps.main.scalaMeter  % Test, // Benchmarks
    ),
    // Tests
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    logBuffered := false
  )

lazy val deps = new {
  val main = new {
    val breeze     = "1.2"
    val scalaMeter = "0.21"
  }
  val test = new {
    val scalaTest  = "3.2.9"
  }
}
