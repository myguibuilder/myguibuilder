package guibuilder

import java.awt.image._
import javafx.embed.swing._
import javafx.scene.image._

import square._
import piece._

import scala.concurrent._
import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import akka.util._

import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global

object Robot extends Module
{	
	var r:java.awt.Robot=null

	val rnd=new scala.util.Random()

	var pstartpos=""
	var popenings=List[String]()
	var popeningsans=List[String]()

	var terminated=false

	var prevscanresult:ScanResult=null

	case class ScanResult(
		var ischanged:Boolean=false,
		pattern:String="",
		fpattern:String="",
		isstart:Boolean=false,
		iswhitestart:Boolean=false,
		isblackstart:Boolean=false,
		ismovetomake:Boolean=false,
		movetomake_m:move=null,
		movetomake_san:String=null,
		movetomake_chess960_algeb:String=null,
		isidenticalstraight:Boolean=false,
		isidenticalflipped:Boolean=false,
		isidenticalstraightorflipped:Boolean=false
	)
	{		
	}

	def Scan(g:game,prev:ScanResult=null):ScanResult=
	{
		val brect=GetBrect

		val bim=GetImage(brect.x, brect.y, brect.width, brect.height)

		val lxwr=brect.hss+brect.ss*0
		val lywr=brect.hss+brect.ss*7

		val wrcol=bim.getRGB(lxwr,lywr)

		val expwrcol=Builder.GI("{boardattrs}#{wrcol}",0)

		wrinplace=(wrcol==expwrcol)

		val battrs=GetBattrs

		var buff=""
		var fbuff=""

		for(rank<- 0 to 7)
		{
			for(file<- 0 to 7)
			{
				def get_bit(file:Int,rank:Int,flip:Boolean):String=
				{
					var f=file
					var r=rank

					if(flip)
					{
						f=7-f
						r=7-r
					}

					val lx=brect.hss+brect.ss*f
					val ly=brect.hss+brect.ss*r

					var ecnt=0

					for(blurx<- -2 to 2)
					{
						for(blury<- -2 to 2)
						{
							val col=bim.getRGB(lx+blurx*brect.hss/10,ly+blury*brect.hss/10)
							if((col==battrs.lcol)||(col==battrs.dcol)) ecnt+=1
						}
					}
					return (if(ecnt>20) "0" else "1")
				}

				buff+=get_bit(file,rank,false)
				fbuff+=get_bit(file,rank,true)
			}
			buff+="\n"
			fbuff+="\n"
		}

		if(prevscanresult!=null) if(buff==prevscanresult.pattern) return {prevscanresult.ischanged=false; prevscanresult}

		var ismovetomake=false
		var isstart=(buff==pstartpos)
		var iswhitestart=((buff==pstartpos)&&wrinplace)
		var isblackstart=false
		var san:String=null
		var m:move=null
		var chess960_algeb:String=null
		var isidenticalstraight:Boolean=false
		var isidenticalflipped:Boolean=false
		var isidenticalstraightorflipped:Boolean=false

		def determine_diff_move(pattern:String,fpattern:String)
		{
			val index=popenings.indexOf(fpattern)
			val b=new board			
			if(index>=0)
			{
				b.reset
				san=popeningsans(index)		
				m=b.sanToMove(san)
				chess960_algeb=b.to_chess960_algeb(m.toAlgeb)
				isblackstart=true
				ismovetomake=true
			}
			else
			{
				b.set_from_fen(g.b.report_fen)
				b.initMoveGen
				var found=false
				while((!found)&&b.nextLegalMove())
				{
					val bc=b.cclone
					bc.makeMove(b.current_move)
					val rpattern=bc.report_pattern
					if((rpattern==pattern)||(rpattern==fpattern)) found=true
				}
				if(found)
				{
					m=b.current_move
					san=b.toSan(m)
					chess960_algeb=b.to_chess960_algeb(m.toAlgeb)
					ismovetomake=true
					println("found "+san)
				}
			}
		}

		if(!iswhitestart) determine_diff_move(buff,fbuff)

		var ischanged=false

		if(prev!=null)
		{
			if(buff!=prev.pattern) ischanged=true
		}

		val gbpattern=g.b.report_pattern

		isidenticalstraight= ( gbpattern == buff )
		isidenticalflipped= ( gbpattern == fbuff )
		isidenticalstraightorflipped= ( isidenticalstraight || isidenticalflipped )

		ScanResult(
			ischanged=ischanged,
			pattern=buff,
			fpattern=fbuff,
			isstart=isstart,
			iswhitestart=iswhitestart,
			isblackstart=isblackstart,
			ismovetomake=ismovetomake,
			movetomake_san=san,
			movetomake_m=m,
			movetomake_chess960_algeb=chess960_algeb,
			isidenticalstraight=isidenticalstraight,
			isidenticalflipped=isidenticalflipped,
			isidenticalstraightorflipped=isidenticalstraightorflipped
		)
	}

