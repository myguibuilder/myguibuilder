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

import java.awt.Toolkit

import scala.concurrent._
import scala.concurrent.duration._
import akka.actor._
import akka.pattern._
import akka.util._

import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global

import java.io._
import scala.io._

////////////////////////////////////////////////////////////////////

// builder is responsible for building up the application

////////////////////////////////////////////////////////////////////
// GuiClass
////////////////////////////////////////////////////////////////////

// GuiClass is the class passed by the main function for execution as a JavaFX application
// it should be minimal, the state of the application should be stored in global objects

class GuiClass extends Application
{
	override def start(primaryStage: Stage)
	{
		// init sets up ModuleManager
		MyApp.Init

		// starting modules
		ModuleManager.Startup

		// starting application
		MyApp.Start(primaryStage)
	}

	override def stop()
	{
		// stop application
		MyApp.Stop

		// shutting down modules
		ModuleManager.Shutdown
	}
}

////////////////////////////////////////////////////////////////////
// Module
////////////////////////////////////////////////////////////////////

// Module defines the minimum functionality of a module
// namely it should have a Startup function, a Shutdown funtcion
// and should report its Name

trait Module
{
	def Startup:Unit // abstract
	def Shutdown:Unit // abstract
	def Name:String // abstract
}

////////////////////////////////////////////////////////////////////
// ModuleManager
////////////////////////////////////////////////////////////////////

// ModuleManager is responsible for startup and shutdown of modules
// it starts modules on startup
// and shuts them down on shutdown
object ModuleManager
{
	// Builder is default module
	private var items=scala.collection.mutable.ArrayBuffer[Module](Builder)

	def Add(m:Module)
	{
		items+=m
	}

	def Startup
	{
		for(item<-items)
		{
			print("starting "+item.Name+" ... ")
			item.Startup
			println("done")
		}
	}

	def Shutdown
	{
		for(item<-items.reverse)
		{
			print("shutting down "+item.Name+" ... ")
			item.Shutdown
			println("done")
		}
	}

}

////////////////////////////////////////////////////////////////////
// MyEvent
////////////////////////////////////////////////////////////////////

// MyEvent is an event used by MyComponent event handling

case class MyEvent(
	var kind:String,
	var comp:MyComponent,
	var value:String
) extends PrintableInterface
{

	MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(ToPrintable))

	def Id:String=
	{
		if(comp==null) return ""
		comp.GetId
	}

	def TrunkId:String=
	{
		if(comp==null) return ""
		comp.GetTrunkId
	}

	def ReportPrintable(level:Int=0,buff:String=""):String=
	{
		kind+" = "+value+" @ "+(if(comp!=null) comp.ToPrintable else "null")
	}
}

////////////////////////////////////////////////////////////////////
// Resource
////////////////////////////////////////////////////////////////////

// Resource provides access to Java resources

object Resource
{
	def asStream(path:String):InputStream=
	{
		getClass().getClassLoader().getResourceAsStream(path)
	}

	def asSource(path:String):Source=
	{
		Source.fromInputStream(asStream(path))
	}

	def asString(path:String):String=
	{
		asSource(path).mkString
	}
}

////////////////////////////////////////////////////////////////////
// Builder
////////////////////////////////////////////////////////////////////

// Builder
// - keeps track of the application's state
//  by loading values on startup
//  and saving them on shutdown
// - provides functions for creating and manipulating stages
// - provides access to components, frequently used component methods and values

object Builder extends Module with GetSetTypedDataValueWithDefault
{	

	// value members

	private var values:Data=MapData()

	private var components=Map[String,MyComponent]()

	private var stages=Map[String,MyStage]()

	// method members

	////////////////////////////////////////////////////////////////////
	// Module interface	

	def Name:String="Builder"

	def Startup
	{
		mkdir("stuff")
		values=Data.FromXMLFile(valuespath)
	}

