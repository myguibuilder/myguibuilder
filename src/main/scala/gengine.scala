package guibuilder

////////////////////////////////////////////////////////////////////

import javafx.application._
import javafx.stage._
import javafx.scene._
import javafx.scene.layout._
import javafx.scene.control._
import javafx.scene.canvas._
import javafx.scene.input._
import javafx.scene.paint._
import javafx.scene.text._
import javafx.scene.web._
import javafx.scene.image._
import javafx.event._
import javafx.geometry._
import javafx.beans.value._
import javafx.concurrent.Worker._

import java.io._

import Builder._
import Commands._
import move._
import piece._

import collection.JavaConverters._

import org.apache.commons.lang.time.DurationFormatUtils.formatDuration
import org.apache.commons.lang.time.DateFormatUtils._
import org.apache.commons.io.FileUtils._

import java.util.Date

import scala.concurrent._
import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import akka.util._

import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global

////////////////////////////////////////////////////////////////////

object GEngineGlobals
{
	val WHITE_COLOR_BACKGROUND="#afffaf"
	val WHITE_COLOR="#007f00"
	val BLACK_COLOR_BACKGROUND="#ffafaf"
	val BLACK_COLOR="#7f0000"
}

case object WatchAnalysis

case object StopWatchAnalysis

case object MakeAnalyzedMoveMsg
case class MakeAnalyzedMoveRunningMsg(undo:Boolean=false,addtobook:Boolean=false)

object EngineManager extends Module
{
	var enginelist:GEngineList=null
	var enginegames:ActorRef=null

	var BASE_ELO=2800.0

	var gamerunning=false

	def IsGameRunning=gamerunning

	var enginesrunning:Boolean=false

	var move_made=false

	def AreEnginesRunning:Boolean=enginesrunning

	def SetMultipv(mpv:Int,g:game)
	{
		enginelist.SetMultipv(mpv,g)
	}

	def TopRunning:GEngine=enginelist.TopRunning
	def TopRunningHasPv:Boolean=enginelist.TopRunningHasPv

	def CheckRestartAllEngines(g:game)
	{
		if(!enginesrunning) return
		StopAllEngines
		StartAllEngines(g)
	}

	def StartAllEngines(g:game)
	{
		enginelist.StartAll(g)
		enginesrunning=true
		enginegames ! WatchAnalysis
	}

	def StopAllEngines
	{
		enginelist.StopAll
		enginesrunning=false
		enginegames ! StopWatchAnalysis
		MyApp.GetMainBoard.clear_score
	}

	def MakeAnalyzedMove
	{
		enginegames ! MakeAnalyzedMoveMsg
	}

	def MakeAnalyzedMoveRunning(undo:Boolean=false,addtobook:Boolean=false)
	{
		enginegames ! MakeAnalyzedMoveRunningMsg(undo=undo,addtobook=addtobook)
	}

	def Startup
	{
		enginelist=GEngineList("{enginelistwebview}")
		enginelist.Load
		enginegames=MyActor.system.actorOf(Props[EngineGames], name = "EngineGames")
	}

	def ReLoad
	{
		enginelist.ReLoad
	}

	def Shutdown
	{
		enginelist.Save
		enginelist.UnloadAll
	}

	def Handle
	{
		enginelist.Handle
	}

	def StartGame
	{
		enginegames ! StartGameMsg
	}

	def StartGameFromCurrent
	{
		enginegames ! StartGameFromCurrentMsg
	}

	def AbortGame
	{
		enginegames ! AbortGameMsg
	}

	def Name:String="EngineManager"

	case class EngineStatsItem(
		white:String,
		black:String,
		result:String
	)
	{
		def pairing_key:String=s"$white vs. $black"
	}

	case class Elos(
		var elos:Map[String,Double]=Map[String,Double]()
	)
	{
		def Update(item:EngineStatsItem)
		{
			// https://en.wikipedia.org/wiki/Elo_rating_system

			var whiteelo=BASE_ELO
			if(elos.contains(item.white)) whiteelo=elos(item.white)

			var blackelo=BASE_ELO
			if(elos.contains(item.black)) blackelo=elos(item.black)
			
			def ESCORE(A:Double,B:Double):Double=1.0/(1.0+scala.math.pow(10.0,(B-A)/400.0))

			var scorewhite:Double=0.5
			var scoreblack:Double=0.5

			if(item.result=="1-0") { scorewhite=1.0; scoreblack=0.0 }
			else if(item.result=="0-1") { scorewhite=0; scoreblack=1.0 }

			val ESCOREW = ESCORE(whiteelo,blackelo)
			val ESCOREB = ESCORE(blackelo,whiteelo)

			val K=32.0

			whiteelo=(whiteelo.toDouble+K*(scorewhite-ESCOREW))
			blackelo=(blackelo.toDouble+K*(scoreblack-ESCOREB))

			elos+=(item.white->whiteelo)
			elos+=(item.black->blackelo)
			
		}

		def ReportEloHTML(player:String,elo:Double):String=
		{
			val elof="%.2f".format(elo)
			s"""
				|<tr>
				|<td><font size="4" color="#00007f">$player</font></td>
				|<td><font size="5">$elof</font></td>
				|</tr>
			""".stripMargin
		}

		def ReportHTML:String=
		{
			val sortedkeys=elos.keys.toList.sortWith( elos(_) > elos(_) )
			val eloscontent=(for(k<-sortedkeys) yield ReportEloHTML(k,elos(k))).mkString("\n")
			s"""
				|<table cellpadding="10" cellspacing="10" border="1">
				|<tr>
				|<td>Engine</td>
				|<td>ELO</td>
				|</tr>
				|$eloscontent
				|</table>
			""".stripMargin
		}
	}

	case class PairingStats(
		var whitewins:Int=0,
		var draw:Int=0,
		var blackwins:Int=0,
		var white:String="",
		var black:String="",
		var key:String=""
		)
	{
		def ReportHTML:String=
		{
			s"""
				|<tr>
				|<td>
				|<font size="4" color="#007f00"><b>$white</b></font>
				| vs. 
				|<font size="4" color="#7f0000"><b>$black</b></font>
				|</td>
				|<td align="center"><font size="6" color="#007f00"><b>$whitewins</b></font></td>
				|<td align="center"><font size="6" color="#00007f"><b>$draw</b></font></td>
				|<td align="center"><font size="6" color="#7f0000"><b>$blackwins</b></font></td>
				|</tr>
			""".stripMargin
		}
	}

	case class EngineStats(
		var items:List[EngineStatsItem]=List[EngineStatsItem](),
		var pairings:Map[String,PairingStats]=Map[String,PairingStats]()
		)
	{
		def Add(item:EngineStatsItem)
		{
			items=items:+item

			val key=item.pairing_key
			var pstat=PairingStats()
			if(pairings.contains(key))
			{
				pstat=pairings(key)
			}
			if(item.result=="1-0") pstat.whitewins+=1
			else if(item.result=="1/2-1/2") pstat.draw+=1
			else if(item.result=="0-1") pstat.blackwins+=1
			pstat.white=item.white
			pstat.black=item.black
			pstat.key=key
			pairings+=(key->pstat)


		}

		def ReportHTML:String=
		{
			val pairingscontent=(for((k,v)<-pairings) yield v.ReportHTML).mkString("\n")
			s"""
				|<table cellpadding="5" cellspacing="5" border="1">
				|<tr>
				|<td>Pairing</td>
				|<td>White wins</td>
				|<td>Draw</td>
				|<td>Black wins</td>
				|</tr>
				|$pairingscontent
				|</table>
			""".stripMargin
		}
	}

	def ReportStatsHTML:String=
	{
		val stats=EngineStats()
		val elos=Elos()
		val dummy=new game
		val gf=new File("enginegames.pgn")
		if(gf.exists())
		{
			val pgncontent=readFileToString(gf,null.asInstanceOf[String])
			val pgns=dummy.split_pgn(pgncontent)
			for(pgn<-pgns)
			{
				dummy.parse_pgn(pgn,head_only=true)
				val white=dummy.get_header("White")
				val black=dummy.get_header("Black")
				val result=dummy.get_header("Result")
				val item=EngineStatsItem(white,black,result)
				stats.Add(item)
				elos.Update(item)
			}			
			return s"""
				|Engine stats created from enginegames.pgn:<br><br>
			""".stripMargin+stats.ReportHTML+"<br><br>\n"+elos.ReportHTML
		}
		"Error: enginegames.pgn missing, stats could not be created."
	}
}

case class GameHistoryItem(
	var eval:Int=0,
	var movetime:Int=0,
	var movetimeformatted:String=""
)
{
}

case class GameHistory(
	val initturn:String="white"
)
{
	import GEngineGlobals._

	var items:List[GameHistoryItem]=List[GameHistoryItem]()

	def Add(item:GameHistoryItem)
	{
		items=items:+item
	}

	def ChartSvg:String=
	{	
		val WIDTH=400
		val HEIGHT=200
		var len=items.length
		var barwidth=32
		while(((barwidth*len)>WIDTH)&&(barwidth>2))
		{
			barwidth=barwidth/2
		}
		var first=true
		var maxeval:Int=0
		var mineval:Int=0
		for(item<-items)
		{
			var eval=item.eval
			if(eval>10000) eval=10000
			if(eval< -10000) eval= -10000
			if(first)
			{
				maxeval=eval
				mineval=eval
				first=false
			} else {
				if(eval> maxeval) maxeval=eval
				if(eval< mineval) mineval=eval
			}
		}
		var range=500
		if(maxeval>range) range=maxeval
		if((-mineval)>range) range= -mineval
		if(range< 500) range=500
		else if(range< 1000) range=1000
		else if(range< 2000) range=2000
		else if(range< 5000) range=5000
		else range=10000
		var i=0
		var svgbars=List[String]()
		for(item<-items)
		{
			val cx=i*barwidth
			var height=(item.eval*HEIGHT)/(2*range)
			var cy=HEIGHT/2-height
			if(height< 0)
			{
				height= -height
				cy=HEIGHT/2
			}
			if(height==0) height=1
			val shift=if(initturn=="white") 0 else 1
			val color=if(((i+shift)%2)==0) WHITE_COLOR_BACKGROUND else BLACK_COLOR_BACKGROUND
			val svgbar=s"""
				|<rect width="$barwidth" x="$cx" y="$cy" height="$height" style="fill:$color;stroke-width:1;stroke:#000000;"/>
			""".stripMargin
			svgbars=svgbars:+svgbar
			i+=1
		}
		val svgbarscontent=svgbars.mkString("")
		s"""
			|<div style="margin-left: 15px;">
			|<svg width="$WIDTH" height="$HEIGHT">
			|$svgbarscontent
			|</svg>
			|</div>
		""".stripMargin
	}
}

