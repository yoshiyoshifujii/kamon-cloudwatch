package kamon.cloudwatch

import java.util
import java.util.Date

import com.amazonaws.services.cloudwatch.model.{ Dimension, MetricDatum, StandardUnit, StatisticSet }
import kamon.Tags
import kamon.cloudwatch.CloudWatchAPIReporter.MeasurementUnitToStandardUnitF
import kamon.metric._

import scala.collection.JavaConverters._

private[cloudwatch] case class Converter(configuration: Configuration,
                                         snapshot: PeriodSnapshot,
                                         f: MeasurementUnitToStandardUnitF) {

  private lazy val timestamp: Date = Date.from(snapshot.from)

  private def toDimensions(tags: Tags): util.Collection[Dimension] =
    tags.map {
      case (k, v) =>
        new Dimension()
          .withName(k)
          .withValue(v)
    }.asJavaCollection

  private def toStandardUnit: MeasurementUnit => StandardUnit =
    f(_).getOrElse {
      case MeasurementUnit.none                  => StandardUnit.None
      case MeasurementUnit.percentage            => StandardUnit.Percent
      case MeasurementUnit.time.seconds          => StandardUnit.Seconds
      case MeasurementUnit.time.milliseconds     => StandardUnit.Milliseconds
      case MeasurementUnit.time.microseconds     => StandardUnit.Microseconds
      case MeasurementUnit.information.bytes     => StandardUnit.Bytes
      case MeasurementUnit.information.kilobytes => StandardUnit.Kilobytes
      case MeasurementUnit.information.megabytes => StandardUnit.Megabytes
      case MeasurementUnit.information.gigabytes => StandardUnit.Gigabytes
      case _                                     => StandardUnit.None
    }

  private def toStatisticSet(distribution: Distribution): StatisticSet =
    new StatisticSet()
      .withSampleCount(distribution.count.toDouble)
      .withSum(distribution.sum.toDouble)
      .withMinimum(distribution.min.toDouble)
      .withMaximum(distribution.max.toDouble)

  private[cloudwatch] def convert(metricValue: MetricValue): MetricDatum =
    new MetricDatum()
      .withMetricName(metricValue.name)
      .withDimensions(toDimensions(metricValue.tags))
      .withTimestamp(timestamp)
      .withValue(metricValue.value.toDouble)
      .withUnit(toStandardUnit(metricValue.unit))
      .withStorageResolution(configuration.storageResolution)

  private[cloudwatch] def convert(metricDistribution: MetricDistribution): MetricDatum =
    new MetricDatum()
      .withMetricName(metricDistribution.name)
      .withDimensions(toDimensions(metricDistribution.tags))
      .withTimestamp(timestamp)
      .withStatisticValues(toStatisticSet(metricDistribution.distribution))
      .withUnit(toStandardUnit(metricDistribution.unit))
      .withStorageResolution(configuration.storageResolution)

  lazy val converts: Seq[MetricDatum] =
  snapshot.metrics.counters.map(convert) ++
  snapshot.metrics.gauges.map(convert) ++
  snapshot.metrics.histograms.map(convert) ++
  snapshot.metrics.rangeSamplers.map(convert)

}
