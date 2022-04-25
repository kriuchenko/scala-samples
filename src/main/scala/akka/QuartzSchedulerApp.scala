package akka

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import com.typesafe.config.ConfigFactory

import java.util.Date

case object ScheduleEvent

object QuartzSchedulerApp extends App{
  class ScheduledActor extends Actor{
    override def receive: Receive = {
      case ScheduleEvent => println(s"ScheduleEvent received at ${new Date()}")
    }
  }

  val conf = ConfigFactory.parseString("""akka.quartz.schedules.Every5Seconds.expression = "0/5 * * ? * *" """)
  val system = ActorSystem(name = "test", config = conf)
  val scheduledActor = system.actorOf(Props[ScheduledActor])
  val scheduler = QuartzSchedulerExtension(system)
  val scheduledAt: Date = scheduler.schedule("Every5Seconds", scheduledActor, ScheduleEvent)
  println(s"Scheduled at $scheduledAt")
}
