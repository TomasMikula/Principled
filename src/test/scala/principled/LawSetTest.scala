package principled

import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION
import org.scalacheck.Properties
import org.scalacheck.Prop

object LawSetTest extends Properties("LawSet") {

  private def dummyProp = Prop.passed

  property("sanity") = {

    val foo = new LawSet("Foo") {
      def bases = Seq()
      def props = Seq("foo1" -> dummyProp)
    }

    val bar = new LawSet("Bar") {
      def bases = Seq("base" -> foo)
      def props = Seq("bar1" -> dummyProp)
    }

    val baz = new LawSet("Baz") {
      def bases = Seq("base" -> foo)
      def props = Seq("baz1" -> dummyProp)
    }

    val qux = new LawSet("Qux") {
      def bases = Seq("bar" -> bar, "baz" -> baz)
      def props = Seq("qux1" -> dummyProp)
    }

    val all = qux.all.properties.map(_._1.split('.').last)

    Prop.all(
      "foo1 included" |: Prop(all.contains("foo1")),
      "no redundancy" |: Prop(all.count(_ == "foo1") <= 1),
      "overall"       |: Prop(all.toSet == Set("foo1", "bar1", "baz1", "qux1"))
    )
  }
}