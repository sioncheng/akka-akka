package part2

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.SupervisorStrategy
import akka.actor.TypedActor.PreStart
import akka.actor.typed.PreRestart

object Supervision {
  object FussyWordCounter {

    def apply(): Behavior[String] = active()
        

    def active(total: Int = 0): Behavior[String] =
        Behaviors.receive[String] {(ctx, msg) =>
            val wordCount = msg.split(" ").length
            ctx.log.info(s"wordCount $wordCount")
            if (msg.startsWith("Q")) throw new RuntimeException("no Q!")
            if (msg.startsWith("W")) throw new NullPointerException
            
            active(total + wordCount)
        }.receiveSignal {
            case (ctx, PreRestart) =>
                ctx.log.info(s"### pre restart")
                Behaviors.same
        }
  }

  def main(args: Array[String]): Unit = {
    val guardian: Behavior[Unit] =
        Behaviors.setup[Unit] {ctx =>
             val sup1 = Behaviors.supervise(FussyWordCounter())
                .onFailure[RuntimeException](SupervisorStrategy.restart)
                //.onFailure[NullPointerException](SupervisorStrategy.resume)
            val supervision = Behaviors.supervise(sup1)
                .onFailure[NullPointerException](SupervisorStrategy.resume)


            val fussyCounter = ctx.spawn(supervision, "fussyCounter")

            fussyCounter ! "Starting to understand this Akka business..."
            fussyCounter ! "Quick! Hide!"
            fussyCounter ! "Are you there?"

            Behaviors.same
        }
    
   
    val system = ActorSystem(guardian, "ActorSystem")
    
    Thread.sleep(1000)
    system.terminate()
  }


}
