package com.github.florinn.veryfay

class AuthorizationEngine {
  val activityRegistry = new ActivityRegistry

  def register[T <: Activity[_]](activity: T, moreActivities: T*): PermissionSet = {
    val ps = new PermissionSet
    activityRegistry.add[T](activity, ps)
    for (a <- moreActivities)
      activityRegistry.add(a, ps)
    ps
  }
  
  def apply[T <: Activity[_]](activity: T): ActivityAuthorization[T] =
    new ActivityAuthorization(activityRegistry, activity)
}