	def Toss(percent:Int):Boolean=
	{
		val ri=rnd.nextInt(100)
		percent > ri
	}

	val vovels="aeiou"
	val consonants="bcdfghjklmnpqrstvwxyz"
	val letterkinds=List(consonants,vovels)

	def RandLetterStr(kind:Int,upper:Boolean=false):String=
	{
		val lset=letterkinds(kind)
		val len=lset.length
		val i=rnd.nextInt(len)
		val c=lset(i)
		""+(if(upper) c.toUpper else c)
	}

	def GenRandWord:String=
	{
		var cnt=0
		var kind=0
		var len=rnd.nextInt(7)+5
		var buff=""
		var first=true
		for(i<- 1 to len)
		{
			var switchp=80
			if(cnt>=2)
			{
				switchp=100
			}
			val doswitch=Toss(switchp)
			if(doswitch)
			{
				cnt=1
				kind=1-kind
			}
			else
			{
				cnt+=1
			}
			buff+=RandLetterStr(kind,first)
			first=false
		}
		buff
	}

	def GenNames
	{
		var names=(for(i<- 1 to 100) yield
		{
			val n1=GenRandWord
			val n1l=n1.toLowerCase
			val n2=GenRandWord
			val n2l=n2.toLowerCase
			val no=rnd.nextInt(990)+10
			s"$i $n1 $n2 $n1l$n2l$no"
		}).mkString("\n")
		DataUtils.WriteStringToFile("stuff/names.txt",names)
	}

	def Startup
	{
		GetRobot

		InitBoardPatterns

		GenNames

		terminated=false

		new Thread(new Runnable{def run{
			game_thread_func
		}}).start()
	}

	def Shutdown
	{
		terminated=true
	}

	def Name="Robot"

	def IsGameOn=Builder.GB("{components}#{boardcontrolpanelclick}",false)

	def MakeABookMove:Boolean=
	{
		val re=Commands.GetRecommendedEntry

		if(re!=null)
		{
			val b=Commands.g.b
			val san=re.san
			val m=b.sanToMove(san)					
			val chess960_algeb=b.to_chess960_algeb(m.toAlgeb)

			if(!b.isAlgebLegal(chess960_algeb))
			{
				return false
			}

			Commands.MakeSanMove(san)
			
			val eib=ExecutionItem(
				client="Robot.MakeBookMove",
				code=new Runnable{def run{
				if(Builder.GB("{components}#{boardcontrolpanelclick}",false))
				{							
					ClickMove(m)
				}
				MyApp.Update
				EngineManager.move_made=true
			}})
			MyActor.queuedExecutor ! eib
			return true
		}

		false
	}

	def MakeAnEngineMove(fixedtime:Int= -1):Boolean=
	{
		val ei2=ExecutionItem(
			client="Robot.LearnBoard",
			code=new Runnable{def run{
				if(!EngineManager.AreEnginesRunning) EngineManager.StartAllEngines(Commands.g) else
				EngineManager.CheckRestartAllEngines(Commands.g)

				Future
				{
					val thinksleep=Builder.GD("{components}#{boardcontrolpanelsleep}",1000.0)

					val timereduction=Builder.GD("{components}#{timingstimereduction}",0.95)

					var reducedsleep=thinksleep*scala.math.pow(timereduction,Commands.g.b.fullmove_number)

					if(fixedtime>=0) reducedsleep=fixedtime

					try{Thread.sleep(reducedsleep.toInt)}catch{case e:Throwable=>}

					var tcnt=0
					while((!EngineManager.TopRunningHasPv)&&(tcnt< 50))
					{
						try{Thread.sleep(100)}catch{case e:Throwable=>}
						tcnt+=1
					}

					val ei3=ExecutionItem(
						client="Robot.LearnBoard",
						code=new Runnable{def run{
						EngineManager.MakeAnalyzedMoveRunning
						}})
					MyActor.queuedExecutor ! ei3
				}
			}})
		MyActor.queuedExecutor ! ei2
		true
	}