case class EngineGames(
) extends Actor
{
	import GEngineGlobals._

	var egame:game=null
	var gamethread:Thread=null
	var fromposition=false
	var isconventional=true
	var isincremental=false
	var initialtime=300000
	var initialtimemin=5
	var initialtimesec=300
	var movestogoperround=40
	var incrementpermove=0
	var incrementpermovesec=0
	var incrementpermovestogo=300000
	var incrementpermovestogomin=5
	var incrementpermovestogosec=300
	var timecontrolverbal=""
	var timecontrolverbalpgn=""
	var movestogo=40
	var level="40 5 0"
	var turn="white"
	var players=Map[String,GEngine]()
	var onturn:GEngine=null
	var issuego:Boolean=false
	var stepcnt:Int=0
	var currentmovesteps:Int=0
	var interrupted:Boolean=false
	var cnt:Int=0
	var gamehistory=GameHistory()
	var gameresult:GameResult=null
	var plycount=0
	var isthematic=false
	var thematictype="From FEN"
	var gameaborted:Boolean=false
	var gamecantstart:String=""
	var gamestartfen:String=""
	var predeterminedopening:String=""
	var initturn:String="white"
	var playerwhite:GEngine=null
	var playerblack:GEngine=null
	var selectplayersmessage=""
	var timestep=100
	var bestmove:String=null

	def FormatResult(result:String,bcolor:String="#00ff00"):String=
	{
		s"""
			|<div style="font-weight: bold; margin-top: 10px; padding: 15px; border-radius: 3px; border-style: solid; border-width: 1px; background-color: $bcolor;">
			|$result
			|</div>
		""".stripMargin
	}

	def GetFormattedResult:String=
	{
		if(gameaborted) return FormatResult("Game aborted!",bcolor="#ff0000")
		if(gamecantstart!="") return FormatResult(gamecantstart,bcolor="#ffff00")
		if(gameresult==null) return s"""
			|<div style="margin-top: 10px; padding: 15px;">
			|<i>Game in progress ... ( ply <b>$plycount</b> ) </i>
			|</div>
		""".stripMargin
		val result=gameresult.resultstr
		val reason=gameresult.resultreason
		FormatResult(s"""Game finished. Result: <font size="5">$result</font><br><font size="4">$reason</font>""")
	}

	def Update(content:String)
	{
		val ei=ExecutionItem(
			client="EngineGames.Update",
			code=new Runnable{def run{
			val we=GetWebEngine("{enginegamewebview}")
			if(we==null) return
			we.loadContent(content)
		}})
		MyActor.queuedExecutor ! ei
	}

	def SelectPlayers:Boolean=
	{
		playerwhite=null
		playerblack=null

		for(engine<-EngineManager.enginelist.enginelist)
		{
			if(engine.Loaded)
			{
				if(playerwhite==null) playerwhite=engine
				else if(playerblack==null) playerblack=engine
				else return true
			}
		}

		if((playerwhite!=null)&&(playerblack!=null)) return true

		selectplayersmessage="Game could not be started. Please load at least two engines."

		return false
	}

	def SecondsVerbal(secs:Int):String=
	{
		val mod=(secs%60)
		val mins=(secs-mod)/60
		if(mod==0)
		{
			return s"$mins min(s)"
		}
		return s"$mins min(s) $mod sec(s)"
	}

	def GetTimeControl
	{
		isconventional=(GS("{radiopaneselections}#{timecontrolradioboxpane}",
			"{timecontrolconventionalrb}")=="{timecontrolconventionalrb}")
		isincremental= !isconventional

		initialtimemin=GI("{components}#{timecontroltime}#{selected}",5)

		initialtimesec=initialtimemin*60

		incrementpermovesec=0

		if(isincremental)
		{
			initialtimemin=GI("{components}#{timecontrolincrementaltime}#{selected}",2)
			incrementpermovesec=GI("{components}#{timecontrolincrementalincrement}#{selected}",3)
		}

		incrementpermove=incrementpermovesec*1000

		initialtime=initialtimemin*60*1000

		incrementpermovestogomin=initialtimemin
		incrementpermovestogosec=initialtimemin*60
		incrementpermovestogo=initialtime

		movestogoperround=GI("{components}#{timecontrolnumberofmoves}#{selected}",40)

		if(isincremental) movestogoperround=0

		movestogo=movestogoperround

		level=s"$movestogoperround $incrementpermovestogomin $incrementpermovesec"

		playerwhite.time=initialtime
		playerblack.time=initialtime

		turn="white"

		players+=("white"->playerwhite)
		players+=("black"->playerblack)

		val ipmtgsverbal=SecondsVerbal(incrementpermovestogo/1000)

		timecontrolverbal=s"$movestogoperround move(s) in $ipmtgsverbal"

		timecontrolverbalpgn=s"$movestogoperround/$incrementpermovestogosec"

		if(isincremental)
		{
			timecontrolverbal=s"$initialtimemin min(s) + $incrementpermovesec sec(s)"

			timecontrolverbalpgn=s"$initialtimesec+$incrementpermovesec"
		}
	}

	def StartThinking(engine:GEngine,turn:String,issuego:Boolean)
	{
		engine.wtime=playerwhite.time
		engine.btime=playerblack.time
		engine.winc=incrementpermove
		engine.binc=incrementpermove
		engine.movestogo=movestogo
		engine.fen=egame.report_fen
		engine.usermove=bestmove
		engine.issuego=issuego
		if(turn=="white") engine.otim=playerblack.time else engine.otim=playerwhite.time
		engine.StartThinking
	}

	def UpdateGameStatus:String=
	{
		val formattedresult=GetFormattedResult

		var startgamebuttons=if(EngineManager.gamerunning) s"""
			|<td><input type="button" value="Abort game" onclick="command='abort';"></td>
		""".stripMargin else
		s"""
			|<td><input type="button" value="Start game" onclick="command='start';"></td>
			|<td><input type="button" value="Start game from current position" onclick="command='startfrompos';"></td>
		""".stripMargin

		if(gamecantstart!="") return s"""
			|$formattedresult
			|<table cellpadding="3" cellspacing="3">
			|<tr>
			|$startgamebuttons
			|</tr>
			|</table>
		""".stripMargin

		val wtime=formatDuration(playerwhite.time,"HH:mm:ss")
		val btime=formatDuration(playerblack.time,"HH:mm:ss")
		val timestyle="font-size:36px; font-family: monospace; font-weight: bold; padding: 3px; border-style: solid; border-width: 2px; border-color: #000000; border-radius: 10px;"
		var whitebckg=if(turn=="white") WHITE_COLOR_BACKGROUND else "#afafaf"
		var blackbckg=if(turn=="black") BLACK_COLOR_BACKGROUND else "#afafaf"
		val sidespanstyle="padding: 5px; border-style: solid; border-width: 2px; border-radius: 10px; font-size: 20px;"
		val sidespanwhite=s"""<span style="$sidespanstyle border-color: $WHITE_COLOR;">"""
		val sidespanblack=s"""<span style="$sidespanstyle border-color: $BLACK_COLOR;">"""
		val namew=playerwhite.GetDisplayName()
		val nameb=playerblack.GetDisplayName()
		val namestyle="font-size: 20px; color: #0000ff; padding: 5px; border-style: dotted; border widht: 2px; border-radius: 10px;"
		val timecontrolcolor="#0000af"
		val timecontrolinfo=if(isconventional) s"<i><font size=5><b>$movestogo</b></font> move(s) to go</i>" else ""
		val chartsvg=gamehistory.ChartSvg
		s"""
			|<script>
			|var command='';
			|</script>
			|<table cellpadding="3" cellspacing="3">
			|<tr>
			|<td>$sidespanwhite<font color="$WHITE_COLOR">White</font></span></td>
			|<td><span style="$timestyle background-color: $whitebckg;">$wtime</span></td>
			|<td>$sidespanblack<font color="$BLACK_COLOR">Black</font></span></td>
			|<td><span style="$timestyle background-color: $blackbckg">$btime</span></td>
			|<td><font color="$timecontrolcolor">$timecontrolinfo</td>
			|</tr>
			|</table>
			|<table cellpadding="3" cellspacing="3">
			|<tr>
			|<td style="color: $WHITE_COLOR;">White</td>
			|<td style="$namestyle border-color: $WHITE_COLOR"><font color="$WHITE_COLOR"><b>$namew</b></font></td>
			|</tr>
			|<tr>
			|<td style="color: $BLACK_COLOR;">Black</td>
			|<td style="$namestyle border-color: $BLACK_COLOR"><font color="$BLACK_COLOR"><b>$nameb</b></font></td>
			|</tr>
			|<tr>
			|<td style="color: $timecontrolcolor;">Time control</td>
			|<td style="color: $timecontrolcolor; font-size: 18px; font-weight: bold;" colspan="3"><i>$timecontrolverbal</i></td>
			|</tr>
			|</table>
			|$formattedresult
			|<table cellpadding="3" cellspacing="3">
			|<tr>
			|$startgamebuttons
			|</tr>
			|</table>
			|$chartsvg
		""".stripMargin
	}

	def game_tick_func
	{

		if(scheduler==null) return

		if(!EngineManager.gamerunning) return
		
		if((stepcnt%10)==0)
		{
			Update(UpdateGameStatus)
		}

		if(onturn.thinking)
		{
			onturn.time-=timestep

			val neverloseontime=true

			if( (onturn.time< 1000) && neverloseontime )
			{
				onturn.time=1000
			}
			currentmovesteps+=1
			if(onturn.time<=0)
			{
				if(turn=="white")
				{
					gameresult=GameResult(-1,"0-1","GUI adjudication: white lost on time")
				} else {
					gameresult=GameResult(1,"1-0","GUI adjudication: black lost on time")
				}
			}
		}
		else
		{
			val thinkingtime=currentmovesteps*timestep
			currentmovesteps=0

			val extremepv=onturn.ExtremePv(lowest=false)

			val depth=onturn.thinkingoutput.maxdepth

			val scorenumerical=extremepv.scorenumerical
			val signedscorenumerical=extremepv.signedscorenumerical

			val movetimeformatted=formatDuration(thinkingtime,"mm:ss")+"."+(thinkingtime%1000)/100

			val historyitem=GameHistoryItem(eval=scorenumerical,movetime=thinkingtime,movetimeformatted=movetimeformatted)

			gamehistory.Add(historyitem)

			bestmove=onturn.bestmove

			val chess960_algeb=egame.b.to_chess960_algeb(bestmove)

			val m=move(fromalgeb=chess960_algeb)

			var scorecp="%.2f".format(scorenumerical.toDouble/100.0)
			scorecp=scorecp.replaceAll(",",".")

			var comment=s"$scorecp/$depth $movetimeformatted"

			egame.makeMove(m,addcomment=comment)
			plycount+=1
			gameresult=egame.report_result

			if(gameresult!=null)
			{

			}
			else
			{
				if(isincremental)
				{
					onturn.time+=incrementpermove
				}

				if(turn=="white") turn="black" else turn="white"
				onturn=players(turn)

				val ei=ExecutionItem(
					client="EngineGames.game_tick_func",
					code=new Runnable{def run{
					onturn.ToTop
				}})
				MyActor.queuedExecutor ! ei

				StartThinking(onturn,turn,issuego)

				issuego=false

				if(turn==initturn)
				{
					if(isconventional)
					{
						movestogo-=1
					}
				}
				
				if(isconventional)
				{
					if(movestogo==0)
					{
						movestogo=movestogoperround
						playerwhite.time+=incrementpermovestogo
						playerblack.time+=incrementpermovestogo
					}
				}
			}

			val ei=ExecutionItem(
				client="EngineGames.game_tick_func",
				code=new Runnable{def run{
				Commands.g.set_from_pgn(egame.report_pgn)
				Commands.g.toend
				MyApp.Update
			}})
			MyActor.queuedExecutor ! ei
		}

		if(gameresult==null)
		{
			try{Thread.sleep(timestep)}catch{case e:Throwable=>{interrupted=true}}
			stepcnt+=1
		}

	}

	def game_cleanup_func
	{
		playerwhite.StopForced
		playerblack.StopForced
		val opening=egame.GetOpening
		egame.pgn_headers+=(if(predeterminedopening=="*") ("Opening"->opening) else ("Opening"->predeterminedopening))
		egame.pgn_headers+=("PlyCount"->(""+plycount))
		if(gameresult==null)
		{
			gameaborted=true
			egame.pgn_headers+=("Result"->"*")
			egame.pgn_headers+=("Termination"->"GUI info: game aborted by user")
			playerwhite.SendResult("*")
			playerblack.SendResult("*")
			Update(UpdateGameStatus)
			
		} else {
			val result=gameresult.resultstr
			val reason=gameresult.resultreason
			egame.pgn_headers+=("Result"->result)
			egame.pgn_headers+=("Termination"->reason)
			playerwhite.SendResult(result)
			playerblack.SendResult(result)
			Update(UpdateGameStatus)
		}
		val ei=ExecutionItem(
			client="EngineGames.game_cleanup_func",
			code=new Runnable{def run{
			Commands.g.set_from_pgn(egame.report_pgn)
			Commands.g.toend
			MyApp.Update
			playerwhite.Reuse
			playerblack.Reuse
		}})
		MyActor.queuedExecutor ! ei
					
		val pgn=egame.report_pgn+"\n\n\n"
		if(!gameaborted)
		{
			writeStringToFile(new File("enginegames.pgn"),pgn,null.asInstanceOf[java.nio.charset.Charset],true)
		} else {
			writeStringToFile(new File("abortedenginegames.pgn"),pgn,null.asInstanceOf[java.nio.charset.Charset],true)
		}
	}

	def prepare_game_func(fromposition:Boolean=false)
	{

		this.fromposition=fromposition

		gameaborted=false
		gamecantstart=""
		if(!SelectPlayers)
		{
			gamecantstart=selectplayersmessage
			Update(UpdateGameStatus)
			return
		}
		if((!playerwhite.CanSetboard)||(!playerblack.CanSetboard))
		{
			gamecantstart="Engine support for starting game from a given position is missing. Game could not be started."
			Update(UpdateGameStatus)
			return
		}
		if(Commands.g.report_result!=null)
		{
			gamecantstart="Game starting position is final. Game could not be started."
			Update(UpdateGameStatus)
			return
		}

		// remove any movelist, start from fen
		isthematic=false
		thematictype="From FEN"

		val addopeningmoves=true

		if((addopeningmoves)&&(Commands.g.is_from_startpos))
		{
			val moves=Commands.g.current_line_moves
			Commands.g.reset
			for(move<-moves) Commands.g.makeSanMove(move,addcomment="THEMATIC MOVE")
			if(moves.length>0)
			{
				isthematic=true
				thematictype="From Moves"
			}
		} else {
			gamestartfen=egame.report_fen
			Commands.g.set_from_fen(gamestartfen)
			isthematic=true
		}

		predeterminedopening=Commands.g.GetOpening

		egame=new game

		egame.set_from_pgn(Commands.g.report_pgn)
		egame.toend

		egame.pgn_headers=Map[String,String]()

		egame.pgn_headers+=("Event"->"Computer chess game")
		egame.pgn_headers+=("Site"->"https://github.com/scalachessgui/scalachessgui")

		val date=new Date()

		val datef=format(date,"yyyy.MM.dd")
		val timef=format(date,"HH:mm:ss")
		val timezonef=format(date,"ZZ")

		egame.pgn_headers+=("Date"->datef)
		egame.pgn_headers+=("Time"->timef)
		egame.pgn_headers+=("TimeZone"->timezonef)

		egame.pgn_headers+=("White"->playerwhite.GetDisplayName(includeauthor=false))
		egame.pgn_headers+=("Black"->playerblack.GetDisplayName(includeauthor=false))

		egame.pgn_headers+=("Result"->"*")
		egame.pgn_headers+=("Termination"->"?")

		egame.pgn_headers+=("Round"->"1")
		egame.pgn_headers+=("Annotator"->"Scalachessgui")
		egame.pgn_headers+=("StartPosition"->(if(isthematic) s"Thematic ($thematictype)" else "Conventional (Standard)"))
		egame.pgn_headers+=("Opening"->predeterminedopening)
		egame.pgn_headers+=("ECO"->"?")

		GetTimeControl

		egame.pgn_headers+=("TimeControl"->timecontrolverbalpgn)

		playerwhite.OpenConsole
		playerblack.OpenConsole

		var firsttimestep=true

		gameresult=null

		val ei=ExecutionItem(
			client="EngineGames.prepare_game_func",
			code=new Runnable{def run{
			Commands.g.set_from_pgn(egame.report_pgn)
			Commands.g.toend
			MyApp.Update
			Update(UpdateGameStatus)
		}})
		MyActor.queuedExecutor ! ei

		interrupted=false
		cnt=0
		bestmove=null
		issuego=true
		if(egame.b.getturn==piece.BLACK) turn="black"
		playerwhite.SetMultipv(1,egame)
		playerblack.SetMultipv(1,egame)
		val fen=egame.report_fen
		playerwhite.NewGame(level,fromposition,fen)
		playerblack.NewGame(level,fromposition,fen)
		initturn=turn
		onturn=players(turn)

		gamehistory=GameHistory(initturn)
		plycount=0
		stepcnt=0
		currentmovesteps=0

		StartThinking(onturn,turn,true)

		val ei2=ExecutionItem(
			client="EngineGames.prepare_game_func",
			code=new Runnable{def run{				
			onturn.ToTop
		}})
		MyActor.queuedExecutor ! ei2
		
	}

	def ShutDown
	{
		AbortGame
	}

	def AbortGame
	{
		stop_ticking

		game_cleanup_func
	}

	def StartGame(fromposition:Boolean=false)
	{
		prepare_game_func(fromposition)

		start_ticking
	}	

	private var scheduler:Cancellable=null

	case object GameTickMsg

	def start_ticking
	{
		if(scheduler!=null) return
		val watchsleep=Builder.GD("{components}#{timingswatchtick}",100.0).toInt
		scheduler = context.system.scheduler.schedule(
			initialDelay = 0 seconds,
			interval = watchsleep milliseconds,
			receiver = self,
			message = GameTickMsg
		)
	}

	def stop_ticking
	{
		if(scheduler!=null)
		{
			scheduler.cancel
			scheduler=null
		}
	}

	private var analysiswatcher:Cancellable=null

	case object AnalysisWatchTickMsg

	def start_analysis_watch_ticking
	{
		if(analysiswatcher!=null) return
		analysiswatcher = context.system.scheduler.schedule(
			initialDelay = 0 milliseconds,
			interval = 200 milliseconds,
			receiver = self,
			message = AnalysisWatchTickMsg
		)
	}

	def stop_analysis_watch_ticking
	{
		if(analysiswatcher!=null)
		{
			analysiswatcher.cancel
			analysiswatcher=null
		}
	}

	def watch_analysis
	{
		EngineManager.enginelist.WatchAnalysis
	}

	def make_analyzed_move
	{
		EngineManager.enginelist.MakeAnalyzedMove()
	}

	def make_analyzed_move_running(undo:Boolean=false,addtobook:Boolean=false)
	{
		EngineManager.enginelist.MakeAnalyzedMove(uselastbestmove=false,checkrestart=false,undo=undo,addtobook=addtobook)
	}

	def receive=
	{
		case WatchAnalysis =>
		{
			start_analysis_watch_ticking
		}

		case StopWatchAnalysis =>
		{
			stop_analysis_watch_ticking
		}

		case AnalysisWatchTickMsg =>
		{
			watch_analysis
		}

		case MakeAnalyzedMoveMsg =>
		{
			make_analyzed_move
		}

		case mam:MakeAnalyzedMoveRunningMsg =>
		{
			make_analyzed_move_running(undo=mam.undo,addtobook=mam.addtobook)
		}

		case StartGameMsg =>
		{
			if(!EngineManager.gamerunning)
			{
				EngineManager.gamerunning=true
				StartGame()				
			}
		}
		case StartGameFromCurrentMsg =>
		{
			if(!EngineManager.gamerunning)
			{
				EngineManager.gamerunning=true
				StartGame(fromposition=true)				
			}
		}
		case AbortGameMsg =>
		{
			if(EngineManager.gamerunning)
			{
				EngineManager.gamerunning=false
				AbortGame
			}
		}
		case GameTickMsg =>
		{
			if(EngineManager.gamerunning)
			{
				game_tick_func
			}
		}
		case _ => println("that was unexpected")
	}
}

