package com.github.florinn.veryfay

trait Role[U, V] {
  def contains(principal: U, extraInfo: Option[V] = None): Boolean
}