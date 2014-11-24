'!BRL.Audio + AudioSample
Import brl.audio
Import brl.audiosample
Type TSoundResource Extends TResource
	Field Flags:Int
	Method SetSound(Name:String, File:String, Flags:Int=-1)
		Self.Flags = Flags
		Set(Name,File)
	EndMethod
	
	Method _Load()
		Self.Resource = LoadSound(Self.File, Self.Flags)
	EndMethod
End Type
Type TAudioSampleResource Extends TResource
	Method _Load()
		Self.Resource = LoadAudioSample(Self.File)
	End Method
End Type

'!BRL.Bank + BRL.BankStream
Import brl.bank
Type TBankResource Extends TResource
	Method _Load()
		Self.Resource = LoadBank(Self.File)
	End Method
	Method _Save()
		SaveBank(TBank(Self.Resource), Self.File)
	End Method
End Type

'!BRL.Font
Import brl.font
Type TFontResource Extends TResource
	Field Size:Int
	Field Style:Int
	
	Method SetFont(Name:String, File:String, Size:Int, Style:Int=SMOOTHFONT)
		Self.Size = Size
		Self.Style = Style
		Set(Name,File)
	EndMethod
	
	Method _Load()
		Self.Resource = LoadFont(Self.File, Self.Size, Self.Style)
	End Method
End Type

'BRL.Max2D
Import brl.max2d
Type TImageResource Extends TResource
	Field Flags:Int			= -1
	Field Animated:Int		= False
	Field CellWidth:Int		= 0
	Field CellHeight:Int	= 0
	Field FirstCell:Int		= 0
	Field CellCount:Int		= 0
	
	Method SetImage(Name:String, File:String, Flags:Int=-1)
		Self.Flags = Flags
		Self.Animated = False
		Set(Name,File)
	End Method
	Method SetAnimImage(Name:String, File:String, CellWidth:Int, CellHeight:Int, FirstCell:Int, CellCount:Int, Flags:Int=-1)
		Self.Flags = Flags
		Self.Animated = True
		Self.CellWidth = CellWidth
		Self.CellHeight = CellHeight
		Self.FirstCell = FirstCell
		Self.CellCount = CellCount
		Set(Name,File)
	EndMethod
	
	Method _Load()
		If Self.Animated = False
			Self.Resource = LoadImage(Self.File, Self.Flags)
		Else
			Self.Resource = LoadAnimImage(Self.File, Self.CellWidth, Self.CellHeight, Self.FirstCell, Self.CellCount, Self.Flags)
		EndIf
	EndMethod
End Type

'BRL.Pixmap
Import brl.pixmap
Import brl.bmploader
Import brl.jpgloader
Import brl.pngloader
Import brl.tgaloader
Type TPixmapResource Extends TResource
	Field IsPNG:Int
	Field Parameter:Int
	
	Method SetPixmapJPG(Name:String, File:String, Quality:Int=75)
		Self.IsPNG = False
		Self.Parameter = Quality
		Set(Name, File)
	End Method
	Method SetPixmapPNG(Name:String, File:String, Compression:Int=5)
		Self.IsPNG = True
		Self.Parameter = Compression
		Set(Name, File)
	End Method
	
	Method _Load()
		Self.Resource = LoadPixmap(Self.File)
	End Method
	Method _Save()
		If IsPNG = True
			SavePixmapPNG(TPixmap(Self.Resource), Self.File, Self.Parameter)
		Else
			SavePixmapJPeg(TPixmap(Self.Resource), Self.File, Self.Parameter)
		EndIf
	End Method
End Type

'BRL.Stream
Import brl.stream
Type TByteArrayResource Extends TResource
	Method _Load()
		Self.Resource = LoadByteArray(Self.File)
	End Method
	Method _Save()
		SaveByteArray(Byte[](Self.Resource), Self.File)
	End Method
End Type
Type TObjectResource Extends TResource
	Method _Load()
		Self.Resource = LoadObject(Self.File)
	End Method
	Method _Save()
		SaveObject(Self.Resource, Self.File)
	End Method
End Type
Type TStringResource Extends TResource
	Method _Load()
		Self.Resource = LoadString(Self.File)
	End Method
	Method _Save()
		SaveString(String(Self.Resource), Self.File)
	End Method
End Type
Type TStreamResource Extends TResource
	Field Readable:Int		= False
	Field Writeable:Int		= False
	
	Method SetStream(Name:String, File:String, Readable:Int, Writeable:Int)
		Readable = Readable
		Writeable = Writeable
		Set(Name, File)
	End Method
	
	Method _Load()
		Self.Resource = OpenStream(Self.File, Self.Readable, Self.Writeable)
	End Method
	Method _Save()
		FlushStream(TStream(Self.Resource))
	End Method
End Type

'BRL.TextStream
Import brl.textstream
Type TTextResource Extends TResource
	Method _Load()
		Self.Resource = LoadText(Self.File)
	End Method
End Type