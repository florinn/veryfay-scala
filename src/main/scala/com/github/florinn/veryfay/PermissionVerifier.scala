package com.github.florinn

import scala.util.Try
import scala.reflect.runtime.universe.TypeTag

package object veryfay {
  type ActivityPermissionListType = Seq[PermissionSet]

  private[veryfay] object PermissionVerifier {

    def verify[T <: Activity[_], U: TypeTag, V: TypeTag](
      activity: T, activityPermissions: ActivityPermissionListType, principal: U, extraInfo: Option[V]): Try[String] =
      Try(verify(activityPermissions, principal, extraInfo))

    @throws(classOf[AuthorizationException])
    private def verify[U: TypeTag, V: TypeTag](
      activityPermissions: ActivityPermissionListType, principal: U, extraInfo: Option[V]): String = {
      var resMsg = None: Option[String]
      val activityPermissionsCuratedRoleSets = this.curateRoleSets(activityPermissions, principal, extraInfo)

      for {
        ap <- activityPermissionsCuratedRoleSets
        denyRoleSet <- ap.roleSets.filter { _.isInstanceOf[DenyRoleSet[_, _]] }
      } {
        val denyRoleSetCasted = denyRoleSet.asInstanceOf[DenyRoleSet[U, V]]
        denyRoleSetCasted.check(principal, extraInfo) match {
          case true =>
            val msg = denyRoleSetCasted.getMsg(principal, extraInfo)
            val tmpMsg = (resMsg :: Some(s"### DENY SET => TRUE:\n $msg") :: Nil).flatten mkString "\n"
            resMsg = Some(tmpMsg)
            throw new AuthorizationException(resMsg.get)

          case false =>
            val msg = denyRoleSetCasted.getMsg(principal, extraInfo)
            val tmpMsg = (resMsg :: Some(s"--- DENY SET => FALSE:\n $msg") :: Nil).flatten mkString "\n"
            resMsg = Some(tmpMsg)
        }
      }

      for {
        ap <- activityPermissionsCuratedRoleSets
        allowRoleSet <- ap.roleSets.filter { _.isInstanceOf[AllowRoleSet[_, _]] }
      } {
        val allowRoleSetCasted = allowRoleSet.asInstanceOf[AllowRoleSet[U, V]]
        allowRoleSetCasted.check(principal, extraInfo) match {
          case true =>
            val msg = allowRoleSetCasted.getMsg(principal, extraInfo)
            val tmpMsg = (resMsg :: Some(s"### ALLOW SET => TRUE:\n $msg") :: Nil).flatten mkString "\n"
            resMsg = Some(tmpMsg)
            return resMsg.get

          case false =>
            val msg = allowRoleSetCasted.getMsg(principal, extraInfo)
            val tmpMsg = (resMsg :: Some(s"--- ALLOW SET => FALSE:\n $msg") :: Nil).flatten mkString "\n"
            resMsg = Some(tmpMsg)
        }
      }
      throw new AuthorizationException(resMsg.getOrElse("NO MATCHING ROLE SET FOUND"))
    }

    private def curateRoleSets[U: TypeTag, V: TypeTag](
      activityPermissions: ActivityPermissionListType, principal: U, extraInfo: Option[V]): ActivityPermissionListType = {
      val tagU = implicitly[TypeTag[U]]
      val tagV = implicitly[TypeTag[V]]

      activityPermissions.map { x =>
        {
          val curatedRoleSets = x.roleSets.filter(y =>
            y.tagU.tpe =:= tagU.tpe &&
              (extraInfo.isEmpty || y.tagV.tpe =:= tagV.tpe))
          val ps = new PermissionSet
          ps.roleSets = curatedRoleSets
          ps
        }
      }
    }
  }
}