	def Shutdown
	{
		values.SaveToXMLFile(valuespath)
		values.SaveToXMLFilePretty(valuesprettypath)
	}

	////////////////////////////////////////////////////////////////////

	def mkdirs(path: List[String])=path.tail.foldLeft(new File(path.head)){(a,b) => a.mkdir; new File(a,b)}.mkdir
	def mkdir(path: String)=mkdirs(List(path))

	def getListOfFiles(dir: String):List[File] =
	{
		val d = new File(dir)
		if (d.exists && d.isDirectory)
		{
			d.listFiles.filter(_.isFile).toList
		}
		else
		{
			List[File]()
		}
	}
	
	def getListOfFileNames(dir: String):List[String] =
		for(f<-getListOfFiles(dir)) yield f.getName

	def getListOfFileNamesWithExt(dir:String,ext:String):List[String]=
	{
		var l=scala.collection.mutable.ArrayBuffer[String]()
		for(name<-getListOfFileNames(dir))
		{
			val parts=name.split("\\.")
			if(parts.length==2)
			{
				if(parts(1)==ext)
				{
					l+=parts(0)
				}
			}
		}
		l.toList
	}

	def valuespath="stuff"+File.separator+"values.xml"
	def valuesprettypath="stuff"+File.separator+"valuespretty.xml"

	////////////////////////////////////////////////////////////////////

	// prefix bindings calculate a prefix to the path of a component

	var PrefixBindingGet:(String)=>Path=DefaultPrefixBindingGet

	var PrefixBindingSet:(String)=>Path=DefaultPrefixBindingSet

	def SetPrefixBindingGet(prefixbinding:(String)=>Path)
	{
		PrefixBindingGet=prefixbinding
	}

	def SetPrefixBindingSet(prefixbinding:(String)=>Path)
	{
		PrefixBindingSet=prefixbinding
	}

	def DefaultPrefixBindingGet(prefixget:String="components"):Path=
	{
		if(prefixget=="settings") return Path.FromString("{settings}")
		if(prefixget=="variant") return Path.FromString(Cve(null))
		Path.FromString("{components}")
	}

	def DefaultPrefixBindingSet(prefixset:String="components"):Path=
	{
		if(prefixset=="variant") return Path.FromString(Cve(null))
		Path.FromString("{components}")
	}

	////////////////////////////////////////////////////////////////////

	def Set(path:Path,value:Data):Data=
	{
		if(path==null) return values
		values=values.Set(path,value)
		values
	}

	def Set(pathstr:String,value:Data)
	{
		Set(Path.FromString(pathstr),value)
	}

	def Set(pathstr:String,value:String)
	{
		Set(Path.FromString(pathstr),StringData(value))
	}

	def Cve(pathstr:String):String=
	{
		val pref="{variantentries}#{"+Settings.get_variant+"}"
		if(pathstr==null) return pref
		s"$pref#$pathstr"
	}

	def Get(path:Path):Data=values.Get(path)

	def Get(pathstr:String):Data=values.Get(Path.FromString(pathstr))

	def GetStringWithDefault(id:String,default:String)=values.GS(id,default)

	def GetIntWithDefault(id:String,default:Int)=values.GI(id,default)

	def GetDoubleWithDefault(id:String,default:Double)=values.GD(id,default)

	def GetBooleanWithDefault(id:String,default:Boolean)=values.GB(id,default)

	def RegisterComponent(id:String,comp:MyComponent)
	{
		if(id==null) return
		if(comp==null) return
		components+=(id->comp)
	}

	def GetComponent(id:String):MyComponent=
	{
		if(id==null) return null
		if(components.contains(id)) return components(id)
		null
	}

