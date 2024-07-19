package part2

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem

object ChildActorsExercise {
  
    sealed trait MasterProtocol
    sealed trait WorkerProtocol
    sealed trait UserProtocol

    //master messages
    case class Initialize(childNum: Int) extends MasterProtocol
    case class WordCountTask(text: String, replyTo: ActorRef[UserProtocol])
        extends MasterProtocol
    case class WordCountReply(id: Int, count: Int)
        extends MasterProtocol
    
    //worker messages
    case class WorkerTask(id: Int, text: String)
        extends WorkerProtocol
    
    // requester (user) messages
    case class Reply(count: Int)
        extends UserProtocol

    trait WordCounterWorker {
        def apply(master: ActorRef[MasterProtocol]): Behavior[WorkerProtocol] = ???
    }
    
    object WordCounterMaster {
        def apply(): Behavior[MasterProtocol] =
            Behaviors.receive {(ctx, msg) =>
                msg match {
                    case Initialize(childNum) => 
                        ctx.log.info(s"[master] initializing with $childNum children")
                        val childRefs = for {
                            i <- 1 to childNum
                        } yield ctx.spawn(WordCounterWorker(ctx.self), s"worker$i")
                        active(childRefs, 0, 0, Map())
                    case x: Any =>
                        ctx.log.info(s"[master] command $x not supported while idle")
                        Behaviors.same
                }
            }
        
        def active(childRefs: Seq[ActorRef[WorkerProtocol]],
            childIndex: Int,
            taskId: Int,
            requestMap: Map[Int, ActorRef[UserProtocol]]): Behavior[MasterProtocol] = 
                Behaviors.receive {(ctx, msg) =>
                    msg match {
                        case WordCountTask(text, replyTo) => 
                            ctx.log.info(s"[master] received $text, will send it to the child $childIndex")
                            //prepare
                            val task = WorkerTask(taskId, text)
                            val childRef = childRefs(childIndex)
                            //send task
                            childRef ! task
                            //update
                            val nextChildIndex = (childIndex + 1) % childRefs.length
                            val nextTaskId = (taskId + 1)
                            val newReqMap = requestMap + (taskId -> replyTo)
                            //change behavior
                            active(childRefs, nextChildIndex, nextTaskId, newReqMap)
                        case WordCountReply(id, count) => 
                            ctx.log.info(s"[master] received a reply for task $id with $count")
                            //prepare
                            val sender = requestMap(id)
                            //send back the result
                            sender ! Reply(count)
                            active(childRefs, childIndex, taskId, requestMap - id)
                        case x: Any =>
                            ctx.log.info(s"[master] command $x not supported while active")
                            Behaviors.same

                    }
                }

    }

    object WordCounterWorker extends WordCounterWorker {
        override def apply(master: ActorRef[MasterProtocol]): Behavior[WorkerProtocol] =
            Behaviors.receive { (ctx, msg) =>
                msg match {
                    case WorkerTask(id, text) => 
                        ctx.log.info(s"[${ctx.self.path} received task $id with $text]")
                        val result = text.split(" ").length
                        master ! WordCountReply(id, result)
                        Behaviors.same
                }

        }
    }

    object Aggregator {
        def apply(): Behavior[UserProtocol] = active()
        def active(total: Int = 0): Behavior[UserProtocol] =
            Behaviors.receive {(ctx, msg) =>
                msg match {
                    case Reply(count) => 
                        ctx.log.info(s"[aggregator] received $count, total is ${total + count}")
                        active(total + count)
                }
            }
    }

    def main(args: Array[String]): Unit = {
        val guardian: Behavior[Unit] = Behaviors.setup {ctx =>
            
            val aggregator = ctx.spawn(Aggregator(), "aggregator")
            val master = ctx.spawn(WordCounterMaster(), "master")

            master ! Initialize(2)
            master ! WordCountTask("I love Akka", aggregator)
            master ! WordCountTask("Scala is super dope", aggregator)
            master ! WordCountTask("Testing round robin scheduling", aggregator)

            Behaviors.empty
        }

        val system = ActorSystem(guardian, "ActorSystem")
        Thread.sleep(1000)
        system.terminate()
    }
}
