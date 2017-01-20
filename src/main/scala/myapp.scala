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
import javafx.collections._

import collection.JavaConversions._

import java.io._
import scala.io._

import Builder._

import scala.concurrent._
import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import akka.util._

import scala.concurrent.ExecutionContext.Implicits.global

import org.apache.commons.lang.time.DurationFormatUtils.formatDuration
import org.apache.commons.lang.time.DateFormatUtils._

////////////////////////////////////////////////////////////////////

// MyApp is responsible for the specifics of the concrete application hosted by myguibuilder
// it should initialize the ModuleManager then wait for being started and stopped by
// GuiClass defined in builder.scala as the class to be passed for execution
// as a JavaFX application to the Application.launch method in the main function

object MyApp
{

	// Init will be called by GuiClass before Start
	// it should:
	// - initialize the ModuleManager ( if necessary )
	// - set prefix bindings in Builder ( if necessary )
	// - initialize stuff/values.xml on the first run ( if necessary )
	// note: the ModuleManager by default contains the Builder module	
	def Init
	{
		ModuleManager.Add(MyActor)
		ModuleManager.Add(Commands)
		ModuleManager.Add(EngineManager)
		ModuleManager.Add(Robot)
		if(!new File(Builder.valuespath).exists())
		{
			DataUtils.WriteStringToFile(Builder.valuespath,s"""
<m>

<m key="components">

<m key="mainboard">
	<m key="font">
		<a key="items">
		<s>AVENFONT</s>
		<s>CASEFONT</s>
		<s>MERIFONTNEW</s>
		<s>MARRFONT</s>
		<s>LUCEFONT</s>
		</a>
		<s key="selected">${GuiBoard.FONT}</s>
	</m>
	<m key="material">
		<a key="items">
		<s>wood</s>
		<s>marble</s>
		<s>rock</s>
		</a>
		<s key="selected">${GuiBoard.MATERIAL}</s>
	</m>
	<s key="whitepiececolor">${GuiBoard.WHITEPIECECOLOR}</s>
	<s key="blackpiececolor">${GuiBoard.BLACKPIECECOLOR}</s>
	<s key="lightsquarecolor">${GuiBoard.LIGHTSQUARECOLOR}</s>
	<s key="darksquarecolor">${GuiBoard.DARKSQUARECOLOR}</s>
	<s key="piecesize">${GuiBoard.PIECESIZE}</s>
	<s key="boardopacity">${GuiBoard.BOARDOPACITY}</s>
	<s key="piecefactor">${GuiBoard.PIECEFACTOR}</s>
</m>

<m key="evalalldepth">
	<a key="items">
		<s>1</s>
		<s>2</s>
		<s>3</s>		
		<s>4</s>
		<s>5</s>
		<s>6</s>
		<s>7</s>
		<s>8</s>
		<s>9</s>
		<s>10</s>
		<s>11</s>
		<s>12</s>
		<s>13</s>
		<s>14</s>
		<s>15</s>
		<s>16</s>
		<s>17</s>
		<s>18</s>
		<s>19</s>
		<s>20</s>
		<s>21</s>
		<s>22</s>
		<s>23</s>
		<s>24</s>
		<s>25</s>
		<s>26</s>
		<s>27</s>
		<s>28</s>
		<s>29</s>
		<s>30</s>
		<s>31</s>
		<s>32</s>
		<s>33</s>
		<s>34</s>
		<s>35</s>
		<s>36</s>
		<s>37</s>
		<s>38</s>
		<s>39</s>
		<s>40</s>
	</a>
	<s key="selected">20</s>
</m>

<m key="minimaxdepth">
	<a key="items">
		<s>0</s>
		<s>1</s>
		<s>2</s>
		<s>3</s>		
		<s>4</s>
		<s>5</s>
		<s>6</s>
		<s>7</s>
		<s>8</s>
		<s>9</s>
		<s>10</s>
		<s>11</s>
		<s>12</s>
		<s>13</s>
		<s>14</s>
		<s>15</s>
		<s>16</s>
		<s>17</s>
		<s>18</s>
		<s>19</s>
		<s>20</s>
		<s>21</s>
		<s>22</s>
		<s>23</s>
		<s>24</s>
		<s>25</s>
		<s>26</s>
		<s>27</s>
		<s>28</s>
		<s>29</s>
		<s>30</s>
		<s>31</s>
		<s>32</s>
		<s>33</s>
		<s>34</s>
		<s>35</s>
		<s>36</s>
		<s>37</s>
		<s>38</s>
		<s>39</s>
		<s>40</s>
	</a>
	<s key="selected">5</s>
</m>

<m key="timecontrolnumberofmoves">
	<a key="items">
		<s>1</s>
		<s>2</s>
		<s>3</s>
		<s>4</s>
		<s>5</s>
		<s>10</s>
		<s>20</s>
		<s>30</s>
		<s>40</s>
		<s>50</s>
		<s>75</s>
		<s>100</s>
	</a>
	<s key="selected">40</s>
</m>

<m key="timecontroltime">
	<a key="items">
		<s>1</s>
		<s>2</s>
		<s>3</s>
		<s>4</s>
		<s>5</s>
		<s>10</s>
		<s>20</s>
		<s>30</s>
		<s>40</s>
		<s>50</s>
		<s>75</s>
		<s>100</s>
	</a>
	<s key="selected">5</s>
</m>

<m key="timecontrolincrementaltime">
	<a key="items">
		<s>1</s>
		<s>2</s>
		<s>3</s>
		<s>4</s>
		<s>5</s>
		<s>6</s>
		<s>7</s>
		<s>8</s>
		<s>9</s>
		<s>10</s>
		<s>15</s>
		<s>20</s>
		<s>25</s>
		<s>30</s>
		<s>35</s>
		<s>40</s>
		<s>45</s>
		<s>50</s>
		<s>75</s>
		<s>100</s>
	</a>
	<s key="selected">2</s>
</m>

<m key="timecontrolincrementalincrement">
	<a key="items">
		<s>1</s>
		<s>2</s>
		<s>3</s>
		<s>4</s>
		<s>5</s>
		<s>6</s>
		<s>7</s>
		<s>8</s>
		<s>9</s>
		<s>10</s>
		<s>15</s>
		<s>20</s>
		<s>25</s>
		<s>30</s>
		<s>60</s>
		<s>120</s>
		<s>180</s>
		<s>240</s>
		<s>300</s>
	</a>
	<s key="selected">3</s>
</m>

</m>

<m key="settings">

<m key="buildcutoff">
	<a key="items">
	<s>1</s>
	<s>3</s>
	<s>5</s>
	<s>10</s>
	<s>20</s>
	<s>30</s>
	<s>40</s>
	<s>50</s>
	<s>100</s>
	</a>
	<s key="selected">${Settings.DEFAULT_BUILD_CUTOFF}</s>
</m>

</m>

</m>
""")
		}
	}

