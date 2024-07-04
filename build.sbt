import Wart._

enablePlugins(ScalaJSPlugin)

ThisBuild / scalaVersion     := "2.13.13"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.lptk"
ThisBuild / organizationName := "LPTK"
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:higherKinds",
  if (insideCI.value) "-Wconf:any:error"
  else                "-Wconf:any:warning",
)

lazy val root = project.in(file("."))
  .aggregate(mlscriptJS, mlscriptJVM)
  .settings(
    publish := {},
    publishLocal := {},
  )

lazy val mlscript = crossProject(JSPlatform, JVMPlatform).in(file("."))
  .settings(
    name := "mlscript",
    scalacOptions ++= Seq(
      "-Ywarn-value-discard",
      "-Ypatmat-exhaust-depth:160",
    ),
    wartremoverWarnings ++= Warts.allBut(
      Recursion, Throw, Nothing, Return, While, IsInstanceOf,
      Var, MutableDataStructures, NonUnitStatements,
      DefaultArguments, ImplicitParameter, ImplicitConversion,
      StringPlusAny, Any, ToString,
      JavaSerializable, Serializable, Product, ToString,
      LeakingSealed, Overloading,
      Option2Iterable, IterableOps, ListAppend, SeqApply,
      TripleQuestionMark,
    ),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test,
    libraryDependencies += "com.lihaoyi" %%% "sourcecode" % "0.3.0",
    libraryDependencies += "com.lihaoyi" %%% "fastparse" % "2.3.3",
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.8.0",
    // 
    watchSources += WatchSource(
      sourceDirectory.value.getParentFile().getParentFile()/"shared/src/test/diff", "*.fun", NothingFilter),
    watchSources += WatchSource(
      sourceDirectory.value.getParentFile().getParentFile()/"shared/src/test/diff", "*.mls", NothingFilter),
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oC"),
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.1.0",
  )

lazy val mlscriptJVM = mlscript.jvm
lazy val mlscriptJS = mlscript.js

lazy val web = crossProject(JSPlatform, JVMPlatform).in(file("web"))
  .settings(
    name := "mlscript-web",
    scalaVersion := "2.13.13",
    scalacOptions ++= Seq("-deprecation")
  )
  .jvmSettings()
  .jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.1.0",
    Compile / fastOptJS / artifactPath := baseDirectory.value / ".." / ".." / "web" / "npm" / "index.js",
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule).withMinify(true) },
    Compile / fullLinkJS / scalaJSLinkerOutputDirectory := baseDirectory.value / ".." / ".." / "web" / "npm" / "dist",
  )
  .dependsOn(mlscript % "compile->compile;test->test")

lazy val webJS = web.js