	def GetMyBox(id:String):MyBox=GetComponent(id).asInstanceOf[MyBox]
	def GetMyText(id:String):MyText=GetComponent(id).asInstanceOf[MyText]
	def GetMyWebView(id:String):MyWebView=GetComponent(id).asInstanceOf[MyWebView]
	def GetMyCheckBox(id:String):MyCheckBox=GetComponent(id).asInstanceOf[MyCheckBox]
	def GetMySlider(id:String):MySlider=GetComponent(id).asInstanceOf[MySlider]
	def GetMyColorPicker(id:String):MyColorPicker=GetComponent(id).asInstanceOf[MyColorPicker]
	def GetMyComboBox(id:String):MyComboBox=GetComponent(id).asInstanceOf[MyComboBox]
	def GetMyTabPane(id:String):MyTabPane=GetComponent(id).asInstanceOf[MyTabPane]
	def GetGuiBoard(id:String):GuiBoard=GetComponent(id).asInstanceOf[GuiBoard]
	def GetGameBrowser(id:String):GameBrowser=GetComponent(id).asInstanceOf[GameBrowser]

	def ExecuteWebScript(id:String,script:String):String=
	{
		val w=GetMyWebView(id)
		if(w==null) return null
		w.ExecuteScript(script)
	}

	def GetWebEngine(id:String):WebEngine=
	{
		val w=GetMyWebView(id)
		if(w==null) return null
		w.GetEngine
	}

	def LoadWebContentAndScrollTo(id:String,content:String,yscroll:Double)
	{
		val w=GetMyWebView(id)
		if(w!=null) w.LoadContentAndScrollTo(content,yscroll)
	}

	def LoadWebContent(id:String,content:String)
	{
		val w=GetMyWebView(id)
		if(w!=null) w.LoadContent(content)
	}

	def WriteWebContent(id:String,content:String)
	{
		val w=GetMyWebView(id)
		if(w!=null) w.WriteContent(content)
	}

	case class MyStage(
		id:String,
		title:String=null,
		blob:String=null,
		s:Stage=new Stage(),
		handler:(MyEvent)=>Unit=MyComponent.default_handler,
		show:Boolean=true,
		andwait:Boolean=false,
		usewidth:Boolean=true,
		useheight:Boolean=true,
		unclosable:Boolean=false,
		modal:Boolean=false,
		store:Boolean=true
	)
	{
		def setTitle(title:String)
		{
			s.setTitle(title)
		}

		if(title!=null) setTitle(title)

		s.setX(GD(s"{stages}#$id#{x}",10.0))
		s.setY(GD(s"{stages}#$id#{y}",10.0))
		
		if(usewidth)
		{
			s.setWidth(GD(s"{stages}#$id#{width}",600.0))
		}

		if(useheight)
		{
			s.setHeight(GD(s"{stages}#$id#{height}",400.0))
		}

		s.xProperty().addListener(
			new ChangeListener[Number]
			{
				def changed(ov:ObservableValue[_ <: Number],old_val:Number,new_val:Number)
				{ 
					if(store) Set(s"{stages}#$id#{x}", new_val.toString())
				}
			}
		)

		s.yProperty().addListener(
			new ChangeListener[Number]
			{
				def changed(ov:ObservableValue[_ <: Number],old_val:Number,new_val:Number)
				{ 
					if(store) Set(s"{stages}#$id#{y}", new_val.toString())
				}
			}
		)

		s.widthProperty().addListener(
			new ChangeListener[Number]
			{
				def changed(ov:ObservableValue[_ <: Number],old_val:Number,new_val:Number)
				{ 
					if(store) Set(s"{stages}#$id#{width}", new_val.toString())
				}
			}
		)

		s.heightProperty().addListener(
			new ChangeListener[Number]
			{
				def changed(ov:ObservableValue[_ <: Number],old_val:Number,new_val:Number)
				{ 
					if(store) Set(s"{stages}#$id#{height}", new_val.toString())
				}
			}
		)

		if(blob!=null)
		{
			val comp=MyComponent.FromBlob(blob,handler)

			comp.CreateNode

			s.setScene(new Scene(comp.GetParent))
		}

		if(modal)
		{
			s.initModality(Modality.APPLICATION_MODAL)
		}

		if(unclosable)
		{
			s.setOnCloseRequest(new EventHandler[WindowEvent]
			{
				def handle(ev:WindowEvent)
				{
					ev.consume()
				}
			})
		} else {
			s.setOnCloseRequest(new EventHandler[WindowEvent]
			{
				def handle(ev:WindowEvent)
				{
					RemoveStage(id)
					Fire(id,handler,"stage closed","")
				}
			})
		}

		stages+=(id->this)

		if(show)
		{
			if(andwait)
			{
				s.showAndWait()
			}
			else
			{
				s.show()
			}
		}

		def SetScene(p:Parent){ s.setScene(new Scene(p)) }
		def Close{ s.close() }
		def Show{ s.show() }
		def SetX(w:Double) { s.setX(w) }
		def SetY(w:Double){ s.setY(w) }
		def GetX():Double=s.getX()
		def GetY():Double=s.getY()
		def SetWidth(w:Double){	s.setWidth(w) }
		def SetHeight(h:Double){ s.setHeight(h) }
		def ToTop{ s.toFront() }
	}