	def MakeAMove:Boolean=
	{
		if(MakeABookMove) return true
		MakeAnEngineMove()
	}

	def MakeAMoveAwait(set_timeout:Int):Boolean=
	{
		EngineManager.move_made=false
		if(!MakeAMove) return false
		var timeout=set_timeout
		while((!EngineManager.move_made)&&(timeout>0))
		{
			try{ Thread.sleep(50) }catch{ case e:Throwable=> }
			timeout-=50
		}
		EngineManager.move_made
	}

	def ClickCreateGame
	{
		val br=GetBrect
		val cx=br.x+br.ss*10
		val cy=br.y+br.ss*2+br.hss
		ClickXY(cx,cy)
		try{ Thread.sleep(3000) }catch{ case e:Throwable=> }
		val cx2=br.x+br.ss*4
		val cy2=br.y+br.ss*6+br.hss
		ClickXY(cx2,cy2)
	}

	def ClickAbort
	{
		val br=GetBrect
		val cx=br.x+br.ss*3+br.hss/2
		val cy=br.y+br.ss*2+br.hss/2
		ClickXY(cx,cy)
		try{ Thread.sleep(3000) }catch{ case e:Throwable=> }
	}

	def ClickNewOpponent(xshift:Int=0,yshift:Int=0)
	{
		val br=GetBrect
		val cx=br.x+br.ss*11-24+xshift
		val cy=br.y+br.ss*5+br.hss/4+yshift
		ClickXY(cx,cy)
		try{ Thread.sleep(2000) }catch{ case e:Throwable=> }
	}

	def ClearSeek
	{
		val br=GetBrect
		val cx0=br.x+br.ss*4
		val cy0=br.y+br.ss
		ClickXY(cx0,cy0,forceforget=true)
		try{ Thread.sleep(3000) }catch{ case e:Throwable=> }
	}

	def CreateGameFromMenu
	{
		val br=GetBrect
		val cx=br.x-3*br.ss
		val cy=br.y-br.ss*2/3
		r.mouseMove(cx,cy)
		try{ Thread.sleep(2000) }catch{ case e:Throwable=> }
		val cx2=cx
		val cy2=cy+30
		ClickXY(cx2,cy2,forceforget=true)
		try{ Thread.sleep(3000) }catch{ case e:Throwable=> }
		val cx3=br.x+br.ss*4-80
		val cy3=br.y+br.ss*6+br.hss+15
		ClickXY(cx3,cy3,forceforget=true)
	}

	def ClickCreateGameFromScratch()
	{
		CreateGameFromMenu
	}

