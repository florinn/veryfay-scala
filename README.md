Veryfay [![build badge](https://travis-ci.org/florinn/veryfay-scala.svg?branch=master)](https://travis-ci.org/florinn/veryfay-scala)
===================

**Veryfay** is a library for doing **activity based authorization** in Scala.

It deals with three things: 
- defining activities and roles
- who is allowed (or denied) to perform activities 
- checking that activities can be performed for certain input data

----------


Overview
-------------
When it comes to building a security system, probably the favoured approach is the so called **role based security (RBA)**. 
The idea is fairly simple, users are assigned roles and in turn the roles have associated permissions (or activities).

While it appears like a good idea at first and it certainly works well if the number of roles is limited to just a few, it may get quickly very complicated when the number of roles increases.   
An apparent drawback is that it becomes difficult to determine all the roles allowed (or denied) for a certain activity.

The alternative is called **activity based authorization (ABA)** and it is the role based security turned on its head, if you will.
It solves the aforementioned drawback by making activity the central concept of the security mechanism.

###### Terminology

- *activity*: aka *permission*, it is the combination btw action (e.g *Read*) and an optional target (e.g. *SomeClass*)
- *role*: a logical grouping of principal types, defined either statically or dynamically
- *principal*: an entity that can be identified and verified

Define authorization rules|
---|
>**RBA**  
`role > activities`

>**ABA**   
`activity > roles`

RBA works by associating activities to each role while ABA associates roles to each activity.

Verify authorization rules|
---|
>**RBA**  
`input principal > roles > activities` & `input activity`

>**ABA**   
*a.* `input activity > roles > principals` & `input principal`   
*b.* `input activity > roles` & `input principal > roles`

When checking for authorization, RBA starts from the specified principal, determines associated roles and intersects their activity sets with the input activity.

ABA mirrors that process, it starts from the specified activity, determines associated roles and intersects their principal sets with the input principal (*case a* shown above).   
*Case b* shows the more practical approach, it starts from the specified activity and determines its associated roles then determines the roles associated to the input principal and intersects the two sets of roles.


Features
-------------
* Define multiple authorization engines in the same application
* Define activities with or without a target class
* Specify allow or deny sets
* Associate roles to multiple activities through hierarchical activity containers
* Check authorization either by returning boolean or exception throwing


Installing
-------------
Add this dependency to your *build.sbt* file: 

```scala
libraryDependencies += "com.github.florinn" % "veryfay" % "0.1"
```

Or add this Maven dependency to your build:

```xml
<dependencies>
    <dependency>
        <groupId>com.github.florinn</groupId>
        <artifactId>veryfay</artifactId>
        <version>0.1</version>
    </dependency>
</dependencies>
```


Usage
-------------

### Define authorization rules

This part consists of a few straightforward preparatory operations that culminate with the creation of an "authorization engine" to be used later to perform authorization verification.

##### Define any custom activities

An activity takes a type parameter describing the target for the activity, which may be any class defined in your application.

For activities with no target, you may omit the type parameter.

There are a few predefined activities: 
- *Create*
- *Read*
- *Update*
- *Patch*
- *Delete*

You may define your own activities by inheriting from `Activity[T]`:

```scala
final case class SomeActivity[T: TypeTag]() extends Activity[T]
```

##### Define any container activities

Container activities help with associating multiple actions to the same role(s).  
Instead of repeating the same activities over and over again, a container activity may be defined holding a list of activities (including container activities).

There a couple predefined container activities:
- *CRUD* containing activities: *Create*, *Read*, *Update*, *Delete*
- *CRUDP* containing activities: *CRUD*, *Patch*

Define your own container activities like so:

```scala
final case class SomeContainerActivity[T: TypeTag]() extends Activity[T] with Container[T] {
  val activities = List(SomeActivity[T], OtherActivity[T], SomeOtherActivity[T])
}
```

>**Note:** Container activities are used only for defining authorization rules, they are not used when verifying authorization rules

##### Define roles

You may define a role by inheriting from `Role[U, V]`, where
- *U* is the type of the principal class passed into the role definition 
- *V* is the type of any extra info that may get passed into the role definition

In `contains` you can place any logic to determine if the input data belongs to that role.

```scala
object SomeRole extends Role[SomePrincipalClass, SomeClass] {
    def contains(principal: SomePrincipalClass, extraInfo: Option[SomeClass]): Boolean = {
      // Some logic to determine if input belongs to the role
    }
  }
```

##### Configure authorization rules 

You may use the `register`, `allow` and `deny` triplet to associate any allow and deny roles with one or more activities in the context of an authorization engine:

```scala
val ae = new AuthorizationEngine {
        register(CRUDP()).allow(Admin).deny(Supervisor, Commiter)
        register(CRUDP[SomeOtherClass]).allow(Admin).allow(Supervisor)
        register(Create()).allow(Commiter).deny(Contributor)
        register(Read()).allow(Commiter).deny(Contributor).allow(Reviewer)
        register(Read[SomeClass]).allow(Supervisor, Commiter)
        register(Read[SomeClass], Read[SomeOtherClass]).allow(Supervisor)
        register(Read[SomeClass]).allow(Reader)
        register(Read[OtherSomeOtherClass]).allow(Reader).deny(Commiter)
      }
```
>**Notes:** 
- Roles specified in the same argument list of `allow` or `deny` are bound together by logical *AND*
- Roles specified in separate argument lists of `allow` or `deny` are bound together by logical *OR*


### Verify authorization rules

To verify the authorization rules you may call either: 

- `isAllowing` which returns `Try[String]` containing a string in case of success, otherwise an instance of `AuthorizationException`

```scala
val result = ae(Read[SomeOtherClass]).isAllowing(OtherPrincipalClass("reader"), Option(1234, "1234"))
```
 
- `verify` which returns `String` in case of success, otherwise throwing `AuthorizationException`

```scala
ae(Read[SomeOtherClass]).verify(OtherPrincipalClass("reader"), Option(1234, "1234"))
```

>**Notes:**
- The string returned by the two methods above contains information about the execution of the authorization rules, especially useful for debug scenarios 
- During rules verification, role definitions are matched both by activity type and by the types of the arguments (for principal and optionally extra info) which are passed in to `isAllowing` or `verify`