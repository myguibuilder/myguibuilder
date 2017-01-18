package guibuilder

////////////////////////////////////////////////////////////////////

import javafx.application._

import scala.concurrent._
import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import akka.util._

import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global

import HeapSize._

////////////////////////////////////////////////////////////////////

// actor implements the akka actor framework


case class CustomException(
	customcause:String
) extends Exception
{
}

//usage: throw CustomException("custom error")

case class ExecutionItem(
	client:String="unknown",
	code:Runnable
)
{
}

case object ExecutionOk
case object ExecutionFailed

class QueuedExecutor extends Actor
{
	var items=List[ExecutionItem]()

	var ecnt=0

	var maxqlen=0

	var clients=Map[String,ClientItem]()

	private var scheduler:Cancellable=null

	case object UpdateMsg

	case class ClientItem(
		name:String,
		var ecnt:Int=0,
		var heapdelta:Double=0.0
	)
	{		
	}

	override def preStart():Unit=
	{
		scheduler = context.system.scheduler.schedule(
			initialDelay = 5 seconds,
			interval = 5 seconds,
			receiver = self,
			message = UpdateMsg
		)
	}

	def ExecNext
	{
		val qlen=items.length
		if(qlen>0)
		{
			if(qlen>maxqlen) maxqlen=qlen
			ecnt+=1
			//println("executing code, queue length "+items.length)
			val ei=items.head
			items=items.tail
			if(!clients.contains(ei.client)) clients+=(ei.client->ClientItem(ei.client))
			clients(ei.client).ecnt+=1

			Platform.runLater(new Runnable{def run{
				try
				{
					val origheap=heapsize_B
					ei.code.run
					val endheap=heapsize_B
					val heapdelta=endheap-origheap
					clients(ei.client).heapdelta=clients(ei.client).heapdelta+heapdelta
				}
				catch
				{
					case e:Throwable =>
					{
						if(e.isInstanceOf[CustomException])						
						{
							//println("custom exception "+e.asInstanceOf[CustomException].customcause)
						}
						//e.printStackTrace()
						self ! ExecutionFailed
						return
					}
				}
				self ! ExecutionOk
			}})
		}
	}

	def ReportHTML:String=
	{
		val clientscontent=(for((k,client)<-clients) yield {
		val heapdelta=client.heapdelta
		s"""
			|<tr>
			|<td>$k</td>
			|<td>${client.ecnt}</td>
			|<td>${heapdelta}</td>
			|</tr>
		""".stripMargin}).mkString("\n")
		s"""
			|<table>
			|<tr>
			|<td>ecnt</td>
			|<td>$ecnt</td>
			|</tr>
			|<tr>
			|<td>maxqlen</td>
			|<td>$maxqlen</td>
			|</tr>
			|<tr>
			|<td>client</td>
			|<td>invoc.</td>
			|<td>heapdelta</td>
			|</tr>
			|$clientscontent
			|</table>
		""".stripMargin
	}

	def Update
	{
		Platform.runLater(new Runnable{def run{
			Builder.LoadWebContent("{execqueue}",ReportHTML)
		}})
	}

	def receive=
	{
		case ei:ExecutionItem =>
		{
			items=items:+ei
			//println("received code")
			ExecNext
		}
		case ExecutionOk =>
		{
			//println("executed ok")
			ExecNext
		}
		case ExecutionFailed =>
		{
			println("execution failed")
			ExecNext
		}
		case UpdateMsg =>
		{
			Update
		}
		case _ => println("that was unexpected")
	}
}



case object AskNameMessage

// TestActor defines a simple actor
 
class TestActor extends Actor
{
	def receive=
	{
		case AskNameMessage => // respond to the "ask" request
		{
			Thread.sleep(0)
			sender ! "Fred"
		}
		case _ => println("that was unexpected")
	}
}

// ScheduledOrderSynchronizer defines an actor that schedules repeat timer

case class SubmitEventRequest(handler:(MyEvent)=>Unit,ev:MyEvent,delay:Int=3){}

class ScheduledOrderSynchronizer extends Actor {

	private val SYNC_ALL_ORDERS = "SYNC_ALL_ORDERS"

	private var scheduler: Cancellable = _

	override def preStart(): Unit=
	{
		val sync=Builder.GD("{components}#{timingssynchronizer}",100.0).toInt
		scheduler = context.system.scheduler.schedule(
			initialDelay = 2 seconds,
			interval = sync milliseconds,
			receiver = self,
			message = SYNC_ALL_ORDERS
		)
	}

	override def postStop():Unit=
	{
		scheduler.cancel()
	}

	private var timers=Map[String,Int]()
	private var requests=Map[String,SubmitEventRequest]()

	var tickcnt=0