	// GuiClass will call Start on JavaFX start
	// Start is only called after the ModuleManager has started all modules
	// it should create a scene for primaryStage, show primaryStage, then return
	def Start(primaryStage:Stage)
	{
		// blob that describes the content of the main window
		val rmvariantcontent=(for(v<-Settings.SUPPORTED_VARIANTS) yield {
			val selected=if(v==Settings.get_variant) """ selected="true"""" else ""
			s"""<radiomenuitem id="{rm$v}"$selected togglegroup="rmvariant" text="$v"/>"""
		}).mkString("\n")
		val rmmultipvcontent=(for(mpv<-List("1","2","3","4","5","6","7","8","9","10","20","50","200")) yield {
			val selected=if(mpv.toInt==Settings.get_multipv) """ selected="true"""" else ""
			s"""<radiomenuitem id="{rmmultipv$mpv}"$selected togglegroup="rmmultipv" text="$mpv"/>"""
		}).mkString("\n")
		val bookbuttonstyle="-fx-padding: 0px 2px 2px 6px; -fx-font-weight: bold; -fx-font-size: 16px; -fx-font-family: monospace;"
		val blob=s"""
			|<vbox>
			|<menubar>
			|<menu text="Copy">
			|<menuitem id="{copyfenmenu}" text="Copy FEN to clipboard"/>
			|<menuitem id="{copypgnmenu}" text="Copy PGN to clipboard"/>
			|<menuitem id="{copypgntreemenu}" text="Copy PGN tree to clipboard"/>
			|<menuitem id="{copycurrentlinemenu}" text="Copy current line"/>
			|<menuitem id="{copycurrentlinealgebmenu}" text="Copy current line algeb"/>
			|</menu>
			|<menu text="Paste">
			|<menuitem id="{pastefenmenu}" text="Paste FEN from clipboard"/>
			|<menuitem id="{pastepgnmenu}" text="Paste PGN from clipboard"/>
			|</menu>
			|<menu text="PGN file">
			|<menuitem id="{openpgnmenu}" text="Open PGN"/>
			|<menuitem id="{openmultiplegamepgnmenu}" text="Open multiple game PGN"/>
			|<menuitem id="{savepgnasmenu}" text="Save PGN as"/>
			|</menu>
			|<menu text="Tools">
			|<menuitem id="{minimaxtoolmenu}" text="Minimax tool"/>
			|<menuitem id="{buildbookpgnmenu}" text="Build book PGN"/>
			|<menuitem id="{lookupsolution}" text="Look up solution"/>			
			|<menuitem id="{filterpgnmenu}" text="Filter PGN"/>
			|<menuitem id="{showdefaultbook}" text="Show default book"/>
			|</menu>
			|<menu text="Engine game">
			|<menuitem id="{enginegametimecontrolmenu}" text="Time control"/>
			|<menuitem id="{enginegamestartmenu}" text="Start game"/>
			|<menuitem id="{enginegamestartfromcurrentmenu}" text="Start game from current position"/>
			|<menuitem id="{enginegameaborttmenu}" text="Abort game"/>
			|<menuitem id="{enginegamestatsmenu}" text="Stats"/>
			|</menu>
			|<menu text="Settings">
			|<menuitem id="{evalallsettingsmenu}" text="Eval all"/>
			|<menuitem id="{boardsettingsmenu}" text="Board"/>
			|<menuitem id="{booksettingsmenu}" text="Book"/>
			|<menuitem id="{profilesettingsmenu}" text="Profile"/>
			|<menuitem id="{setupboardmenu}" text="Setup board manually"/>
			|<menuitem id="{recordrectmenu}" text="Record rect"/>
			|<menuitem id="{learncolorsmenu}" text="Learn colors"/>
			|<menuitem id="{timingsmenu}" text="Timings"/>
			|</menu>
			|<menu text="Multipv">
			|$rmmultipvcontent
			|</menu>
			|<menu text="Variant">
			|$rmvariantcontent
			|</menu>
			|</menubar>
			|<hbox>
			|<vbox gap="1" padding="0">
			|<guiboard id="{mainboard}"/>
			|<hbox padding="0" gap="10">
			|<label id="{boardfenlabel}"/>
			|<label id="{heapsizelabel}"/>
			|</hbox>
			|<vbox>
			|<hbox id="boardcontrolpanelhbox" bimage="control.jpg" cover="false" gap="3" padding="5">
			|	<button id="{boardcontrolpanelflip}" img="icons/flip.png" style="round"/>
			|	<button id="{boardcontrolpanelreset}" img="icons/resett.png" style="round"/>
			|	<button id="{boardcontrolpaneltobegin}" img="icons/begint.png" style="round"/>
			|	<button id="{boardcontrolpanelback}" img="icons/backt.png" style="round"/>
			|	<button id="{boardcontrolpanelforward}" img="icons/forwardt.png" style="round"/>
			|	<button id="{boardcontrolpaneltoend}" img="icons/endt.png" style="round"/>
			|	<button id="{boardcontrolpaneldel}" img="icons/delt.png" style="round"/>
			|	<button id="{boardcontrolpanelstart}" img="icons/startt.png" style="round"/>
			|	<button id="{boardcontrolpanelstop}" img="icons/stopt.png" style="round"/>
			|	<button id="{boardcontrolpanelmake}" img="icons/maket.png" style="round"/>
			|	<button id="{boardcontrolpaneloptions}" img="icons/optionst.png" style="round"/>
			|	<button id="{boardcontrolpanelhint}" img="icons/hint.png" style="round"/>
			|</hbox>
			|<slider id="{boardcontrolpanelnavigator}" delay="5" value="0.0" width="450.0" height="50.0" min="0.0" max="100.0" majortickunit="10.0" minortickcount="4" showticklabels="true" showtickmarks="true"/>
			|</vbox>
			|</vbox>
			|<tabpane id="{maintabpane}">
			|<tab caption="PGN">
			|<webview id="{colorpgnwebview}"/>
			|</tab>
			|<tab caption="Moves">
			|<webview id="{moveswebview}"/>
			|</tab>
			|<tab caption="Book">
			|<vbox>
			|<hbox bimage="wood.jpg" cover="false" padding="5">
			|<button id="{pastepgnbutton}" img="icons/paste.png"/>
			|<button id="{addmovetobook}" img="icons/add.png"/>			
			|<button id="{addexclammovetobookfw}" style="-fx-background-color: #7fff7f; $bookbuttonstyle" text="!f"/>
			|<button id="{addpromisingmovetobookfw}" style="-fx-background-color: #7f7fff; $bookbuttonstyle" text="!?f"/>
			|<button id="{adddoubleexclammovetobookfw}" style="-fx-background-color: #00ff00; $bookbuttonstyle" text="!!f"/>			
			|<button id="{addmatedmovetobookfw}" style="-fx-background-color: #ff0000; $bookbuttonstyle" text="??f"/>
			|<button id="{addbadmovetobookfw}" style="-fx-background-color: #ff7f7f; $bookbuttonstyle" text="?f"/>
			|<button id="{lookupsolutionbutton}" img="icons/look.png"/>			
			|<button id="{delandlookupsolutionbutton}" style="-fx-background-color: #ff0000;" img="icons/look.png"/>			
			|<button id="{addexclammovetobook}" style="-fx-background-color: #7fff7f; $bookbuttonstyle" text="!"/>
			|<button id="{addpromisingmovetobook}" style="-fx-background-color: #7f7fff; $bookbuttonstyle" text="!?"/>
			|<button id="{adddoubleexclammovetobook}" style="-fx-background-color: #00ff00; $bookbuttonstyle" text="!!"/>
			|<button id="{addmatedmovetobook}" style="-fx-background-color: #ff0000; $bookbuttonstyle" text="??"/>
			|<button id="{addbadmovetobook}" style="-fx-background-color: #ff7f7f; $bookbuttonstyle" text="?"/>
			|<combobox id="{selectbookcombo}"/>
			|<button id="{createnewbook}" img="icons/add.png"/>
			|<button id="{deletebook}" img="icons/del.png"/>
			|<button id="{addcurrentgame}" img="icons/board.png"/>
			|<button id="{buildpgn}" img="icons/build.png"/>
			|</hbox>
			|<hbox id="boardcontrolpanelhbox" bimage="control.jpg" cover="false" gap="5" padding="5">						
			|	<button id="{evalallmoves}" text="Eval all"/>
			|	<button id="{evalsinglemove}" img="icons/build.png"/>
			|	<combobox id="{evalalldepth}"/>
			|	<button id="{boardcontrolpanelback}" img="icons/backt.png" style="round"/>
			|	<button id="{boardcontrolpanelforward}" img="icons/forwardt.png" style="round"/>			
			|	<button id="{boardcontrolpaneldel}" img="icons/delt.png" style="round"/>
			|	<button id="{boardcontrolpanelstart}" img="icons/startt.png" style="round"/>
			|	<button id="{boardcontrolpanelstop}" img="icons/stopt.png" style="round"/>
			|	<button id="{boardcontrolpanelmake}" img="icons/maket.png" style="round"/>
			|<button id="{addexclammovetobookfwcont}" style="-fx-background-color: #7fff7f; $bookbuttonstyle" text="!f"/>
			|<button id="{addpromisingmovetobookfwcont}" style="-fx-background-color: #7f7fff; $bookbuttonstyle" text="!?f"/>
			|<button id="{adddoubleexclammovetobookfwcont}" style="-fx-background-color: #00ff00; $bookbuttonstyle" text="!!f"/>			
			|<button id="{addmatedmovetobookfwcont}" style="-fx-background-color: #ff0000; $bookbuttonstyle" text="??f"/>
			|<button id="{addbadmovetobookfwcont}" style="-fx-background-color: #ff7f7f; $bookbuttonstyle" text="?f"/>
			|<button id="{addfen}" img="icons/add.png"/>
			|<button id="{delfen}" img="icons/del.png"/>
			|<button id="{randomfen}" img="icons/angrybird.png"/>			
			|<button id="{delallmoves}" img="icons/caution.png"/>			
			|</hbox>
			|<webview id="{bookwebview}"/>
			|</vbox>
			|</tab>
			|<tab caption="Book games">
			|<gamebrowser id="{bookgamesbrowser}" purpose="book"/>
			|</tab>
			|<tab caption="PGN games">
			|<gamebrowser id="{pgngamesbrowser}" purpose="pgn"/>
			|</tab>
			|<tab caption="Engine list">
			|<webview id="{enginelistwebview}"/>
			|</tab>
			|<tab caption="Engine game">
			|<webview id="{enginegamewebview}"/>
			|</tab>
			|<tab caption="Log">
			|<webview id="{systemlog}"/>
			|</tab>
			|<tab caption="Queue">
			|<webview id="{execqueue}"/>
			|</tab>
			|</tabpane>
			|</hbox>
			|</vbox>
		""".stripMargin

		// MyStage creates the scene for primaryStage from blob and shows primaryStage
		MyStage("{main}","akkachess",blob,s=primaryStage,handler=handler)

		RebuildBoard

		MyActor.logSupervisor ! MyLog(wid="{systemlog}")

		VariantChanged

		selectedmaintab=Builder.GI("{selectedmaintab}",selectedmaintab)

		SelectTab("{maintabpane}",selectedmaintab)

		//SystemPopUp("Welcome","Welcome to Akkachess!")

		/*val gb=new guiboard

		GetMyBox("{boardvbox}").ReplaceNode(gb.rooth)*/
	}

	// GuiClass will call Stop on JavaFX stop
	// before shutting down modules
	def Stop
	{

	}

	def GetMainBoard:GuiBoard=GetGuiBoard("{mainboard}")

	def RebuildBoard
	{
		val gb=GetMainBoard
		gb.Build
		GetStage("{main}").SetHeight(gb.canvas_size+210)
		GetStage("{main}").SetWidth(gb.canvas_size+775)
	}

	var selectedmaintab=0

	def AfterLoadingPgn
	{
		val navtobookonopenpgn=GB("{components}#{navtobookonopenpgn}",false)

		if(navtobookonopenpgn) SelectBookTab else SelectPgnTab

		val myhandle=GS("{settings}#{myhandle}")

		val playerblack=Commands.g.get_header("Black")

		SetGuiFlip(playerblack==myhandle)
	}

