package com.github.florinn.veryfay

import scala.reflect.runtime.universe.TypeTag
import scala.util.{ Try, Success, Failure }

class ActivityAuthorization[T <: Activity[_]](activityRegistry: ActivityRegistry, activity: T) {
  def isAllowing[U: TypeTag, V: TypeTag](principal: U, extraInfo: Option[V] = None): Try[String] =
    this.authorize(principal, extraInfo)

  @throws(classOf[AuthorizationException])
  def verify[U: TypeTag, V: TypeTag](principal: U, extraInfo: Option[V] = None): String =
    this.authorize(principal, extraInfo) match {
      case Success(msg) => msg
      case Failure(e) => throw e
    }

  private def authorize[U: TypeTag, V: TypeTag](principal: U, extraInfo: Option[V]): Try[String] = {
    val activityPermissions = activityRegistry.get(activity) match {
      case Success(apList) => apList
      case Failure(e) => return Try(throw new AuthorizationException(e.getMessage))
    }
    PermissionVerifier.verify(activity, activityPermissions, principal, extraInfo)
  }
}