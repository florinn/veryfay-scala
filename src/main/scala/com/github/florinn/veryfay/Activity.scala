package com.github.florinn.veryfay

import scala.reflect.runtime.universe.TypeTag

abstract class Activity[T](implicit val target: TypeTag[T])
trait Container[T] { def activities: Seq[_ <: Activity[T]] }

final case class Create[T: TypeTag]() extends Activity[T]
final case class Read[T: TypeTag]() extends Activity[T]
final case class Update[T: TypeTag]() extends Activity[T]
final case class Patch[T: TypeTag]() extends Activity[T]
final case class Delete[T: TypeTag]() extends Activity[T]

final case class CRUD[T: TypeTag]() extends Activity[T] with Container[T] {
  val activities = List(Create[T], Read[T], Update[T], Delete[T])
}
final case class CRUDP[T: TypeTag]() extends Activity[T] with Container[T] {
	val activities = List(CRUD[T], Patch[T])
}