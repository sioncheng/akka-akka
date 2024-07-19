package part4

import akka.actor.typed.scaladsl.Behaviors

import part4.AkkaConfiguration.SimpileLoggingActor
import akka.actor.typed.DispatcherSelector
import scala.util.Random
import akka.actor.typed.ActorSystem

object DispatchersDemo {
  def demoDispatcherConfig(): Unit = {
    val guardian = Behaviors.setup[Unit] {ctx =>
        // val childActorDispatcherDetaul =
        //     ctx.spawn(SimpileLoggingActor(), "childActorDispatcherDefault", DispatcherSelector.default())
        
        val actors = (1 to 10).map(i =>
            ctx.spawn(SimpileLoggingActor(), s"child$i", DispatcherSelector.fromConfig("my-dispatcher"))    
        )

        val r = new Random()
        (1 to 1000).foreach(i => actors(r.nextInt(10)) ! s"task$i")

        Behaviors.empty
    }

    val system = ActorSystem(guardian, "demoDispatchers")
    Thread.sleep(2000)
    system.terminate()

    }

  def main(args: Array[String]): Unit = {
    demoDispatcherConfig()
  }
}
