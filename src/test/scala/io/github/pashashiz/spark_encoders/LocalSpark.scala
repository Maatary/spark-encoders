package io.github.pashashiz.spark_encoders

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.internal.SQLConf._

import java.util.TimeZone

case class LocalSpark(
    parallelism: Int = 4,
    shufflePartitions: Int = 4,
    sparkUI: Boolean = false,
    java8Api: Boolean = true,
    appID: String =
      getClass.getName + math.floor(math.random * 10e4).toLong.toString,
    conf: Map[String, String] = Map.empty) {

  def create: SparkSession = {
    val configs = Map(
      "spark.ui.enabled" -> sparkUI.toString,
      "spark.ui.showConsoleProgress" -> sparkUI.toString,
      "spark.app.id" -> appID,
      "spark.driver.host" -> "localhost",
      "spark.serializer" -> "org.apache.spark.serializer.KryoSerializer",
      "spark.driver.extraJavaOptions" -> LocalSpark.moduleOpts,
      "spark.executor.extraJavaOptions" -> LocalSpark.moduleOpts,
      DATETIME_JAVA8API_ENABLED.key -> java8Api.toString,
      SHUFFLE_PARTITIONS.key -> shufflePartitions.toString,
      "spark.sql.sources.parallelPartitionDiscovery.parallelism" -> 2.toString,
      // for some reason parquet vectorized reader cannot handle UDT inside arrays
      // however, that works on databricks, looks like they have custom implementation
      "spark.sql.parquet.enableNestedColumnVectorizedReader" -> "false")
    val sparkConf = new SparkConf().setMaster(s"local[$parallelism]")
      .setAppName("test")
      .setAll(configs ++ conf)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    SparkSession.builder().config(sparkConf).getOrCreate()
  }
}

object LocalSpark {
  val moduleOpts: String =
    Seq(
      "-XX:+IgnoreUnrecognizedVMOptions",
      "--add-modules=jdk.incubator.vector",
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
      "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
      "--add-opens=java.base/java.io=ALL-UNNAMED",
      "--add-opens=java.base/java.net=ALL-UNNAMED",
      "--add-opens=java.base/java.nio=ALL-UNNAMED",
      "--add-opens=java.base/java.time=ALL-UNNAMED",
      "--add-opens=java.base/java.util=ALL-UNNAMED",
      "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
      "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
      "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED",
      "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
      "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
      "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
      "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
      "-Djdk.reflect.useDirectMethodHandle=false",
      "-Dio.netty.tryReflectionSetAccessible=true"
    ).mkString(" ")
}
