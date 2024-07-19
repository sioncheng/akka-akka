package part2

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.PostStop
import akka.actor.typed.ActorSystem

object StoppingActors {
  
    object SensitiveActor {
        def apply(): Behavior[String] =
            Behaviors.receive[String] {(ctx, msg) =>
                ctx.log.info(s"received $msg")
                if (msg == "shutup")
                    Behaviors.stopped
                else
                    Behaviors.same
            }
            .receiveSignal {
                case (ctx, PostStop) =>
                    ctx.log.info(s"stopping")
                    Behaviors.same
            }
    }

    def main(args: Array[String]): Unit = {
        val guardian = Behaviors.setup[Unit] {ctx =>
            val sensitiveActor =
                ctx.spawn(SensitiveActor(), "sensitiveActor")
            
            sensitiveActor ! "Hi"
            sensitiveActor ! "How are you"
            sensitiveActor ! "shutup"

            Behaviors.same
        }

        val system = ActorSystem(guardian, "ActorSystem")
        Thread.sleep(1000)
        system.terminate()
    }

}
