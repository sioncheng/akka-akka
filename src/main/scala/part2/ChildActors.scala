package part2

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.Terminated
import part2.Parent.CreateChild
import part2.Parent.WatchChild
import part2.Parent.TellChild
import part2.Parent.StopChild

object Child {
  def apply(): Behavior[String] =
    Behaviors.receive {(ctx, msg) => 
        ctx.log.info(s"[${ctx.self.path.name}] received $msg")
        Behaviors.same
    }    
}

object Parent {
    sealed trait Command
    case class CreateChild(name: String) extends Command
    case class TellChild(name: String, message: String) extends Command
    case class WatchChild(name: String) extends Command
    case class StopChild(name: String) extends Command

    def apply(): Behavior[Command] = active(Map())

    def active(children: Map[String, ActorRef[String]]): Behavior[Command] =
        Behaviors.receive[Command] {(ctx,msg) =>
            msg match {
                case CreateChild(name) => 
                    children.get(name) match {
                        case None => ctx.log.info(s"[parent] creating child '$name'")
                        val child = ctx.spawn(Child(), name)
                        active(children + (name -> child))
                    case Some(value) =>
                        ctx.log.info(s"[parent] child '$name' has already been created")
                        Behaviors.same
                    }
                case StopChild(name) => 
                    val child = children.get(name)
                    child match {
                        case None => 
                            ctx.log.info(s"[parent] child '$name' could not be found")
                        case Some(value) => 
                            ctx.stop(value)
                    }
                    Behaviors.same
                case WatchChild(name) => 
                    val child = children.get(name)
                    child.fold(ctx.log.info(s"[parent] child '$name' could not be found"))(ctx.watch)
                    Behaviors.same
                case TellChild(name, message) =>
                    val child = children.get(name)
                    child.fold(ctx.log.info(s"[parent] child '$name' could not be found"))(c => c ! message)
                    Behaviors.same
            }
        }.receiveSignal {
            case (ctx, Terminated(ref)) =>
                val name = ref.path.name
                ctx.log.info(s"[parent] child $name ${ref.path} was stopped")
                active(children - name)
        }

}

object ChildActors extends App {

  val system = ActorSystem(Behaviors.empty, "ActorSystem")

  val child = system.systemActorOf(Child(), "child")
  child ! "abc"

  val parent = system.systemActorOf(Parent(), "parent")
  parent ! CreateChild("c1")
  parent ! CreateChild("c1")
  parent ! WatchChild("c1")
  parent ! WatchChild("c2")
  parent ! CreateChild("c2")
  parent ! WatchChild("c3")
  parent ! TellChild("c1", "hi")
  parent ! TellChild("c2", "hello")
  parent ! TellChild("c3", "chao")
  parent ! StopChild("c1")
  parent ! StopChild("c2")

  Thread.sleep(1000)
  system.terminate()
}
