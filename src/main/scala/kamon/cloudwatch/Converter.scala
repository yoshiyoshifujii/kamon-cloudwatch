package kamon.cloudwatch

import java.util
import java.util.Date

import com.amazonaws.services.cloudwatch.model.{ Dimension, MetricDatum, StandardUnit, StatisticSet }
import kamon.Tags
import kamon.cloudwatch.CloudWatchAPIReporter.MeasurementUnitToStandardUnitF
import kamon.metric._
import org.slf4j.Logger

import scala.collection.JavaConverters._

private[cloudwatch] case class Converter(configuration: Configuration,
                                         snapshot: PeriodSnapshot,
                                         f: MeasurementUnitToStandardUnitF)(implicit logger: Logger) {

  private lazy val timestamp: Date = Date.from(snapshot.from)

  private def toDimensions(tags: Tags): util.Collection[Dimension] =
    tags.map {
      case (k, v) =>
        new Dimension()
          .withName(k)
          .withValue(v)
    }.asJavaCollection

  private val MeasurementUnitMap: Map[MeasurementUnit, StandardUnit] =
    Map(
      MeasurementUnit.none                  -> StandardUnit.None,
      MeasurementUnit.percentage            -> StandardUnit.Percent,
      MeasurementUnit.time.seconds          -> StandardUnit.Seconds,
      MeasurementUnit.time.milliseconds     -> StandardUnit.Milliseconds,
      MeasurementUnit.time.microseconds     -> StandardUnit.Microseconds,
      MeasurementUnit.time.nanoseconds      -> StandardUnit.Microseconds,
      MeasurementUnit.information.bytes     -> StandardUnit.Bytes,
      MeasurementUnit.information.kilobytes -> StandardUnit.Kilobytes,
      MeasurementUnit.information.megabytes -> StandardUnit.Megabytes,
      MeasurementUnit.information.gigabytes -> StandardUnit.Gigabytes
    )

  private def toStandardUnit: MeasurementUnit => StandardUnit =
    unit =>
      f(unit).getOrElse(MeasurementUnitMap.getOrElse(unit, {
        logger.warn(unit.toString)
        StandardUnit.None
      }))

  private def calculateValue: MeasurementUnit => Double => Double = {
    case unit if unit == MeasurementUnit.time.nanoseconds => _ / 1000
    case _                                                => identity
  }

  private def toStatisticSet(distribution: Distribution)(f: Double => Double): Seq[StatisticSet] =
    if (distribution.count > 0)
      Seq(
        new StatisticSet()
          .withSampleCount(f(distribution.count.toDouble))
          .withSum(f(distribution.sum.toDouble))
          .withMinimum(f(distribution.min.toDouble))
          .withMaximum(f(distribution.max.toDouble))
      )
    else Seq.empty

  private[cloudwatch] def convert(metricValue: MetricValue): MetricDatum =
    new MetricDatum()
      .withMetricName(metricValue.name)
      .withDimensions(toDimensions(metricValue.tags))
      .withTimestamp(timestamp)
      .withValue(calculateValue(metricValue.unit)(metricValue.value.toDouble))
      .withUnit(toStandardUnit(metricValue.unit))
      .withStorageResolution(configuration.storageResolution)

  private[cloudwatch] def convert(metricDistribution: MetricDistribution): Seq[MetricDatum] =
    toStatisticSet(metricDistribution.distribution)(calculateValue(metricDistribution.unit))
      .map { statisticSet =>
        new MetricDatum()
          .withMetricName(metricDistribution.name)
          .withDimensions(toDimensions(metricDistribution.tags))
          .withTimestamp(timestamp)
          .withStatisticValues(statisticSet)
          .withUnit(toStandardUnit(metricDistribution.unit))
          .withStorageResolution(configuration.storageResolution)
      }

  lazy val converts: Seq[MetricDatum] =
  snapshot.metrics.counters.map(convert) ++
  snapshot.metrics.gauges.map(convert) ++
  snapshot.metrics.histograms.flatMap(convert) ++
  snapshot.metrics.rangeSamplers.flatMap(convert)

}