	def handler(ev:MyEvent)
	{
		val id=ev.Id
		val value=ev.value

		if(ev.kind=="gamebrowser game loaded")
		{
			AfterLoadingPgn

			Update
		}

		if(ev.kind=="textfield entered")
		{
			/*SystemPopUp("Textfield entered",s"""
				|<b>$id textfield entered $value</b>
			""".stripMargin)*/
		}

		if(ev.kind=="button pressed")
		{
			val text=ev.comp.GS("text")
			/*SystemPopUp("Button pressed",s"""
				|<b>$id button $text pressed</b>
			""".stripMargin)*/
			if(ev.Id=="{boardcontrolpanelstart}")
			{
				EngineManager.StartAllEngines(Commands.g)
			}

			if(ev.Id=="{boardcontrolpanelstop}")
			{
				EngineManager.StopAllEngines
			}

			if(ev.Id=="{boardcontrolpanelmake}")
			{
				EngineManager.MakeAnalyzedMove
			}

			if(ev.Id=="{boardcontrolpanelhint}")
			{
				Hint
			}

			if(ev.Id=="{boardcontrolpaneloptions}")
			{
				EngineManager.StopAllEngines

				Commands.Reset

				SetGuiFlip(false)

				Update
			}

			if(ev.Id=="{pastepgnbutton}")
			{
				PastePgn
			}

			if(ev.Id=="{addmovetobook}")
			{
				AddMoveToGuiBook()
			}

			if(ev.Id=="{addexclammovetobook}")
			{
				AddMoveToGuiBookFw("!",static=true)
			}

			if(ev.Id=="{adddoubleexclammovetobook}")
			{
				AddMoveToGuiBookFw("!!",static=true)
			}

			if(ev.Id=="{addpromisingmovetobook}")
			{
				AddMoveToGuiBookFw("!?",static=true)
			}

			if(ev.Id=="{addinterestingmovetobook}")
			{
				AddMoveToGuiBookFw("?!",static=true)
			}

			if(ev.Id=="{addbadmovetobook}")
			{
				AddMoveToGuiBookFw("?",static=true)
			}

			if(ev.Id=="{addmatedmovetobook}")
			{
				AddMoveToGuiBookFw("??",static=true)
			}

			if(ev.Id=="{addexclammovetobookfw}")
			{
				AddMoveToGuiBookFw("!")
			}

			if(ev.Id=="{addpromisingmovetobookfw}")
			{
				AddMoveToGuiBookFw("!?")
			}

			if(ev.Id=="{adddoubleexclammovetobookfw}")
			{
				AddMoveToGuiBookFw("!!")
			}

			if(ev.Id=="{addbadmovetobookfw}")
			{
				AddMoveToGuiBookFw("?")
			}

			if(ev.Id=="{addmatedmovetobookfw}")
			{
				AddMoveToGuiBookFw("??")
			}

			if(ev.Id=="{evalallmoves}")
			{
				EvalAllMoves()
			}

			if(ev.Id=="{evalsinglemove}")
			{
				EvalSingleMove
			}

			if(ev.Id=="{addexclammovetobookfwcont}")
			{
				AddMoveToGuiBookFw("!",stop=false)
			}

			if(ev.Id=="{addpromisingmovetobookfwcont}")
			{
				AddMoveToGuiBookFw("!?",stop=false)
			}

			if(ev.Id=="{adddoubleexclammovetobookfwcont}")
			{
				AddMoveToGuiBookFw("!!",stop=false)
			}

			if(ev.Id=="{addbadmovetobookfwcont}")
			{
				AddMoveToGuiBookFw("?",stop=false)
			}

			if(ev.Id=="{addmatedmovetobookfwcont}")
			{
				AddMoveToGuiBookFw("??",stop=false)
			}

			if(ev.Id=="{randomline}")
			{
				RandomLine
			}

			if(ev.Id=="{addfen}")
			{
				AddFen
			}

			if(ev.Id=="{delfen}")
			{
				DelFen
			}

			if(ev.Id=="{randomfen}")
			{
				RandomFen
			}

			if(ev.Id=="{delallmoves}")
			{
				DelAllMoves
			}

			if(ev.Id=="{lookupsolutionbutton}")
			{
				LookUpSolution
			}

			if(ev.Id=="{delandlookupsolutionbutton}")
			{
				DelAllMoves
				
				LookUpSolution
			}

			if(ev.Id=="{addcurrentgame}")
			{
				AddCurrentGame
			}

			if(ev.Id=="{createnewbook}")
			{
				CreateNewBook
			}

			if(ev.Id=="{deletebook}")
			{
				val currentbook=Settings.get_current_book()
				if(Confirm(s"Delete book $currentbook ?")) DeleteGuiBook
				Update
			}

			if(ev.Id=="{buildpgn}")
			{
				BuildPgn()
			}

			if(ev.Id=="{boardcontrolpanelflip}")
			{
				GetGuiBoard("{mainboard}").SetFlip(Commands.ToggleFlip)
			}
			if(ev.Id=="{boardcontrolpanelback}")
			{
				Commands.Back
				Update
			}
			if(ev.Id=="{boardcontrolpanelforward}")
			{
				Commands.Forward
				Update
			}
			if(ev.Id=="{boardcontrolpaneltobegin}")
			{
				Commands.ToBegin
				Update
			}
			if(ev.Id=="{boardcontrolpaneltoend}")
			{
				Commands.ToEnd
				Update
			}
			if(ev.Id=="{boardcontrolpaneldel}")
			{
				Commands.Delete
				Update
			}
			if(ev.Id=="{boardcontrolpanelreset}")
			{
				Commands.Reset
				Update
			}
		}

		if(ev.kind=="checkbox changed")
		{
			/*SystemPopUp("Checkbox changed",s"""
				|<b>$id checkbox changed to $value</b>
			""".stripMargin)*/
			if(ev.Id=="{bookenabled}")
			{
				Commands.SetBookEnabled(DataUtils.ParseBoolean(ev.value,false))

				Update
			}

			if(ev.Id=="{incmovecount}")
			{
				Commands.SetIncMoveCount(DataUtils.ParseBoolean(ev.value,false))
			}

			if(ev.Id=="{addgames}")
			{
				Commands.SetAddGames(DataUtils.ParseBoolean(ev.value,false))
			}

			if(ev.Id=="{updateresult}")
			{
				Commands.SetUpdateResult(DataUtils.ParseBoolean(ev.value,false))
			}
		}

		if(ev.kind=="stage closed")
		{
			/*println(s"$id stage closed")
			SystemPopUp("Stage closed",s"""
				|<b>$id stage closed</b>
			""".stripMargin)*/
			if(ev.Id=="{main}")
			{
				CloseAllStages
			}			
		}

		if(ev.kind=="slider changed")
		{			
			/*SystemPopUp("Slider changed",s"""
				|<b>$id slider changed to $value</b>
			""".stripMargin)*/
			if(List(
				"{mainboard}#{piecesize}",
				"{mainboard}#{boardopacity}",
				"{mainboard}#{piecefactor}"
			).contains(ev.Id))
			{
				RebuildBoard
			}

			if(ev.Id=="{boardcontrolpanelnavigator}")
			{
				val navp=GD("{components}#{boardcontrolpanelnavigator}",0.0)
				NavGameToP(navp)
			}
		}

		if(ev.kind=="combobox selected")
		{			
			/*SystemPopUp("Combobox selected",s"""
				|<b>$id combobox selected $value</b>
			""".stripMargin)*/
			if(ev.Id=="{buildcutoff}")
			{
				Commands.SetCutOff(ev.value.toInt)
			}

			if(ev.Id=="{selectbookcombo}")
			{
				val selectedbook=ev.value
				Commands.SetCurrentBook(selectedbook)
				Update
			}

			if(List(
				"{mainboard}#{font}",
				"{mainboard}#{material}"
			).contains(ev.Id))
			{
				RebuildBoard
			}
		}

		if(ev.kind=="color picked")
		{			
			/*SystemPopUp("Color picked",s"""
				|<b>$id color picked $value</b>
			""".stripMargin)*/
			if(List(
				"{mainboard}#{whitepiececolor}",
				"{mainboard}#{blackpiececolor}",
				"{mainboard}#{lightsquarecolor}",
				"{mainboard}#{darksquarecolor}"
			).contains(ev.Id))
			{
				RebuildBoard
			}
		}

		if(ev.kind=="tab selected")
		{			
			if(ev.Id=="{maintabpane}")
			{				
				selectedmaintab=ev.value.toInt
				Builder.Set("{selectedmaintab}",""+selectedmaintab)
			}
		}

		if(ev.kind=="menuitem clicked")
		{			
			/*SystemPopUp("Menuitem clicked",s"""
				|<b>$id clicked $value</b>
			""".stripMargin)*/

			if(ev.Id=="{minimaxtoolmenu}")
			{
				MinimaxTool
			}

			if(ev.Id=="{buildbookpgnmenu}")
			{
				BuildBookPgn
			}

			if(ev.Id=="{lookupsolution}")
			{
				LookUpSolution
			}

			if(ev.Id=="{filterpgnmenu}")
			{
				FilterPGN
			}

			if(ev.Id=="{showdefaultbook}")
			{
				ShowDefaultBook
			}

			if(ev.Id=="{enginegametimecontrolmenu}")
			{
				val blob=s"""
					|<vbox>
					|<radioboxpane id="{timecontrolradioboxpane}">
					|<radiobox id="{timecontrolconventionalrb}">
					|	<vbox id="{conventionalvbox}" padding="5" gap="5">
					|		<hbox padding="5" gap="5">
					|			<label text="Conventional clock"/>
					|		</hbox>
					|		<hbox padding="5" gap="5" width="400">
					|			<label text="Number of moves"/>
					|			<combobox id="{timecontrolnumberofmoves}"/>
					|			<label text="Time ( minute(s) )"/>
					|			<combobox id="{timecontroltime}"/>
					|		</hbox>
					|	</vbox>
					|</radiobox>
					|<radiobox id="{timecontrolincrementalrb}">
					|	<vbox id="{incrementalvbox}" padding="5" gap="5">
					|		<hbox padding="5" gap="5">
					|			<label text="Incremental clock"/>
					|		</hbox>
					|		<hbox padding="5" gap="5" width="400">
					|			<label text="Initial time ( minute(s) )"/>
					|			<combobox id="{timecontrolincrementaltime}"/>
					|			<label text="Increment ( second(s) )"/>
					|			<combobox id="{timecontrolincrementalincrement}"/>
					|		</hbox>
					|	</vbox>
					|</radiobox>
					|</radioboxpane>
					|</vbox>
				""".stripMargin

				MyStage("{timecontroldialog}","Time control",blob,
					modal=true,usewidth=false,useheight=false,handler=handler)
			}

			if(ev.Id=="{enginegamestartmenu}")
			{
				if(EngineManager.IsGameRunning) return
				GuiGameReset
				SelectEngineGameTab
				EngineManager.StartGame
			}

			if(ev.Id=="{enginegamestartfromcurrentmenu}")
			{
				SelectEngineGameTab
				EngineManager.StartGameFromCurrent
			}

			if(ev.Id=="{enginegameaborttmenu}")
			{				
				EngineManager.AbortGame
			}

			if(ev.Id=="{enginegamestatsmenu}")
			{	
				LoadWebContent("{enginegamewebview}",EngineManager.ReportStatsHTML)
				SelectEngineGameTab
			}

			if(ev.Id=="{openpgnmenu}")
			{
				OpenPgn
			}

			if(ev.Id=="{openmultiplegamepgnmenu}")
			{
				OpenMultipleGamePgn
			}

			if(ev.Id=="{savepgnasmenu}")
			{
				SavePgnAs
			}

			if(ev.Id=="{copyfenmenu}")
			{
				MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(
					"copied to clipboard: "+Commands.CopyFen))
			}

			if(ev.Id=="{copypgnmenu}")
			{
				MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(
					"copied to clipboard: "+Commands.CopyPgn))
			}

