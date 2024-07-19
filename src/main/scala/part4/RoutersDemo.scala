package part4

import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Routers
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem

object RoutersDemo {

  object Logger {
    def apply(): Behavior[String] = 
        Behaviors.receive[String] {(ctx, msg) =>
            val name = ctx.self.path.name
            ctx.log.info(s"[logger $name] $msg")
            Behaviors.same
        }
  }  
  def demoGroupRouter(): Unit = {
    val serviceKey = ServiceKey[String]("logWorker")

    val userGuardian = Behaviors.setup[Unit] {ctx =>
      val workers = (1 to 5).map(i => ctx.spawn(Logger(), s"worker$i"))
      workers.foreach(worker => ctx.system.receptionist ! Receptionist.Register(serviceKey, worker))

      val groupBahavior = Routers.group[String](serviceKey).withRoundRobinRouting()
      val groupRouter = ctx.spawn(groupBahavior, "workerGroup")

      (1 to 10).foreach(i => groupRouter ! s"work task $i")

      Thread.sleep(1000)
      ctx.log.info("================")

      val extraWorker = ctx.spawn(Logger(), "extraWorker")
      ctx.system.receptionist ! Receptionist.Register(serviceKey, extraWorker)
      Thread.sleep(1000)
      (1 to 10).foreach(i => groupRouter ! s"work task $i")

      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "DemoGroupRouter")
    Thread.sleep(2000)
    system.terminate()
  }

  def main(args: Array[String]): Unit = {
    demoGroupRouter()
  }
}
