package guibuilder

import scala.xml._

import java.io._
import java.nio.file._

import Settings._

import scala.io.Source

import org.apache.commons.io.FileUtils._

import Encode32._

object butils
{	
	val pborders=Map[Int,String](
		0  -> "",
		1  -> "border-style: solid; border-width: 1px; border-color: #00af00;",
		2  -> "border-style: solid; border-width: 2px; border-color: #00af00;",
		3  -> "border-style: solid; border-width: 3px; border-color: #00af00;",
		4  -> "border-style: solid; border-width: 4px; border-color: #00ff00;",
		5  -> "border-style: solid; border-width: 5px; border-color: #00ff00;",
		6  -> "border-style: solid; border-width: 6px; border-color: #00af00;",
		7  -> "border-style: solid; border-width: 7px; border-color: #00af00;",
		8  -> "border-style: solid; border-width: 8px; border-color: #00af00;",
		9  -> "border-style: solid; border-width: 9px; border-color: #00ff00;",
		10 -> "border-style: solid; border-width: 10px; border-color: #00ff00;"
	)

	var interrupted=false

	val PLUS_MATE_THRESOLD= 9000
	val MINUS_MATE_THRESOLD= -PLUS_MATE_THRESOLD

	val PLUS_GOOD_MOVE_THRESOLD= 500
	val MINUS_GOOD_MOVE_THRESOLD= -PLUS_GOOD_MOVE_THRESOLD

	val PLUS_PROMISING_MOVE_THRESOLD= 300
	val MINUS_PROMISING_MOVE_THRESOLD= -PLUS_PROMISING_MOVE_THRESOLD

	def IsMated(eval:Int):Boolean=(eval < MINUS_MATE_THRESOLD)
	def IsMate(eval:Int):Boolean=(eval > PLUS_MATE_THRESOLD)

	def IsBad(eval:Int):Boolean=((eval < MINUS_GOOD_MOVE_THRESOLD)&&(!IsMated(eval)))
	def IsGood(eval:Int):Boolean=((eval > PLUS_GOOD_MOVE_THRESOLD)&&(!IsMate(eval)))

	def IsInteresting(eval:Int):Boolean=((eval < MINUS_PROMISING_MOVE_THRESOLD)&&(!IsBad(eval))&&(!IsMated(eval)))
	def IsPromising(eval:Int):Boolean=((eval > PLUS_PROMISING_MOVE_THRESOLD)&&(!IsGood(eval))&&(!IsMate(eval)))

	def IsEval(what:String):Boolean=
	{
		var isint=true

		try{
			val eval=what.toInt
		}catch{case e:Throwable=>{isint=false}}

		if(isint) return true

		if(!DataUtils.StartsWith(what,'E')) return false

		try{
			val eval=what.substring(2,what.length).toInt
		}catch{case e:Throwable=>{return false}}

		true
	}

	def Eval(what:String):Int=
	{
		if(IsEval(what))
		{
			if(DataUtils.StartsWith(what,'E')) return what.substring(2,what.length).toInt
			else return what.toInt
		}
		0
	}

	def list_books(v:String=get_variant):List[String]=
	{
		Builder.getListOfFileNamesWithExt(s"stuff/books/$v","txt")
	}

	def default_log_callback(what:String)
	{
		println(what)
	}

	var log_callback:(String)=>Unit=default_log_callback

	def del_book(db:String,v:String=get_variant)
	{

		interrupted=false

		val b=new book(List("stuff","book",v))

		val pl=new PosList(db)

		for(fen<-pl.pl.keys)
		{
			if(interrupted) return

			log_callback("deleting position "+fen)

			b.loadPos(fen,loadcurrent=false)

			if(b.booklist.remove_book(db))
			{
				log_callback("done")
			}

			b.savePos(addcurrent=false)
		}

		val path=(pl.path.split("/")).mkString(File.separator)
		
		val f=new File(path)

		log_callback("deleting "+path)
		
		try
		{
			f.setWritable(true)
			f.delete()
		}
		catch
		{
			case e:NoSuchFileException => log_callback("no such file")
			case e:IOException => log_callback("io exception")
			case e:Throwable => log_callback("exception")
		}

	}
}

