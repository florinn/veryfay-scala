package com.github.florinn.veryfay

import scala.util.control.Breaks._
import scala.reflect.runtime.universe.TypeTag

private[veryfay] sealed abstract class RoleSet[U, V](implicit val tagU: TypeTag[U], val tagV: TypeTag[V]) {
  def roles: Seq[Role[U, V]]

  def check(principal: U, extraInfo: Option[V]): Boolean = {
    var result = true
    val visitor = (role: Role[U, V]) => {
      if (!role.contains(principal, extraInfo)) {
        result = false
        break
      }
    }
    traverse(this.roles, visitor)
    result
  }

  def getMsg(principal: U, extraInfo: Option[V]): String = {
    var msg = ""
    val visitor = (role: Role[U, V]) => {
      if (role.contains(principal, extraInfo)) {
        msg += s"[$role] contains [$principal] and [$extraInfo] AND\n"
      } else {
        msg += s"[$role] DOES NOT contain [$principal] and [$extraInfo] \n"
        break
      }
    }
    traverse(this.roles, visitor)
    msg
  }

  def traverse(roles: Seq[Role[U, V]], visitor: (Role[U, V]) => Unit) {
    breakable {
      for (role <- this.roles) {
        visitor(role)
      }
    }
  }
}

private[veryfay] final case class AllowRoleSet[U: TypeTag, V: TypeTag](roles: Seq[Role[U, V]]) extends RoleSet[U, V]
private[veryfay] final case class DenyRoleSet[U: TypeTag, V: TypeTag](roles: Seq[Role[U, V]]) extends RoleSet[U, V]  