	def Fire(id:String,handler:(MyEvent)=>Unit,kind:String,value:String)
	{
		val comp=new MyDummy(id)
		val ei=ExecutionItem(
			client=s"Builder.MyStage",
			new Runnable{def run{
			handler(MyEvent(kind,comp,value))
		}})
		MyActor.queuedExecutor ! ei
	}

	def HasStage(id:String):Boolean=stages.contains(id)

	def GetStage(id:String):MyStage=
	{
		if(HasStage(id)) return stages(id)
		null
	}

	def RemoveStage(id:String)
	{
		if(HasStage(id))
		{
			stages-=id
		}
	}

	def CloseStage(id:String)
	{
		if(HasStage(id))
		{
			stages(id).Close
			stages-=id
		}
	}

	def SystemPopUp(title:String,content:String,dur:Int=1500)
	{		
		val ei=ExecutionItem(
			client="Builder.SystemPopup",
			code=new Runnable{def run{
			Builder.DoSystemPopUp(title,content,dur)
		}})
		MyActor.queuedExecutor ! ei
	}

	var popupcnt=0
	def DoSystemPopUp(title:String,content:String,dur:Int=1500)
	{
		val id=s"{systempopup$popupcnt}"
		popupcnt+=1
		val wid=id+"#{web}"
		val blob=s"""
			|<vbox>
			|<webview id="$wid"/>
			|</vbox>
		""".stripMargin
		val s=MyStage(id,title,blob,modal=true,unclosable=false,store=false)
		s.SetWidth(300.0)
		s.SetHeight(200.0)
		val m=GetStage("{main}")
		if(m!=null)
		{
			val shift=(popupcnt%3)*25
			s.SetX(m.GetX+50.0+shift)
			s.SetY(m.GetY+50.0+shift)
		}
		WriteWebContent(wid,content)
		Future
		{
			Thread.sleep(dur)
			val ei=ExecutionItem(
				client="Builder.DoSystemPopUp",
				code=new Runnable{def run{
				CloseStage(id)
			}})
			MyActor.queuedExecutor ! ei
		}
	}

	def WriteDocumentScript(content:String):String=
	{
		def replq(content:String):String=(for(char<-content.replaceAll("\\s"," ").toList) yield
		{					
			char match {
				case '"' => """'"'"""
				case '\\' => """"\\""""
				case _ => s""""$char""""
			}
		}).mkString("+")
		"document.open();\n"+(for(line<-content.split("\n").map(replq))
			yield s"""document.write($line);""").mkString("\n")+
		"document.close();"
	}

