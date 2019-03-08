package kamon.cloudwatch

import java.util.concurrent.{ ExecutionException, Future => JavaFuture }

import scala.concurrent.{ ExecutionContext, Future => ScalaFuture }

object JavaFutureConverter {

  implicit class FutureExtension[A](val self: JavaFuture[A]) extends AnyVal {
    def toScala(implicit ec: ExecutionContext): ScalaFuture[A] = JavaFutureConverter.toScala(self)
  }

  def toScala[A](jf: JavaFuture[A])(implicit ec: ExecutionContext): ScalaFuture[A] = {
    ScalaFuture(jf.get())
      .recoverWith {
        case e: ExecutionException => ScalaFuture.failed(e.getCause)
      }
  }
}