case class bookEntry(
	var san:String="",
	var plays:Int=0,
	var annot:String="",
	var wins:Int=0,
	var draws:Int=0,
	var losses:Int=0,
	var comment:String="-",
	var uci:String="-",
	var priority:Int=0
	)
{
	def toXml=
	{
		<move san={san}>
		<plays>{plays}</plays>
		<annot>{annot}</annot>
		<wins>{wins}</wins>
		<draws>{draws}</draws>
		<losses>{losses}</losses>
		<comment>{comment}</comment>
		<uci>{uci}</uci>
		<priority>{priority}</priority>
		</move>
	}

	def fromXml(move:NodeSeq)
	{
		san=(move \ "@san").text

		plays=DataUtils.ParseInt((move \ "plays").text,0)
		annot=(move \ "annot").text
		wins=DataUtils.ParseInt((move \ "wins").text,0)
		draws=DataUtils.ParseInt((move \ "draws").text,0)
		losses=DataUtils.ParseInt((move \ "losses").text,0)
		comment=(move \ "comment").text
		if((comment==null)||(comment=="")) comment="-"
		uci=(move \ "uci").text
		if((uci==null)||(uci=="")) uci="-"
		priority=DataUtils.ParseInt((move \ "priority").text,0)
	}
}

case class bookPosition()
{

	var fen:String=""

	var entries=Map[String,bookEntry]()

	var games=""

	def FromFen(set_fen:String):bookPosition=
	{
		fen=Transp.mstrip(set_fen)
		return this
	}

	def update_result(san:String,result:String,uci:String)
	{
		val win=(result=="1-0")
		val draw=(result=="1/2-1/2")
		val loss=(result=="0-1")

		if(entries.contains(san))
		{
			if(win) entries(san).wins+=1
			if(draw) entries(san).draws+=1
			if(loss) entries(san).losses+=1
		}
		else
		{
			if(win) entries+=(san->bookEntry(san=san,wins=1,uci=uci))
			if(draw) entries+=(san->bookEntry(san=san,draws=1,uci=uci))
			if(loss) entries+=(san->bookEntry(san=san,losses=1,uci=uci))
		}
	}

	def getEval(san:String):Int=
	{
		if(entries.contains(san))
		{
			val comment=entries(san).comment
			return butils.Eval(comment)
		}
		0
	}

	def setEval(san:String,eval:Int)
	{
		if(entries.contains(san))
		{
			entries(san).comment="E "+eval
		}
	}

	def setPlays(san:String,plays:Int)
	{
		if(entries.contains(san))
		{
			entries(san).plays=plays
		}
	}

	def incPriority(san:String)
	{
		if(entries.contains(san))
		{
			val currentpriority=entries(san).priority
			val newpriority=if(currentpriority<10) currentpriority+1 else 10
			entries(san).priority=newpriority			
		}
	}

	def setPriority(san:String,priority:Int)
	{
		if(entries.contains(san))
		{
			entries(san).priority=priority			
		}
	}

	def decPriority(san:String)
	{		
		if(entries.contains(san))
		{
			val currentpriority=entries(san).priority
			val newpriority=if(currentpriority>0) currentpriority-1 else 0
			entries(san).priority=newpriority
		}
	}

	def inc_move_count(san:String,uci:String)
	{
		if(entries.contains(san))
		{
			entries(san).plays+=1
		}
		else
		{
			entries+=(san->bookEntry(san=san,plays=1,uci=uci))
		}
	}

	def get_entry(san:String):bookEntry=
	{
		if(!entries.contains(san)) return null
		return entries(san)
	}

	def annot(san:String,annot:String=null,uci:String,addcomment:String=null,count:Int= -1)
	{
		if(entries.contains(san))
		{
			if(annot!=null) entries(san).annot=annot
			if(count>=0)entries(san).plays=count
		}
		else
		{
			entries+=(san->bookEntry(san=san,annot=(if(annot!=null) annot else ""),uci=uci,plays=(if(count>=0) count else 1)))
		}
		if(addcomment!=null)
		{
			entries(san).comment=addcomment
		}
	}

	def comment(san:String,comment:String,uci:String)
	{
		val col=Builder.GS("{bookcommentbackgroundcolor}","#ffffff")

		if(entries.contains(san))
		{
			entries(san).comment=comment
		}
		else
		{
			entries+=(san->bookEntry(san=san,comment=comment,uci=uci))
		}
	}

	def get_move_comment(san:String):String=
	{
		if(!entries.contains(san)) return "-"
		entries(san).comment
	}

	def del(san:String)
	{
		if(entries.contains(san))
		{
			entries-=san
		}
	}

	def delall
	{
		entries=Map[String,bookEntry]()
	}

	def game_found(gameMd5:String):Boolean=((gameMd5!="")&&games.contains(gameMd5))

	def add_game(gameMd5:String)
	{

		if(gameMd5=="") return

		if(game_found(gameMd5)) return

		if(games=="")
		{
			games=gameMd5
		}
		else
		{
			games+=("_"+gameMd5)
		}
		
	}

	def toXml=
	{
		val entry_list=(for((k,v)<-entries) yield v.toXml).toList

		{
			<position fen={fen}>
			<movelist>
				{entry_list}
			</movelist>
			<games>{games}</games>
			</position>
		}
	}

	def fromXml(xml:NodeSeq)
	{

		entries=Map[String,bookEntry]()

		fen=(xml \ "position" \ "@fen").text		

		val moves=xml \ "position" \ "movelist" \ "move"
		
		for(move<-moves)
		{

			val entry=bookEntry()

			entry.fromXml(move)

			entries+=(entry.san->entry)

		}

		games=(xml \ "position" \ "games").text
		if(games==null) games=""

	}

	def sortedKeys:Array[String]=
	{
		def sortfunc(ak:String,bk:String):Boolean=
		{
			val a=entries(ak)
			val b=entries(bk)
			val apr=entries(ak).priority
			val bpr=entries(bk).priority
			if((apr>5)||(bpr>5))
			{
				return apr > bpr
			}			
			val aa=ANNOTATIONS.reverse.indexOf(a.annot)
			val ba=ANNOTATIONS.reverse.indexOf(b.annot)
			if(aa!=ba) return aa > ba
			val ap=entries(ak).plays
			val bp=entries(bk).plays
			if(ap!=bp) return ap > bp			
			var gr=false
			var evalsequal=true
			if(DataUtils.StartsWith(a.comment,'E')&&DataUtils.StartsWith(b.comment,'E'))
			{				
				try{
				val evala=a.comment.substring(2,a.comment.length).toInt
				val evalb=b.comment.substring(2,b.comment.length).toInt
				evalsequal=(evala==evalb)
				gr=(evala > evalb)
				}catch{case e:Throwable=>{}}
			}
			if(evalsequal)
			{
				return apr > bpr
			}			
			gr
		}

		entries.keys.toArray.sortWith(sortfunc)
	}

	def IsRecommendedAnnot(annot:String,comment:String=null):Boolean=
	{
		if(!(annot.contains("!")&&(!annot.contains("?")))) return false
		if(annot.contains("!!")) return true
		if(comment==null) return true
		if(!DataUtils.StartsWith(comment,'E')) return true
		val playenginepercent=Builder.GD("{components}#{timingsplayenginepercent}",70.0)
		if(r.nextInt(100)< playenginepercent) return true
		false
	}

	val r=new scala.util.Random()

	def GetRecommendedEntry:bookEntry=
	{
		val playbookpercent=Builder.GD("{components}#{timingsplaybookpercent}",90)

		if(playbookpercent>r.nextInt(100))
		{
			val keys=sortedKeys.filter(key => IsRecommendedAnnot(entries(key).annot,entries(key).comment))

			val len=keys.length

			if(len<=0) return null

			var e:bookEntry=null
			for(j<- 0 to 2)
			{
				val i=r.nextInt(len)

				e=entries(keys(i))

				if(e.annot.contains("!!")) return e
			}
			e
		}
		else
		{
			null
		}
	}

	def getMovesWithEval:List[String]=
	{
		(for((san,entry)<-entries if(butils.IsEval(entry.comment))) yield san).toList
	}

	val commentcols=Map[String,String](
		"solution" -> "#ffff9f"
	)

	def toPrintable(html:Boolean=false,madesans:List[String]=List[String]()):String=
	{

		val keys=sortedKeys

		if(!html) return(
		"\n%-10s | %5s | %5s | %5s | %5s".format("move","plays","1-0","draw","0-1")+
		"\n-----------+-------+-------+-------+-------\n"+
		(for(k<-keys) yield {
			"%-10s | %5d | %5d | %5d | %5d".format(k+" "+entries(k).annot,entries(k).plays,entries(k).wins,entries(k).draws,entries(k).losses)
			}).mkString("\n")+"\n"
		)

		val td="""<td align="center">"""

		val items=
		(for(k<-keys) yield {

			val annot=entries(k).annot
			val plays=entries(k).plays
			val wins=entries(k).wins
			val draws=entries(k).draws
			val losses=entries(k).losses
			val comment=entries(k).comment
			val uci=entries(k).uci
			var commentcol="#FFFFFF"
			var priority=entries(k).priority

			if(commentcols.contains(comment)) commentcol=commentcols(comment)

			val col=get_annot_color(annot)

			val annots=(for(a<-ANNOTATIONS) yield
			{
				val acol=get_annot_color(a)
				s"""
					|<td align="center" onmousedown="setclick('$k','annot','$a','$uci');">
					|<span style="cursor: pointer;">
					|<font color="$acol" size="3">$a</font>
					|</span>
					|</td>
				""".stripMargin
			}).mkString("\n")			

			var border=butils.pborders(priority)

			val uri=new File("web/forwardt.png").toURI()

			val made=if(madesans.contains(k))
				s"""<img src="$uri">&nbsp;"""
			else ""

			s"""
				|<tr style="background-color: $commentcol; $border">
				|<td width="100" align="center" onmousedown="setclick('$k','make','','$uci');">
				|<a name="dummy"></a>
				|<a href="#dummy" style="text-decoration: none;">
				|<font color="$col" size="6"><b>
				|$made$k
				|</b></font>
				|</a>
				|</td>
				|$td<font color="$col" size="5"><b>$annot</b></font></td>
				|
				|<td align="center" onmousedown="setclick('$k','incpriority','','$uci');">
				|<a name="dummy2"></a>
				|<a href="#dummy2" style="text-decoration: none;">
				|<font color="#7fff7f" size="3">up</font>
				|</a>
				|</td>
				|
				|<td align="center" onmousedown="setclick('$k','setpriority','','$uci');">
				|<a name="dummy2"></a>
				|<a href="#dummy2" style="text-decoration: none;">
				|<font color="#7f7fff" size="4">$priority</font>
				|</a>
				|</td>
				|
				|<td align="center" onmousedown="setclick('$k','decpriority','','$uci');">
				|<a name="dummy3"></a>
				|<a href="#dummy3" style="text-decoration: none;">
				|<font color="ff7f7f" size="3">dn</font>
				|</a>
				|</td>
				|
				|$td<font color="#000000" size="4"><b>$plays</b></font></td>
				|$td<font color="#007f00" size="4"><b>$wins<b></font></td>
				|$td<font color="#00007f" size="4"><b>$draws<b></font></td>
				|$td<font color="#7f0000" size="4"><b>$losses<b></font></td>
				|$annots
				|$td
				|<span style="cursor: pointer;">
				|<font color="#ff0000" size="3" onmousedown="setclick('$k','del','','$uci');">X</font>
				|</span>
				|</td>
				|$td
				|<span style="cursor: pointer;">
				|<font color="#000000" size="3" onmousedown="setclick('$k','comment','','$uci');">$comment</font>
				|</span>
				|</td>
				|</tr>
				|<tr>
				|<td colspan="10">
				|</td>
				|</tr>
			""".stripMargin
			}).mkString("\n")

		val numannots=ANNOTATIONS.length

		s"""
			|<script>
			|var key="";
			|var action="";
			|var param="";
			|var uci="";
			|function setclick(setkey,setaction,setparam,setuci)
			|{
			|	key=setkey;
			|	action=setaction;
			|	param=setparam;
			|	uci=setuci;
			|}
			|</script>
			|<table border="0" cellpadding="3" cellspacing="3" style="border-collapse: collapse;">
			|<tr>
			|$td <i>move</i></td>
			|$td <i>annot</i></td>
			|<td align="center" colspan="3"><i>priority</i></td>
			|$td <i>plays</i></td>
			|$td <i>white wins</i></td>
			|$td <i>draws</i></td>
			|$td <i>black wins</i></td>
			|<td align="center" colspan="$numannots"><i>annotate</i></td>
			|<td align="center"><i>del</i></td>
			|<td align="center"><i>comment</i></td>
			|</tr>
			|$items
			|</table>
		""".stripMargin
	}

}

