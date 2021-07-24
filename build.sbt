lazy val root = project.in(file("."))
  .settings(
    organization  := "gr.armand",
    name          := "stsc",
    version       := "1.0",
    scalaVersion  := "2.13.6",
    scalacOptions += "-deprecation",
    libraryDependencies  ++= Seq(
      // Breeze.
      "org.scalanlp" %% "breeze"            % deps.main.breeze,
      "org.scalanlp" %% "breeze-natives"    % deps.main.breeze,
      // The unit test library.
      "org.scalactic" %% "scalactic"        % deps.test.scalaTest,
      "org.scalatest" %% "scalatest"        % deps.test.scalaTest % Test,
      // Benchmarks
      "com.storm-enroute" %% "scalameter"   % deps.main.scalaMeter,
      // Hadoop HDFS to merge the results.
      "org.apache.hadoop" % "hadoop-client" % deps.main.hadoop,
      "org.apache.hadoop" % "hadoop-hdfs"   % deps.main.hadoop
      // Logs
      //"org.slf4j" % "slf4j-simple" % "1.6.4"
    ),
    // Assembly
    assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    assembly / test := {},
    // Tests
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    logBuffered := false
  )

lazy val deps = new {
  val main = new {
    val breeze     = "1.2"
    val hadoop     = "2.10.1"
    val scalaMeter = "0.21"
    val spark      = "2.0.0"
  }
  val test = new {
    val scalaTest  = "3.2.9"
  }
}
