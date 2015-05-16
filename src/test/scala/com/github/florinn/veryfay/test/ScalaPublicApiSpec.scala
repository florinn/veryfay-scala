package com.github.florinn.veryfay.test

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import com.github.florinn.veryfay.Role
import com.github.florinn.veryfay.AuthorizationEngine
import com.github.florinn.veryfay.Read
import com.github.florinn.veryfay.AuthorizationException
import com.github.florinn.veryfay.CRUD
import com.github.florinn.veryfay.CRUDP
import com.github.florinn.veryfay.Patch
import com.github.florinn.veryfay.Delete
import com.github.florinn.veryfay.Create

class ScalaPublicApiSpec extends FunSpec with BeforeAndAfter {

  class SomeClass
  class SomeOtherClass
  class OtherSomeOtherClass

  case class PrincipalClass(username: String)
  case class OtherPrincipalClass(username: String)

  object Admin extends Role[PrincipalClass, Int] {
    def contains(principal: PrincipalClass, extraInfo: Option[Int]): Boolean = {
      if (principal.username == "admin")
        true
      else
        false
    }
  }
  object Supervisor extends Role[PrincipalClass, String] {
    def contains(principal: PrincipalClass, extraInfo: Option[String]): Boolean = {
      if (principal.username == "supervisor" ||
        principal.username == "supervisor-commiter")
        true
      else
        false
    }
  }
  object Commiter extends Role[PrincipalClass, String] {
    def contains(principal: PrincipalClass, extraInfo: Option[String]): Boolean = {
      if (principal.username == "commiter" ||
        principal.username == "supervisor-commiter")
        true
      else
        false
    }
  }
  object Contributor extends Role[OtherPrincipalClass, Int] {
    def contains(principal: OtherPrincipalClass, extraInfo: Option[Int]): Boolean = {
      if (principal.username == "contributor" ||
        principal.username == "contributor-reader")
        true
      else
        false
    }
  }
  object Reviewer extends Role[PrincipalClass, Int] {
    def contains(principal: PrincipalClass, extraInfo: Option[Int]): Boolean = {
      if (principal.username == "contributor" ||
        principal.username == "commiter")
        true
      else
        false
    }
  }
  object Reader extends Role[OtherPrincipalClass, (Int, String)] {
    def contains(principal: OtherPrincipalClass, extraInfo: Option[(Int, String)]): Boolean = {
      if (principal.username == "reader" &&
        extraInfo.isDefined && extraInfo.get._1 == 1234)
        true
      else
        false
    }
  }

  private var ae: AuthorizationEngine = null