	def InputTexts(title:String=null,prompts:List[String]=null,
		applyname:String="Apply",candelete:Boolean=false,deletemsg:String="Delete this item"):InputTextResult=
	{
		var canceled=true
		var delete=false
		var results:Map[String,String]=null

		def GetResults
		{
			results=(for(prompt<-prompts) yield 
			(prompt->GetMyText(s"{textinputs}#{$prompt}").GetText)).toMap
		}

		def GetResultsAndCloseStage
		{
			GetResults
			CloseStage("{inputtextdialog}")
		}

		def input_text_handler(ev:MyEvent)
		{
			if(ev.kind=="button pressed")
			{
				if(ev.Id=="{textinputdelete}")
				{
					canceled=false
					delete=true
					GetResultsAndCloseStage
				}

				if(ev.Id=="{textinputcancel}")
				{
					canceled=true
					GetResultsAndCloseStage
				}

				if(ev.Id=="{textinputok}")
				{
					canceled=false
					delete=false
					GetResultsAndCloseStage
				}
			}

			if(ev.kind=="textfield entered")
			{
				canceled=false
				delete=false
				GetResultsAndCloseStage
			}
		}

		if((title==null)||(prompts==null)) return InputTextResult()

		var r=0
		val promptscontent=(for(prompt<-prompts) yield
		{
			r+=1
			s"""
				|<label text="$prompt" r="$r" c="1"/>
				|<textfield id="{textinputs}#{$prompt}" width="300.0" style="-fx-font-size: 24px;" r="$r" c="2"/>
			""".stripMargin
		}).mkString("\n")

		val deletecontent=if(!candelete) "" else s"""			
			|<button id="{textinputdelete}" style="-fx-background-color: #ffafaf;" text="$deletemsg"/>
		""".stripMargin

		r+=1
		val blob=s"""
			|<vbox>
			|<gridpane vgap="5" hgap="10">
			|$promptscontent
			|<button id="{textinputcancel}" style="-fx-font-size: 20px;" text="Cancel" r="$r" c="1"/>
			|<button id="{textinputok}" width="300.0" style="-fx-font-size: 24px;" r="$r" c="2" text="$applyname"/>
			|</gridpane>
			|$deletecontent
			|</vbox>
		""".stripMargin

		MyStage("{inputtextdialog}",title,blob,modal=true,unclosable=false,
			andwait=true,usewidth=false,useheight=false,handler=input_text_handler)

		InputTextResult(canceled,delete,results)
	}

	def Confirm(title:String=null):Boolean=
	{
		var canceled=true

		def confirm_handler(ev:MyEvent)
		{
			if(ev.kind=="button pressed")
			{
				if(ev.Id=="{confirmcancel}")
				{
					canceled=true
					CloseStage("{confirmdialog}")
				}

				if(ev.Id=="{confirmok}")
				{
					canceled=false
					CloseStage("{confirmdialog}")
				}
			}
		}

		if(title==null) return false

		val blob=s"""
			|<vbox padding="10">
			|<gridpane vgap="10" hgap="10">
			|<button id="{confirmok}" width="300.0" style="-fx-font-size: 24px;" text="Ok" r="1" c="2"/>
			|<button id="{confirmcancel}" style="-fx-font-size: 20px;" text="Cancel" r="2" c="1"/>
			|</gridpane>
			|</vbox>
		""".stripMargin

		MyStage("{confirmdialog}",title,blob,modal=true,unclosable=false,
			andwait=true,usewidth=false,useheight=false,handler=confirm_handler)

		!canceled
	}

	def RemoveSpecials(fromwhat:String):String=
	{
		fromwhat.replaceAll("[^a-zA-z0-9\\s_]","")
	}

	def GetTabList(id:String):Tuple2[List[String],TabPane]=
	{
		val mytabpane=GetMyTabPane(id)

		if(mytabpane==null) return null

		val tabspane=mytabpane.GetNode.asInstanceOf[TabPane]

		val tabs=tabspane.getTabs()

		Tuple2((for(i<-0 to tabs.size-1) yield tabs.get(i).getText()).toList,tabspane)
	}

	def SelectTab(id:String,tabname:String)
	{
		val tablist=GetTabList(id)

		val i=tablist._1.indexOf(tabname)

		if(i< 0) return

		tablist._2.getSelectionModel().select(i)
	}

