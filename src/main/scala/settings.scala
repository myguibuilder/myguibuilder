package guibuilder

// object Settings holds the most important constants and settings of the application

object Settings
{

	val SUPPORTED_VARIANTS=List(
			"Standard",
			"Atomic",
			"King of the Hill",
			"Chess960",
			"Antichess",
			"Horde",
			"From Position",
			"Three-check",
			"Racing Kings"
		)

	def IS_STANDARD=(variant=="Standard")
	def IS_ATOMIC=(variant=="Atomic")
	def IS_KING_OF_THE_HILL=(variant=="King of the Hill")
	def IS_CHESS960=(variant=="Chess960")
	def IS_ANTICHESS=(variant=="Antichess")
	def IS_HORDE=(variant=="Horde")
	def IS_FROM_POSITION=(variant=="From Position")
	def IS_THREE_CHECK=(variant=="Three-check")
	def IS_RACING_KINGS=(variant=="Racing Kings")

	val ANNOTATIONS=List("!!","!","!?","-","?!","?","??")

	val ANNOT_COLORS=Map(
			"!!"->"#00ff00",
			"!"->"#007f00",
			"!?"->"#0000ff",
			"-"->"#0000af",
			"?!"->"#00007f",
			"?"->"#7f0000",
			"??"->"#ff0000"
		)

	def get_annot_color(annot:String):String=
	{
		if(ANNOT_COLORS.contains(annot)) return ANNOT_COLORS(annot)
		"#7f7f7f"
	}

	// default settings values

	val DEFAULT_VARIANT="Standard"
	val DEFAULT_GAME_PGN=""
	val DEFAULT_GAME_CURRENT_LINE=""
	val DEFAULT_FLIP=false
	val DEFAULT_BOOK_ENABLED=true
	val DEFAULT_INC_MOVE_COUNT=false
	val DEFAULT_ADD_GAMES=true
	val DEFAULT_UPDATE_RESULT=true
	val DEFAULT_BUILD_CUTOFF=10
	val DEFAULT_BUILD_DIR=""
	val DEFAULT_ENGINE_DIR=""
	val DEFAULT_PGN_DIR=""
	val DEFAULT_MULTIPV=1

	// settings variables are private

	private var variant="Standard"
	private var game_pgn=DEFAULT_GAME_PGN
	private var game_current_line=DEFAULT_GAME_CURRENT_LINE
	private var flip=DEFAULT_FLIP
	private var book_enabled=DEFAULT_BOOK_ENABLED
	private var inc_move_count=DEFAULT_INC_MOVE_COUNT
	private var add_games=DEFAULT_ADD_GAMES
	private var update_result=DEFAULT_UPDATE_RESULT
	private var build_cutoff=DEFAULT_BUILD_CUTOFF
	private var build_dir=DEFAULT_BUILD_DIR
	private var engine_dir=DEFAULT_ENGINE_DIR
	private var pgn_dir=DEFAULT_PGN_DIR
	private var multipv=DEFAULT_MULTIPV

	// getters and setters for settings variables

	def get_variant=variant

	def set_variant(v:String):Boolean=
	{
		if(!SUPPORTED_VARIANTS.contains(v)) return false

		variant=v

		true
	}

	def get_game_pgn=game_pgn

	def set_game_pgn(pgn:String)
	{
		game_pgn=pgn
	}

	def get_game_current_line=game_current_line

	def set_game_current_line(pgn_movelist:String)
	{
		game_current_line=pgn_movelist
	}

	def get_flip=flip

	def set_flip(f:Boolean)
	{
		flip=f
	}

	def get_book_enabled=book_enabled

	def set_book_enabled(be:Boolean)
	{
		book_enabled=be
		Builder.Set("{settings}#{bookenabled}",""+book_enabled)
	}

	def get_inc_move_count=inc_move_count

	def set_inc_move_count(incmc:Boolean)
	{
		inc_move_count=incmc
		Builder.Set("{settings}#{incmovecount}",""+inc_move_count)
	}

