package part5

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem

object StashDemo {
  sealed trait Command
  case object Open extends Command
  case object Close extends Command
  case object Read extends Command
  case class Write(data: String) extends Command

  object ResourceActor {
    def apply(): Behavior[Command] = closed("42")

    def closed(data: String): Behavior[Command] = Behaviors.withStash(128) {buffer =>
      Behaviors.receive {(ctx, msg) =>
        msg match {
            case Open => 
                ctx.log.info("Opening Resource")
                buffer.unstashAll(open(data))
            case _ =>
                ctx.log.info(s"stashing $msg because the resource is closed")
                buffer.stash(msg)
                Behaviors.same
        }
      }
    }

    def open(data: String): Behavior[Command] = Behaviors.receive{(ctx, msg) =>
      msg match {
        case Read => 
            ctx.log.info(s"I have read $data")
            Behaviors.same
        case Write(newData) => 
            ctx.log.info(s"I have written $newData")
            open(newData)
        case Close =>
            ctx.log.info(s"Closing Resource")
            closed(data)
        case _ =>
            ctx.log.info(s"$msg not supported while resource is open")
            Behaviors.same
            
      }
      
    }
  }

  def main(args: Array[String]): Unit = {
    val guardian = Behaviors.setup[Unit] { ctx =>
      val resourceAcotr = ctx.spawn(ResourceActor(), "resource")

      resourceAcotr ! Read
      resourceAcotr ! Open
      resourceAcotr ! Open
      resourceAcotr ! Write("I love stash")
      resourceAcotr ! Write("This is pretty cool")
      resourceAcotr ! Read
      resourceAcotr ! Read
      resourceAcotr ! Close
      resourceAcotr ! Read

      Behaviors.empty
    }

    val system = ActorSystem(guardian, "demoStash")

    Thread.sleep(2000)

    system.terminate()
  }

}
