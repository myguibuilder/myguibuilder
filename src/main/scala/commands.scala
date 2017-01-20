package guibuilder

import piece._
import square._
import board._
import movetable._
import book._

import Settings._

// object Commands keeps track of the chess state and provides high level commands to manipulate it

object Commands extends Module
{

	// g represents the game state within the application
	var g=new game

	// only the main game is entitled to make modifications to the book
	g.SetBookModificationsAllowed(true)
	
	// Module interface

	def Name="Commands"

	def Startup
	{

		load_settings

		save_settings

		SetVariant(get_variant)

		g.from_pgn_and_current_line(get_game_pgn,get_game_current_line)

		new PosList(get_current_book())

	}

	def Shutdown
	{

		set_game_pgn(g.report_pgn)

		set_game_current_line(g.current_line)

		save_settings

	}

	// commands

	def Reset
	{
		g.reset
	}

	def Back
	{
		g.back
	}

	def ToBegin
	{
		g.tobegin
	}

	def Delete
	{
		g.delete
	}

	def Forward
	{
		g.forward
	}

	def ToEnd
	{
		g.toend
	}

	def DeleteBook(db:String=get_current_book())
	{
		val old_pgn=g.report_pgn

		butils.del_book(db)

		g.set_from_pgn(old_pgn)

		g.tobegin
		
		SetCurrentBook("default")
	}

	def SetCurrentBook(scb:String=get_current_book())
	{
		set_current_book(scb)
		new PosList(scb)
		g.pos_changed
	}

	def SetCutOff(cutoff:Int=0)
	{
		set_build_cutoff(cutoff)
	}

	def SortPgnFile(path:String)
	{
		g.sort_pgn(DataUtils.ReadFileToString(path))
	}

	def ListBooks:List[String]=
	{
		butils.list_books()
	}

	// i starts from 1
	def GetBookGame(i:Int)
	{
		g.get_game(g.book_games,i-1)
	}

	// i starts from 1
	def GetPgnGame(i:Int)
	{
		g.get_game(g.pgn_games,i-1)
	}

	def SetVariant(v:String):Boolean=
	{
		if(!set_variant(v)) return false
		movetable.init(v)
		g.reset
		val current_book=get_current_book()
		SetCurrentBook(current_book)
		true
	}

	def BuildBook(pgn:String)
	{
		val old_pgn=g.report_pgn

		g.build_book(pgn)

		g.set_from_pgn(old_pgn)

		g.tobegin
	}

	def SortPgn(pgn:String)
	{
		g.sort_pgn(pgn)

		g.reset
	}

	def AnnotateMove(san:String,annot:String,uci:String,addcomment:String=null,count:Int= -1,dosave:Boolean=true)
	{
		g.book.currentPos.annot(san,annot,uci,addcomment,count)
		if(dosave) SaveGamePos
	}

	def DeleteMoveFromBook(san:String,dosave:Boolean=true)
	{
		g.book.currentPos.del(san)
		if(dosave) SaveGamePos
	}

	def DeleteAllMovesFromBook(dosave:Boolean=true)
	{
		g.book.currentPos.delall
		if(dosave) SaveGamePos
	}

	def CopyFen:String=
	{
		val fen=g.report_fen
		ClipboardSimple.clipset(fen)
		fen
	}

	def CopyPgn:String=
	{
		val pgn=g.report_pgn
		ClipboardSimple.clipset(pgn)
		pgn
	}

	def CopyPgnTree:String=
	{
		val pgntree=g.report_pgn_tree
		ClipboardSimple.clipset(pgntree)
		pgntree
	}

	def CopyCurrentLine:String=
	{
		val currentline=g.current_line_pgn
		ClipboardSimple.clipset(currentline)
		currentline
	}

	def CopyCurrentLineAlgeb:String=
	{
		val currentlinealgeb=g.current_line_algeb
		ClipboardSimple.clipset(currentlinealgeb)
		println("calculating current line algeb")
		currentlinealgeb
	}

	def PasteFen:String=
	{
		val fen=ClipboardSimple.clipget
		g.set_from_fen(fen)
		fen
	}

	def PastePgn:String=
	{
		val pgn=ClipboardSimple.clipget
		g.set_from_pgn(pgn)
		pgn
	}