case object StartGameMsg
case object StartGameFromCurrentMsg
case object AbortGameMsg

case class GEngine(
	var id:Int=0,
	val enginedata:Data=null,
	val handler:(MyEvent)=>Unit=null
)
{
	var path:String=""
	var pathid:String=""
	var commandline:String=""
	var protocol:String="UCI"

	val protocols=List("UCI","XBOARD")

	var engineprocess:Process=null
	var enginein:InputStream=null
	var engineout:OutputStream=null
	var enginereadthread:Thread=null

	var autoload=false

	var xboardstate="Observing"

	val globalhandler=handler

	FromData(enginedata)

	var multipv=1

	var time=300000

	var otim=300000

	var wtime=300000

	var btime=300000

	var winc=0

	var binc=0

	var movestogo=40

	var usermove:String=null

	var issuego=true

	var thinking=false

	var fen=""

	var root_fen=""

	def Loaded:Boolean=(engineprocess!=null)

	def SendResult(result:String="?")
	{
		if(protocol=="XBOARD")
		{
			IssueCommand("result "+result)
		}
	}

	def ExtremePv(lowest:Boolean=true):PvItem=
	{
		thinkingoutput.ExtremePv(lowest)
	}

	def CanSetboard:Boolean=
	{
		if(protocol=="UCI") return true

		if(protocol=="XBOARD")
		{
			if(features.setboard) return true
			return false
		}

		return false
	}

	def Reload
	{
		if(!Loaded) return
		Unload
		Load
		OpenConsole
	}

	def Reuse
	{
		if(protocol=="XBOARD")
		{
			if(!features.reuse)
			{
				Reload
			}
		}
	}

	def IssueVariantXBOARD
	{
		if(Settings.get_variant=="Atomic")
		{
			IssueCommand("variant atomic")
		}
	}

	def NewGame(level:String="40 5 0",fromposition:Boolean=false,fen:String="")
	{
		usermove=null

		if(protocol=="UCI")
		{
			if(fromposition)
			{
				IssueCommand(s"position fen "+fen)
			}
			else
			{
				IssueCommand(s"position startpos")
			}
		}

		if(protocol=="XBOARD")
		{
			IssueCommand("new")
			IssueCommand("random")
			IssueVariantXBOARD
			IssueCommand(s"level $level")
			IssueCommand("post")
			IssueCommand("force")
			if(fromposition)
			{
				IssueCommand(s"setboard "+fen)
			}
		}
	}

	def StartThinking
	{

		root_fen=fen

		thinkingoutput=ThinkingOutput()

		thinking=true

		if(protocol=="UCI")
		{
			IssueCommand(s"position fen $fen")
			val issuemovestogo=if(movestogo!=0) s" movestogo $movestogo" else ""
			IssueCommand(s"go wtime $wtime btime $btime winc $winc binc $binc"+issuemovestogo)
		}

		def IssueXBOARDStart
		{
			if(usermove!=null)
			{
				IssueCommand(s"usermove $usermove")
				if(issuego)
				{
					IssueCommand("go")
				}
			}
			else
			{
				IssueCommand("go")
			}
		}

		def IssueXBOARDTimeControl
		{
			val centitime:Int=(time/10).toInt
			val centiotim:Int=(otim/10).toInt
			IssueCommand(s"time $centitime")
			IssueCommand(s"otim $centiotim")
		}

		if(protocol=="XBOARD")
		{			
			if(features.colors)
			{
				IssueXBOARDStart
				IssueXBOARDTimeControl
			} else {
				IssueXBOARDTimeControl
				IssueXBOARDStart
			}
		}		

	}

	def SetPath(p:String)
	{
		path=p
		CreatePathId
	}

	def SetCommandLine(cv:String)
	{
		val wasloaded=Loaded
		Unload
		commandline=cv
		CreatePathId
		if(wasloaded) Load
	}

	def SetMultipv(set_multipv:Int,g:game)
	{
		if(engineprocess==null) return

		multipv=set_multipv

		if(options.hasmultipv)
		{
			if(multipv< options.minmultipv) return
			if(multipv> options.maxmultipv) return
			val wasrunning=running
			if(running) Stop
			if(protocol=="UCI")
			{
				IssueCommand("setoption name MultiPV value "+multipv)
			}
			if(protocol=="XBOARD")
			{
				IssueCommand("option MultiPV="+multipv)
			}
			if(wasrunning) Start(g)
		}
	}

	def console_handler(ev:MyEvent)
	{
		globalhandler(ev)

		if(ev.kind=="button pressed")
		{
			if(ev.Id==s"{$pathid}#{issueenginecommand}")
			{
				IssueConsoleEngineCommand
			}

			if(ev.Id==s"{$pathid}#{setpreferredsize}")
			{
				SetPreferredSize
			}

			if(ev.Id==s"{$pathid}#{setcompactsize}")
			{
				SetCompactSize
			}
		}

		if(ev.kind=="textfield entered")
		{			
			if(ev.Id==s"{$pathid}#{enginecommand}")
			{
				IssueConsoleEngineCommand
			}
		}
	}

	def IssueConsoleEngineCommand
	{
		val etext=GetMyText(s"{$pathid}#{enginecommand}")
		val command=etext.GetText
		etext.SetText("")
		Set(s"{components}#{$pathid}#{enginecommand}","")
		IssueCommand(command)
	}

	def SetPreferredSize
	{
		val sid=s"{$pathid}"
		if(HasStage(sid))
		{
			GetStage(sid).SetWidth(640.0)
			GetStage(sid).SetHeight(740.0)
			GetStage(sid).SetY(10.0)
			return
		}	
	}

	def SetCompactSize
	{
		val sid=s"{$pathid}"
		if(HasStage(sid))
		{
			GetStage(sid).SetWidth(520.0)
			GetStage(sid).SetHeight(300.0)
			GetStage(sid).SetY(10.0)
			return
		}	
	}

	def CloseConsole
	{
		val ei=ExecutionItem(
			client="GEngine.CloseConsole",
			code=new Runnable{def run{
			CloseConsoleFunc
		}})
		MyActor.queuedExecutor ! ei
	}

	def CloseConsoleFunc
	{
		if(HasStage(s"{$pathid}"))
		{
			CloseStage(s"{$pathid}")
			EngineManager.enginelist.Update
		}
	}

	def ToTop
	{
		if(HasStage(s"{$pathid}"))
		{
			GetStage(s"{$pathid}").ToTop
		}	
	}

	def OpenConsole
	{
		val ei=ExecutionItem(
			client="GEngine.OpenConsole",
			code=new Runnable{def run{
			OpenConsoleFunc
		}})
		MyActor.queuedExecutor ! ei
	}

	def OpenConsoleFunc
	{
		if(engineprocess==null)
		{
			CloseConsole
			return
		}

		if(HasStage(s"{$pathid}"))
		{
			GetStage(s"{$pathid}").ToTop
			return
		}	

		val blob=s"""
			|<scrollpane>
			|<tabpane>
			|<tab caption="Search output">
			|<scrollpane id="engineoutscrollpane" width="600.0" height="645.0">
			|<webview id="{$pathid}#{engineouttext}" height="3000.0" width="3000.0"/>
			|</scrollpane>
			|</tab>
			|<tab caption="Console">
			|<vbox padding="5" gap="5">
			|<hbox padding="5" gap="5">
			|<textfield style="-fx-font-size: 18px; -fx-text-fill: #00007f;" id="{$pathid}#{enginecommand}"/>
			|<button id="{$pathid}#{issueenginecommand}" text="Issue" style="round"/>
			|<button id="{$pathid}#{setpreferredsize}" text="Preferred size" />
			|<button id="{$pathid}#{setcompactsize}" text="Compact size" />
			|</hbox>
			|<scrollpane id="engineconsolescrollpane" width="600.0" height="600.0">
			|<webview id="{$pathid}#{engineconsoletext}" height="3000.0" width="3000.0"/>
			|</scrollpane>
			|</vbox>
			|</tab>
			|<tab caption="Settings">
			|<scrollpane id="enginesettingsscrollpane" width="600.0" height="645.0">
			|<vbox id="{$pathid}#{enginesettingsvbox}" height="3000.0" width="3000.0"/>
			|</scrollpane>
			|</tab>
			|</tabpane>
			|</scrollpane>
		""".stripMargin
		Builder.MyStage(s"{$pathid}",modal=false,handler=console_handler,
			title=GetDisplayName(includeauthor=false)+" console",blob=blob)
		BuildOptions
		log.Update
		EngineManager.enginelist.Update
	}

	def Console
	{
		if(engineprocess==null)
		{
			CloseConsole
			return
		}
		if(HasStage(s"{$pathid}"))
		{
			CloseConsole
		} else {
			OpenConsole
		}
	}

	def SendQuit
	{
		if(engineprocess!=null)
		{
			if(protocol=="UCI")
			{
				IssueCommand("quit")
			}

			if(protocol=="XBOARD")
			{
				IssueCommand("quit")
			}
		}
	}

	def Unload
	{	

		SendQuit

		if(enginereadthread!=null)
		{
			enginereadthread.interrupt()
			enginereadthread=null
		}

		if(engineprocess!=null)
		{
			engineprocess.destroy()
			engineprocess=null
		}

		enginein=null
		engineout=null

		running=false
		thinking=false

		CloseConsole

		L(s"engine $pathid unloaded")

	}

	case class Option(
		var name:String="",
		var kind:String="",
		var minstr:String="",
		var maxstr:String="",
		var defaultstr:String="",
		var send:Boolean=true,
		var subkind:String="",
		var vars:List[String]=List[String]()
	)
	{
		def dosend:Boolean=
		{
			if(kind=="separator") return false
			send
		}

		def Apply(reset:Boolean=false)
		{
			if((kind!="button")&&(dosend==true))
			{
				var id=s"{engineoptions}#{$pathid}#{$name}"

				var sliderid=id+"#{slider}"
				var textid=id+"#{text}"
				val comboid=id+"#{selected}"

				var value=if(kind=="combo") GS(Cve(comboid))
				else GS(Cve(if(kind=="spin") textid else id),defaultstr)

				if((kind=="spin")&&(value!=defaultstr)) value=""+value.toDouble.toInt

				if(reset)
				{
					if(kind=="combo")
					{
						Set(Cve(comboid),defaultstr)
					}
					else if(kind=="spin")
					{
						Set(Cve(textid),defaultstr)
						Set(Cve(sliderid),defaultstr)
					}
					else
					{
						Set(Cve(id),defaultstr)
					}
					value=defaultstr
				}

				if(protocol=="UCI")
				{
					IssueCommand("setoption name "+name+" value "+value)
				}

				if(protocol=="XBOARD")
				{
					var xboardvalue=value
					if(kind=="check")
					{
						xboardvalue=if(value=="true") "1" else "0"
					}

					IssueCommand("option "+name+"="+xboardvalue)
				}
			}
		}

		def ParseLine(line:String):Option=
		{
			val tokenizer=Tokenizer(line)
			val head=tokenizer.Poll
			if(head==null) return null

			if(protocol=="UCI")
			{
				if(tokenizer.Get!="option") return null

				val reservedtokens=List("name","type","min","max","default","var")

				def IsReserved(token:String)=reservedtokens.contains(token)

				while(tokenizer.HasToken)
				{
					var currenttoken=tokenizer.Get

					if(!IsReserved(currenttoken))
					{
						if((name=="")||(kind=="")) return null

						return this
					}

					var fieldbuff=List[String]()

					while(tokenizer.HasToken&&(!IsReserved(tokenizer.Poll)))
					{
						fieldbuff=fieldbuff:+tokenizer.Get
					}

					val field=fieldbuff.mkString(" ")

					if(currenttoken=="name") name=field
					if(currenttoken=="type") kind=field
					if(currenttoken=="min") minstr=field
					if(currenttoken=="max") maxstr=field
					if(currenttoken=="default") defaultstr=field
					if(currenttoken=="var") vars=vars:+field
				}
			}

			return this
		}

		def ReportXML(i:Int):String=
		{
			var td1=""
			var td2=""
			var td3=""
			val id=s"{engineoptions}#{$pathid}#{$name}"
			if(kind=="button")
			{
				td1=s"""
					|<button id="$id" text="$name"/>
				""".stripMargin
			}
			if(kind=="check")
			{
				td1=s"""
					|<label text="$name"/>
				""".stripMargin
				var value=GS(Cve(id),defaultstr)
				Set(Cve(id),StringData(value))				
				td2=s"""
				|<checkbox id="$id" prefixget="variant" prefixset="variant"/>
				""".stripMargin
			}
			if(kind=="string")
			{
				td1=s"""
					|<label text="$name"/>
				""".stripMargin
				var value=GS(Cve(id),defaultstr)
				Set(Cve(id),StringData(value))				
				td2=s"""
				|<textfield id="$id" prefixget="variant" prefixset="variant"/>
				""".stripMargin
				td3=s"""
					|<button id="$id#{apply}" qualifier="apply" text="Apply"/>
				""".stripMargin
			}
			if(kind=="spin")
			{				
				val minv=ParseInt(minstr,0)
				val maxv=ParseInt(maxstr,100)
				var span=maxv-minv
				if(minv==1) span+=1
				var unit=1
				if(span>10)
				{
					unit=span/10
				}
				val textid=id+"#{text}"
				val sliderid=id+"#{slider}"
				var value=GS(Cve(textid),""+defaultstr.toDouble)
				val intvalue=value.toDouble.toInt
				Set(Cve(textid),""+intvalue)
				Set(Cve(sliderid),value)				
				td1=s"""
					|<label text="$name"/>
				""".stripMargin
				td2=s"""
				|<slider width="300.0" id="$id#{slider}" prefixget="variant" prefixset="variant" min="$minstr" max="$maxstr" majortickunit="$unit" showticklabels="true"/>
				""".stripMargin
				td3=s"""
				|<textfield id="$id#{text}" qualifier="text" prefixget="variant" prefixset="variant" text="$intvalue" width="100.0" />
				""".stripMargin
			}
			if(kind=="combo")
			{
				td1=s"""
					|<label text="$name"/>
				""".stripMargin
				var value=GS(Cve(id),defaultstr)
				val items=(for(v <- vars) yield ("<s>"+v+"</s>")).mkString("\n")
				val sel=GS(Cve(id)+"#{selected}",defaultstr)
				val data=Data.FromXMLString(s"""
					|<m>
					|<a key="items">
					|$items
					|</a>
					|<s key="selected">$sel</s>
					|</m>
				""".stripMargin)
				Set(Cve(id),data)
				td2=s"""
				|<combobox id="$id" prefixget="variant" prefixset="variant"/>
				""".stripMargin
			}
			if(kind=="separator")
			{
				td1=s"""
					|<hbox width="600.0" height="1.0" style="-fx-border-style:solid; -fx-border-width: 1px;" cs="3" />
				""".stripMargin
			}
			def tdrc(td:String,c:Int):String=
			{
				td.replaceAll(".>",s""" r="$i" c="$c" />""")
			}
			td1=tdrc(td1,1); td2=tdrc(td2,2); td3=tdrc(td3,3);
			s"""
				|$td1
				|$td2
				|$td3
			""".stripMargin
		}
	}

	case class Id(
		var name:String="",
		var author:String=""
	)
	{
		def ParseLine(line:String)
		{
			val tokenizer=Tokenizer(line)
			val head=tokenizer.Get
			if(head!=null)
			{
				if(protocol=="UCI")
				{
					if(head=="id")
					{
						val token=tokenizer.Get
						val value=tokenizer.GetRest
						if(value==null) return
						if(token=="name") name=value
						if(token=="author") author=value
					}
				}
			}
		}
	}

	case class Options(var options:List[Option]=List[Option]())
	{

		var hasmultipv=false
		var minmultipv=1
		var maxmultipv=1

		InitOptions

		def InitOptions
		{
			Add(Option(name="Reset defaults",kind="button"))
			Add(Option(kind="separator"))
		}

		def AddDefaultOptions
		{
			Add(Option(kind="separator"))
			Add(Option(name="Auto set FEN",kind="check",defaultstr="true",send=false))
			Add(Option(name="Command after set FEN",kind="string",defaultstr="",send=false))
			Add(Option(name="Auto start",kind="check",defaultstr="true",send=false))
		}

		def Add(o:Option)
		{
			if(o.name=="MultiPV")
			{
				if(!IsInt(o.minstr)) return
				if(!IsInt(o.maxstr)) return
				hasmultipv=true
				minmultipv=o.minstr.toInt
				maxmultipv=o.maxstr.toInt
			}
			options=options:+o
		}

		def ReportXML:String=
		{
			var i=0;
			val content=(for(option<-options) yield { i+=1; option.ReportXML(i) }).mkString("\n")
			content
		}

		def ApplyAll
		{
			AddDefaultOptions
			for(option<-options) option.Apply()
		}

		def ResetAll
		{
			for(option<-options) option.Apply(reset=true)
		}
	}

	def GetNameFromId(id:String):String=
	{
		val parts=id.split("#").toList
		parts.reverse.head.replaceAll("[\\{\\}]","")
	}

	def options_handler(ev:MyEvent)
	{

		if(ev.kind=="textfield entered")
		{
			if(ev.comp.GS("qualifier")=="text")
			{
				val trueid=ev.TrunkId

				var slidername=GetNameFromId(trueid)

				val valuestr=GetMyText(ev.Id).GetText

				if(IsInt(valuestr))
				{

					val intvalue=valuestr.toInt

					val sliderid=trueid+"#{slider}"

					Set(Cve(sliderid),""+intvalue.toDouble)

					GetMySlider(sliderid).SetValue(intvalue.toDouble)

					if(protocol=="UCI")
					{
						IssueCommand("setoption name "+slidername+" value "+intvalue)
					}
					if(protocol=="XBOARD")
					{
						IssueCommand("option "+slidername+"="+intvalue)
					}

				}
			}
		}

		if(ev.kind=="button pressed")
		{

			val isapply=ev.comp.GS("qualifier")=="apply"

			val trueid=if(isapply) ev.TrunkId else ev.Id

			val buttonname=GetNameFromId(trueid)

			if(buttonname=="Reset defaults")
			{	
				options.ResetAll
				BuildOptions
				return
			}

			if(isapply)
			{

				var value=""

				val comp=GetComponent(trueid)

				if(comp!=null)
				{
					value=comp.asInstanceOf[MyText].GetText
					Set(Cve(trueid),value)
				}

				if(protocol=="UCI")
				{
					IssueCommand("setoption name "+buttonname+" value "+value)
				}
				if(protocol=="XBOARD")
				{
					IssueCommand("option "+buttonname+"="+value)
				}

			} else {

				if(protocol=="UCI")
				{
					IssueCommand("setoption name "+buttonname+" value ")
				}
				if(protocol=="XBOARD")
				{
					IssueCommand("option "+buttonname)
				}

			}
		}

		if(ev.kind=="slider changed")
		{
			val trunkid=ev.TrunkId
			val sliderid=trunkid+"#{slider}"
			val textid=trunkid+"#{text}"

			Set(Cve(textid),StringData(ev.value))

			val slidername=GetNameFromId(trunkid)

			val sliderint=ev.value.toDouble.toInt

			GetMyText(textid).SetText(""+sliderint)

			if(protocol=="UCI")
			{
				IssueCommand("setoption name "+slidername+" value "+sliderint)
			}
			if(protocol=="XBOARD")
			{
				IssueCommand("option "+slidername+"="+sliderint)
			}
		}

		if(ev.kind=="checkbox changed")
		{
			Set(Cve(ev.Id),StringData(ev.value))

			val checkboxname=GetNameFromId(ev.Id)

			val checkboxbool=ev.value

			if(protocol=="UCI")
			{
				IssueCommand("setoption name "+checkboxname+" value "+checkboxbool)
			}
			if(protocol=="XBOARD")
			{
				val xboardcheckboxbool=if(checkboxbool=="true") "1" else "0"
				IssueCommand("option "+checkboxname+"="+xboardcheckboxbool)
			}
		}

		if(ev.kind=="combobox selected")
		{
			val comboboxname=GetNameFromId(ev.Id)

			val value=ev.value

			if(protocol=="UCI")
			{
				IssueCommand("setoption name "+comboboxname+" value "+value)
			}
			if(protocol=="XBOARD")
			{
				IssueCommand("option "+comboboxname+"="+value)
			}
		}
	}

	def BuildOptions
	{
		val svboxcomp=GetMyBox(s"{$pathid}#{enginesettingsvbox}")
		if(svboxcomp==null) return
		val svbox=svboxcomp.GetNode.asInstanceOf[VBox]
		if(svbox==null) return
		val optionscontent=options.ReportXML
		val blob=s"""
			|<vbox id="{$pathid}#{enginesettingsvboxscenegraph}" padding="3" gap="3" width="600.0" height="625.0">
			|<gridpane hgap="10" vgap="5">
			|$optionscontent
			|</gridpane>
			|</vbox>
		""".stripMargin
		val scenegraph=MyComponent.FromBlob(blob,options_handler)
		scenegraph.CreateNode
		svbox.getChildren().clear()
		val parent=scenegraph.GetParent
		svbox.getChildren().add(parent)
	}

	def ParseXBOARDBool(str:String,default:Boolean):Boolean=
	{
		if(str==null) return default
		if(str=="1") return true
		if(str=="0") return false
		return default
	}

	case class Features(
		var setboard:Boolean=false,
		var analyze:Boolean=true,
		var colors:Boolean=true,
		var done:Boolean=false,
		var reuse:Boolean=true,
		var myname:String=""
	)
	{
		def ParseLine(line:String)
		{
			val tokenizer=Tokenizer(line)
			val head=tokenizer.Get
			if(head==null) return
			if(head!="feature") return
			while(tokenizer.HasToken)
			{
				val token=tokenizer.Get
				val parts=token.split("=").toList
				if(parts.length==2)
				{
					val feature=parts(0)
					var value=parts(1)

					if(value.length>0)
					{
						var needjoin=false
						var joinparts=List[String]()
						if(value(0)=='"')
						{
							if(value.length>1)
							{
								if(value(value.length-1)=='"')
								{
									value=value.substring(1,value.length-1)
								} else {
									joinparts=List[String](value.substring(1))
									needjoin=true
								}
							} else {
								joinparts=List[String]("")
								needjoin=true
							}
							var closed=false
							if(needjoin)
							{
								while(tokenizer.HasToken&&(!closed))
								{
									var part=tokenizer.Get
									if(part.length>0)
									{
										if(part(part.length-1)=='"')
										{
											part=part.substring(0,part.length-1)
											closed=true
										}
									}
									joinparts=joinparts:+part
								}
								value=joinparts.mkString(" ")
							}
						}
					}

					if(feature=="myname") myname=value
					if((feature=="setboard")&&(value=="1")) setboard=true
					if((feature=="analyze")&&(value=="0")) analyze=false
					if((feature=="colors")&&(value=="0")) colors=false
					if((feature=="reuse")&&(value=="0")) reuse=false
					if((feature=="done")&&(value=="1")) done=true

					if(feature=="option")
					{
						val vtokenizer=Tokenizer(value)
						var nameparts=List[String]()
						var nameend=false
						while(vtokenizer.HasToken&&(!nameend))
						{
							val token=vtokenizer.Poll
							if(token.length>0)
							{
								if(token(0)=='-')
								{
									nameend=true
								} else {
									vtokenizer.Get
									nameparts=nameparts:+token
								}
							}
						}
						var kind=vtokenizer.Get
						if(kind!=null)
						{
							if(kind.length>0)
							{
								val name=nameparts.mkString(" ")
								kind=kind.substring(1)

								if(kind=="check")
								{
									val checkdefaulttoken=vtokenizer.Get
									val checkdefaultstr=""+ParseXBOARDBool(checkdefaulttoken,false)									
									val option=Option(kind=kind,name=name,defaultstr=checkdefaultstr)
									options.Add(option)
								} else if((kind=="spin")||(kind=="slider")) {
									val spindefaulttoken=vtokenizer.Get
									val spindefaultstr=""+ParseInt(spindefaulttoken,1)
									val spinmintoken=vtokenizer.Get
									val spinminstr=""+ParseInt(spinmintoken,0)
									val spinmaxtoken=vtokenizer.Get
									val spinmaxstr=""+ParseInt(spinmaxtoken,0)
									val option=Option(kind="spin",name=name,
										defaultstr=spindefaultstr,minstr=spinminstr,maxstr=spinmaxstr,subkind=kind)
									options.Add(option)
								} else if((kind=="button")||(kind=="save")||(kind=="reset")) {
									val option=Option(kind="button",name=name,subkind=kind)
									options.Add(option)
								} else if((kind=="string")||(kind=="file")||(kind=="path")) {
									val stringdefaulttoken=vtokenizer.GetRest
									val stringdefaultstr=if(stringdefaulttoken==null) "" else stringdefaulttoken
									val option=Option(kind="string",name=name,defaultstr=stringdefaultstr,subkind=kind)
									options.Add(option)
								} else if(kind=="combo") {
									// not implemented
								} else {
									// unknown option
								}
							}
						}
					}
				}
			}
		}
	}

	def ParseStartup(line:String)
	{
		val tokenizer=Tokenizer(line)

		if(protocol=="UCI")
		{
			val head=tokenizer.Poll
			if(head=="uciok")
			{
				startup=false
				options.ApplyAll
			} else {
				val option=Option().ParseLine(line)
				if(option!=null)
				{					
					options.Add(option)					
				}
				uciid.ParseLine(line)
			}
		}

		if(protocol=="XBOARD")
		{
			features.ParseLine(line)
			if(features.done)
			{
				startup=false
				options.ApplyAll
			}
		}
	}

	var bestmove:String=null

	def IsSuperfluousEngineOutput(line:String):Boolean=
	{
		if(protocol=="UCI")
		{
			if(running||thinking)
			{
				if(line.contains("info")&&(!line.contains(" pv "))) return true
			}
		}
		false
	}

	def ProcessEngineOut(line:String)
	{
		// ignore superfluous engine output
		if(IsSuperfluousEngineOutput(line)) return

		log.Add(LogItem(line,"out"))

		if(startup)
		{
			ParseStartup(line)
		}
		else
		{
			thinkingoutput.ParseLine(line)
		}

		var tokenizer=Tokenizer(line)

		if(protocol=="UCI")
		{
			val token=tokenizer.Get
			if(token=="bestmove")
			{
				bestmovereceived=true
				bestmove=tokenizer.Get
				thinking=false
				running=false
			}
		}

		if(protocol=="XBOARD")
		{
			val token=tokenizer.Get
			if(token=="move")
			{
				bestmovereceived=true
				bestmove=tokenizer.Get
				thinking=false
			}
		}
	}

	case class LogItem(line:String,kind:String)
	{
		def ReportHTML:String=
		{
			val color=if(kind=="in") "#ff0000" else "#0000ff"
			var rline=line.replaceAll("<","&lt;")
			rline=rline.replaceAll(">","&gt;")
			rline=rline.replaceAll("\\n","<br>")
			s"""
				|<font color="$color">$rline</font>
			""".stripMargin
		}
	}

	def UpdateConsoleLog(content:String)
	{
		val cwe=GetWebEngine(s"{$pathid}#{engineconsoletext}")
		if(cwe==null) return
		cwe.loadContent(content)
	}

	case class Log(buffersize:Int=1000)
	{
		var Items=List[LogItem]()

		def Update
		{
			val ei=ExecutionItem(
				client="Log.Update",
				code=new Runnable{def run{
				UpdateConsoleLog(ReportHTML)
			}})
			MyActor.queuedExecutor ! ei
		}

		def Add(item:LogItem)
		{
			Items=Items:+item
			while(Items.length>buffersize) Items=Items.tail
			Update
		}

		def ReportHTML:String=
		{
			val itemshtml=Items.reverse.map(item => item.ReportHTML).mkString("<br>\n")
			s"""
				|<div style="font-family: monospace;">
				|$itemshtml
				|</div>
			""".stripMargin
		}
	}

	var log=Log()

	def UpdateXBOARDState(command:String)
	{
		if(command=="analyze")
		{
			xboardstate="Analyzing"
		}
		if(command=="exit")
		{
			xboardstate="Observing"
		}
	}

	var prevcommand="";

	def DuplicateCommand(command:String):Boolean=
	{
		if(command!=prevcommand) return false
		if(protocol=="XBOARD")
		{
			if(command=="exit") return true
		}
		return false
	}

	def IssueCommand(command:String)
	{
		if(command==null) return
		if(DuplicateCommand(command)) return
		prevcommand=command
		val fullcommand=command+"\n"
		try
		{
			engineout.write(fullcommand.getBytes())
			engineout.flush()
			UpdateXBOARDState(command)
			log.Add(LogItem(command,"in"))
		}
		catch
		{
			case e: Throwable =>
			{
				println(s"engine write IO exception, command: $command, id: $id, pathid: $pathid")
				e.printStackTrace
			}
		}
	}

	def CreateEngineReadThread:Thread={new Thread(new Runnable{def run{
		var buffer=""
		while (!Thread.currentThread().isInterrupted()){
			try
			{
				val chunkobj=enginein.read()
				try
				{ 
					val chunk=chunkobj.toChar
					if(chunk=='\n')
					{						
						ProcessEngineOut(buffer)
						buffer=""
					} else {
						buffer+=chunk
					}
				}
				catch
				{
					case e: Throwable => 
					{
						println(s"engine read not a char exception, chunk: $chunkobj, id: $id, pathid: $pathid")
					}
				}
			}
			catch
			{
				case e: Throwable =>
				{
					println(s"engine read IO exception, id: $id, pathid: $pathid")
				}
			}
		}
	}})}

	var options=Options()
	var features=Features()
	var uciid=Id()

	def GetDisplayName(includeauthor:Boolean=true):String=
	{
		if(protocol=="UCI")
		{
			if(uciid.name!="")
			{
				if((uciid.author!="")&&includeauthor)
				{
					return uciid.name+" by "+uciid.author
				} else {
					return uciid.name
				}
			}
		}
		if(protocol=="XBOARD")
		{
			if(features.myname!="") return features.myname
		}
		ParseEngineNameFromPath(path)
	}

	def ProtocolStartup
	{
		startup=true

		options=Options()
		features=Features()
		uciid=Id()

		if(protocol=="UCI")
		{
			IssueCommand("uci")
		}

		if(protocol=="XBOARD")
		{
			startup=false
			IssueCommand("xboard")
			IssueCommand("protover 2")
			startup=true
		}

		val timeoutlimit=20
		var cnt=0
		while((startup)&&(cnt< timeoutlimit))
		{
			try{Thread.sleep(100)}catch{case e:Throwable=>{}}
			cnt+=1
		}

		L(s"protocol startup for $pathid took $cnt cycles")

		startup=false
	}

	def Load:Boolean=
	{
		Unload
		val progandargs:List[String]=path+:commandline.split(" ").toList
		val processbuilder=new ProcessBuilder(progandargs.asJava)
		val epf=new File(path)
		L("starting engine process "+progandargs.mkString(" "))
		if(epf.exists())
		{
			processbuilder.directory(new File(epf.getParent()))
		}
		else
		{
			L("failed, directory does not exist")
			return false
		}
		try
		{
			engineprocess=processbuilder.start()
		}
		catch
		{
			case e: Throwable =>
			{
				L("failed, process could no be created")
				return false
			}
		}
		enginein=engineprocess.getInputStream()
        engineout=engineprocess.getOutputStream()
        enginereadthread=CreateEngineReadThread
        enginereadthread.start()
        ProtocolStartup
        L(s"engine $pathid loaded")
		return true
	}

	def ParseEngineNameFromPath(path:String):String=
	{
		val f=new File(path)
		var engine_name=""
		if(f.exists)
		{
			val engine_full_name=f.getName()
			val engine_full_name_parts=engine_full_name.split("\\.").toList
			engine_name=engine_full_name_parts.head
		}
		engine_name
	}

	def SetId(setid:Int):GEngine=
	{
		id=setid
		return this
	}

	def FromData(enginedata:Data)
	{
		if(enginedata == null) return

		if(enginedata.isInstanceOf[MapData])
		{
			val enginemapdata=enginedata.asInstanceOf[MapData]

			val pathdata=enginemapdata.G("{path}")

			if(pathdata != null)
			{
				if(pathdata.isInstanceOf[StringData])
				{
					path=pathdata.asInstanceOf[StringData].value
				}
			}

			val protocoldata=enginemapdata.G("{protocol}")

			if(protocoldata != null)
			{
				if(protocoldata.isInstanceOf[StringData])
				{
					protocol=protocoldata.asInstanceOf[StringData].value
				}
			}

			val autoloaddata=enginemapdata.G("{autoload}")

			if(autoloaddata != null)
			{
				if(autoloaddata.isInstanceOf[StringData])
				{
					autoload=false
					if(autoloaddata.asInstanceOf[StringData].value=="true")
					{
						autoload=true
					}
				}
			}

			val commandlinedata=enginemapdata.G("{commandline}")

			if(commandlinedata != null)
			{
				if(commandlinedata.isInstanceOf[StringData])
				{
					commandline=commandlinedata.asInstanceOf[StringData].value					
				}
			}

			CreatePathId
		}
	}

	def CreatePathId
	{
		pathid=path+" "+commandline
	}

	def ToData:Data=
	{
		var mapdata=Map[String,Data]()
		mapdata+=("path" -> StringData(path))
		mapdata+=("protocol" -> StringData(protocol))
		mapdata+=("autoload" -> StringData(""+autoload))
		mapdata+=("commandline" -> StringData(commandline))
		MapData(mapdata)
	}

	def SwitchAutoload
	{
		autoload= !autoload
		if(autoload)
		{
			if(!Loaded)
			{
				Load
			}
		} else {
			if(Loaded)
			{
				Unload
			}
		}
	}

	def ReportHTML:String=
	{
		val name=ParseEngineNameFromPath(path)
		val protocolselect=protocols.map(p =>{
			val style=if(p==protocol) "background-color: #ffffaf; border-style: solid; border-width: 1px; border-color: #afafaf; border-radius: 5px; padding: 3px;" else
				"padding: 4px;"
			s"""<span style='$style; cursor: pointer; font-size: 10px;' onmousedown="idstr='$id'; command='protocolselected'; param='$p';">$p</span>"""
		}).mkString("\n")
		val status=if(engineprocess==null) "<font color='red'>not active</font>" else "<font color='green'>active</font>"
		val autoloadbackground=if(autoload) "#ffffaf" else "#ffffff"
		val divbackground=if(engineprocess!=null) "#afffaf" else "#ffafaf"
		val autoloadstatus=if(autoload) "On" else "Off"
		val consoleopen=HasStage(s"{$pathid}")
		val consoletext=if(consoleopen) "Close Console/Settings" else "Open Console/Settings"
		var consolebuttontext=s"""
			|<td><input type="button" value="$consoletext" onclick="idstr='$id'; command='console';"></td>
		""".stripMargin
		if(engineprocess==null) consolebuttontext="<td>Tip: for Console/Settings press Load</td>"
		s"""
			|<div style="background-color: $divbackground; border-width: 2px; border-style: dotted; border-color: #afafaf; border-radius: 10px; margin: 3px;">
			|<table>
			|<tr><td>
			|<table>
			|<tr>
			|<td class="italiclabel">name</td>
			|<td style="padding-left: 3px; padding-right: 3px; border-style: dotted; border-radius: 5px; border-color: #7f7fff; font-size: 20px; font-weight: bold; color: #0000ff">$name</td>
			|<td><input type="button" value="Load" onclick="idstr='$id'; command='load';"></td>
			|<td><span onmousedown="idstr='$id'; command='autoload';" style="cursor: pointer; border-style: solid; border-width: 1px; border-radius: 5px; font-size: 12px; padding-left: 6px; padding-right: 9px; padding-top: 4px; padding-bottom: 4px; background-color: $autoloadbackground;">Auto Load</span></td>
			|<td><input type="button" value="Unload" onclick="idstr='$id'; command='unload';"></td>
			|<td class="italiclabel">status</td>
			|<td><span style="border-style: solid; border-color: #000000; border-width: 1px; border-radius: 5px; padding-left: 3px; padding-right: 3px; padding-bottom: 2px;">$status</span></td>
			|<td class="italiclabel">protocol</td>
			|<td>$protocolselect</td>
			|</tr>
			|</table>
			|</td></tr>
			|<tr><td>
			|<table>
			|<tr>
			|<td><input type="button" value="To top" onclick="idstr='$id'; command='top';"></td>
			|<td><input type="button" value="Up" onclick="idstr='$id'; command='up';"></td>
			|<td><input type="button" value="Down" onclick="idstr='$id'; command='down';"></td>
			|<td><input type="button" value="To bottom" onclick="idstr='$id'; command='bottom';"></td>
			|$consolebuttontext
			|</tr>
			|</table>
			|</td></tr>
			|<tr><td>
			|<table>
			|<tr>
			|<td><input type="button" value="..." onclick="idstr='$id'; command='editpath';"></td>
			|<td class="italiclabel">path</td><td><font color='blue'>$path</font></td>
			|<td><input type="button" value="Delete Engine" onclick="idstr='$id'; command='del';"></td>
			|</tr>
			|</table>
			|<table>
			|<tr>
			|<td class="italiclabel">command line</td>
			|<td><input type="text" id="commandline$id" value="$commandline"></td>
			|<td><input type="button" value="Apply command line" onclick="idstr='$id'; command='applycommandline';"></td>
			|</tr>
			|</table>
			|</td></tr>
			|</table>
			|</div>
		""".stripMargin
	}

	var running=false

	var startup=false

	val clip=Clipboard.getSystemClipboard()

	def GetOption(name:String,default:String=""):String=
	{
		val id=s"{engineoptions}#{$pathid}#{$name}"

		var value=GS(Cve(id),default)

		value
	}

	def GetBoolOption(name:String,default:Boolean):Boolean=
	{
		val boolstr=GetOption(name,""+default)

		if(boolstr=="true") return true

		false
	}

	def XBOARDIssueExitOrForce
	{
		if(xboardstate=="Analyzing")
		{
			IssueCommand("exit")
		} else {
			IssueCommand("force")
		}				
	}

	def Start(g:game)
	{
		if(engineprocess==null) return
		if(startup) return
		if(running) return
		thinkingoutput=ThinkingOutput()
		OpenConsole
		val autosetfen=GetBoolOption("Auto set FEN",true)
		val commandaftersetfen=GetOption("Command after set FEN","")
		val autostart=GetBoolOption("Auto start",true)

		def IssueCommandAfterSetFEN
		{
			if(commandaftersetfen!="") IssueCommand(commandaftersetfen)
		}

		if(protocol=="XBOARD")
		{
			// truealgebline is the line leading to the current position from the game root position
			// in engine algebraic notation
			// this is called 'true' to distinguish it from internally used Chess960 algebraic notation
			val truealgebline=g.current_line_true_algeb_moves

			val fen=g.report_fen

			if(autosetfen)
			{
				IssueCommand("force")
				IssueCommand("post")
				IssueVariantXBOARD

				//if(g.is_from_startpos)
				if(false)
				{
					IssueCommand("new")
					IssueCommand("force")

					for(truealgeb<-truealgebline)
					{
						IssueCommand(s"usermove $truealgeb")
					}
				}
				else
				{
					if(!features.setboard) return

					IssueCommand(s"setboard $fen")
				}

				IssueCommandAfterSetFEN

				if(autostart)
				{
				
					IssueCommand("analyze")

					root_fen=fen

					running=true

				}
			}
		}
		if(protocol=="UCI")
		{	
			if(autosetfen)
			{
				val fen=g.report_fen

				root_fen=fen

				IssueCommand("position fen "+fen)

				IssueCommandAfterSetFEN

				if(autostart)
				{

					IssueCommand("go infinite")

					running=true

				}
			}
		}
	}

	var bestmovereceived=false

	def Stop
	{
		StopInner()
	}

	def StopForced
	{
		StopInner(forced=true)
	}

	def StopInner(forced:Boolean=false)
	{
		if(engineprocess==null) return
		if(startup) return
		if(forced)
		{
			if(!thinking) return
		} else {
			if(!running) return
		}
		if(protocol=="XBOARD")
		{
			XBOARDIssueExitOrForce
			val xboardsleep=Builder.GD("{components}#{timingsxboardsleep}",200.0).toInt
			try{Thread.sleep(xboardsleep)}catch{case e:Throwable=>{}}
			running=false
		}
		if(protocol=="UCI")
		{
			bestmovereceived=false

			IssueCommand("stop")

			if(!forced)
			{
				var timeoutcnt=0;
				while((!bestmovereceived)&&(timeoutcnt< 100))
				{
					try{Thread.sleep(50)}catch{case e:Throwable=>{}}
					timeoutcnt+=1;
				}
				if(timeoutcnt>=100)
				{
					val blob=s"""
						|<vbox padding="10" gap="10">
						|<label style="-fx-font-size: 24px; -fx-text-fill: #ff0000; -fx-font-weight: bold;" text="Engine timed out on stop."/>
						|<label style="-fx-font-size: 18px; -fx-text-fill: #0000ff;" text="Check protocol."/>
						|</vbox>
					""".stripMargin
					MyStage("{enginetimeout}",modal=true,useheight=false,usewidth=false,
						handler=handler,title="Engine time out",blob=blob)
				}
			}
			running=false
		}
	}

	def CheckRestart(g:game)
	{
		if(engineprocess==null) return
		if((!GetBoolOption("Auto start",true))||(!GetBoolOption("Auto set FEN",true))) return
		if(running)
		{
			Stop
			Start(g)
		}
	}

	def Strip(line:String):String=
	{
		var sline=line
		sline=sline.replaceAll("\\r|\\n|^\\s+|\\s+$","")
		sline=sline.replaceAll("\\s+"," ")
		sline
	}

	def Tokens(line:String):List[String]=
	{
		Strip(line).split(" ").toList
	}

	case class Tokenizer(line:String="")
	{
		var tokens=Tokens(line)

		def Get:String=
		{
			if(tokens.length>0)
			{
				val token=tokens.head
				tokens=tokens.tail
				return token
			}
			return null
		}

		def Poll:String=
		{
			if(tokens.length>0)
			{
				val token=tokens.head
				return token
			}
			return null
		}

		def GetRest:String=
		{
			if(tokens.length>0)
			{
				val str=tokens.mkString(" ")
				tokens=List[String]()
				return str
			}
			return null
		}

		def HasToken:Boolean=
		{
			tokens.length>0
		}
	}

	def ParseInt(str:String,default:Int):Int=
	{
		if(str==null) return default
		try
		{
			val intvalue=str.toInt
			return intvalue
		}
		catch{case e: Throwable => {}}
		return default
	}

	def IsInt(str:String):Boolean=
	{
		if(str==null) return false
		try
		{
			val intvalue=str.toInt
			return true
		}
		catch{case e: Throwable => {}}
		return false
	}

	case class PvItem(
		var multipv:Int=0,
		var hasmultipv:Boolean=false,
		var depth:Int=0,
		var hasdepth:Boolean=false,
		var nodes:Int=0,
		var nodesverbal:String="",
		var hasnodes:Boolean=false,
		var time:Int=0,
		var hastime:Boolean=false,
		var nps:Int=0,
		var npsverbal:String="",
		var hasnps:Boolean=false,
		var scorestr:String="",
		var scorekind:String="",
		var scorecp:Int=0,
		var scoremate:Int=0,
		var scorenumerical:Int=0,
		var signedscorenumerical:String="",
		var scoreverbal:String="",
		var hasscore:Boolean=false,
		var pv:String="",
		var pvrest:List[String]=List[String](),
		var pvreststr:String="",
		var haspv:Boolean=false,
		var bestmove:String="",
		var bestmovesan:String="",
		var pvsan:String=""
	)
	{
		def AsString:String=
		{
			s"$bestmove $signedscorenumerical depth $depth nodes $nodes nps $nps pv $pvreststr"
		}
		def FormatNodes(nodes:Int,unit:Int):String=
		{
			"%.2f".format(nodes.toDouble/unit.toDouble)
		}
		def GetNodesVerbal(nodes:Int):String=
		{
			if(nodes< 1000) return ""+nodes else
			if(nodes< 1000000) return ""+FormatNodes(nodes,1000)+" kN" else return FormatNodes(nodes,1000000)+" MN"
		}
		def GetNpsVerbal(nodes:Int):String=
		{
			if(nps< 1000) return ""+nps else
			if(nps< 1000000) return ""+FormatNodes(nps,1000)+" kN/s" else return FormatNodes(nps,1000000)+" MN/s"
		}
		def pvToSan(pv:String)
		{
			val dummy=new game
			dummy.set_from_fen(root_fen)
			val algebparts=pv.split(" ")
			var first=true
			bestmovesan=algebparts(0)
			for(uci<-algebparts)
			{
				val algeb=dummy.b.to_chess960_algeb(uci)
				if(dummy.b.isAlgebLegal(algeb))
				{
					val m=move(fromalgeb=algeb)
					if(first)
					{
						bestmovesan=dummy.b.toSan(m)
						first=false
					}
					dummy.makeMove(m)
				}
			}
			pvsan=dummy.current_line_pgn
		}
		def ParseLine(line:String):PvItem=
		{
			val tokenizer=Tokenizer(line)
			if(protocol=="UCI")
			{
				if(!tokenizer.HasToken) return this
				if(tokenizer.Get!="info") return this
				while(tokenizer.HasToken)
				{
					val name=tokenizer.Get
					if(name=="multipv")
					{
						multipv=ParseInt(tokenizer.Get,multipv)
						hasmultipv=true
					}
					if(name=="score")
					{
						val kind=tokenizer.Get
						val value=ParseInt(tokenizer.Get,if(kind=="mate") scoremate else scorecp)
						scorestr=kind+" "+value
						if(kind=="mate")
						{
							scoremate=value
							if(value>=0)
							{
								scorenumerical=10000-value
							} else {
								scorenumerical= -10000-value
							}
						} else {
							scorecp=value
							scorenumerical=value
						}
						signedscorenumerical=if(scorenumerical>0) "+"+scorenumerical else ""+scorenumerical
						scoreverbal=if(kind=="mate") "mate "+scoremate else signedscorenumerical
						scorekind=kind
						hasscore=true
					}
					if(name=="depth")
					{
						depth=ParseInt(tokenizer.Get,depth)
						hasdepth=true
					}
					if(name=="nodes")
					{
						nodes=ParseInt(tokenizer.Get,nodes)
						nodesverbal=GetNodesVerbal(nodes)
						hasnodes=true
					}
					if(name=="nps")
					{
						nps=ParseInt(tokenizer.Get,nps)
						npsverbal=GetNodesVerbal(nps)
						hasnps=true
					}
					if(name=="time")
					{
						time=ParseInt(tokenizer.Get,time)
						hastime=true
					}
					if(name=="pv")
					{
						pv=tokenizer.GetRest
						if(pv!=null)
						{
							haspv=true
							val pvtokens=Tokens(pv)
							bestmove=pvtokens.head
							pvrest=pvtokens.tail
							pvreststr=pvrest.mkString(" ")
							pvToSan(pv)
						}
					}
				}
			}
			if(protocol=="XBOARD")
			{
				val parts=line.split(" ").toList
				val len=parts.length
				if(len< 5) return this
				if(IsInt(parts(0))&&IsInt(parts(1))&&IsInt(parts(2))&&IsInt(parts(3)))
				{
					depth=ParseInt(parts(0),depth)
					hasdepth=true

					scorenumerical=ParseInt(parts(1),depth)
					signedscorenumerical=if(scorenumerical>0) "+"+scorenumerical else ""+scorenumerical
					scoreverbal=if(scorenumerical>0) "+"+scorenumerical else ""+scorenumerical
					hasscore=true

					time=ParseInt(parts(2),time)*10
					hastime=true

					nodes=ParseInt(parts(3),nodes)
					nodesverbal=GetNodesVerbal(nodes)
					hasnodes=true

					tokenizer.Get
					tokenizer.Get
					tokenizer.Get
					tokenizer.Get
					pv=tokenizer.GetRest
					haspv=true
					val pvtokens=Tokens(pv)
					bestmove=pvtokens.head
					pvrest=pvtokens.tail
					pvreststr=pvrest.mkString(" ")
					pvToSan(pv)
				}
			}
			return this
		}

		def UpdateWith(ui:PvItem):PvItem=
		{
			if(ui.hasmultipv) multipv=ui.multipv ; hasmultipv=true
			if(ui.hasdepth) depth=ui.depth ; hasdepth=true
			if(ui.hasnodes) nodes=ui.nodes ; hasnodes=true
			if(ui.hastime) time=ui.time ; hastime=true
			if(ui.hasnps) nps=ui.nps ; hasnps=true
			if(ui.haspv)
			{
				pv=ui.pv
				pvrest=ui.pvrest
				pvreststr=ui.pvreststr
				bestmove=ui.bestmove
				bestmovesan=ui.bestmovesan
				pvsan=ui.pvsan
				haspv=true
			}
			if(ui.hasscore)
			{
				scorestr=ui.scorestr
				scorekind=ui.scorekind
				scorecp=ui.scorecp
				scoremate=ui.scoremate
				scorenumerical=ui.scorenumerical
				signedscorenumerical=ui.signedscorenumerical
				scoreverbal=ui.scoreverbal
				nodesverbal=ui.nodesverbal
				npsverbal=ui.npsverbal
				hasscore=true
			}
			return this
		}

		def ReportHTMLTableRow:String=
		{
			val scorecolor=if(scorenumerical>=0) "#007f00" else "#7f0000"
			val timeformatted=formatDuration(time,"mm:ss")
			s"""
			|<tr>
			|<td><font color="blue"><b>$bestmovesan</b></font></td>
			|<td><font color="$scorecolor"><b>$scoreverbal</b></font></td>
			|<td><font color="blue"><b>$depth</b></font></td>
			|<td><font color="#007f7f"><small>$timeformatted<small></font></td>
			|<td><small>$nodesverbal</small></td>
			|<td><small>$npsverbal</small></td>
			|<td><font color="#0000af"><small>$pvsan</small></font></td>
			|</tr>
			""".stripMargin
		}
	}

	case class DepthItem(depth:Int=1)
	{
		var maxmultipv=1
		var pvitems=Map[Int,PvItem]()

		def SortedMultipvs:List[Int]=pvitems.keys.toList.sorted

		def ExtremePv(lowest:Boolean=true):PvItem=
		{
			val sortedmultipvs=SortedMultipvs
			if(sortedmultipvs.length>0)
			{
				if(lowest)
				{
					return pvitems(sortedmultipvs(0))
				} else {
					return pvitems(sortedmultipvs(sortedmultipvs.length-1))
				}
			} else {
				return PvItem()
			}
		}

		def ParseLine(line:String,parent:ThinkingOutput=null)
		{
			val pvitem=PvItem().ParseLine(line)
			if(pvitem.haspv)
			{
				val multipv=if(pvitem.hasmultipv) pvitem.multipv else { maxmultipv+=1 ; maxmultipv }

				if(!pvitems.contains(multipv)) pvitems+=(multipv->PvItem())

				pvitems+=(multipv->pvitems(multipv).UpdateWith(pvitem))

				if(parent!=null)
				{
					parent.access_bestmove(update=true,newbestmove=pvitem.bestmove)
				}
			}
			if(pvitem.hasscore)
			{
				if(parent!=null)
				{
					parent.access_scorenumerical(update=true,newscorenumerical=pvitem.scorenumerical)
				}
			}
		}

		def ReportHTML:String=
		{
			val multipvs=SortedMultipvs
			val multipvscontent=(for(multipv<-multipvs) yield pvitems(multipv).ReportHTMLTableRow).mkString("\n")
			s"""
				|<tr style="font-size: 12px;">
				|<td width="40">Move</td>
				|<td width="40">Score</td>
				|<td width="20">Dpt</td>
				|<td width="27">Time</td>
				|<td width="60">Nodes</td>
				|<td width="60">Nps</td>
				|<td>Pv</td>
				|</tr>
				|$multipvscontent
				|<tr>
				|<td colspan="7"><hr></td>
				|</tr>
			""".stripMargin
		}
	}

	var thinkingoutput=ThinkingOutput()

	case class ThinkingOutput()
	{
		var maxdepth=1
		var depthitems=Map[Int,DepthItem]()

		var bestmove:String=null
		var scorenumerical:Int=0

		def access_bestmove(update:Boolean=false,newbestmove:String=null):String=
		{
			this.synchronized
			{
				if(update) bestmove=newbestmove
				bestmove
			}
		}

		def access_scorenumerical(update:Boolean=false,newscorenumerical:Int=0):Int=
		{
			this.synchronized
			{
				if(update) scorenumerical=newscorenumerical
				scorenumerical
			}
		}

		def UpdateEngineOut(content:String)
		{
			val cwe=GetWebEngine(s"{$pathid}#{engineouttext}")
			if(cwe==null) return
			cwe.loadContent(content)
		}

		def ParseLine(line:String)
		{
			val pvitem=PvItem().ParseLine(line)
			val depth=if(pvitem.hasdepth) pvitem.depth else maxdepth
			if(depth>maxdepth) maxdepth=depth
			if(!depthitems.contains(depth)) depthitems+=(depth->DepthItem(depth))
			depthitems(depth).ParseLine(line,this)
			val ei=ExecutionItem(
				client="GEngine.ParseLine",
				code=new Runnable{def run{
				UpdateEngineOut(ReportHTML)				
			}})
			MyActor.queuedExecutor ! ei
		}

		def SortedDepths:List[Int]=depthitems.keys.toList.sorted.reverse

		def HighestDepthItem:DepthItem=
		{
			val sorteddepths=SortedDepths
			val len=sorteddepths.length
			for(i<- 0 to (len-1)) if(depthitems(sorteddepths(i)).ExtremePv().haspv) return depthitems(sorteddepths(i))
			DepthItem()
		}

		def ExtremePv(lowest:Boolean=true):PvItem=
		{
			HighestDepthItem.ExtremePv(lowest)
		}

		def ReportHTML:String=
		{
			val depths=SortedDepths
			val depthscontent=(for(depth<-depths) yield depthitems(depth).ReportHTML).mkString("\n")
			s"""
				|<table cellpadding="3" cellspacing="3">
				|$depthscontent
				|<table>
			""".stripMargin
		}
	}
}

