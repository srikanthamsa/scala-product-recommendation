ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "Srikant"
  )
// Define versions for Spark
val spark_version = "3.5.0"

// Define library dependencies
libraryDependencies ++= Seq(
  // Spark dependencies
  "org.apache.spark" %% "spark-core" % spark_version,
  "org.apache.spark" %% "spark-sql" % spark_version,
  "org.apache.spark" %% "spark-mllib" % spark_version
)