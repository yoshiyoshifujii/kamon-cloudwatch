package kamon.cloudwatch

import com.typesafe.config.Config

case class Configuration(region: String, namespace: String, storageResolution: Int)

object Configuration {

  def readConfiguration(config: Config): Configuration = {
    val cloudWatchConfig = config.getConfig("kamon.cloudwatch")

    Configuration(
      region = cloudWatchConfig.getString("region"),
      namespace = cloudWatchConfig.getString("namespace"),
      storageResolution = cloudWatchConfig.getInt("storage-resolution")
    )
  }

}