case class GEngineList(var wid:String=null)
{
	var enginelist=Array[GEngine]()

	var multipv=1

	def TopRunning:GEngine=
	{
		for(e<-enginelist) if(e.running) return e
		null
	}

	def TopRunningHasPv:Boolean=
	{
		val tr=TopRunning
		if(tr==null) return false
		tr.thinkingoutput.bestmove!=null
	}

	var lastbestmove:String=null

	def WatchAnalysis
	{
		val tr=TopRunning
		if(tr==null) return
		val extremepv=tr.ExtremePv(lowest=false)
		if(!extremepv.haspv) return
		val bestmove=extremepv.bestmove
		lastbestmove=bestmove
		val score=extremepv.scorenumerical
		val gb=MyApp.GetMainBoard
		gb.print_score(score)
		gb.highlight_engine_move(bestmove,score)
	}

	def MakeAnalyzedMove(uselastbestmove:Boolean=true,checkrestart:Boolean=true,undo:Boolean=false,addtobook:Boolean=false)
	{
		var bestmove:String=null
		var scorenumerical:Int=0
		
		if(uselastbestmove)
		{
			bestmove=lastbestmove
		}
		else
		{			
			if(!TopRunningHasPv)
			{
				bestmove=lastbestmove
			}
			else
			{
				val trto=TopRunning.thinkingoutput
				bestmove=trto.access_bestmove()
				scorenumerical=trto.access_scorenumerical()
			}			
		}

		if(bestmove==null)
		{
			return
		}
		
		val b=Commands.g.b
		val chess960_algeb=b.to_chess960_algeb(bestmove)
		
		if(!b.isAlgebLegal(chess960_algeb,test=false))
		{			
			return
		}

		val m=move(fromalgeb=chess960_algeb)
		val san=b.toSan(m)
		
		val ei=ExecutionItem(
			client="GEngineList.MakeAnalyzedMove",
			code=new Runnable{def run{

			Commands.MakeSanMove(san)

			if(addtobook)
			{
				Commands.AddMoveToBook(addcomment="E "+scorenumerical,dosave=false)
				Commands.ColorMove(san,scorenumerical,dosave=false)
				Commands.SaveGamePos
				Commands.MakeSanMove(san)
			}

			if(undo) Commands.g.back

			if(Builder.GB("{components}#{boardcontrolpanelclick}",false))
			{
				Robot.ClickMove(m)
			}

			MyApp.UpdateFunc(checkrestart=checkrestart)
			if(checkrestart) CheckRestartAll(Commands.g) else
			{				
				EngineManager.StopAllEngines
			}
			EngineManager.move_made=true

		}})
		MyActor.queuedExecutor ! ei
	}

	def SetMultipv(set_multipv:Int,g:game)
	{
		multipv=set_multipv
		for(engine<-enginelist) engine.SetMultipv(multipv,g)
	}

	def StartAll(g:game)
	{
		for(engine<-enginelist) engine.Start(g)
		Update
	}

	def StopAll()
	{
		for(engine<-enginelist) engine.Stop
	}

	def CheckRestartAll(g:game)
	{
		for(engine<-enginelist) engine.CheckRestart(g)
	}

	def handler(ev:MyEvent)
	{
		if(ev.kind=="stage closed")
		{
			Update
		}
	}

	def BrowsePath(id:Int)
	{
		val fc=new FileChooser()

		if(new File(Settings.get_engine_dir).exists)
		{
			fc.setInitialDirectory(new File(Settings.get_engine_dir))
		}

		val f=fc.showOpenDialog(new Stage())

		if(f!=null)
		{

			val dir=f.getParent()

			Settings.set_engine_dir(dir)

			val path=f.getPath()

			enginelist(id).SetPath(path)

		}
	}

	def Del(id:Int)
	{
		val engine=enginelist(id)
		engine.Unload
		var i= -1
		var j= -1
		enginelist=for(engine <- enginelist; if({ i+=1; i != id })) yield { j+=1; engine.SetId(j) }
	}

	def Move(id:Int,dir:Int):Boolean=
	{
		val last=enginelist.length-1
		if(((id==0)&&(dir== -1))||((id==last)&&(dir==1))) return false
		val temp=enginelist(id)
		enginelist(id)=enginelist(id+dir).SetId(id)
		enginelist(id+dir)=temp.SetId(id+dir)
		true
	}

	def ToEdge(id:Int,dir:Int)
	{
		var i=id
		while(Move(i,dir)){i+=dir}
	}

	def Renumber
	{
		var i=0
		for(engine<-enginelist)
		{
			engine.SetId(i)
			i+=1
		}
	}

	def Handle
	{
		val we=GetWebEngine(wid)

		val command=we.executeScript("command").toString()
		val idstr=we.executeScript("idstr").toString()
		val param=we.executeScript("param").toString()

		if(command=="applycommandline")
		{
			val cv=we.executeScript(s"document.getElementById('commandline$idstr').value").toString()
			enginelist(idstr.toInt).SetCommandLine(cv)
			Update
		}

		if(command=="console")
		{
			enginelist(idstr.toInt).Console
			Update
		}

		if(command=="add")
		{
			enginelist=GEngine(0,handler=handler)+:enginelist
			Renumber
			Update
		}

		if(command=="top")
		{
			ToEdge(idstr.toInt,-1)
			Update
		}

		if(command=="up")
		{
			Move(idstr.toInt,-1)
			Update
		}

		if(command=="down")
		{
			Move(idstr.toInt,1)
			Update
		}

		if(command=="bottom")
		{
			ToEdge(idstr.toInt,1)
			Update
		}

		if(command=="editpath")
		{
			BrowsePath(idstr.toInt)
			Update
		}

		if(command=="del")
		{
			Del(idstr.toInt)
			Update
		}

		if(command=="protocolselected")
		{
			enginelist(idstr.toInt).protocol=param
			Update
		}

		if(command=="load")
		{
			enginelist(idstr.toInt).Load
			Update
		}

		if(command=="unload")
		{
			enginelist(idstr.toInt).Unload
			Update
		}

		if(command=="autoload")
		{
			enginelist(idstr.toInt).SwitchAutoload
			Update
		}
	}

	def Update
	{
		Save
		val content=ReportHTML
		val we=GetWebEngine(wid)
		val st=we.executeScript("document.body.scrollTop").toString().toDouble
		we.loadContent(content)
		we.getLoadWorker().stateProperty().addListener(new ChangeListener[State]{
	        def changed(ov: ObservableValue[_ <: State], oldState: State, newState: State)
	        {
                if (newState == State.SUCCEEDED)
                {
                	we.executeScript("window.scrollTo(" + 0 + ", " + st + ")");
				}
			}
		})
	}

	def UnloadAll
	{
		for(engine<-enginelist)
		{
			engine.Unload
		}
	}

	def LoadAllAuto
	{
		for(engine<-enginelist)
		{
			if(engine.autoload) engine.Load
		}
	}

	def IndexOfPathId(pathid:String):Int=
	{
		var i=0
		for(engine<-enginelist) if(engine.pathid==pathid) return i else i+=1
		return -1
	}

	def BringUp(pathid:String):Boolean=
	{
		val i=IndexOfPathId(pathid)
		if(i< 0) return false
		ToEdge(i,-1)
		enginelist(0).Load
		val ei=ExecutionItem(
			client="GEngineList.BringUp",
			code=new Runnable{def run{
			Update
		}})
		MyActor.queuedExecutor ! ei
		if(enginelist(0).Loaded) return true
		false
	}

	def Load
	{
		enginelist=Array[GEngine]()
		val enginelistdata=Get(Cve("{enginelist}")).asInstanceOf[ArrayData]
		if(enginelistdata != null)
		{
			var i= -1
			enginelist=for(enginedata<-enginelistdata.array.toArray) yield  { i+=1; GEngine(i,enginedata,handler=handler) }
		}
	}

	def ReLoad
	{
		UnloadAll
		Load
		LoadAllAuto
		Update
	}

	def ToData:Data=
	{
		val d=ArrayData()
		for(engine <- enginelist) d.array+=engine.ToData
		d
	}

	def Save
	{
		Set(Cve("{enginelist}"),ToData)
	}

	def ReportHTML:String=
	{
		val listhtml=enginelist.map(e => e.ReportHTML).mkString("\n")
		s"""
			|<style>
			|.italiclabel {
    		|	font-style: italic;
    		|	font-size: 12px;
			|}
			|</style>
			|<script>
			|var command='';
			|var idstr='0';
			|var param='';
			|</script>
			|<input type="button" value="Add new engine" onclick="command='add';""><br>
			|$listhtml
		""".stripMargin
	}
}

////////////////////////////////////////////////////////////////////