			if(ev.Id=="{copypgntreemenu}")
			{
				MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(
					"copied to clipboard: "+Commands.CopyPgnTree))
			}

			if(ev.Id=="{copycurrentlinemenu}")
			{
				MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(
					"copied to clipboard: "+Commands.CopyCurrentLine))
			}

			if(ev.Id=="{copycurrentlinealgebmenu}")
			{
				MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(
					"copied to clipboard: "+Commands.CopyCurrentLineAlgeb))
			}

			if(ev.Id=="{pastefenmenu}")
			{
				MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(
					"pasted from clipboard: "+Commands.PasteFen))
				Update
			}

			if(ev.Id=="{pastepgnmenu}")
			{
				PastePgn
			}

			if(ev.Id=="{evalallsettingsmenu}")
			{
				val blob=s"""
					|<vbox>
					|<hbox gap="5" padding="5">
					|<label text="Reevaluate important moves"/>
					|<checkbox id="{reevalgood}"/>
					|</hbox>
					|<hbox gap="5" padding="5">
					|<label text="Stop at first mate"/>
					|<checkbox id="{stopatmate}"/>
					|</hbox>
					|<hbox gap="5" padding="5">
					|<label text="Eval limit"/>
					|<slider id="{reevallimit}" value="300.0" width="450.0" height="50.0" min="0.0" max="1000.0" majortickunit="100.0" minortickcount="4" showticklabels="true" showtickmarks="true"/>					
					|</hbox>
					|<hbox gap="5" padding="5">
					|<label text="Number limit"/>
					|<slider id="{numberlimit}" value="3.0" width="450.0" height="50.0" min="0.0" max="20.0" majortickunit="1.0" minortickcount="0" showticklabels="true" showtickmarks="true"/>
					|</hbox>
					|<hbox gap="5" padding="5">
					|<label text="Depth bonus"/>
					|<slider id="{reevaldepthbonus}" value="5.0" width="450.0" height="50.0" min="0.0" max="20.0" majortickunit="1.0" minortickcount="0" showticklabels="true" showtickmarks="true"/>
					|</hbox>
					|<hbox gap="5" padding="5">
					|<label text="Max iterations"/>
					|<slider id="{maxiterations}" value="3.0" width="450.0" height="50.0" min="0.0" max="10.0" majortickunit="1.0" minortickcount="0" showticklabels="true" showtickmarks="true"/>
					|</hbox>
					|<hbox gap="5" padding="5">
					|<label text="Hint time"/>
					|<slider id="{hinttime}" value="1000.0" width="450.0" height="50.0" min="0.0" max="10000.0" majortickunit="1000.0" minortickcount="1" showticklabels="true" showtickmarks="true"/>
					|</hbox>
					|<hbox gap="5" padding="5">
					|<label text="Undo hint"/>
					|<checkbox id="{undohint}"/>
					|<label text="Don't add hint to book"/>
					|<checkbox id="{dontaddhint}"/>
					|</hbox>
					|<hbox gap="5" padding="5">
					|<label text="Color moves"/>
					|<checkbox id="{colormoveseval}"/>
					|</hbox>
					|</vbox>
				""".stripMargin

				MyStage("{evalallsettingsdialog}","Eval all settings",blob,
					modal=true,usewidth=false,useheight=false,handler=handler)
			}

			if(ev.Id=="{boardsettingsmenu}")
			{
				val blob=s"""
					|<vbox>
					|<gridpane hgap="10" vgap="10">
					|<label text="Piece size" r="1" c="1"/>
					|<slider id="{mainboard}#{piecesize}" value="${GuiBoard.PIECESIZE}" r="1" cs="3" c="2" width="300.0" min="20.0" max="100.0" majortickunit="10.0" minortickcount="2" showticklabels="true" showtickmarks="true"/>
					|<label text="White Pc.Col." r="2" c="1"/>
					|<colorpicker r="2" c="2" color="${GuiBoard.WHITEPIECECOLOR}" id="{mainboard}#{whitepiececolor}"/>
					|<label text="Black Pc.Col." r="2" c="3"/>
					|<colorpicker r="2" c="4" color="${GuiBoard.BLACKPIECECOLOR}" id="{mainboard}#{blackpiececolor}"/>
					|<label text="Light Sq.Col." r="3" c="1"/>
					|<colorpicker r="3" c="2" color="${GuiBoard.LIGHTSQUARECOLOR}" id="{mainboard}#{lightsquarecolor}"/>
					|<label text="Dark Sq.Col." r="3" c="3"/>
					|<colorpicker r="3" c="4" color="${GuiBoard.DARKSQUARECOLOR}" id="{mainboard}#{darksquarecolor}"/>
					|<label text="Font" r="4" c="1"/>
					|<combobox r="4" c="2" selected="${GuiBoard.FONT}" id="{mainboard}#{font}"/>
					|<label text="Material" r="4" c="3"/>
					|<combobox r="4" c="4" selected="${GuiBoard.MATERIAL}" id="{mainboard}#{material}"/>
					|<label text="Brd. Opacity" r="5" c="1"/>
					|<slider id="{mainboard}#{boardopacity}" value="${GuiBoard.BOARDOPACITY}" r="5" cs="3" c="2" width="300.0" min="0.0" max="1.0" majortickunit="0.1" minortickcount="2" showticklabels="true" showtickmarks="true"/>
					|<label text="Piece factor" r="6" c="1"/>
					|<slider id="{mainboard}#{piecefactor}" value="${GuiBoard.PIECEFACTOR}" r="6" cs="3" c="2" width="300.0" min="0.0" max="1.0" majortickunit="0.1" minortickcount="2" showticklabels="true" showtickmarks="true"/>
					|</gridpane>
					|</vbox>
				""".stripMargin

				MyStage("{boardsettingsdialog}","Board settings",blob,
					modal=true,usewidth=false,useheight=false,handler=handler)
			}

			if(ev.Id=="{booksettingsmenu}")
			{
				val blob=s"""
					|<vbox>
					|<gridpane hgap="10" vgap="10">
					|<label text="Book enabled" r="1" c="1"/>
					|<checkbox id="{bookenabled}" r="1" c="2" prefixget="settings"/>
					|<label text="Increase move count always" r="2" c="1"/>
					|<checkbox id="{incmovecount}" r="2" c="2" prefixget="settings"/>
					|<label text="Add games when building" r="3" c="1"/>
					|<checkbox id="{addgames}" r="3" c="2" prefixget="settings"/>
					|<label text="Update result when building" r="4" c="1"/>
					|<checkbox id="{updateresult}" r="4" c="2" prefixget="settings"/>
					|<label text="Cut build at move" r="5" c="1"/>
					|<combobox id="{buildcutoff}" r="5" c="2" prefixget="settings"/>
					|<label text="Build black book" r="6" c="1"/>
					|<checkbox id="{buildblack}" r="6" c="2"/>					
					|<label text="To book on open PGN" r="7" c="1"/>
					|<checkbox id="{navtobookonopenpgn}" r="7" c="2"/>					
					|</gridpane>
					|</vbox>
				""".stripMargin

				MyStage("{booksettingsdialog}","Book settings",blob,
					modal=true,usewidth=false,useheight=false,handler=handler)
			}

			if(ev.Id=="{profilesettingsmenu}")
			{
				def profilesettings_handler(ev:MyEvent)
				{
					if(ev.kind=="button pressed")
					{
						if(ev.Id=="{profilesettingsok}")
						{
							val myhandle=GetMyText("{myhandle}").GetText

							Set("{settings}#{myhandle}",myhandle)

							CloseStage("{profilesettingsdialog}")
						}
					}
				}

				val blob=s"""
					|<vbox>
					|<hbox gap="5" padding="5">
					|<label text="My handle"/>
					|<textfield id="{myhandle}" width="250.0" style="-fx-font-size: 18px;"/>
					|</hbox>
					|<hbox gap="5" padding="5">
					|<button id="{profilesettingsok}" width="300.0" style="-fx-font-size: 18px;" text="Ok"/>
					|</hbox>
					|</vbox>
				""".stripMargin

				MyStage("{profilesettingsdialog}","Profile settings",blob,
					modal=true,usewidth=false,useheight=false,handler=profilesettings_handler)

				val myhandle=GS("{settings}#{myhandle}")

				GetMyText("{myhandle}").SetText(myhandle)
			}

			if(ev.Id=="{recordrectmenu}")
			{
				val blob=s"""
					|<vbox>
					|<label text="Adjust position and size"/>
					|</vbox>
				""".stripMargin

				MyStage("{recordrectdialog}","Record rect",blob,
					modal=true,usewidth=true,useheight=true,handler=handler)
			}

			if(ev.Id=="{learncolorsmenu}")
			{
				Robot.LearnBoardColors
			}

			if(ev.Id=="{timingsmenu}")
			{
				val blob=s"""
					|<vbox>
					|<gridpane hgap="5" vgap="5">
					|<label r="1" c="1" text="Synchronizer"/>
					|<slider r="1" c="2" id="{timingssynchronizer}" value="100.0" width="600.0" height="50.0" min="0.0" max="500.0" majortickunit="50.0" minortickcount="4" showticklabels="true" showtickmarks="true"/>
					|<label r="2" c="1" text="XBOARD sleep"/>
					|<slider r="2" c="2" id="{timingsxboardsleep}" value="200.0" width="600.0" height="50.0" min="0.0" max="500.0" majortickunit="50.0" minortickcount="4" showticklabels="true" showtickmarks="true"/>
					|<label r="3" c="1" text="Watch analysis"/>
					|<slider r="3" c="2" id="{timingswatchtick}" value="100.0" width="600.0" height="50.0" min="0.0" max="500.0" majortickunit="50.0" minortickcount="4" showticklabels="true" showtickmarks="true"/>
					|<label r="4" c="1" text="Click"/>
					|<slider r="4" c="2" id="{timingsclick}" value="100.0" width="600.0" height="50.0" min="0.0" max="500.0" majortickunit="50.0" minortickcount="4" showticklabels="true" showtickmarks="true"/>
					|<label r="5" c="1" text="Game thread tick"/>
					|<slider r="5" c="2" id="{timingsgamethreadtick}" value="100.0" width="600.0" height="50.0" min="0.0" max="500.0" majortickunit="50.0" minortickcount="4" showticklabels="true" showtickmarks="true"/>
					|<label r="6" c="1" text="Game thread timeout"/>
					|<slider r="6" c="2" id="{timingsgamethreadtimeout}" value="30.0" width="600.0" height="50.0" min="0.0" max="180.0" majortickunit="10.0" minortickcount="1" showticklabels="true" showtickmarks="true"/>
					|<label r="7" c="1" text="Remember cursor position"/>
					|<checkbox r="7" c="2" id="{timingsremembercursor}"/>
					|<label r="8" c="1" text="Play book percent"/>
					|<slider r="8" c="2" id="{timingsplaybookpercent}" value="90.0" width="600.0" height="50.0" min="0.0" max="100.0" majortickunit="10.0" minortickcount="1" showticklabels="true" showtickmarks="true"/>
					|<label r="9" c="1" text="Play engine percent"/>
					|<slider r="9" c="2" id="{timingsplayenginepercent}" value="70.0" width="600.0" height="50.0" min="0.0" max="100.0" majortickunit="10.0" minortickcount="1" showticklabels="true" showtickmarks="true"/>
					|<label r="10" c="1" text="Time reduction"/>
					|<slider r="10" c="2" id="{timingstimereduction}" value="0.95" width="600.0" height="50.0" min="0.0" max="1.0" majortickunit="0.1" minortickcount="4" showticklabels="true" showtickmarks="true"/>
					|<label r="11" c="1" text="Click retries"/>
					|<slider r="11" c="2" id="{timingsclickretries}" value="2.0" width="600.0" height="50.0" min="0.0" max="10.0" majortickunit="1.0" minortickcount="0" showticklabels="true" showtickmarks="true"/>
					|</gridpane>
					|</vbox>
				""".stripMargin

				MyStage("{timingsdialog}","Timings",blob,
					modal=true,usewidth=false,useheight=false,handler=handler)
			}

			if(ev.Id=="{setupboardmenu}")
			{
				def CloseSetupBoardStage
				{
					CloseStage("{setupboarddialog}")
				}

				val fen=Commands.g.report_fen

				val posfen=Commands.g.b.report_pos_fen
				val turnfen=Commands.g.b.report_turn_fen
				val castlingfen=Commands.g.b.report_castling_fen
				val epfen=Commands.g.b.report_ep_fen
				val halfmovefen=Commands.g.b.report_halfmove_fen
				val fullmovefen=Commands.g.b.report_fullmove_fen

				def setup_board_handler(ev:MyEvent)
				{
					if(ev.kind=="button pressed")
					{
						if(ev.Id=="{clearsetupboard}")
						{
							GetGuiBoard("{setupboard}").clear_board
						}

						if(ev.Id=="{cancelsetupboard}")
						{
							CloseSetupBoardStage
						}

						if(ev.Id=="{applysetupboard}")
						{
							val newposfen=GetGuiBoard("{setupboard}").b.report_pos_fen
							val newturnfen=if(GetMyComboBox("{turncombo}").GetSelected=="White") "w" else "b"
							val newcastlingfen=GetMyComboBox("{castlingrightscombo}").GetSelected
							val newepfen=GetMyComboBox("{epsquarecombo}").GetSelected
							val newhalfmovefen=DataUtils.ParseInt(GetMyText("{halfmoveclocktext}").GetText,0)
							val newfullmovefen=DataUtils.ParseInt(GetMyText("{fullmovenumbertext}").GetText,1)

							val newfen=s"$newposfen $newturnfen $newcastlingfen $newepfen $newhalfmovefen $newfullmovefen"							

							CloseSetupBoardStage

							Commands.g.set_from_fen(newfen)

							Update
						}
					}
				}

				val blob=s"""
					|<hbox>
					|<guiboard id="{setupboard}" setup="true"/>
					|<vbox>
					|<button id="{clearsetupboard}" text="Clear board"/>
					|<label text="Turn:"/>
					|<combobox width="75.0" id="{turncombo}"/>
					|<label text="Castling rights:"/>
					|<combobox width="150.0" id="{castlingrightscombo}"/>
					|<label text="Ep square:"/>
					|<combobox width="75.0" id="{epsquarecombo}"/>
					|<label text="Halfmove clock:"/>
					|<textfield width="75.0" forcedefault="true" text="$halfmovefen" id="{halfmoveclocktext}"/>
					|<label text="Fullmove number:"/>
					|<textfield width="75.0" forcedefault="true" text="$fullmovefen" id="{fullmovenumbertext}"/>
					|<button id="{applysetupboard}" text="Apply changes"/>
					|<button id="{cancelsetupboard}" text="Cancel"/>
					|</vbox>
					|</hbox>
				""".stripMargin

				MyStage("{setupboarddialog}","Setup board manually",blob,
					modal=true,usewidth=false,useheight=false,handler=setup_board_handler)

				GetGuiBoard("{setupboard}").set_from_fen(fen)

				GetMyComboBox("{turncombo}").CreateFromItems(List(
					"White","Black"),if(turnfen=="w") "White" else "Black")
				GetMyComboBox("{castlingrightscombo}").CreateFromItems(List(
					"-","KQkq","KQ","kq","K","Q","k","q","KQk","KQq","Kkq","Qkq","Kk","Kq","Qk","qq"),castlingfen)
				GetMyComboBox("{epsquarecombo}").CreateFromItems(List(
					"-","a3","a6","b3","b6","c3","c6","d3","d6","e3","e6","f3","f6","g3","g6","h3","h6"),epfen)
			}
		}

		if(ev.kind=="radiomenuitem clicked")
		{			
			/*SystemPopUp("Radiomenuitem clicked",s"""
				|<b>$id clicked $value</b>
			""".stripMargin)*/
			val innerid=DataUtils.InnerString(ev.Id)

			if(innerid.contains("rmmultipv"))
			{
				val mpvs=innerid.replaceAll("rmmultipv","")
				val mpv=mpvs.toInt

				Commands.SetMultipv(mpv)

				EngineManager.SetMultipv(mpv,Commands.g)
			}

			if(Settings.SUPPORTED_VARIANTS.contains(ev.value))
			{
				Settings.set_variant(ev.value)

				GuiGameReset
			}
		}

		if(ev.kind=="board clicked")
		{
			if(ev.value.toInt>3) Commands.Forward
			else Commands.Back
			Update
		}

		if(ev.kind=="manual move made")
		{			
			/*SystemPopUp("Manual move made",s"""
				|<b>$id made manual move $value</b>
			""".stripMargin)*/
			val san=ev.value			

			Commands.MakeSanMove(san)

			Update
		}

		if(ev.kind=="webview clicked")
		{			
			/*SystemPopUp("WebView clicked",s"""
				|<b>$id clicked $value</b>
			""".stripMargin)*/
			if(ev.Id=="{moveswebview}")
			{
				val san=ExecuteWebScript("{moveswebview}","x")
				if(san!="")
				{
					val m=Commands.g.b.sanToMove(san)
					if(m!=null)
					{
						Commands.MakeSanMove(san)

						Update
					}
				}
			}

			if(ev.Id=="{enginegamewebview}")
			{				
				val command=ExecuteWebScript("{enginegamewebview}","command")
				if(command=="abort")
				{
					EngineManager.AbortGame
				}
				if(command=="start")
				{
					if(EngineManager.IsGameRunning) return
					GuiGameReset
					EngineManager.StartGame
				}
				if(command=="startfrompos")
				{
					EngineManager.StartGameFromCurrent
				}
			}

			if(ev.Id=="{enginelistwebview}")
			{
				EngineManager.Handle
			}

			if(ev.Id=="{bookwebview}")
			{
				BookClicked
			}

			if(ev.Id=="{colorpgnwebview}")
			{
				PgnClicked
			}
		}
	}

	def GuiUpdate()
	{
		Update
	}

	def Update
	{
		UpdateFunc()
	}

	def UpdateFunc(checkrestart:Boolean=true)
	{

		// update board
		val gb=GetMainBoard

		val fen=Commands.g.report_fen

		gb.set_from_fen(fen)

		gb.clear_engine

		HighlightLastMove

		GetMyText("{boardfenlabel}").SetText(fen)

		val playerwhite=Commands.g.get_header("White")
		val playerwhiterating=Commands.g.get_header("WhiteElo")
		val playerblack=Commands.g.get_header("Black")
		val playerblackrating=Commands.g.get_header("BlackElo")
		val result=Commands.g.get_header("Result")
		val timecontrol=Commands.g.get_header("TimeControl")
		val termination=Commands.g.get_header("Termination")
		val plycount=Commands.g.get_header("PlyCount")

		GetStage("{main}").setTitle(
			s"""akkachess | $playerwhite $playerwhiterating - $playerblack $playerblackrating | $timecontrol $termination $plycount | $result""")

		// update slider
		val sg=new game
		sg.set_from_pgn(Commands.g.report_pgn)
		sg.toend
		val lsg=sg.current_line_length
		if(lsg>0)
		{
			val lc=Commands.g.current_line_length
			var navp=lc.toDouble/lsg.toDouble*100.0
			if(navp>100.0) navp=100.0
			GetMySlider("{boardcontrolpanelnavigator}").SetValue(navp,dotrigger=false)
		}
		else
		{
			GetMySlider("{boardcontrolpanelnavigator}").SetValue(0.0,dotrigger=false)
		}
		

		// update pgn
		val colorpgncontent=Commands.g.report_pgn_html(Commands.g.current_node)

		val mi=colorpgncontent.indexOf("padding: 3px")
		val len=colorpgncontent.length.toDouble
		var yscroll=if(mi>0) (mi-2000)/len else 0.0

		LoadWebContentAndScrollTo("{colorpgnwebview}",colorpgncontent,yscroll)

		// update moves

		LoadWebContent("{moveswebview}",Commands.g.b.genPrintableMoveList(html=true))

		// update book
		val madesans=Commands.g.current_node.childs.keys.toList
		val book_content=Commands.g.book.currentPos.toPrintable(html=true,madesans=madesans)
        LoadWebContent("{bookwebview}",book_content)
        
        if(Builder.HasStage("{showdefaultbookdialog}"))
        {
        	val b=new book(List("stuff","book",Settings.get_variant))

        	b.loadPos(fen,setbook="default")

        	val dbook_content=b.currentPos.toPrintable(html=true)

        	LoadWebContent("{showdefaultbookwebview}",dbook_content)
    	}

        val booklist=butils.list_books()
        val selectedbook=Settings.get_current_book()
        GetMyComboBox("{selectbookcombo}").CreateFromItems(booklist,selectedbook)

        // update game browsers
        val bgb=GetGameBrowser("{bookgamesbrowser}")
        val pgb=GetGameBrowser("{pgngamesbrowser}")

        Commands.g.build_book_game_list

        bgb.game_list=Commands.g.book_games
        pgb.game_list=Commands.g.pgn_games

        bgb.update
        pgb.update

        if(checkrestart) EngineManager.CheckRestartAllEngines(Commands.g)
	}

	def BookClicked
	{
		val e=GetWebEngine("{bookwebview}")
		if(e==null) return
		val key=e.executeScript("key").toString()
		val action=e.executeScript("action").toString()
		val param=e.executeScript("param").toString()
		val uci=e.executeScript("uci").toString()

		println(action)

		if(action=="annot")
		{
			Commands.AnnotateMove(key,param,uci)
		}
		else if(action=="make")
		{
			Commands.MakeSanMove(key)
		}
		else if(action=="del")
		{
			Commands.DeleteMoveFromBook(key)
		}
		else if(action=="comment")
		{
			val commentedsan=key
			val commenteduci=uci

			val comment=Commands.GetBookMoveComment(commentedsan)

			Set("{components}#{textinputs}#{Comment}",comment)

			val r=InputTexts("Edit book comment",List("Comment"),
					applyname="Apply changes",candelete=true,deletemsg="Delete comment")

			if(r.canceled) return

			if(r.deleteitem)
			{
				Commands.CommentBookMove(commentedsan,"-",commenteduci)
			}
			else
			{
				Commands.CommentBookMove(commentedsan,r.texts("Comment"),commenteduci)
			}
		}
		else if(action=="decpriority")
		{
			Commands.DecBookMovePriority(key)
		}
		else if(action=="setpriority")
		{
			SetPriority(key)
		}
		else if(action=="incpriority")
		{
			Commands.IncBookMovePriority(key)
		}

		Update
	}

	def PgnClicked
	{
		val index_str=ExecuteWebScript("{colorpgnwebview}","x")

		if(index_str!=null)
		{
			if(index_str=="edit")
			{
				val field=ExecuteWebScript("{colorpgnwebview}","field")

				val value=Commands.g.get_header(field)

				Set("{components}#{textinputs}#{Field name}",field)
				Set("{components}#{textinputs}#{Field value}",value)

				val r=InputTexts("Edit PGN field",List("Field name","Field value"),
					applyname="Apply changes",candelete=true,deletemsg="Delete this field")

				if(r.canceled) return

				if(r.deleteitem)
				{
					Commands.g.pgn_headers-=field
				}
				else
				{
					Commands.g.pgn_headers+=(r.texts("Field name")->r.texts("Field value"))
				}

				Update
			}
			else if(DataUtils.IsInt(index_str))
			{
				val index=index_str.toInt

				val gn=Commands.g.html_pgn_nodes(index)

				val action=ExecuteWebScript("{colorpgnwebview}","action")

				if(action=="editcomment")
				{
					val comment=gn.comment
					
					Set("{components}#{textinputs}#{Comment}",comment)

					val r=InputTexts("Edit PGN comment",List("Comment"),applyname="Apply changes",
						candelete=true,deletemsg="Delete comment")

					if(r.canceled) return

					if(r.deleteitem)
					{
						Commands.g.current_node.comment=""
					}
					else
					{
						Commands.g.current_node.comment=r.texts("Comment")
					}

					Update
				}
				else
				{
        			Commands.g.tonode(gn)

        			Update
				}
    		}
		}
	}

	def AddMoveToGuiBook(annot:String=null,forward:Boolean=false,count:Int= -1,del:Boolean=false,addcomment:String=null):String=
	{
		val res=Commands.AddMoveToBook(annot,forward,count,del,addcomment)

		Update

		res
	}

	def CreateNewBook
	{
		Set("{components}#{textinputs}#{Book name}","")

		val r=InputTexts("Enter book name",List("Book name"),applyname="Create")

		if(r.canceled) return

		var bookname=RemoveSpecials(r.texts("Book name"))

		Commands.SetCurrentBook(bookname)

		Update
	}

	def AbortBuildBookCallback()
	{
		Commands.g.interrupted=true

		MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem("book building abort requested"))

		CloseAbortDialog
	}

	def AbortDeleteBookCallback()
	{
		butils.interrupted=true

		MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem("book deletion abort requested"))

		CloseAbortDialog
	}

	def BuildBookLogCallback(what:String)
	{
		MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(what))
	}

	def DeleteBookLogCallback(what:String)
	{
		MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(what))
	}

	def BuildPgn(set_pgn:String=null,sort_only:Boolean=false)
	{
		var pgn=set_pgn

		if(pgn==null)
		{
			val f=ChooseFile(if(sort_only) "openpgn" else "buildpgn")

			if(f==null) return

			if(!f.exists()) return

			val path=f.toString()

			pgn=DataUtils.ReadFileToString(path)
		}

		SelectTab("{maintabpane}","Log")

		AbortDialog("Abort building book",AbortBuildBookCallback)

		Commands.g.log_callback=BuildBookLogCallback

		new Thread(new Runnable{def run{
			if(sort_only) Commands.SortPgn(pgn) else Commands.BuildBook(pgn)
			CloseAbortDialog
			MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(
				if(Commands.g.interrupted) "book building aborted" else "book built ok"))
			Platform.runLater(new Runnable{def run{
				if(sort_only) SelectTab("{maintabpane}","PGN games") else SelectTab("{maintabpane}","Book")
				Update
			}})
		}}).start()
	}

	def DeleteGuiBook
	{
		SelectTab("{maintabpane}","Log")

		AbortDialog("Abort deleting book",AbortDeleteBookCallback)

		butils.log_callback=DeleteBookLogCallback

		new Thread(new Runnable{def run{
			Commands.DeleteBook()
			CloseAbortDialog
			MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(
				if(butils.interrupted) "book deletion aborted" else "book deleted ok"))
			Platform.runLater(new Runnable{def run{
				SelectTab("{maintabpane}","Book")
				Update
			}})
		}}).start()
	}

	def AbortBuildBookPgnCallback()
	{
		Commands.g.buildbookpgninterrupted=true

		MyActor.Log("build book pgn abort requested")

		CloseAbortDialog
	}

	val spath="Antichess\\Long\\"

	val apath="stuff\\antichessfens.txt"

	val rnd=new scala.util.Random()

	def GetFens:Array[String]=DataUtils.ReadFileToString(apath).split("\n")

	def RandomFen
	{
		val fens=GetFens

		val index=rnd.nextInt(fens.length)

		val fen=fens(index)

		Commands.g.set_from_fen(fen)

		SetGuiFlip(Commands.g.b.turn!=piece.WHITE)

		SelectTab("{maintabpane}","Moves")

		Update
	}

	def WriteFens(newfens:Array[String])
	{
		DataUtils.WriteStringToFile(apath,newfens.mkString("\n"))
	}

	def DelFen
	{
		val fen=Commands.g.b.report_fen

		val fens=GetFens

		val newfens=fens.filter(_ != fen)

		WriteFens(newfens)
	}

	def AddFen
	{
		val fen=Commands.g.b.report_fen

		val f=new java.io.File(apath)

		val fens=if(f.exists) GetFens else Array[String]()

		if(!fens.contains(fen))
		{
			val newfens=fens:+fen
			WriteFens(newfens)
		}
	}

	def GetMoves(path:String):Array[String]=
	{
		val page=DataUtils.ReadFileToString(path)

		val lines=page.split("\n")

		val lineindex=rnd.nextInt(lines.length)

		val line=lines(lineindex)

		val moves=line.split(" ")

		val maxmoves=20
		val minmoves=5

		var nomoves=rnd.nextInt(if(moves.length< maxmoves) moves.length else maxmoves)

		if(nomoves< minmoves) nomoves=minmoves

		val selmoves=moves.slice(0,nomoves)

		selmoves
	}

	def RandomLine
	{
		var list=getListOfFiles(spath)

		val lengths=list.map(_.length)		

		val totallength=lengths.reduceLeft(_ + _)

		val index=rnd.nextInt(totallength.toInt)

		var count:Int=0

		var current:File=null

		while(count< index)
		{
			current=list.head
			count+=current.length.toInt
			list=list.tail
		}

		val selmoves=GetMoves(current.getAbsolutePath)

		Commands.g.reset

		for(algeb <- selmoves)
		{
			val m=move(fromalgeb=algeb)
			Commands.g.makeMove(m)
		}

		LookUpSolution

		SelectTab("{maintabpane}","Moves")
	}

	def LookUpSolution
	{
		val algebline=Commands.g.current_line_algeb
		val alen=algebline.length
		var list=getListOfFileNames(spath)
		val lists=Map[String,List[String]](
			"e2e3 b7b5" -> List[String]("e3b5.lines.txt"),
			"e2e3 c7c5" -> List[String]("e3c5.lines.txt"),
			"e2e3 g7g5" -> List[String]("e3g5.lines.txt"),
			"e2e3 c7c6" -> List[String]("e3c6.lines.txt"),
			"e2e3 e7e6" -> List[String]("e3e6.lines.txt"),
			"e2e3 g8h6" -> List[String]("e3Nh6.lines.txt"),
			"e2e3 b8c6" -> List[String]("e3Nc6.lines.txt"),
			"e2e3 b7b6" -> List[String]("e3b6.lines.part1.txt","e3b6.lines.part2.txt")
			)
		if(algebline.length>=9)
		{
			val key=algebline.substring(0,9)
			if(lists.contains(key)) list=lists(key)
		}
		println("\n------------\nLooking for : "+algebline+"\n------------\n")
		var allmoves=Map[String,Int]()
		for(name<-list)
		{
			print("Reading : "+name+" ")
			val page=DataUtils.ReadFileToString(spath+name)
			print("( size : "+page.length+" ) lines : ")
			val lines=page.split("\n")
			println(lines.length)
			var count=0
			var moves=Map[String,Int]()
			for(line<-lines)
			{
				if(line.length>=alen)
				{
					if(line.substring(0,alen)==algebline)
					{
						count+=1

						if(line.length>=(alen+5))
						{
							val offset=(if(algebline=="") 0 else 1)
							var move=line.substring(alen+offset,alen+offset+4)
							if(line.length>=(alen+6))
							{
								val pp=line.substring(alen+offset+4,alen+offset+5)
								if(pp!=" ") move+=pp
							}
							if(moves.contains(move))
							{
								moves+=(move->(moves(move)+1))
							}
							else
							{
								moves+=(move->1)
							}
							if(allmoves.contains(move))
							{
								allmoves+=(move->(allmoves(move)+1))
							}
							else
							{
								allmoves+=(move->1)
							}
						}
					}					
				}
			}
			if(count>0)
			{
				println("Matches : "+count+" Moves : "+moves)
			}
		}
		val allmovessorted=scala.collection.immutable.ListMap(allmoves.toSeq.sortWith(_._2 > _._2):_*)
		println("\n\nSummary :\n\n"+(for((move,num)<-allmovessorted) yield (move+" ( "+num+" )" )).mkString("\n"))

		val cg=Commands.g
		val cgb=cg.b
		var i=0
		val madesans=Commands.g.current_node.childs.keys.toList
		for((algeb,num)<-allmovessorted)
		{
			if(cgb.isAlgebLegal(algeb))
			{
				val m=move()
				m.from_algeb(algeb)
				val san=Commands.g.b.toSan(m)
				cg.makeMove(m)
				AddMoveToGuiBook(if(cgb.getturn==piece.BLACK) "!" else "!?",false,num,!madesans.contains(san),"solution")
				i+=1
			}
		}
	}

	def BuildBookPgn
	{
		Commands.g.buildbookpgninterrupted=false

		SelectTab("{maintabpane}","Log")

		AbortDialog("Abort deleting book",AbortBuildBookPgnCallback)

		new Thread(new Runnable{def run{
			Commands.BuildBookPgn
			CloseAbortDialog
			MyActor.Log(if(Commands.g.buildbookpgninterrupted) "build book pgn aborted" else "book pgn built ok")
			Platform.runLater(new Runnable{def run{
				SelectTab("{maintabpane}","PGN")
				Update
			}})
		}}).start()
	}

	def AddCurrentGame
	{
		val pgn=Commands.g.report_pgn
		BuildPgn(pgn)
	}

	def VariantChanged
	{
		EngineManager.ReLoad

		EngineManager.SetMultipv(Settings.get_multipv,Commands.g)

		Robot.InitBoardPatterns

		Update
	}

	def HighlightLastMove
	{
		GetMainBoard.clear_highlight
		if(Commands.g.current_node!=Commands.g.root)
		{
			val dummy=new board
			dummy.set_from_fen(Commands.g.current_node.parent.fen)
			val m=dummy.sanToMove(Commands.g.current_node.genSan)
			if(m!=null)
			{
				val ram=dummy.toRealAlgebMove(m)
				GetMainBoard.highlight_move(ram)
			}
		}
	}

	def DoOpenPgn(mult:Boolean=false)
	{
		val f=ChooseFile("openpgn")

		if(f==null) return

		if(!f.exists()) return

		val path=f.toString()

		val pgn=DataUtils.ReadFileToString(path)

		Commands.g.reset_all

		if(mult)
		{
			BuildPgn(pgn,sort_only=true)
		}
		else
		{
			Commands.g.set_from_pgn(pgn)

			Commands.g.toend

			AfterLoadingPgn

			Update
		}
	}

	def OpenPgn
	{
		DoOpenPgn()
	}

	def OpenMultipleGamePgn
	{
		val pgb=GetGameBrowser("{pgngamesbrowser}")
		pgb.lastclicked= -1

		DoOpenPgn(mult=true)
	}

	def SelectLogTab()
	{
		SelectTab("{maintabpane}","Log")
	}

	def SelectPgnTab()
	{
		SelectTab("{maintabpane}","PGN")
	}

	def SelectBookTab()
	{
		SelectTab("{maintabpane}","Book")
	}

	def SelectEngineGameTab()
	{
		SelectTab("{maintabpane}","Engine game")
	}

	def SavePgnAs
	{
		SaveFileAsDialog("Save PGN as","savepgn",Commands.g.report_pgn,SelectLogTab)
	}

	def GuiGameReset
	{
		Commands.Reset

		VariantChanged
	}

	def SetGuiFlip(f:Boolean)
	{
		Commands.SetFlip(f)
		GetGuiBoard("{mainboard}").SetFlip(f)
	}

	def PastePgn
	{
		MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(
					"pasted from clipboard: "+Commands.PastePgn))
		//Commands.ToEnd
		Commands.ToBegin
		Commands.Forward
		Update
	}

	val annotcolors=Map[String,String](
				"!"->"#007f00",
				"!?"->"#00007f",
				"!!"->"#00ff00",
				"?"->"#7f0000",
				"??"->"#ff0000"
				)

	def AddMoveToGuiBookFw(annot:String,static:Boolean=false,stop:Boolean=true,addcomment:String=null)
	{

		val running=EngineManager.enginesrunning

		if(running)
		{
			if(static)
			{
				val san=AddMoveToGuiBook(annot,forward=true,addcomment=addcomment)
			}
			else
			{
				EngineManager.move_made=false

				EngineManager.MakeAnalyzedMove
			}
		}

		val annotcolor=annotcolors(annot)

		if(running||(!static))
		{
			SystemPopUp("Book message",s"""<font color="$annotcolor" size="5"><b>Annotating move as $annot""",dur=500)
		}
		else
		{
			val san=AddMoveToGuiBook(annot,forward=false,addcomment=addcomment)

			return
		}

		val ei=ExecutionItem(
			client="Add move to GUI book",
			code=new Runnable{def run{
				Future{
					if(EngineManager.enginesrunning)
					{
						while(EngineManager.move_made==false)
						{
							try{Thread.sleep(100)}catch{case e:Throwable=>{}}
						}

						if(stop&&(!static)) EngineManager.StopAllEngines
					}

					val san=AddMoveToGuiBook(annot,forward=true,addcomment=addcomment)
				}
			}}
		)
		MyActor.queuedExecutor ! ei
		
	}

	var stopevalmoves=false
	var evalrunning=false

	var reevalgood=false

	var stopatmate=false

	var reevallimit=300

	var numberlimit=3

	var reevaldepthbonus=5

	var maxiterations=3

	var hinttime=1000

	var undohint=false

	var dontaddhint=false

	var evalindex=0

	var numeval=0

	var bestscore="?"

	var matefound=false

	def EvalMove(san:String,usedepthbonus:Boolean=false)
	{
		val book_content=Commands.g.book.currentPos.toPrintable(html=true)

		val hassan=Commands.g.current_node.childs.contains(san)

		val uci=Commands.g.b.sanToMove(san).toAlgeb

		Commands.g.makeSanMove(san)

		val haslegalb=new board
		haslegalb.set_from_fen(Commands.g.report_fen)
		haslegalb.genMoveList
		if(haslegalb.move_list.length==0)
		{
			if(hassan) Commands.g.back else Commands.g.delete

			val score=if(haslegalb.status==board.IS_MATE) -9999 else 0
		
			Commands.AnnotateMove(san,"-",uci,"E "+score,dosave=false)

			if(colormoveseval) Commands.ColorMove(san,score,dosave=false)

			Commands.SaveGamePos

			if(usedepthbonus) deepsans=deepsans:+san

			return
		}

		val deep=if(usedepthbonus) "<b>deep</b>&nbsp;<small>#"+deepgo+"</small>&nbsp;" else "shallow&nbsp;"

		Platform.runLater(new Runnable{def run{							
			val blob=s"""
				|<font size=5>Analyzing move &nbsp;$deep
				|<font color=blue><b>$san</b></font>&nbsp;
				|<small>[ $evalindex / $numeval<small> ]</small>
				|<hr>
				|$book_content
			""".stripMargin
			LoadWebContent("{moveevalwebview}",blob)
			Update

			if(!EngineManager.AreEnginesRunning) EngineManager.StartAllEngines(Commands.g) else
			EngineManager.CheckRestartAllEngines(Commands.g)
		}})						

		var tr=EngineManager.TopRunning
		
		while(tr==null)
		{
			try{Thread.sleep(500)}catch{case e:Throwable=>{}}
			tr=EngineManager.TopRunning
		}

		val tre=tr

		var reqdepth=GI("{components}#{evalalldepth}#{selected}",20)

		if(usedepthbonus) reqdepth+=Builder.GD("{components}#{reevaldepthbonus}",5.0).toInt

		while(tre.thinkingoutput.maxdepth< reqdepth)
		{
			try{Thread.sleep(500)}catch{case e:Throwable=>{}}
		}

		Platform.runLater(new Runnable{def run{
			EngineManager.StopAllEngines
		}})

		tr=EngineManager.TopRunning
		
		while(tr!=null)
		{
			try{Thread.sleep(500)}catch{case e:Throwable=>{}}
			tr=EngineManager.TopRunning
		}

		val score= -tre.thinkingoutput.scorenumerical
		val actdepth=tre.thinkingoutput.maxdepth
		val bestmove=tre.bestmove

		if(bestscore!="?")
		{
			val bestscoreint=bestscore.toInt
			if(score>bestscoreint) bestscore=""+score
		}
		else
		{
			bestscore=""+score
		}
		
		if(hassan) Commands.g.back else Commands.g.delete
		
		Commands.AnnotateMove(san,"-",bestmove,"E "+score,dosave=false)

		if(colormoveseval) Commands.ColorMove(san,score,dosave=false)

		Commands.SaveGamePos

		if(usedepthbonus) deepsans=deepsans:+san

		if(score > 9000) matefound=true
	}

	var deepsans=List[String]()

	var deepgo=1

	var colormoveseval=false

	def EvalAllIterativeFunc
	{
		deepsans=List[String]()

		deepgo=1

		var ready=false

		while(!ready)
		{
			InitEvalAll(true)

			if((numeval<=0)||(deepgo>maxiterations)) ready=true else
			{
				EvalAllFunc(true)

				deepgo+=1

				if(stopatmate && matefound) ready=true
			}
		}
	}

	var sanssortedfiltered=scala.collection.immutable.ListMap[String,Int]()

	def InitEvalAll(filter:Boolean=false,singlesan:String=null)
	{
		var b=new board

		b.set_from_fen(Commands.g.report_fen)

		b.initMoveGen

		bestscore="?"

		matefound=false

		var evals=Map[String,Int]()		

		while(b.nextLegalMove())
		{
			val san=b.toSan(b.current_move)

			val eval=Commands.GetBookMoveEval(san)

			if(singlesan==null) evals+=(san->eval)
			else if(san==singlesan) evals+=(san->eval)
		}

		var sanssortedorig=scala.collection.immutable.ListMap(evals.toSeq.sortWith(_._2 > _._2):_*)

		sanssortedfiltered=if(filter)
		{
			var temp=Map[String,Int]()
			var i=0
			for((san,eval) <- sanssortedorig)
			{
				val contained=deepsans.contains(san)
				val filterok=(eval > -reevallimit) && (eval < reevallimit) && (!contained)
				if(filterok||contained) i+=1
				if(filterok && (i <= numberlimit))
				{
					temp+=(san->eval)
				}
			}
			scala.collection.immutable.ListMap(temp.toSeq.sortWith(_._2 > _._2):_*)
		}
		else sanssortedorig

		numeval=sanssortedfiltered.keys.toList.length
	}
	
	def EvalAllFunc(filter:Boolean=false)
	{
		evalindex=0		

		for( (san,eval) <- sanssortedfiltered )
		{
			if(stopevalmoves) return

			if(stopatmate && matefound) return

			evalindex+=1

			EvalMove(san,filter)
		}
	}

	def EvalSingleMove
	{
		val cn=Commands.g.current_node
		if(cn.parent==null) return
		val san=cn.genSan
		Commands.g.back
		EvalAllMoves(singlesan=san)
	}

	def EvalAllMoves(singlesan:String=null)
	{
		if(singlesan==null) DelAllMoves

		def eval_handler(ev:MyEvent)
		{
			if(ev.kind=="button pressed")
			{
				if(ev.Id=="{close}")
				{
					val ei=ExecutionItem(
					client="Eval all moves close",
					code=new Runnable{def run{
						Future{
						if(HasStage(s"{moveevaldialog}"))
						{
							if(evalrunning)
							{					
								stopevalmoves=true
								while(evalrunning) try{Thread.sleep(500);}catch{case e:Throwable=>{}}								
							}
							Platform.runLater(new Runnable{def run{
								CloseStage(s"{moveevaldialog}")
							}})
							
						}
					}}})
					MyActor.queuedExecutor ! ei
				}
			}

		}

		val blob=s"""
			|<vbox>
			|<hbox gap="5" padding="5">
			|<button id="{close}" text="Abort"/>
			|</hbox>
			|<webview id="{moveevalwebview}"/>
			|</vbox>
		""".stripMargin

		val s=MyStage("{moveevaldialog}","Eval all moves",blob,modal=true,unclosable=true,store=true,handler=eval_handler)

		EngineManager.StopAllEngines

		val ei=ExecutionItem(
			client="Eval all moves",
			code=new Runnable{def run{
				Future{

					evalrunning=true

					reevalgood=GB("{components}#{reevalgood}",false)

					stopatmate=GB("{components}#{stopatmate}",false)

					reevallimit=Builder.GD("{components}#{reevallimit}",300.0).toInt

					numberlimit=Builder.GD("{components}#{numberlimit}",3.0).toInt

					maxiterations=Builder.GD("{components}#{maxiterations}",3.0).toInt					

					colormoveseval=GB("{components}#{colormoveseval}",false)

					InitEvalAll(singlesan=singlesan)

					EvalAllFunc(filter=(singlesan!=null))

					var posnormal=true

					try{
						if(bestscore.toInt > (2*reevallimit)) posnormal=false
						if(bestscore.toInt < (-2*reevallimit)) posnormal=false
					}catch{case e:Throwable=>{}}

					if(reevalgood && posnormal && ( !(stopatmate && matefound) ) && (singlesan==null) ) EvalAllIterativeFunc

					stopevalmoves=false

					evalrunning=false

					Platform.runLater(new Runnable{def run{
						if(HasStage(s"{moveevaldialog}"))
						{
							CloseStage(s"{moveevaldialog}")
						}
						Update
					}})

				}
			}}
		)
		MyActor.queuedExecutor ! ei
	}

	def DelAllMoves
	{
		Commands.DeleteAllMovesFromBook()

		Update
	}

	var minimaxrunning=false

	var nodes=0

	var actmaxdepth=0

	var minimaxstudymode=false

	var colormoves=false

	var recordnodes=false

	var minimaxlog=""

	var timer=new Timer()

	var mtimer=new Timer()

	def MinimaxTool
	{		

		def minimax_handler(ev:MyEvent)
		{
			if(ev.kind=="button pressed")
			{
				if(ev.Id=="{start}")
				{
					if(minimaxrunning) return

					minimaxlog=""

					nodes=0

					actmaxdepth=0

					minimaxstudymode=GB("{components}#{minimaxstudy}",false)

					colormoves=GB("{components}#{colormoves}",false)

					recordnodes=GB("{components}#{recordnodes}",false)					

					minimaxrunning=true					

					timer=new Timer()

					mtimer=new Timer()

					val ei=ExecutionItem(
					client="Minimax tool",
					code=new Runnable{def run{
						Future{
							val maxdepth=GI("{components}#{minimaxdepth}#{selected}",20)
							minimax_recursive(0,maxdepth,List[String]())
							minimaxrunning=false
							Platform.runLater(new Runnable{def run{
								if(HasStage(s"{minimaxdialog}"))
								{
									CloseStage(s"{minimaxdialog}")
								}
								Update
							}})
							
						}
					}})
					MyActor.queuedExecutor ! ei
				}

				if(ev.Id=="{abort}")
				{
					if(!minimaxrunning)
					{
						if(HasStage(s"{minimaxdialog}"))
						{
							CloseStage(s"{minimaxdialog}")
						}
						Update
					}
					minimaxrunning=false
				}
			}
		}

		val blob=s"""
			|<vbox>
			|<hbox gap="5" padding="5">
			|<button id="{start}" text="Start"/>
			|<combobox id="{minimaxdepth}"/>
			|<label text="Study mode"/>
			|<checkbox id="{minimaxstudy}"/>
			|<label text="Color moves"/>
			|<checkbox id="{colormoves}"/>
			|<label text="Record nodes"/>
			|<checkbox id="{recordnodes}"/>
			|<button id="{abort}" text="Abort"/>
			|</hbox>
			|<webview id="{minimaxwebview}"/>
			|</vbox>
		""".stripMargin

		val s=MyStage("{minimaxdialog}","Minimax tool",blob,modal=true,unclosable=true,store=true,handler=minimax_handler)

	}	

	def minimax_recursive(depth:Int,maxdepth:Int,line:List[String]):Int=
	{

		val linestr=line.mkString(" ")		

		val time=timer.elapsed
		val timef=formatDuration((time*1000).toLong,"HH:mm:ss")

		val nps=if(time>0) ("%.2f".format(nodes/time)) else "?"

		minimaxlog=s"""
			|$linestr<br>
		""".stripMargin+minimaxlog

		if(minimaxlog.length>5000) minimaxlog=minimaxlog.substring(0,5000)

		if((time<3)||(mtimer.elapsed>1)) Platform.runLater(new Runnable{def run{
			val blob=s"""
				|<font size=5>
				|nodes <font color="blue">$nodes</font> 
				|maxdepth <font color="red">$actmaxdepth</font> 
				|time <font color="green">$timef</font> 
				|nps <font color="green">$nps</font> 
				|</font>
				|<hr>
				|$minimaxlog				
			""".stripMargin
			LoadWebContent("{minimaxwebview}",blob)
			mtimer=new Timer()
		}})

		var alpha= butils.MINUS_INFINITE

		if(depth>maxdepth) return alpha

		if(depth>actmaxdepth) actmaxdepth=depth
		
		val sans=Commands.GetBookMovesWithEval

		if(sans.length==0) return alpha

		val sanssorted=sans.toSeq.sortWith(Commands.GetBookMoveEval(_) > Commands.GetBookMoveEval(_)).toList

		for(san <- sanssorted)
		{

			// count every move made on the board as a node ( rather than every recursive call )
			nodes+=1

			if(!minimaxrunning) return butils.MINUS_INFINITE

			var value=Commands.GetBookMoveEval(san)

			var solution=false

			if(value==butils.PLUS_INFINITE)
			{
				if(Commands.g.b.getturn==piece.WHITE) value= 10000 else value= -10000
				solution=true
			}

			Commands.g.makeSanMove(san)

			val nodes0=nodes

			var eval= value

			if(!solution) eval = -minimax_recursive(depth+1,maxdepth,line:+san)			

			val nodes1=nodes

			val count=nodes1-nodes0

			if(eval==butils.PLUS_INFINITE)
			{
				eval=value
			}			

			if(minimaxstudymode)
			{
				val comment="E "+eval
				Commands.g.current_node.comment=comment
			}

			if(minimaxstudymode) Commands.g.back else Commands.g.delete

			if(!solution) Commands.SetBookMoveEval(san,eval,dosave=false)

			if(!solution) Commands.SetBookMovePlays(san,if(recordnodes) count else 1,dosave=false)

			if((colormoves)&&(!solution))
			{
				Commands.ColorMove(san,eval,dosave=false)
			}

			Commands.SaveGamePos

			if(eval>alpha) alpha=eval

		}		

		alpha

	}

	def NavGameToP(navp:Double)
	{
		val sg=new game
		sg.set_from_pgn(Commands.g.report_pgn)
		sg.toend
		val lsg=sg.current_line_length

		if(lsg==0)
		{
			Update

			return
		}

		Commands.g.tobegin

		var ready=false
		do {
			val lc=Commands.g.current_line_length
			val cnavp=lc.toDouble/lsg.toDouble*100.0
			if((cnavp>=navp)||(Commands.g.current_node.childs==null)) ready=true
			else Commands.g.forward
		} while(!ready)

		Update
	}

	def FilterPGN
	{
		val variants=(for(v <- Settings.SUPPORTED_VARIANTS) yield
		{
			s"""
				|<hbox gap="5" padding="5">
				|<label text="$v"/>
				|<checkbox id="{filtervariant $v}"/>
				|</hbox>
			""".stripMargin
		}).mkString("\n")

		val blob=s"""
					|<vbox>					
					|
					|<hbox gap="5" padding="5">
					|<label text="File name"/>
					|<textfield id="{filterfilename}"/>
					|<label text="Player white"/>
					|<textfield id="{filterplayerwhite}"/>
					|<label text="Player black"/>
					|<textfield id="{filterplayerblack}"/>
					|<button id="{filtersearch}" width="200.0" text="Search"/>
					|</hbox>
					|
					|<hbox gap="5" padding="5">
					|<label text="Rating"/>
					|<slider id="{filterrating}" value="2000.0" width="800.0" height="50.0" min="1000.0" max="3000.0" majortickunit="100.0" minortickcount="0" showticklabels="true" showtickmarks="true"/>
					|</hbox>
					|
					|<hbox gap="5" padding="5">
					|<label text="Max games"/>
					|<slider id="{filtermaxgames}" value="100.0" width="800.0" height="50.0" min="0.0" max="2000.0" majortickunit="100.0" minortickcount="0" showticklabels="true" showtickmarks="true"/>
					|</hbox>
					|
					|<hbox gap="5" padding="5">
					|<label text="Min plies"/>
					|<slider id="{filterminplies}" value="5.0" width="800.0" height="50.0" min="0.0" max="50.0" majortickunit="5.0" minortickcount="4" showticklabels="true" showtickmarks="true"/>
					|</hbox>
					|					|
					|<hbox gap="5" padding="5">
					|<label text="Min time"/>
					|<slider id="{filtermintime}" value="60.0" width="800.0" height="50.0" min="0.0" max="600.0" majortickunit="60.0" minortickcount="3" showticklabels="true" showtickmarks="true"/>
					|</hbox>
					|
					|<hbox gap="5" padding="5">					
					|$variants
					|</hbox>
					|
					|</vbox>
				""".stripMargin

		def filter_handler(ev:MyEvent)
		{
			val id=ev.Id
			val value=ev.value

			if(ev.kind=="button pressed")
			{
				if(id=="{filtersearch}")
				{
					FilterSearch
				}
			}

			if(ev.kind=="stage closed")
			{
				SelectBookTab
			}
		}		

		MyStage("{filterpgndialog}","Filter PGN",blob,
			modal=true,usewidth=false,useheight=false,handler=filter_handler)

		val defaultfilterfilename=GS("{settings}#{defaultfilterfilename}","filtered")		
		val defaultfilterplayerwhite=GS("{settings}#{defaultfilterplayerwhite}","")		
		val defaultfilterplayerblack=GS("{settings}#{defaultfilterplayerblack}","")		

		GetMyText("{filterfilename}").SetText(defaultfilterfilename)
		GetMyText("{filterplayerwhite}").SetText(defaultfilterplayerwhite)
		GetMyText("{filterplayerblack}").SetText(defaultfilterplayerblack)
	}

	def SetPriority(san:String)
	{
		val buttons=(for(i <- butils.pborders.keys.toList.sorted.reverse) yield
		{
			val style=butils.pborders(i).replaceAll("border","-fx-border")
			s"""
				|<button id="{setpriority $i}" width="300.0" style="$style" text="$i"/>
			""".stripMargin
		}).mkString("\n")

		val blob=s"""
					|<vbox padding="5" gap="5">					
					|$buttons
					|</vbox>
				""".stripMargin

		def setpriority_handler(ev:MyEvent)
		{
			if(ev.kind=="button pressed")
			{
				val trueid=ev.Id.replaceAll("[\\{\\}]","")
				val parts=trueid.split(" ").toList

				if(parts(0)=="setpriority")
				{
					val i=parts(1).toInt

					Commands.SetBookMovePriority(san,i)

					CloseStage("{setprioritydialog}")

					Update
				}
			}
		}		

		MyStage("{setprioritydialog}","Set priority",blob,
			modal=true,usewidth=false,useheight=false,handler=setpriority_handler)		
	}

	def FilterSearch
	{
		SelectLogTab

		val ei=ExecutionItem(
		client="Filter search",
		code=new Runnable{def run{
			Future{
				FilterSearchFunc
			}
		}})
		MyActor.queuedExecutor ! ei
	}

	def FilterSearchFunc
	{	

		val filename=GetMyText("{filterfilename}").GetText
		Set("{settings}#{defaultfilterfilename}",filename)
		val filterplayerwhite=GetMyText("{filterplayerwhite}").GetText
		Set("{settings}#{defaultfilterplayerwhite}",filterplayerwhite)
		val filterplayerblack=GetMyText("{filterplayerblack}").GetText
		Set("{settings}#{defaultfilterplayerblack}",filterplayerblack)
		val filterrating=GD("{components}#{filterrating}",2000.0).toInt
		val filtermaxgames=GD("{components}#{filtermaxgames}",100.0).toInt
		val filterminplies=GD("{components}#{filterminplies}",5.0).toInt
		val filtermintime=GD("{components}#{filtermintime}",60.0).toInt

		val variants:Map[String,Boolean] = (for(v <- Settings.SUPPORTED_VARIANTS) yield
		{
			v -> GB(s"{components}#{filtervariant $v}",false)
		}).toMap

		MyActor.Log("reading stuff/sorted/All.pgn")
		
		val pgnfile=DataUtils.ReadFileToString("stuff/sorted/All.pgn")

		MyActor.Log("length "+pgnfile.length)

		MyActor.Log("splitting file")

		val dummy=new game
		val pgns=dummy.split_pgn(pgnfile)

		MyActor.Log("done")
		
		var total=0
		var count=0

		val collection=scala.collection.mutable.ArrayBuffer[String]()

		for(pgn <- pgns if(count < filtermaxgames))
		{
			total+=1

			dummy.parse_pgn(pgn,head_only=true)

			val playerwhite=dummy.get_header("White")
			val playerblack=dummy.get_header("Black")
			val playerwhiteok=(filterplayerwhite=="")||(playerwhite==filterplayerwhite)
			val playerblackok=(filterplayerblack=="")||(playerblack==filterplayerblack)
			var whiterating=2000
			var blackrating=2000
			try{
				whiterating=dummy.get_header("WhiteElo").toInt
				blackrating=dummy.get_header("BlackElo").toInt
			}catch{case e:Throwable=>{}}
			var plycount=0
			try{
				plycount=dummy.get_header("PlyCount").toInt
			}catch{case e:Throwable=>{}}
			val plycountok=plycount>=filterminplies
			var time=60
			try{
				val timeh=dummy.get_header("TimeControl")
				val parts=timeh.split("\\+")
				time=parts(0).toInt
			}catch{case e:Throwable=>{}}
			val timeok=time>=filtermintime
			val rating=(whiterating+blackrating)/2.toInt
			val ratingok=rating>=filterrating
			val variant=dummy.get_header("Variant")
			val variantok=if(variants.contains(variant)) variants(variant) else false			

			if(playerwhiteok&&playerblackok&&ratingok&&variantok&&plycountok&&timeok)
			{
				count+=1
				collection+=pgn

				val readyp=(total.toDouble/pgns.length.toDouble*100.0).toInt

				MyActor.Log(readyp+" % "+variant+" "+playerwhite+" - "+playerblack+" "+count+" / "+total)
			}
		}

		MyActor.Log("done, found "+collection.length)

		val path="stuff/sorted/"+filename+".pgn"

		DataUtils.WriteStringToFile(path,collection.mkString("\n\n"))
	}

	def ShowDefaultBook
	{
		val blob=s"""
			|<vbox>
			|<webview id="{showdefaultbookwebview}"/>
			|</vbox>
		""".stripMargin

		val s=MyStage("{showdefaultbookdialog}","Default book",blob,modal=false,unclosable=false,store=true,handler=handler)

		Update
	}

	def Hint
	{
		hinttime=Builder.GD("{components}#{hinttime}",1000.0).toInt

		undohint=Builder.GB("{components}#{undohint}",false)

		dontaddhint=Builder.GB("{components}#{dontaddhint}",false)

		Robot.MakeAnEngineMove(hinttime,undo=undohint,addtobook=(!dontaddhint))
	}

}

