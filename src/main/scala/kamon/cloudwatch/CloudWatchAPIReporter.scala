package kamon.cloudwatch

import com.amazonaws.ClientConfiguration
import com.amazonaws.services.cloudwatch.model.{ MetricDatum, PutMetricDataRequest }
import com.amazonaws.services.cloudwatch.{ AmazonCloudWatchAsync, AmazonCloudWatchAsyncClientBuilder }
import com.typesafe.config.Config
import kamon.metric.{ MetricDistribution, MetricValue, PeriodSnapshot }
import kamon.{ Kamon, MetricReporter }
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class CloudWatchAPIReporter(other: ClientConfiguration = new ClientConfiguration())(implicit ex: ExecutionContext)
    extends MetricReporter {
  import CloudWatchAPIReporter._
  import JavaFutureConverter._

  private val logger           = LoggerFactory.getLogger(classOf[CloudWatchAPIReporter])
  private var configuration    = Configuration.readConfiguration(Kamon.config())
  private var cloudWatchClient = createCloudWatchClient(configuration)

  override def start(): Unit =
    logger.info("Started the CloudWatch API reporter.")

  override def stop(): Unit =
    logger.info("Stopped the CloudWatch API reporter.")

  override def reconfigure(config: Config): Unit = {
    val newConfig = Configuration.readConfiguration(config)
    configuration = newConfig
    cloudWatchClient = createCloudWatchClient(newConfig)
    logger.info("Reconfigure the CloudWatch API reporter.")
  }

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = {
    val metricData: Seq[MetricDatum] =
    snapshot.metrics.counters.map(convert) ++
    snapshot.metrics.gauges.map(convert) ++
    snapshot.metrics.histograms.map(convert) ++
    snapshot.metrics.rangeSamplers.map(convert)

    val putMetricDataRequest = new PutMetricDataRequest()
      .withNamespace(configuration.namespace)
      .withMetricData(metricData.asJava)

    cloudWatchClient
      .putMetricDataAsync(putMetricDataRequest)
      .toScala
      .onComplete(
        _.fold(
          cause => logger.error("Kamon CloudWatch API Reporter is error.", cause),
          _ => ()
        )
      )
  }

  private[cloudwatch] def createCloudWatchClient(configuration: Configuration): AmazonCloudWatchAsync =
    AmazonCloudWatchAsyncClientBuilder
      .standard()
      .withClientConfiguration(new ClientConfiguration(other))
      .withRegion(configuration.region)
      .build()

}

object CloudWatchAPIReporter {

  case class Configuration(region: String, namespace: String)

  object Configuration {

    def readConfiguration(config: Config): CloudWatchAPIReporter.Configuration = {
      val cloudWatchConfig = config.getConfig("kamon.cloudwatch")

      CloudWatchAPIReporter.Configuration(
        region = cloudWatchConfig.getString("region"),
        namespace = cloudWatchConfig.getString("namespace")
      )
    }

  }

  def convert(metricValue: MetricValue): MetricDatum               = ???
  def convert(metricDistribution: MetricDistribution): MetricDatum = ???
}
