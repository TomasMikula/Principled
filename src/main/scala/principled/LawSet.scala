package principled

import org.scalacheck.Prop
import org.scalacheck.Properties

abstract class LawSet(val name: String) {
  def bases: Seq[(String, LawSet)]
  def props: Seq[(String, Prop)]

  final def all: Properties = new Properties(name) {
    for {
      (laws, path) <- allLawSets
      (name, prop) <- laws.props
    } property(path + name) = prop
  }


  private type Path = List[String]
  def root: Path = Nil

  private lazy val allLawSets: Map[LawSet, String] =
    collect(root)
      .groupBy(_._1)
      .mapValues(_ map { _._2.mkString(".") })
      .mapValues(_.sorted)
      .mapValues {
        case Seq("") => ""
        case Seq(x)  => x + "."
        case seq     => seq.mkString("{", ", ", "}.")
      }

  private def collect(prefix: Path): Seq[(LawSet, Path)] =
    Seq((this, prefix)) ++ bases.flatMap { case (name, base) =>
      base.collect(prefix :+ s"${name}:${base.name}") }
}