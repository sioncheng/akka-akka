package part4

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._
import akka.actor.typed.ActorSystem
import akka.actor.Cancellable
import part4.Schedulers.ResettingTimeoutActor.resttingTimeoutActor

object Schedulers {
  object LoggerActor{
    def apply(): Behavior[String] =
        Behaviors.receive[String] { (ctx, msg) =>
            ctx.log.info(s"[${ctx.self.path}] received: $msg")
            Behaviors.same
        }
  }

  def demoScheduler(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { ctx =>
      val loggerActor = ctx.spawn(LoggerActor(), "loggerActor")

      ctx.log.info(s"[system] starting")
      ctx.scheduleOnce(1.second, loggerActor, "1second")

      Behaviors.same
    }

    val system = ActorSystem(userGuardian, "demoScheduler")
    Thread.sleep(2000)
    system.terminate()
  }

  object ResettingTimeoutActor {
    def apply(): Behavior[String] = Behaviors.setup { (ctx) =>
      resttingTimeoutActor(ctx.scheduleOnce(1.second, ctx.self, "timeout"))
    }
    def resttingTimeoutActor(schedule: Cancellable): Behavior[String] =
      Behaviors.receive {(ctx, msg) =>
      msg match {
        case "timeout" =>
            ctx.log.info("stopping")
            Behaviors.stopped
        case _: String => 
            ctx.log.info(s"[${ctx.self.path}] received $msg")
            schedule.cancel()
            resttingTimeoutActor(ctx.scheduleOnce(1.second, ctx.self, "timeout"))
      }
    }
  }

  def demoActorTimeout(): Unit = {
    val guardian = Behaviors.setup[Unit] {ctx =>
        val resetter = ctx.spawn(ResettingTimeoutActor(), "resetter")

        resetter ! "start"
        Thread.sleep(500)
        resetter ! "reset"
        Thread.sleep(700)
        resetter ! "this should still be visible"
        Thread.sleep(1200)
        resetter ! "this should NOT be visible"

        Behaviors.empty
    } 

    val system = ActorSystem(guardian, "demoActorTimeout")
    Thread.sleep(4000)
    system.terminate()
  }

  def main(args: Array[String]): Unit = {
    //demoScheduler()
    demoActorTimeout()
  }
}