	var clicking_under_way=false
	def game_thread_func
	{
	try{ Thread.sleep(5000) }catch{ case e:Throwable=> }
	Log("robot game thread started")
	val ei=ExecutionItem(
		client="Robot.game_thread_func",
		code=new Runnable{def run{
			EngineManager.StartAllEngines(Commands.g)
			try{ Thread.sleep(500) }catch{ case e:Throwable=> }
			EngineManager.StopAllEngines
			try{ Thread.sleep(500) }catch{ case e:Throwable=> }
			MyApp.Update
		}})
	MyActor.queuedExecutor ! ei
	var timer=new Timer()
	while(!terminated)
	{
		val sr=Scan(Commands.g,prevscanresult)
		prevscanresult=sr
		val click=Builder.GB("{components}#{boardcontrolpanelclick}",false)
		val create=Builder.GB("{components}#{boardcontrolpanelcreate}",false)
		if(sr.ischanged&&click&&(!clicking_under_way))
		{
			timer=new Timer()
			Log("pos changed")

			var makeamove:Boolean=false

			var opening=false

			if(sr.iswhitestart)
			{				
				Commands.Reset

				val ei=ExecutionItem(
				client="Robot.game_thread_func",
				code=new Runnable{def run{
					EngineManager.StopAllEngines
					MyApp.SetGuiFlip(false)
				}})
				MyActor.queuedExecutor ! ei

				opening=true
				makeamove=true
				Log("white start")
			}
			else if(sr.isblackstart)
			{				
				Commands.Reset
				Commands.MakeSanMove(sr.movetomake_san)

				val ei=ExecutionItem(
				client="Robot.game_thread_func",
				code=new Runnable{def run{
					EngineManager.StopAllEngines
					MyApp.SetGuiFlip(true)
				}})
				MyActor.queuedExecutor ! ei

				opening=true
				makeamove=true
				Log("black start")
			}
			else if(sr.ismovetomake)
			{
				Commands.MakeSanMove(sr.movetomake_san)
				makeamove=true
				Log("move to make")
			}

			if(makeamove)
			{				
				val ei=ExecutionItem(
					client="Robot.game_thread_func",
					code=new Runnable{def run{
						MyApp.UpdateFunc(checkrestart=false)
					}})
				MyActor.queuedExecutor ! ei
				if(opening)
				{
					try{ Thread.sleep(1000) }catch{ case e:Throwable=> }
				}
				MakeAMoveAwait(5000)
			}
		}

		val tick=Builder.GD("{components}#{timingsgamethreadtick}",100.0).toInt

		val timeout=Builder.GD("{components}#{timingsgamethreadtimeout}",30.0).toInt

		try{ Thread.sleep(if(click) tick else(250)) }catch{ case e:Throwable=> }

		if(clicking_under_way)
		{			
		}
		else if(!click)
		{
			timer=new Timer()
		}
		else if(!create)
		{
			timer=new Timer()
		}
		else if(timer.elapsed>timeout)
		{
			clicking_under_way=true
			val ei=ExecutionItem(
				client="Robot.game_thread_func",
				code=new Runnable{def run{					
					ClickCreateGameFromScratch()					
					timer=new Timer()
					clicking_under_way=false
				}})
			MyActor.queuedExecutor ! ei
		}
	}
	}

	def Log(what:String)
	{
		MyActor.Log(what)
	}

	def GetRobot
	{
		try
		{
			r=new java.awt.Robot()
		}
		catch
		{
			case e:Throwable => r=null
		}
	}

	def ClickXY(x:Int,y:Int,forceforget:Boolean=false)
	{	
		var pi=java.awt.MouseInfo.getPointerInfo()
		
		var p=pi.getLocation()
		
		r.mouseMove(x,y)
		r.mousePress(java.awt.event.InputEvent.BUTTON1_MASK)
		val clicksleep=Builder.GD("{components}#{timingsclick}",100.0).toInt
		try{ Thread.sleep(clicksleep/2) }catch{ case e:Throwable=> }
		r.mouseRelease(java.awt.event.InputEvent.BUTTON1_MASK)
		
		if((Builder.GB("{components}#{timingsremembercursor}",false))&&(!forceforget)) r.mouseMove(p.x,p.y)		
	}

	//SwingFXUtils.toFXImage(bimage,null)
    
    def GetImage(x0:Int,y0:Int,w:Int,h:Int):BufferedImage=r.createScreenCapture(new java.awt.Rectangle(x0,y0,w,h))

    case class BRect(x:Int=25, y:Int=25, width:Int=400, height:Int=400, ss:Int=50, hss:Int=25) {}

	def GetBrect:BRect=
	{
		val x=Builder.GD("{stages}#{recordrectdialog}#{x}",25.0).toInt
		val y=Builder.GD("{stages}#{recordrectdialog}#{y}",25.0).toInt
		val width=Builder.GD("{stages}#{recordrectdialog}#{width}",400.0).toInt
		val height=Builder.GD("{stages}#{recordrectdialog}#{height}",400.0).toInt

		val ss=(width+height)/16
		val hss=ss/2

		BRect(x,y,width,height,ss,hss)
	}

