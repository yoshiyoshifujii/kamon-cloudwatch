Amazon CloudWatch Integration
=============================

[![Build Status](https://travis-ci.org/yoshiyoshifujii/kamon-cloudwatch.svg?branch=master)](https://travis-ci.org/yoshiyoshifujii/kamon-cloudwatch)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.yoshiyoshifujii/kamon-cloudwatch_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.yoshiyoshifujii/kamon-cloudwatch_2.12)

Reporting Metrics to Amazon CloudWatch
======================================

[Amazon CloudWatch](https://aws.amazon.com/cloudwatch/) is a monitoring service for AWS cloud resources and the applications you run on AWS.

### Getting Started

Supported releases and dependencies are shown below.

| kamon-cloudwatch  | status | jdk  | scala            |
|:-----------------:|:------:|:----:|------------------|
|  0.1.0            | stable | 1.8+ | 2.12             |

To get started with SBT, simply add the following to your `build.sbt` file:

```scala
libraryDependencies += "com.github.yoshiyoshifujii" %% "kamon-cloudwatch" % "0.1.0"
```

And add the API reporter to Kamon:

```scala
implicit val system: ActorSystem = ActorSystem()

Kamon.addReporter(CloudWatchAPIReporter()(system.dispatchers.lookup("blocking-io-dispatcher-cloudwatch")))
```

Configuration
-------------

To specify Amazon CloudWatch region, please write it in `application.conf` as follows.

```application.conf
kamon {

  cloudwatch {

    region = "us-east-1"

    namespace = "Kamon"

  }
}
```
