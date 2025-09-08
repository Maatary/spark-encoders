package io.github.pashashiz.spark_encoders

import org.apache.spark.sql.catalyst.encoders.{AgnosticEncoder, AgnosticEncoders, EncoderUtils}
import org.apache.spark.sql.catalyst.expressions.{Expression, If, IsNull, Literal}
import org.apache.spark.sql.catalyst.expressions.objects.{UnwrapOption, WrapOption}
import org.apache.spark.sql.types.{DataType, ObjectType}

import scala.reflect.ClassTag

case class OptionEncoder[A]()(implicit inner: TypedEncoder[A]) extends TypedEncoder[Option[A]] {

  override def nullable = true

  override def catalystRepr: DataType = inner.catalystRepr

  override protected[spark_encoders] def agnostic: AgnosticEncoder[Option[A]] =
    AgnosticEncoders.OptionEncoder(inner.agnostic)

  override def toCatalyst(path: Expression): Expression = {
    // note: we want to make sure we always get Object type here,
    // for example, when we have primitive type such as IntegerType,
    // we want to end up having ObjectType(java.lang.Integer) which is translated to java.lang.Integer
    // instead of IntegerType which is translated to int
    val optionType = ObjectType(EncoderUtils.javaBoxedType(inner.jvmRepr))
    val unwrapped = UnwrapOption(optionType, path)
    // note: unboxed is noop for objects
    val unboxed = Primitive.unbox(unwrapped, catalystRepr)
    val innerExpr = inner.toCatalyst(unboxed)
    val nullExpr = Literal.create(null, innerExpr.dataType)
    If(IsNull(unboxed), nullExpr, innerExpr)
  }

  override def fromCatalyst(path: Expression): Expression = {
    val valueExpr = inner.fromCatalyst(path)
    val nullExpr = Literal.create(null, inner.jvmRepr)
    WrapOption(If(IsNull(path), nullExpr, valueExpr), inner.jvmRepr)
  }

  override def toString: String = s"OptionEncoder(${inner.jvmRepr})"
}