object Transp
{
	def mstrip(fen:String):String=
	{
		val parts=fen.split(" ")
		if(parts.length<=4) return fen
		parts(0)+" "+parts(1)+" "+parts(2)+" "+parts(3)
	}
}

case class bookList()
{

	var fen:String=""

	def name=encode(fen,true)

	var books=Map[String,bookPosition]()

	def FromFen(set_fen:String):bookList=
	{
		fen=Transp.mstrip(set_fen)
		return this
	}

	def remove_book(name:String):Boolean=
	{
		if(books.contains(name))
		{
			books=books-name
			return true
		}

		false
	}

	def toXml=
	{
		<booklist fen={fen}>
			{
				for((name,book)<-books) yield 
				{
					<book name={name} fen={fen}>{book.toXml}</book>
				}
			}
		</booklist>
	}

	def fromXml(xml:NodeSeq)
	{
		books=Map[String,bookPosition]()

		fen=(xml \ "@fen").text		

		val booksXml=xml \ "book"

		for(bookXml<-booksXml)
		{
			val bp=bookPosition()

			val name=(bookXml \ "@name").text

			bp.fromXml(bookXml)

			books+=(name->bp)
		}
	}
}

case class book(
	pathlist:List[String]=List[String]()
	)
{

	if(pathlist.length>0)
	{
		Builder.mkdirs(pathlist)
	}

	val path=pathlist.mkString("/")

	var currentPos=bookPosition()

	var booklist=bookList()

	var current_book="default"

	def loadPos(fen:String,loadcurrent:Boolean=true,setbook:String=null)
	{

		booklist=bookList().FromFen(fen)

		current_book=if(setbook!=null) setbook else get_current_book()

		var p=bookPosition().FromFen(fen)

		val fullpath=path+"/"+booklist.name+".xml"

		if(new File(fullpath).exists)
		{

			val xml=scala.xml.XML.loadFile(fullpath)

			booklist.fromXml(xml)

			if(loadcurrent)
			{

				if(booklist.books.contains(current_book))
				{
					p=booklist.books(current_book)
				}

			}

		}

		currentPos=p

	}

	def savePos(addcurrent:Boolean=true,booklistfen:String=null)
	{

		val fullpath=path+"/"+(if(booklistfen!=null) encode(Transp.mstrip(booklistfen),true) else booklist.name)+".xml"

		if(booklistfen!=null) currentPos.fen=Transp.mstrip(booklistfen)

		if(addcurrent) booklist.books+=(current_book->currentPos)

		val xml=booklist.toXml

		scala.xml.XML.save(fullpath,xml)

		if(addcurrent)
		{

			val pl=PosList(current_book)

			pl.add(currentPos.fen)

			pl.save

		}

	}

}

case class PosList(cb:String,v:String=get_variant)
{

	val path=s"stuff/books/$v/$cb.txt"

	var pl=Map[String,Boolean]()

	load

	def add(fen:String)
	{
		pl+=(fen->true)
	}

	def load
	{
		pl=Map[String,Boolean]()

		if(new File(path).exists)
		{
			val content=readFileToString(new File(path),null.asInstanceOf[String])
			for(line<-content.split("\n"))
			{
				pl+=(line->true)
			}
		}
		else
		{
			save
		}
	}

	def save
	{
		Builder.mkdirs(List("stuff","books",v))

		val content=(for((k,v)<-pl) yield k).mkString("\n")

		DataUtils.WriteStringToFile(path,content)
	}

}