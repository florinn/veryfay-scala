package com.github.florinn.veryfay.test

import scala.util.{Try, Success, Failure}

object Helpers {

  def getMsg(input: Try[String]): String = input match {
    case Success(msg) => msg
    case Failure(e) => e.getMessage
  } 
}