  describe("activity") {
    before {
      ae = new AuthorizationEngine {
        register(CRUDP()).allow(Admin).deny(Supervisor, Commiter).deny(Contributor)
        register(CRUDP[SomeOtherClass]).allow(Admin).allow(Supervisor).allow(Reader).allow(Contributor)
        register(Create()).allow(Commiter).deny(Contributor)
        register(Read()).allow(Commiter).deny(Contributor).allow(Reviewer)
        register(Read[SomeClass]).allow(Supervisor, Commiter)
        register(Read[SomeClass], Read[SomeOtherClass]).allow(Supervisor).allow(Contributor).deny(Reader)
        register(Read[SomeClass]).allow(Reader)
        register(Read[OtherSomeOtherClass]).allow(Reader).deny(Commiter).allow(Reviewer)
      }
    }

    describe("when action target not found") {
      it("should fail") {
        val result = ae(Create[SomeClass]).isAllowing(PrincipalClass("commiter"))
        intercept[AuthorizationException] { ae(Create[SomeClass]).verify(PrincipalClass("commiter")) }
        assert(result.isFailure, Helpers.getMsg(result))
      }
    }
    describe("when action target found") {
      it("should fail when target type not matching") {
        val result = ae(Read[OtherSomeOtherClass]).isAllowing(PrincipalClass("supervisor"))
        intercept[AuthorizationException] { ae(Read[OtherSomeOtherClass]).verify(PrincipalClass("supervisor")) }
        assert(result.isFailure, Helpers.getMsg(result))
      }
      describe("when deny role found") {
        describe("once") {
          it("should fail when principal match the deny role definition") {
            val result = ae(Read()).isAllowing(OtherPrincipalClass("contributor"))
            intercept[AuthorizationException] { ae(Read()).verify(OtherPrincipalClass("contributor")) }
            assert(result.isFailure, Helpers.getMsg(result))
          }
          it("should succeed when principal does not match every deny role definition in a set") {
            val result = ae(Create()).isAllowing(PrincipalClass("commiter"))
            ae(Create()).verify(PrincipalClass("commiter"))
            assert(result.isSuccess, Helpers.getMsg(result))
          }
          it("should fail when principal match every deny role definition in a set") {
            val result = ae(Create()).isAllowing(PrincipalClass("supervisor-commiter"))
            intercept[AuthorizationException] { ae(Create()).verify(PrincipalClass("supervisor-commiter")) }
            assert(result.isFailure, Helpers.getMsg(result))
          }
          it("should fail when principal and extra info match the type of the deny role defintion") {
            val result = ae(Read[SomeOtherClass]).isAllowing(OtherPrincipalClass("reader"), Option(1234, "1234"))
            intercept[AuthorizationException] { ae(Read[SomeOtherClass]).verify(OtherPrincipalClass("reader"), Option(1234, "1234")) }
            assert(result.isFailure, Helpers.getMsg(result))
          }
          it("should succeed when principal type does not match the type of the deny role definition") {
            val result = ae(Read()).isAllowing(PrincipalClass("contributor"))
            ae(Read()).verify(PrincipalClass("contributor"))
            assert(result.isSuccess, Helpers.getMsg(result))
          }
          it("should succeed when extra info type does not match the type of the deny role definition") {
            val result = ae(Read[OtherSomeOtherClass]).isAllowing(PrincipalClass("commiter"), Option(1234))
            ae(Read[OtherSomeOtherClass]).verify(PrincipalClass("commiter"), Option(1234))
            assert(result.isSuccess, Helpers.getMsg(result))
          }
        }
        describe("more than once") {
          it("should fail when principal and extra info match any deny role definition") {
            val result = ae(Read()).isAllowing(OtherPrincipalClass("contributor"))
            intercept[AuthorizationException] { ae(Read()).verify(OtherPrincipalClass("contributor")) }
            assert(result.isFailure, Helpers.getMsg(result))
          }
          it("should fail when principal and extra info match any contained deny role definition") {
            val result = ae(Patch()).isAllowing(OtherPrincipalClass("contributor"))
            intercept[AuthorizationException] { ae(Patch()).verify(OtherPrincipalClass("contributor")) }
            assert(result.isFailure, Helpers.getMsg(result))
          }
          it("should fail when principal and extra info match any deny role definition in an embedded container action") {
            val result = ae(Delete()).isAllowing(OtherPrincipalClass("contributor"))
            intercept[AuthorizationException] { ae(Delete()).verify(OtherPrincipalClass("contributor")) }
            assert(result.isFailure, Helpers.getMsg(result))
          }
        }
      }
      describe("when deny role not found") {
        describe("when allow role not found") {
          it("should fail") {
            val result = ae(Read[SomeClass]).isAllowing(PrincipalClass("laura"))
            intercept[AuthorizationException] { ae(Read[SomeClass]).verify(PrincipalClass("laura")) }
            assert(result.isFailure, Helpers.getMsg(result))
          }
        }
        describe("when allow role found") {
          describe("once") {
            it("should succeed when principal match the allow role definition") {
              val result = ae(Read[SomeOtherClass]).isAllowing(OtherPrincipalClass("contributor"))
              ae(Read[SomeOtherClass]).verify(OtherPrincipalClass("contributor"))
              assert(result.isSuccess, Helpers.getMsg(result))
            }
            it("should fail when principal does not match every allow role definition in a set") {
              val result = ae(Read[SomeClass]).isAllowing(PrincipalClass("commiter"))
              intercept[AuthorizationException] { ae(Read[SomeClass]).verify(PrincipalClass("commiter")) }
              assert(result.isFailure, Helpers.getMsg(result))
            }
            it("should succeed when principal does match every allow role definition in a set") {
              val result = ae(Read[SomeClass]).isAllowing(PrincipalClass("supervisor-commiter"))
              ae(Read[SomeClass]).verify(PrincipalClass("supervisor-commiter"))
              assert(result.isSuccess, Helpers.getMsg(result))
            }
            it("should succeed when principal and extra info match the type of the allow role defintion") {
              val result = ae(Read[OtherSomeOtherClass]).isAllowing(OtherPrincipalClass("reader"), Option(1234, "1234"))
              ae(Read[OtherSomeOtherClass]).verify(OtherPrincipalClass("reader"), Option(1234, "1234"))
              assert(result.isSuccess, Helpers.getMsg(result))
            }
            it("should fail when principal type does not match the type of the allow role definition") {
              val result = ae(Read[SomeOtherClass]).isAllowing(PrincipalClass("reader"), Option(1234, "1234"))
              intercept[AuthorizationException] { ae(Read[SomeOtherClass]).verify(PrincipalClass("reader"), Option(1234, "1234")) }
              assert(result.isFailure, Helpers.getMsg(result))
            }
            it("should fail when extra info type does not match the type of the allow role definition") {
              val result = ae(Read[SomeOtherClass]).isAllowing(OtherPrincipalClass("reader"), Option("1234"))
              intercept[AuthorizationException] { ae(Read[SomeOtherClass]).verify(OtherPrincipalClass("reader"), Option("1234")) }
              assert(result.isFailure, Helpers.getMsg(result))
            }
          }
          describe("more than once") {
            it("should succeed when principal and extra info match any allow role definition") {
              val result = ae(Read[SomeClass]).isAllowing(PrincipalClass("supervisor"))
              ae(Read[SomeClass]).verify(PrincipalClass("supervisor"))
              assert(result.isSuccess, Helpers.getMsg(result))
            }
            it("should fail when principal and extra info do not match the param types of any allow role definition") {
              val result = ae(Read[SomeClass]).isAllowing(OtherPrincipalClass("supervisor"))
              intercept[AuthorizationException] { ae(Read[SomeClass]).verify(OtherPrincipalClass("supervisor")) }
              assert(result.isFailure, Helpers.getMsg(result))
            }
            it("should succeed when principal and extra info match any contained allow role definition") {
              val result = ae(Patch[SomeOtherClass]).isAllowing(PrincipalClass("admin"))
              ae(Patch[SomeOtherClass]).verify(PrincipalClass("admin"))
              assert(result.isSuccess, Helpers.getMsg(result))
            }
            it("should succeed when principal and extra info match any allow role definition in an embedded container action") {
              val result = ae(Delete[SomeOtherClass]).isAllowing(PrincipalClass("admin"))
              ae(Delete[SomeOtherClass]).verify(PrincipalClass("admin"))
              assert(result.isSuccess, Helpers.getMsg(result))
            }
          }
        }
      }
    }
  }
}