	def SetBookEnabled(be:Boolean)
	{
		set_book_enabled(be)

		SetCurrentBook()
	}

	def SetIncMoveCount(incmc:Boolean)
	{
		set_inc_move_count(incmc)
	}

	def SetAddGames(ag:Boolean)
	{
		set_add_games(ag)
	}

	def SetUpdateResult(ur:Boolean)
	{
		set_update_result(ur)
	}

	def MakeSanMove(san:String)
	{			
		val m=g.b.sanToMove(san)
		if(m==null)
		{
			println("illegal move")
		}
		else
		{
			g.makeMove(m)
		}
	}

	def ToggleFlip:Boolean=
	{
		set_flip(!get_flip)
		get_flip
	}

	def AddMoveToBook(annot:String=null,forward:Boolean=false,count:Int= -1,del:Boolean=false,addcomment:String=null,dosave:Boolean=true):String=
	{
		if(g.current_node!=g.root)
		{
			val san=g.current_node.genSan
			g.back
			g.SetForceIncMoveCount(true)
			g.makeSanMove(san)
			g.SetForceIncMoveCount(false)
			if(del) g.delete else g.back

			g.AnnotMove(san,annot,count,addcomment)

			if(dosave) SaveGamePos

			if(forward)
			{
				g.makeSanMove(san)
			}
			
			san
		}
		else
		{
			""
		}
	}

	def GetBookMoveComment(commentedsan:String):String=g.GetBookMoveComment(commentedsan)

	def CommentBookMove(commentedsan:String,comment:String,commenteduci:String,dosave:Boolean=true)
	{
		g.book.currentPos.comment(commentedsan,comment,commenteduci)
		if(dosave) SaveGamePos
	}

	def GetBookMovesWithEval:List[String]=g.book.currentPos.getMovesWithEval

	def GetBookMoveEval(san:String):Int=g.book.currentPos.getEval(san)

	def SetBookMoveEval(san:String,eval:Int,dosave:Boolean=true)
	{
		g.book.currentPos.setEval(san,eval)
		if(dosave) SaveGamePos
	}

	def SetBookMovePlays(san:String,plays:Int,dosave:Boolean=true)
	{
		g.book.currentPos.setPlays(san,plays)
		if(dosave) SaveGamePos
	}

	def SetBookMovePriority(san:String,priority:Int,dosave:Boolean=true)
	{
		g.book.currentPos.setPriority(san,priority)
		if(dosave) SaveGamePos
	}

	def IncBookMovePriority(san:String,dosave:Boolean=true)
	{
		g.book.currentPos.incPriority(san)
		if(dosave) SaveGamePos
	}

	def DecBookMovePriority(san:String,dosave:Boolean=true)
	{
		g.book.currentPos.decPriority(san)
		if(dosave) SaveGamePos
	}

	def SaveGamePos
	{
		g.book.savePos(booklistfen=g.report_fen)
	}

	def SetMultipv(mpv:Int)
	{
		set_multipv(mpv)
	}

	def GetRecommendedEntry=
	{
		g.GetRecommendedEntry
	}

	def SetFlip(f:Boolean)
	{
		Settings.set_flip(f)
	}

	def BuildBookPgn
	{
		Commands.g.build_book_pgn
	}

	def ColorMove(san:String,eval:Int,dosave:Boolean=true)
	{
		val uci=g.b.sanToMove(san).toAlgeb

		AnnotateMove(san,"-",uci,dosave=dosave)
		if(butils.IsMated(eval)) Commands.AnnotateMove(san,"??",uci,dosave=false)
		else if(butils.IsMate(eval)) Commands.AnnotateMove(san,"!!",uci,dosave=false)
		else if(butils.IsBad(eval)) Commands.AnnotateMove(san,"?",uci,dosave=false)
		else if(butils.IsGood(eval)) Commands.AnnotateMove(san,"!",uci,dosave=false)
		else if(butils.IsInteresting(eval)) Commands.AnnotateMove(san,"?!",uci,dosave=false)
		else if(butils.IsPromising(eval)) Commands.AnnotateMove(san,"!?",uci,dosave=false)		
	}	

}