	def SelectTab(id:String,tabindex:Int)
	{
		val tablist=GetTabList(id)
		
		tablist._2.getSelectionModel().select(tabindex)
	}

	def ChooseFile(purpose:String):java.io.File=
	{
		val fc=new FileChooser()

		var initial_dir:String=purpose match {
			case "openpgn" => Settings.get_pgn_dir
			case "buildpgn" => Settings.get_build_dir
			case _ => null
		}
		
		if(!new File(initial_dir).exists()) initial_dir=null

		if(initial_dir!=null) if(new File(initial_dir).exists)
		{
			fc.setInitialDirectory(new File(initial_dir))
		}

		val s=new Stage()

		s.initModality(Modality.APPLICATION_MODAL)

		val f=fc.showOpenDialog(s)

		if(f!=null)
		{
			val dir=f.getParent()

			val name=f.getName()

			Set(s"{settings}#{lastopen}#{$purpose}#{dir}",dir)
			Set(s"{settings}#{lastopen}#{$purpose}#{name}",name)

			purpose match {
				case "openpgn" => Settings.set_pgn_dir(dir)
				case "buildpgn" => Settings.set_build_dir(dir)
				case _ =>
			}
		}

		f
	}

	def SaveFileAsDialog(title:String,purpose:String,content:String,successcallback:()=>Unit=null)
	{
		var initial_dir:String=""

		if(purpose=="savepgn") initial_dir=Settings.get_pgn_dir

		if(initial_dir=="") initial_dir=GS("{components}#{saveasdir}","")

		if(!new File(initial_dir).exists()) initial_dir=""

		var initial_name=GS("{components}#{saveasname}","")

		if(purpose=="savepgn") initial_name=GS("{settings}#{lastopen}#{openpgn}#{name}","default.pgn")

		Set("{components}#{saveasdir}",initial_dir)
		Set("{components}#{saveasname}",initial_name)

		def DoSave
		{
			var name=GetMyText("{saveasname}").GetText

			if(name=="") name="default"

			val parts=name.split("\\.").toList

			if(parts(parts.length-1).toLowerCase!="pgn")
			{
				name+=".pgn"
			}
			
			var savepath=initial_dir+File.separator+name

			val initialdirexists=new File(initial_dir).exists()

			if(!initialdirexists) savepath=name

			var confirm=true

			if(new File(savepath).exists()) confirm=Confirm("File already exists, replace?")

			if(confirm)
			{
				DataUtils.WriteStringToFile(savepath,content)

				if(initialdirexists)
				{
					if(purpose=="savepgn") Settings.set_pgn_dir(initial_dir)

					Set("{components}#{saveasdir}",initial_dir)
				}

				if(purpose=="savepgn") Set("{settings}#{lastopen}#{openpgn}#{name}",name)

				Set("{components}#{saveasname}",name)

				Fire("{savefileasdialog}",MyComponent.default_handler,"file saved as",savepath+" : "+content)

				CloseStage("{savefileasdialog}")

				if(successcallback!=null) successcallback()
			}
		}

		def saveas_handler(ev:MyEvent)
		{
			if(ev.kind=="textfield entered")
			{
				if(ev.Id=="{saveasname}")
				{
					DoSave
				}
			}

			if(ev.kind=="button pressed")
			{
				if(ev.Id=="{cancel}")
				{
					CloseStage("{savefileasdialog}")
				}

				if(ev.Id=="{save}")
				{
					DoSave
				}

				if(ev.Id=="{choosesaveasdir}")
				{
					val dc=new DirectoryChooser()

					if(new File(initial_dir).exists)
					{
						dc.setInitialDirectory(new File(initial_dir))
					}

					val f=dc.showDialog(new Stage())

					if(f!=null)
					{
						val path=f.getPath()

						initial_dir=path

						GetMyText("{saveasdir}").SetText(path)
					}
				}
			}
		}

		val blob=s"""
			|<vbox>
			|<gridpane hgap="10" vgap="10">
			|<label text="File name" r="1" c="1"/>
			|<textfield id="{saveasname}" style="-fx-font-size: 24px;" r="1" c="2"/>
			|<label text="Directory" r="2" c="1"/>
			|<label id="{saveasdir}" width="400.0" style="-fx-font-size: 10px;" r="2" c="2"/>
			|<button id="{choosesaveasdir}" text="Choose directory" r="3" c="2"/>
			|<button id="{cancel}" style="-fx-font-size: 20px;" text="Cancel" r="4" c="1"/>
			|<button id="{save}" width="400.0" style="-fx-font-size: 24px;" text="Save" r="4" c="2" cs="2"/>
			|</gridpane>
			|</vbox>
		""".stripMargin

		val s=MyStage("{savefileasdialog}",title,blob,modal=true,
			handler=saveas_handler,usewidth=false,useheight=false)
	}

