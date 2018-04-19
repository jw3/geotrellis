import Dependencies._
import sbt.Keys._

scalaVersion := Version.scala

scalaVersion in ThisBuild := Version.scala

lazy val commonSettings = Seq(
  version := Version.geotrellis,
  scalaVersion := Version.scala,
  description := Info.description,
  organization := "org.locationtech.geotrellis",
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url(Info.url)),
  scmInfo := Some(ScmInfo(
    url("https://github.com/locationtech/geotrellis"), "scm:git:git@github.com:locationtech/geotrellis.git"
  )),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:existentials",
    "-language:experimental.macros",
    "-feature"
  ),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },

  publishTo := {
    val sonatype = "https://oss.sonatype.org/"
    val locationtech = "https://repo.locationtech.org/content/repositories"
    if (isSnapshot.value) {
      // Publish snapshots to LocationTech
      Some("LocationTech Snapshot Repository" at s"${locationtech}/geotrellis-snapshots")
    } else {
      val milestoneRx = """-(M|RC)\d+$""".r
      milestoneRx.findFirstIn(Version.geotrellis) match {
        case Some(v) =>
          // Public milestones to LocationTech
          Some("LocationTech Release Repository" at s"${locationtech}/geotrellis-releases")
        case None =>
          // Publish releases to Sonatype
          Some("Sonatype Release Repository" at s"${sonatype}service/local/staging/deploy/maven2")
      }
    }
  },

  credentials ++= List(Path.userHome / ".ivy2" / ".credentials")
    .filter(_.asFile.canRead)
    .map(Credentials(_)),

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.4" cross CrossVersion.binary),
  addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full),

  pomExtra := (
    <developers>
      <developer>
        <id>echeipesh</id>
        <name>Eugene Cheipesh</name>
        <url>http://github.com/echeipesh/</url>
      </developer>
      <developer>
        <id>lossyrob</id>
        <name>Rob Emanuele</name>
        <url>http://github.com/lossyrob/</url>
      </developer>
    </developers>),

  shellPrompt := { s => Project.extract(s).currentProject.id + " > " },
  dependencyUpdatesFilter := moduleFilter(organization = "org.scala-lang"),
  resolvers ++= Seq(
    "geosolutions" at "http://maven.geo-solutions.it/",
    "osgeo" at "http://download.osgeo.org/webdav/geotools/"
  ),
  headerLicense := Some(HeaderLicense.ALv2("2017", "Azavea")),
  scapegoatVersion in ThisBuild := "1.3.3",
  updateOptions := updateOptions.value.withGigahorse(false)
)

lazy val root = Project("geotrellis", file(".")).
  aggregate(
    geotools,
    macros,
    proj4,
    raster,
    `raster-testkit`,
    shapefile,
    slick,
    util,
    vector,
    `vector-testkit`,
    vectortile
  ).
  settings(commonSettings: _*).
  enablePlugins(ScalaUnidocPlugin).
  settings(
    initialCommands in console :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.spark._
      """
  )

lazy val macros = project
  .settings(commonSettings)

lazy val vectortile = project
  .dependsOn(vector)
  .settings(commonSettings)

lazy val vector = project
  .dependsOn(proj4, util)
  .settings(commonSettings)
  .settings(
    unmanagedClasspath in Test ++= (fullClasspath in (LocalProject("vector-testkit"), Compile)).value
  )

lazy val `vector-testkit` = project
  .dependsOn(raster % Provided, vector % Provided)
  .settings(commonSettings)

lazy val proj4 = project
  .settings(commonSettings)
  .settings(javacOptions ++= Seq("-encoding", "UTF-8"))

lazy val raster = project
  .dependsOn(util, macros, vector)
  .settings(commonSettings)
  .settings(
    unmanagedClasspath in Test ++= (fullClasspath in (LocalProject("raster-testkit"), Compile)).value
  )
  .settings(
    unmanagedClasspath in Test ++= (fullClasspath in (LocalProject("vector-testkit"), Compile)).value
  )

lazy val `raster-testkit` = project
  .dependsOn(raster % Provided, vector % Provided)
  .settings(commonSettings)

lazy val geotools = project
  .dependsOn(raster, vector, proj4, `vector-testkit` % Test, `raster-testkit` % Test,
    `raster` % "test->test" // <-- to get rid  of this, move `GeoTiffTestUtils` to the testkit.
  )
  .settings(commonSettings)

lazy val slick = project
  .dependsOn(vector)
  .settings(commonSettings)


lazy val shapefile = project
  .dependsOn(raster, `raster-testkit` % Test)
  .settings(commonSettings)

lazy val util = project
  .settings(commonSettings)