	def ClickSquare(sq:TSquare,flip:Boolean)
	{
		var rank=rankOf(sq)
		if(flip) rank=7-rank
		var file=fileOf(sq)
		if(flip) file=7-file

		val brect=GetBrect

		val screenx:Int=brect.x+file*brect.ss+brect.hss
		val screeny:Int=brect.y+rank*brect.ss+brect.hss

		ClickXY(screenx,screeny)
	}

	def ClickMove(m:move)
	{
		var from=m.from
		var to=m.to

		var patternchanged=false
		var tries=0

		val srorig=Scan(Commands.g)

		val retries=Builder.GD("{components}#{timingsclickretries}",3.0).toInt
		while((!patternchanged)&&(tries< retries))
		{
			Log("click move "+m.toAlgeb+" try "+tries)

			ClickSquare(m.from,Settings.get_flip)
			val clicksleep=Builder.GD("{components}#{timingsclick}",100.0).toInt
			try{ Thread.sleep(clicksleep) }catch{ case e:Throwable=> }
			ClickSquare(m.to,Settings.get_flip)

			var timeout=10
			while((!patternchanged)&&(timeout>0))
			{
				val sr=Scan(Commands.g)
				patternchanged=sr.pattern!=srorig.pattern
				if(!patternchanged) try{ Thread.sleep(100) }catch{ case e:Throwable=> }
				timeout-=1
			}

			tries+=1
		}
	}

	def LearnBoardColors
	{
		val brect=GetBrect
		val bim=GetImage(brect.x, brect.y, brect.width, brect.height)

		val lxl=brect.hss+brect.ss*3
		val lyl=brect.hss+brect.ss*3

		val lxd=brect.hss+brect.ss*4
		val lyd=brect.hss+brect.ss*3

		val lcol=bim.getRGB(lxl,lyl)
		val dcol=bim.getRGB(lxd,lyd)

		Builder.S("{boardattrs}#{lcol}",StringData(""+lcol))
		Builder.S("{boardattrs}#{dcol}",StringData(""+dcol))

		val lxwr=brect.hss+brect.ss*0
		val lywr=brect.hss+brect.ss*7

		val wrcol=bim.getRGB(lxwr,lywr)

		Builder.S("{boardattrs}#{wrcol}",StringData(""+wrcol))

		println("wrcol "+wrcol)
	}

	case class BAttrs(lcol:Int,dcol:Int) {}

	def GetBattrs:BAttrs=
	{
		val lcol=Builder.GI("{boardattrs}#{lcol}",0)
		val dcol=Builder.GI("{boardattrs}#{dcol}",1)

		BAttrs(lcol,dcol)
	}

	def InitBoardPatterns
	{
		val b=new board
		b.reset
		pstartpos=b.report_pattern
		b.initMoveGen
		popenings=List[String]()
		popeningsans=List[String]()
		while(b.nextLegalMove()) {
			val bc=b.cclone
			bc.makeMove(b.current_move)
			popeningsans=popeningsans:+b.toSan(b.current_move)
			popenings=popenings:+bc.report_pattern
		}
	}

	var wrinplace:Boolean=false

	def LearnBoardPattern(flip:Boolean):String=
	{
		val brect=GetBrect

		val bim=GetImage(brect.x, brect.y, brect.width, brect.height)

		val lxwr=brect.hss+brect.ss*0
		val lywr=brect.hss+brect.ss*7

		val wrcol=bim.getRGB(lxwr,lywr)

		val expwrcol=Builder.GI("{boardattrs}#{wrcol}",0)

		wrinplace=(wrcol==expwrcol)

		val battrs=GetBattrs

		var buff=""

		for(rank<- 0 to 7)
		{
			for(file<- 0 to 7)
			{
				var f=file
				var r=rank

				if(flip)
				{
					f=7-f
					r=7-r
				}

				val lx=brect.hss+brect.ss*f
				val ly=brect.hss+brect.ss*r

				var ecnt=0

				for(blurx<- -2 to 2)
				{
					for(blury<- -2 to 2)
					{
						val col=bim.getRGB(lx+blurx*brect.hss/10,ly+blury*brect.hss/10)
						if((col==battrs.lcol)||(col==battrs.dcol)) ecnt+=1
					}
				}

				buff+=(if(ecnt>20) "0" else "1")
			}
			buff+="\n"
		}
		buff
	}