	def get_add_games=add_games

	def set_add_games(ag:Boolean)
	{
		add_games=ag
		Builder.Set("{settings}#{addgames}",""+add_games)
	}

	def get_update_result=update_result

	def set_update_result(ur:Boolean)
	{
		update_result=ur
		Builder.Set("{settings}#{updateresult}",""+update_result)
	}

	def get_build_cutoff=build_cutoff

	def set_build_cutoff(cutoff:Int)
	{
		build_cutoff=cutoff
		Builder.Set("{settings}#{buildcutoff}#{selected}",""+build_cutoff)
	}

	def get_build_dir=build_dir

	def set_build_dir(bd:String)
	{
		build_dir=bd
	}

	def get_engine_dir=engine_dir

	def set_engine_dir(ed:String)
	{
		engine_dir=ed
	}

	def get_pgn_dir=pgn_dir

	def set_pgn_dir(pd:String)
	{
		pgn_dir=pd
	}

	def get_multipv=multipv

	def set_multipv(mpv:Int)
	{
		multipv=mpv
		Builder.Set("{settings}#{multipv}",""+multipv)
	}

	def get_current_book(v:String=get_variant):String=
	{
		val path=s"{variantentries}#{$v}#{currentbook}"

		val cb=Builder.GS(path,"default")

		Builder.Set(path,cb)

		cb
	}

	def set_current_book(cb:String,v:String=get_variant)
	{
		val path=s"{variantentries}#{$v}#{currentbook}"

		Builder.Set(path,cb)
	}

	// load settings from values

	def load_settings
	{
		set_variant(Builder.GS("{settings}#{variant}",DEFAULT_VARIANT))
		flip=Builder.GB("{settings}#{flip}",DEFAULT_FLIP)
		book_enabled=Builder.GB("{settings}#{bookenabled}",DEFAULT_BOOK_ENABLED)
		inc_move_count=Builder.GB("{settings}#{incmovecount}",DEFAULT_INC_MOVE_COUNT)
		add_games=Builder.GB("{settings}#{addgames}",DEFAULT_ADD_GAMES)
		update_result=Builder.GB("{settings}#{updateresult}",DEFAULT_UPDATE_RESULT)
		build_cutoff=Builder.GI("{settings}#{buildcutoff}#{selected}",DEFAULT_BUILD_CUTOFF)
		build_dir=Builder.GS("{settings}#{builddir}",DEFAULT_BUILD_DIR)
		engine_dir=Builder.GS("{settings}#{enginedir}",DEFAULT_ENGINE_DIR)
		pgn_dir=Builder.GS("{settings}#{pgndir}",DEFAULT_PGN_DIR)
		game_pgn=Encode32.encode(Builder.GS("{settings}#{gamepgn}",""),false)
		game_current_line=Encode32.encode(Builder.GS("{settings}#{gamecurrentline}",""),false)
		multipv=Builder.GI("{settings}#{multipv}",DEFAULT_MULTIPV)
	}

	// save settings to values

	def save_settings
	{
		Builder.Set("{settings}#{variant}",variant)
		Builder.Set("{settings}#{flip}",""+flip)
		Builder.Set("{settings}#{bookenabled}",""+book_enabled)
		Builder.Set("{settings}#{incmovecount}",""+inc_move_count)
		Builder.Set("{settings}#{addgames}",""+add_games)
		Builder.Set("{settings}#{updateresult}",""+update_result)
		Builder.Set("{settings}#{buildcutoff}#{selected}",""+build_cutoff)
		Builder.Set("{settings}#{builddir}",""+build_dir)
		Builder.Set("{settings}#{enginedir}",""+engine_dir)
		Builder.Set("{settings}#{pgndir}",""+pgn_dir)
		Builder.Set("{settings}#{gamepgn}",Encode32.encode(game_pgn,true))
		Builder.Set("{settings}#{gamecurrentline}",Encode32.encode(game_current_line,true))
		Builder.Set("{settings}#{multipv}",""+multipv)
	}

}