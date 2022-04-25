package akka

import akka.actor.{Actor, ActorSystem, Props}

object SimpleActorApp extends App{

  class HelloActor extends Actor{
    override def receive: Receive = {
      case s: String => println(s"Received a string $s")
      case i: Int => println(s"Received an int $i")
      case smth =>
        Thread.sleep(5000)
        println(s"Received something $smth")
        system.terminate()
    }
  }

  val system = ActorSystem(name = "test")
  val helloActor = system.actorOf(Props[HelloActor])
  helloActor ! "aString"
  helloActor ! 123
  helloActor ! helloActor
}