	def receive = {
	case ser:SubmitEventRequest =>
	{
		val id=ser.ev.Id
		requests+=(id->ser)
		timers+=(id->ser.delay)
	}
	case SYNC_ALL_ORDERS =>
		try
		{
			tickcnt+=1
			if((tickcnt%10)==0)
			{
				val ei=ExecutionItem(
					client="ScheduledOrderSynchronizer",
					code=new Runnable{def run{
					Builder.GetMyText("{heapsizelabel}").SetText("heap size "+heapsize)
				}})
				MyActor.queuedExecutor ! ei
			}
			/*if((tickcnt%1)==0)
			{
				if(Builder.GB("{components}#{boardcontrolpanellearn}",false))
				{
					Robot.LearnBoard
				}
			}*/
			// synchronize all the orders
			for((k,v)<-timers)
			{
				if(v==0)
				{
					val r=requests(k)        		
					timers-=k
					requests-=k
					MyActor.Log("delayed request "+r.ev.Id)
					val ei=ExecutionItem(
						client="ScheduledOrderSynchronizer",
						code=new Runnable{def run{
						r.handler(r.ev)
					}})
					MyActor.queuedExecutor ! ei
				}
				else
				{
					timers+=(k->(v-1))
				}
			}
		}
		catch
		{
			case t: Throwable =>
				// report errors
		}
	} 

}
 
// MyActor module start the actor system on startup and shuts it down on shutdown

object MyActor extends Module
{

	var system:ActorSystem=null
	var myActor:ActorRef=null
	var repeatActor:ActorRef=null
	var logSupervisor:ActorRef=null
	var queuedExecutor:ActorRef=null
	implicit val timeout = Timeout(5 seconds)

	def Log(what:String)
	{
		val ei=ExecutionItem(
		client="Robot.Log",
		code=new Runnable{def run{
			MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(what))
		}})
		MyActor.queuedExecutor ! ei		
	}
	
	def Startup
	{
		// create the system and actor
		system=ActorSystem("MyActorSystem")
		myActor=system.actorOf(Props[TestActor], name = "MyActor")
		repeatActor=system.actorOf(Props(new ScheduledOrderSynchronizer), name = "RepeatActor")
		logSupervisor=system.actorOf(Props[MyLogSupervisor], name = "LogSupervisor")
		queuedExecutor=system.actorOf(Props[QueuedExecutor], name = "QueuedExecutor")
	}

	def Shutdown
	{
		// shut down actor system
		system.terminate()
	}

	def AskName
	{
		val future = myActor ? AskNameMessage
		try
		{
			val result = Await.result(future, timeout.duration).asInstanceOf[String]
			println(result)  
		}
		catch
		{
			case e : akka.pattern.AskTimeoutException => println("timed out")
			case _ : Throwable => println("what??")
		}
	}

	def Name="MyActor"
	
}

case class MyLogItem(
	text:String=""
)
{	
	def ReportHTMLTableRow:String=
	{
		s"""
			|<tr>
			|<td>$text</td>
			|</tr>
		""".stripMargin
	}
}

case class MyLog(
	wid:String=null,
	buffersize:Int=200,
	var items:scala.collection.mutable.ArrayBuffer[MyLogItem]=scala.collection.mutable.ArrayBuffer[MyLogItem]()
)
{
	def Clear
	{
		items=scala.collection.mutable.ArrayBuffer[MyLogItem]()
	}

	def Add(item:MyLogItem)
	{
		items+=item
		while(items.length>buffersize)
		{
			items=items.tail
		}
	}

	def ReportHTML:String=
	{
		val itemscontent=(for(item<-items.reverse) yield item.ReportHTMLTableRow).mkString("\n")
		s"""
			|<table>
			|$itemscontent
			|</table>
		""".stripMargin
	}

	def Update
	{
		Builder.LoadWebContent(wid,ReportHTML)
	}
}

case class AddItem(
	wid:String,
	item:MyLogItem
)
{	
}

class MyLogSupervisor extends Actor
{
	var logs=Map[String,ActorRef]()

	def receive=
	{
		case l:MyLog =>
		{
			if(l.wid!=null)
			{
				if(!logs.contains(l.wid))
				{
					logs+=(l.wid->context.actorOf(Props[MyLogActor], name = Builder.RemoveSpecials(l.wid)))
				}
				logs(l.wid) ! l
			}
		}

		case ai:AddItem =>
		{
			if(logs.contains(ai.wid))
			{
				logs(ai.wid) ! ai.item
			}
		}

		case _ => println("that was unexpected")
	}
}

class MyLogActor extends Actor
{
	var log:MyLog=null

	def receive=
	{
		case l:MyLog => log=l

		case li:MyLogItem =>
		{
			if(log!=null)
			{
				log.Add(li)
				val ei=ExecutionItem(
					client="MyLogActor",
					code=new Runnable{def run{
					log.Update
				}})
				MyActor.queuedExecutor ! ei
			}
		}

		case _ => println("that was unexpected")
	}
}
