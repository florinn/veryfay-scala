package com.github.florinn.veryfay

import scala.reflect.runtime.universe.TypeTag
import scala.collection.mutable.ListBuffer

class PermissionSet {
  private[veryfay] var roleSets = ListBuffer.empty[RoleSet[_, _]]

  def allow[U: TypeTag, V: TypeTag](role: Role[U, V], moreRoles: Role[U, V]*): PermissionSet = {
    roleSets += AllowRoleSet(role :: moreRoles.toList)
    this
  }

  def deny[U: TypeTag, V: TypeTag](role: Role[U, V], moreRoles: Role[U, V]*): PermissionSet = {
    roleSets += DenyRoleSet(role :: moreRoles.toList)
    this
  }
}

