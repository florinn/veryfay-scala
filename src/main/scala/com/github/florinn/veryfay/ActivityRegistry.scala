package com.github.florinn.veryfay

import scala.util.Try
import scala.reflect.runtime.universe.Type

private[veryfay] class ActivityRegistry {
  private val registeredPermissions = scala.collection.mutable.Map.empty[(Class[_ <: Activity[_]], Type), ActivityPermissionListType]

  def add[T <: Activity[_]](activity: T, ps: PermissionSet): Unit = activity match {
    case container: Container[_] =>
      for (a <- container.activities)
        this.add(a, ps)
    case simple => this.addActivityPermissions(activity, ps)
  }

  private def addActivityPermissions[T <: Activity[_]](activity: T, ps: PermissionSet) = {
    val activityPermissionList = registeredPermissions.getOrElse((activity.getClass, activity.target.tpe), List.empty)
    registeredPermissions += ((activity.getClass, activity.target.tpe) -> (activityPermissionList :+ ps))
  }

  def get[T <: Activity[_]](activity: T): Try[ActivityPermissionListType] =
    Try{
      registeredPermissions.filterKeys {
        case (x, y) =>
          x == activity.getClass &&
            y =:= activity.target.tpe
      }.values.toList match {
        case x :: xs => x
        case _ => throw new RuntimeException(s"no registered activity of type ${activity} - ${activity.target}")
      }}
}