	var prevbuff:String=""

	def LearnBoard
	{

		val buff=LearnBoardPattern(false)

		if(buff==prevbuff) return

		prevbuff=buff

		val fbuff=LearnBoardPattern(true)

		val b=new board
		b.set_from_fen(Commands.g.b.report_fen)
		b.initMoveGen

		var found=false
		var opening=false

		while(b.nextLegalMove() && !found)
		{			
			val bc=b.cclone
			if(popenings.contains(fbuff))
			{
				if(bc.report_pattern==pstartpos)
				{
					EngineManager.StopAllEngines
					val index=popenings.indexOf(fbuff)
					MyApp.SetGuiFlip(true)
					Commands.Reset
					Commands.MakeSanMove(popeningsans(index))
					found=true
					opening=true
				}
				else return
			}
			else if(buff==pstartpos)
			{
				if(wrinplace)
				{
					if(!popenings.contains(Commands.g.b.report_pattern))
					{
						EngineManager.StopAllEngines
						MyApp.SetGuiFlip(false)
						Commands.Reset
						found=true
						opening=true
					}
					else return
				}
				else return
			}
			else
			{
				bc.makeMove(b.current_move)
				if(buff==bc.report_pattern)
				{
					found=true
				}
				if(fbuff==bc.report_pattern)
				{				
					found=true
				}
			}
			if(found)
			{

				val san=b.toSan(b.current_move)
				val algeb=b.current_move.toAlgeb

				if(!opening)
				{
					if(Commands.g.b.isAlgebLegal(algeb))
					{
						Commands.MakeSanMove(san)
					}
					else return
				}

				val ei=ExecutionItem(
					client="Robot.LearnBoard",
					code=new Runnable{def run{
						MyApp.UpdateFunc(checkrestart=false)
					}})
				MyActor.queuedExecutor ! ei

				if(!Builder.GB("{components}#{boardcontrolpanelclick}",false)) return

				val turn=Commands.g.b.turn

				if((turn==WHITE)&&(Settings.get_flip)) return
				if((turn==BLACK)&&(!Settings.get_flip)) return

				// check if book move is available

				val re=Commands.GetRecommendedEntry

				if(re!=null)
				{
					val b=Commands.g.b
					val san=re.san
					val m=b.sanToMove(san)					
					val chess960_algeb=b.to_chess960_algeb(m.toAlgeb)
					if(!b.isAlgebLegal(chess960_algeb))
					{
						return
					}

					Commands.MakeSanMove(san)
					
					val eib=ExecutionItem(
						client="Robot.MakeBookMove",
						code=new Runnable{def run{
						if(opening)
						{
							try{Thread.sleep(1000)}catch{case e:Throwable=>}
						}
						if(Builder.GB("{components}#{boardcontrolpanelclick}",false))
						{							
							ClickMove(m)
						}
						MyApp.Update
						EngineManager.CheckRestartAllEngines(Commands.g)						
					}})
					MyActor.queuedExecutor ! eib
					return
				}

				val ei2=ExecutionItem(
					client="Robot.LearnBoard",
					code=new Runnable{def run{
						if(!EngineManager.AreEnginesRunning) EngineManager.StartAllEngines(Commands.g) else
						EngineManager.CheckRestartAllEngines(Commands.g)

						Future
						{
							val thinksleep=Builder.GD("{components}#{boardcontrolpanelsleep}",1000.0).toInt

							try{Thread.sleep(thinksleep)}catch{case e:Throwable=>}

							var tcnt=0
							while((!EngineManager.TopRunningHasPv)&&(tcnt< 50))
							{
								try{Thread.sleep(100)}catch{case e:Throwable=>}
								tcnt+=1
							}

							val ei3=ExecutionItem(
								client="Robot.LearnBoard",
								code=new Runnable{def run{
								EngineManager.MakeAnalyzedMoveRunning
								}})
							MyActor.queuedExecutor ! ei3
						}
					}})
				MyActor.queuedExecutor ! ei2
			}
		}

	}
}