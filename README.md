Principled
==========

Principled is a thin add-on to [ScalaCheck](http://www.scalacheck.org/) for (algebra-like) law checking. It is an alternative to [discipline](https://github.com/typelevel/discipline).


Purpose
-------

The purpose of Principled is to allow law inheritance. This is common in algebraic structures: monoid inherits all laws of semigroup, and adds some more. In addition, we want to avoid checking the same law multiple times in case of diamond inheritance.

Approach
--------

Principled defines `LawSet`, a collections of laws (a law is just a named ScalaCheck property). A `LawSet` can have zero or more _base_ `LawSet`s. A `LawSet` inherits all of the laws from all of its bases, but removes duplicates.

Example
-------

```scala
case class SemigroupLaws[A: Arbitrary](S: Semigroup[A]) extends LawSet("Semigroup") {
  implicit def semigroup = S

  override val bases = Seq() // Semigroup does not inherit any laws

  // define Semigroup laws
  override val props = Seq(
    "associativity" -> forAll((a: A, b: A, c: A) =>
        ((a |+| b) |+| c) == (a |+| (b |+| c)))
  )
}

case class MonoidLaws[A: Arbitrary](M: Monoid[A]) extends LawSet("Monoid") {
  implicit def monoid = M

  override val bases = Seq("semigroup" -> SemigroupLaws(M)) // inherit Semigroup laws

  // add some more laws
  override val props = Seq(
    "leftIdentity" -> forAll((a: A) =>
      (M.zero |+| a) == a),
    "rightIdentity" -> forAll((a: A) =>
      (a |+| M.zero) == a))
}

case class OrderLaws[A: Arbitrary](O: Order[A]) extends LawSet("Order") {
  implicit def order = O

  override def bases = Seq() // Order does not inherit any laws

  // define Order laws
  override val props = Seq(
    "reflexivity" -> forAll((a: A) =>
      a lte a),
    "antisymmetry" -> forAll((a: A, b: A) =>
      (a cmp b) == (b cmp a).complement),
    "transitivity" -> forAll((a: A, b: A, c: A) => {
      if(a lte b)
        (b lte c) ==> (a lte c)
      else // a > b
        (b gte c) ==> (a gte c)
    }))
}

case class OrderedSemigroupLaws[A: Arbitrary](S: OrderedSemigroup[A])
extends LawSet("OrderedSemigroup") {
  implicit def orderedSemigroup = S

  override val bases = Seq(
    "semigroup" -> SemigroupLaws[A](S), // inherit Semigroup laws
    "order"     -> OrderLaws[A](S))     // inherit Order laws

  // add some more laws
  override val props = Seq(
    "leftCompatibility" -> forAll((a: A, b: A, c: A) =>
      (a cmp b) == ((c |+| a) cmp (c |+| b))),
    "rightCompatibility" -> forAll((a: A, b: A, c: A) =>
      (a cmp b) == ((a |+| c) cmp (b |+| c))))
}

case class OrderedMonoidLaws[A: Arbitrary](M: OrderedMonoid[A]) extends LawSet("OrderedMonoid") {

  override val bases = Seq(
    "orderedSemigroup" -> OrderedSemigroupLaws(M), // inherit OrderedSemigroup laws
    "monoid"           -> MonoidLaws(M))           // inherit Monoid laws

  // no additional laws
  override def props = Seq()
}
```

Note that `OrderedMonoidLaws` inherit `SemigroupLaws` twice: once via `OrderedSemigroupLaws` and once via `MonoidLaws`. However, Principled makes sure that `SemigroupLaws` will be checked only once.

Testing `OrderMonoidLaws` of a particular instance of `OrderedMonoid`:

```scala
object LawTests extends org.scalacheck.Properties("Laws") {

  val myOrderedMonoid: OrderedMonoid[A] = ???

  include(OrderedMonoidLaws(myOrderedMonoid).all)

}
```


Comparison to Discipline
------------------------

This project was motivated by the following shortcomings of Discipline.

Discipline introduces quite a bit of _complexity_ in organizing the law inheritance hierarchy:
 - There are two types of ancestors: _parent_ and _base_, with different inheritance semantics. With such _complex inheritance semantics_, ensuring each law is checked exactly once likely requires you to know the whole inheritance hierarchy, thus defying local reasoning and being fragile with respect to future changes in the hierarchy.
 - You are required you to classify `RuleSet`s into _kinds_. Such classification is often _unnatural_. For example, [algebra](https://github.com/non/algebra) defines an _Order_ kind and a _Group_ kind. `OrderedSemigroupLaws` above does not fall into either of those kinds (more precisely, it falls equally well into both kinds).
 - _Not modular:_ Extending kinds is not robust, since property names within a kind have to be unique.
 - As a result of the above, your best bet to make sure you don't miss any law is to only use _base_ inheritance. That, however, does not avoid duplicates.

Principled does not try to come up with a clever way of structuring your type classes/laws in order to avoid duplicates. Instead, it relies on testing equality of `LawSet`s. Notice in the example above that law sets are defined as case classes, which give a proper implementation of `==`.

Try it out
----------

1. **Publish locally**
    ```sh
    git clone https://github.com/TomasMikula/Principled.git
    cd Principled
    sbt publish-local
    ```

2. **Add to dependencies**
    ```scala
    libraryDependencies ++= Seq(
      "org.principled" %% "principled" % "0.1-SNAPSHOT"
    )
    ```
