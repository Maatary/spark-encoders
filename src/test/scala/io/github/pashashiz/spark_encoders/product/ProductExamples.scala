package io.github.pashashiz.spark_encoders.product

import io.github.pashashiz.spark_encoders.{TypedEncoder, TypedEncoderImplicits}
import java.sql.Timestamp

case class User(id: Long, name: String)
case class RawChange[T](
    entity: T,
    changeType: String,
    commitVersion: Long,
    commitTimestamp: Timestamp)

object ProductExamples extends TypedEncoderImplicits {
  implicit val userEncoder: TypedEncoder[User] = derive[User]
  implicit def rawChangeEncoder[T: TypedEncoder]: TypedEncoder[RawChange[T]] =
    derive[RawChange[T]]
}
