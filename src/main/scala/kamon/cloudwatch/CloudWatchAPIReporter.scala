package kamon.cloudwatch

import com.amazonaws.ClientConfiguration
import com.amazonaws.services.cloudwatch.model._
import com.amazonaws.services.cloudwatch.{ AmazonCloudWatchAsync, AmazonCloudWatchAsyncClientBuilder }
import com.typesafe.config.Config
import kamon.cloudwatch.CloudWatchAPIReporter.MeasurementUnitToStandardUnitF
import kamon.metric._
import kamon.{ Kamon, MetricReporter }
import org.slf4j.{ Logger, LoggerFactory }

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.util.Failure

private[cloudwatch] class CloudWatchAPIReporter(other: ClientConfiguration, f: MeasurementUnitToStandardUnitF)(
    implicit ex: ExecutionContext
) extends MetricReporter {
  import JavaFutureConverter._

  private implicit val logger: Logger = LoggerFactory.getLogger(classOf[CloudWatchAPIReporter])
  private var configuration           = Configuration.readConfiguration(Kamon.config())
  private var cloudWatchClient        = createCloudWatchClient(configuration)

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

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit =
    Converter(configuration, snapshot, f).converts.grouped(20).foreach { metricData =>
      val putMetricDataRequest = new PutMetricDataRequest()
        .withNamespace(configuration.namespace)
        .withMetricData(metricData.asJavaCollection)

      cloudWatchClient
        .putMetricDataAsync(putMetricDataRequest)
        .toScala
        .onComplete {
          case Failure(cause) => logger.error("Kamon CloudWatch API Reporter is error.", cause)
          case _              => ()
        }
    }

  private def createCloudWatchClient(configuration: Configuration): AmazonCloudWatchAsync =
    AmazonCloudWatchAsyncClientBuilder
      .standard()
      .withClientConfiguration(new ClientConfiguration(other))
      .withRegion(configuration.region)
      .build()

}

object CloudWatchAPIReporter {

  type MeasurementUnitToStandardUnitF = MeasurementUnit => Option[StandardUnit]

  private val defaultF: MeasurementUnitToStandardUnitF = _ => None

  def apply()(implicit ex: ExecutionContext): CloudWatchAPIReporter =
    new CloudWatchAPIReporter(other = new ClientConfiguration(), f = defaultF)

  def apply(other: ClientConfiguration)(implicit ex: ExecutionContext) =
    new CloudWatchAPIReporter(other = other, f = defaultF)

  def apply(f: MeasurementUnitToStandardUnitF)(implicit ex: ExecutionContext) =
    new CloudWatchAPIReporter(other = new ClientConfiguration(), f = f)

  def apply(other: ClientConfiguration, f: MeasurementUnitToStandardUnitF)(implicit ex: ExecutionContext) =
    new CloudWatchAPIReporter(other = other, f = f)
}