	def AbortDialog(title:String="Abort operation",callback:()=>Unit=null)
	{
		val blob=s"""
			|<vbox>
			|<button id="{abortoperation}" width="300.0" style="-fx-font-size: 24px;" text="Abort"/>
			|</vbox>
		""".stripMargin

		def abort_handler(ev:MyEvent)
		{
			if(ev.kind=="button pressed")
			{
				if(ev.Id=="{abortoperation}")
				{
					CloseAbortDialog
					callback()
				}
			}
		}

		val s=MyStage("{abortoperationdialog}",title,blob,modal=true,unclosable=true,
			handler=abort_handler,usewidth=false,useheight=false)

		val m=GetStage("{main}")
		if(m!=null)
		{			
			s.SetX(m.GetX+50.0)
			s.SetY(m.GetY+50.0)
		}
	}

	def CloseAbortDialog
	{
		val ei=ExecutionItem(
			client="Builder.CloseAbortDialog",
			code=new Runnable{def run{
			CloseStage("{abortoperationdialog}")
		}})
		MyActor.queuedExecutor ! ei
	}

	def L(what:String)
	{
		MyActor.logSupervisor ! AddItem("{systemlog}",MyLogItem(what))
	}

	def PrintStages
	{
		println("stages "+stages.keys)
	}

	def CloseAllStages
	{
		for((id,stage)<-stages) CloseStage(id)
	}

}

// InputTextResult holds the result of an input text dialog

case class InputTextResult(
	canceled:Boolean=true,
	deleteitem:Boolean=false,
	texts:Map[String,String]=null
)
{	
}

// Timer is a system timer

class Timer
{
	var t0=System.nanoTime()
	def elapsed:Double = (System.nanoTime() - t0)/1.0e9
}

// HeapSize reports the system heap size in Mbytes

object HeapSize
{
	def heapsize = Runtime.getRuntime().totalMemory()/1000000
	def heapsize_B = Runtime.getRuntime().totalMemory()
}

// ClipboardSimple provides access to the system clipboard

object ClipboardSimple extends java.awt.datatransfer.ClipboardOwner
{

	override def lostOwnership(aClipboard:java.awt.datatransfer.Clipboard,aContents:java.awt.datatransfer.Transferable)
	{
		//do nothing
	}

	def clipget:String=getClipboardContents

	def getClipboardContents:String=
	{
		var result=""

		val clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()

		val contents = clipboard.getContents(null)

		val hasTransferableText =
			(contents != null) &&
			contents.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)

		if(hasTransferableText)
		{
			try
			{
				result = contents.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor).toString
			}
			catch
			{
				case _ : Throwable => result=""
			}
		}

		result
	}

	def clipset(content:String)
	{
		setClipboardContents(content)
	}

	def setClipboardContents(content:String)
	{
		val stringSelection = new java.awt.datatransfer.StringSelection(content)

		val clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()

		clipboard.setContents(stringSelection, this)
	}

}

