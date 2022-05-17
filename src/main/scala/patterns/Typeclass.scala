package patterns


object Typeclass extends App{
  trait Show[A] {
    def show(a: A): String
  }

  object Show {
    def apply[A](a: A)(implicit show: Show[A]): String = show.show(a)
  }

  implicit val showString: Show[String] = {s: String => s}
  implicit val showInt: Show[Int] = {i: Int => i.toString}
  implicit def showList[T : Show]: Show[List[T]] = {list: List[T] =>
    list.map{Show(_)}.mkString("List(", ",", ")")
  }

  implicit class ShowSyntax[T](val t: T) extends AnyVal {
    def show(implicit instance: Show[T]): String = instance.show(t)
  }

  println(List(1,2,3,4,5,6).show)
}
