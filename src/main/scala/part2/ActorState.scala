package part2

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem

object ActorState {
  
  //state actor
  object WordCounter {
     def apply(): Behavior[String] = Behaviors.setup {ctx =>
      var total = 0

      Behaviors.receiveMessage(msg => {
        val words = msg.split(" ").length
        total += words
        ctx.log.info(s"$msg has $words words, total $total words.")
        Behaviors.same
      })
    }
  }

  //stateless actor
  object WordCounter2 {
    def apply(): Behavior[String] = count(0)

    def count(total: Int): Behavior[String] = 
        Behaviors.setup {ctx =>
            Behaviors.receiveMessage {message =>
                val words = message.split(" ").length
                val c = total + words
                ctx.log.info(s"$message has $words words, total $c words.")
                count(c)
            }
        }
  }

  //switch behavior
  object Person {
    def happy(): Behavior[String] =
      Behaviors.receive {(ctx,msg) =>
        msg match {
          case "Bad" =>
            ctx.log.info(s"Bad? Why?")
            sad()
          case _: String => 
            ctx.log.info(s"just $msg")
            Behaviors.same
        }
      }
    
    def sad(): Behavior[String] =
      Behaviors.receive {(ctx,msg) =>
        msg match {
          case "Good" =>
            ctx.log.info(s"Good! Yeah!")
            happy()
          case _: String => 
            ctx.log.info(s"just $msg")
            Behaviors.same
        }
      }
    
    def apply(): Behavior[String] = happy()
  }

  def main(args: Array[String]): Unit = {
        val system = ActorSystem(Behaviors.empty, "ActorSystem")
        val counter = system.systemActorOf(WordCounter(), "counter")

        counter ! "hello"
        counter ! "scala"
        counter ! "hello scala"

        val counter2 = system.systemActorOf(WordCounter2(), "counter2")

        counter2 ! "hello"
        counter2 ! "scala"
        counter2 ! "hello scala"

        Thread.sleep(1000)

        val person = system.systemActorOf(Person(), "person")
        person ! "hi"
        person ! "Good"
        person ! "Bad"
        person ! "woo"
        person ! "Good"

        system.terminate()
  }
}
