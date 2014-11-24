SuperStrict

Import brl.linkedlist
Import brl.bank
Import brl.bankstream
Import brl.filesystem
Import brl.retro
Import brl.endianstream
Import pub.zlib

Module xaymar.datapak
ModuleInfo "License: Public Domain"
ModuleInfo "Original Author: Michael Dirks <support@levelnull.de>"
ModuleInfo "Purpose: Store a lot of Data in an easy way."

Const TDP_HEADERVERSION:String		= "TDP2"
?Debug Const TDP_ERRORLEVEL:Int		= 0			'0 = Don't Throw errors, 1 = Throw errors(Debug only)
?

Const TDP_OPENCONTAINER:Byte		= %00100001
Const TDP_CLOSECONTAINER:Byte		= %10100001
Const TDP_CONTAINER:Byte			= %00000001
Const TDP_BYTE:Byte					= %00000010
Const TDP_SHORT:Byte				= %00000011
Const TDP_INT:Byte					= %00000100
Const TDP_LONG:Byte					= %00000101
Const TDP_FLOAT:Byte				= %00000110
Const TDP_DOUBLE:Byte				= %00000111
Const TDP_STRING:Byte				= %00001000
Const TDP_BANK:Byte					= %00001001
Const TDP_BYTEARRAY:Byte			= %00010010
Const TDP_SHORTARRAY:Byte			= %00010011
Const TDP_INTARRAY:Byte				= %00010100
Const TDP_LONGARRAY:Byte			= %00010101
Const TDP_FLOATARRAY:Byte			= %00010110
Const TDP_DOUBLEARRAY:Byte			= %00010111
Const TDP_STRINGARRAY:Byte			= %00011000
Const TDP_BANKARRAY:Byte			= %00011001

Rem
	Not implemented due to this being V2.
	These can save a lot of space, nearly always.
	Will implement in V3 or when requested/needed.
	
	__COMMENT2__
End Rem

Const TDP_FLAG_PASSWORDED:Byte		= %00000001
Const TDP_FLAG_COMPRESSED:Byte		= %00000010

Type TDataPakType
	Method SetName(Name:String) Abstract
	Method GetName:String() Abstract
	
	Method GetType:Int() Abstract
	Method ToStream(Stream:TStream) Abstract
	Method GetTotalSize:Long() Abstract
	
	Method Destroy() Abstract
End Type

Type TDataPakContainer Extends TDataPakType
	Field sName:String
	Field vData:TList
	
	Method New();vData = New TList;End Method
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	
	Method GetType:Int()
		Return TDP_CONTAINER
	End Method

	Method AddData(Data:TDataPakType);vData.AddLast(Data);EndMethod
	Method RemoveData(Data:TDataPakType);vData.Remove(Data);EndMethod
	Method GetDataByName:TDataPakType[](Name:String)
		Local DataList:TList = New TList
		For Local Data:TDataPakType = EachIn vData
			If Data.GetName() = Name
				DataList.AddLast(Data)
			EndIf
		Next
		Local DataArray:TDataPakType[] = TDataPakType[](DataList.ToArray())
		DataList.Clear()
		DataList = Null
		Return DataArray
	EndMethod
	Method GetData:TDataPakType[]()
		Return TDataPakType[](vData.ToArray())
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_CONTAINER)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteByte(TDP_OPENCONTAINER)
		For Local tData:TDataPakType = EachIn vData
			tData.ToStream(Stream)
		Next
		Stream.WriteByte(TDP_CLOSECONTAINER)
	End Method
	Method GetTotalSize:Long()
		Local Size:Long = sName.Length + 6
		For Local tData:TDataPakType = EachIn vData
			Size :+ tData.GetTotalSize()
		Next
		Return Size
	End Method
	
	Method Destroy()
		sName = Null
		For Local tObj:TDataPakType = EachIn vData
			tObj.Destroy()
		Next
		vData.Clear()
		vData = Null
	End Method
End Type
Type TDataPak Extends TDataPakContainer
	Method AddDataByte:TDataPakByte(Name:String, Data:Byte)
		Local tData:TDataPakByte = New TDataPakByte
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	EndMethod
	Method AddDataShort:TDataPakShort(Name:String, Data:Short)
		Local tData:TDataPakShort = New TDataPakShort
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataInt:TDataPakInt(Name:String, Data:Int)
		Local tData:TDataPakInt = New TDataPakInt
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataLong:TDataPakLong(Name:String, Data:Long)
		Local tData:TDataPakLong = New TDataPakLong
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataFloat:TDataPakFloat(Name:String, Data:Float)
		Local tData:TDataPakFloat = New TDataPakFloat
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataDouble:TDataPakDouble(Name:String, Data:Double)
		Local tData:TDataPakDouble = New TDataPakDouble
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataString:TDataPakString(Name:String, Data:String)
		Local tData:TDataPakString = New TDataPakString
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataBank:TDataPakBank(Name:String, Data:TBank)
		Local tData:TDataPakBank = New TDataPakBank
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataByteArray:TDataPakByteArray(Name:String, Data:Byte[])
		Local tData:TDataPakByteArray = New TDataPakByteArray
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	EndMethod
	Method AddDataShortArray:TDataPakShortArray(Name:String, Data:Short[])
		Local tData:TDataPakShortArray = New TDataPakShortArray
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataIntArray:TDataPakIntArray(Name:String, Data:Int[])
		Local tData:TDataPakIntArray = New TDataPakIntArray
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataLongArray:TDataPakLongArray(Name:String, Data:Long[])
		Local tData:TDataPakLongArray = New TDataPakLongArray
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataFloatArray:TDataPakFloatArray(Name:String, Data:Float[])
		Local tData:TDataPakFloatArray = New TDataPakFloatArray
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataDoubleArray:TDataPakDoubleArray(Name:String, Data:Double[])
		Local tData:TDataPakDoubleArray = New TDataPakDoubleArray
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataStringArray:TDataPakStringArray(Name:String, Data:String[])
		Local tData:TDataPakStringArray = New TDataPakStringArray
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataBankArray:TDataPakBankArray(Name:String, Data:TBank[])
		Local tData:TDataPakBankArray = New TDataPakBankArray
		tData.SetData(Data)
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	Method AddDataContainer:TDataPak(Name:String)
		Local tData:TDataPak = New TDataPak
		tData.SetName(Name)
		vData.AddLast(tData)
		Return tData
	End Method
	
	Method ToFile(path:String, Flags:Byte = 0, pwd:String = "")
		Local Stream:TStream = WriteFile(path)
		If Stream <> Null
			Stream.WriteString(TDP_HEADERVERSION)
			Stream.WriteInt(Flags)
			
			Local BufferSize:Long = Self.GetTotalSize()
			Local BankBuffer:TBank = CreateBank(BufferSize)
			Local BufferStream:TStream = CreateBankStream(BankBuffer)
			
			Self.ToStream(BufferStream)
			BufferStream.Flush()
			
			If (Flags ~ TDP_FLAG_COMPRESSED) < Flags
				Stream.WriteInt(BufferSize)
				Local BankCompress:TBank = CreateBank(BufferSize)
				Local SizeCompressed:Int = BufferSize
				compress(BankCompress.Lock(), SizeCompressed, BankBuffer.Lock(), BufferSize)
				BankCompress.Unlock();BankBuffer.Unlock()
				BankCompress.Resize(SizeCompressed)
				
				'Recreate Buffer
				BufferStream = Null;BankBuffer = Null
				BufferSize = SizeCompressed;BankBuffer = BankCompress;BufferStream = CreateBankStream(BankBuffer)
			End If
			
			If (Flags ~ TDP_FLAG_PASSWORDED) < Flags
				Local Hash:String = __SHA256(pwd)
				For Local I:Long = 0 To BufferSize-1
					BufferStream.Seek(I)
					Local Data:Int = BufferStream.ReadByte()
					
					Data = Data + Asc(Hash[(I Mod Hash.Length)])
					Data = 256 - ((256 - (Data Mod 256)) Mod 256)
					Rem
						Mod in both directions, - and +! - PoC
							1 = 256 - ((256 - (1 Mod 256)) Mod 256)
							1 = 256 - ((256 - 1) Mod 256)
							1 = 256 - (255 Mod 256)
							1 = 256 - 255
							1 = 1
							
							-1 = 256 - ((256 - (-1 Mod 256)) Mod 256)
							-1 = 256 - ((256 - -1) Mod 256)
							-1 = 256 - (257 Mod 256)
							-1 = 256 - 1
							-1 = 255
							
							255 = 256 - ((256 - (255 Mod 256)) Mod 256)
							255 = 256 - ((256 - 255) Mod 256)
							255 = 256 - (1 Mod 256)
							255 = 256 - 1
							255 = 255
							
							-255 = 256 - ((256 - (-255 Mod 256)) Mod 256)
							-255 = 256 - ((256 - -255) Mod 256)
							-255 = 256 - (511 Mod 256)
							-255 = 256 - 255
							-255 = 1
					EndRem
										
					BufferStream.Seek(I)
					BufferStream.WriteByte(Data)
				Next
				BufferStream.Seek(0)
				BufferStream.Flush()
			End If
			BankBuffer.Write(Stream,0,BufferSize)
			
			'Close everything
			Stream.Close()
			BufferStream = Null
			BankBuffer = Null
		Else
			?Debug If TDP_ERRORLEVEL = 1 Then Throw TDataPakException.Create("Unable to create file '"+path+"'.")
			?
		End If
	End Method
	
	Function FromFile:TDataPak(url:String, pwd:String="")
		If FileType(url) = True Or url.StartsWith("incbin://") = True
			Local Stream:TStream = ReadFile(url)
			If Stream <> Null
				'The Data must be buffered for optimal speed. Not doing this will probably result in bad lag when reading/writing.
				Local BankBuffer:TBank = CreateBank(Stream.Size());BankBuffer.Read(Stream, 0, Stream.Size())
				Local BufferStream:TStream = CreateBankStream(BankBuffer)
				
				'Read Header Data
				Local HdrType:String		= BufferStream.ReadString(4)
				If HdrType = TDP_HEADERVERSION
					Local HdrFlag:Int			= BufferStream.ReadInt()
					Local HdrPassworded:Int		= ((HdrFlag ~ TDP_FLAG_PASSWORDED) < HdrFlag)
					Local HdrCompressed:Int		= ((HdrFlag ~ TDP_FLAG_COMPRESSED) < HdrFlag)
					Local SizeOriginal:Int
					
					If HdrCompressed = True Then SizeOriginal = BufferStream.ReadInt()
					
					If HdrPassworded = True		'Decrypt Data first
						Local Hash:String = __SHA256(pwd)
						Local StartPos:Int = BufferStream.Pos()
						For Local I:Long = StartPos To BufferStream.Size()-1
							BufferStream.Seek(I)
							Local Data:Int = BufferStream.ReadByte()
							
							Data = Data - Asc(Hash[((I-StartPos) Mod Hash.Length)])
							Data = 256 - ((256 - (Data Mod 256)) Mod 256)
							
							BufferStream.Seek(I)
							BufferStream.WriteByte(Data)
						Next
						BufferStream.Seek(StartPos)
						BufferStream.Flush()
					End If
					
					If HdrCompressed = True		'Decompress Data next
						Local SizeCompress:Int = BufferStream.Size()-12
						
						Local BankOriginal:TBank = CreateBank(SizeOriginal)
						Local BankCompress:TBank = CreateBank(SizeCompress)
						
						BankCompress.Read(BufferStream, 0, SizeCompress)
						uncompress(BankOriginal.Lock(), SizeOriginal, BankCompress.Lock(), SizeCompress)
						BankOriginal.Unlock();BankCompress.Unlock()
						BankCompress = Null
						
						'Recreate Buffer
						BufferStream = Null;BankBuffer = Null
						BankBuffer = BankOriginal;BufferStream = CreateBankStream(BankBuffer)
					End If
					
					Return TDataPak.FromStream(BufferStream)
				Else
					BufferStream = Null
					BankBuffer = Null
					Stream = Null
					?Debug If TDP_ERRORLEVEL = 1 Then Throw TDataPakException.Create("File Header does not match with current Header(f"+HdrType+"<>c"+TDP_HEADERVERSION+").")
					?
					Return Null
				End If
			End If
		Else
			?Debug If TDP_ERRORLEVEL = 1 Then Throw TDataPakException.Create("URL does not lead to a file.")
			?
			Return Null
		EndIf
	End Function
	Function Create:TDataPak(Name:String)
		Local DataPak:TDataPak = New TDataPak
		DataPak.SetName(Name)
		Return DataPak
	End Function
	
	Function FromStream:TDataPak(Stream:TStream)
		If Stream <> Null
			If Stream.Eof() = False
				Return TDataPak.__FromStreamHandler(BigEndianStream(Stream))
			Else
				?Debug If TDP_ERRORLEVEL = 1 Then Throw TDataPakException.Create("End of Stream reached.")
				?
				Return Null
			End If
		Else
			?Debug If TDP_ERRORLEVEL = 1 Then Throw TDataPakException.Create("Invalid Stream given.")
			?
			Return Null
		End If
	End Function
	Function __FromStreamHandler:TDataPak(Stream:TStream, DataPak:TDataPak=Null)
		While Not Stream.Eof()
			Local vType:Byte = Stream.ReadByte()
			Select vType
				Case TDP_CONTAINER
					Local Data:TDataPak = New TDataPak
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					Data.SetName(Name)
					TDataPak.__FromStreamHandler(Stream, Data)
					If DataPak <> Null Then DataPak.AddData(Data)
					Return Data
				Case TDP_BYTE
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					DataPak.AddDataByte(Name, Stream.ReadByte())
				Case TDP_SHORT
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					DataPak.AddDataShort(Name, Stream.ReadShort())
				Case TDP_INT
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					DataPak.AddDataInt(Name, Stream.ReadInt())
				Case TDP_LONG
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					DataPak.AddDataLong(Name, Stream.ReadLong())
				Case TDP_FLOAT
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					DataPak.AddDataFloat(Name, Stream.ReadFloat())
				Case TDP_LONG
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					DataPak.AddDataDouble(Name, Stream.ReadDouble())
				Case TDP_STRING
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					Local DataLen:Int = Stream.ReadInt()
					DataPak.AddDataString(Name, Stream.ReadString(DataLen))
				Case TDP_BANK
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					Local DataLen:Int = Stream.ReadInt()
					Local Data:TBank = TBank.Create(DataLen)
					Stream.ReadBytes(Data.Buf(), DataLen)
					DataPak.AddDataBank(Name, Data)
				Case TDP_BYTEARRAY
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					Local DataLen:Int = Stream.ReadInt()
					Local Data:Byte[DataLen]
					For Local I:Int = 0 To DataLen
						Data[I] = Stream.ReadByte()
					Next
					DataPak.AddDataByteArray(Name, Data)
				Case TDP_SHORTARRAY
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					Local DataLen:Int = Stream.ReadInt()
					Local Data:Short[DataLen]
					For Local I:Int = 0 To DataLen
						Data[I] = Stream.ReadShort()
					Next
					DataPak.AddDataShortArray(Name, Data)
				Case TDP_INTARRAY
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					Local DataLen:Int = Stream.ReadInt()
					Local Data:Int[DataLen]
					For Local I:Int = 0 To DataLen
						Data[I] = Stream.ReadInt()
					Next
					DataPak.AddDataIntArray(Name, Data)
				Case TDP_LONGARRAY
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					Local DataLen:Int = Stream.ReadInt()
					Local Data:Long[DataLen]
					For Local I:Int = 0 To DataLen
						Data[I] = Stream.ReadLong()
					Next
					DataPak.AddDataLongArray(Name, Data)
				Case TDP_FLOATARRAY
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					Local DataLen:Int = Stream.ReadInt()
					Local Data:Float[DataLen]
					For Local I:Int = 0 To DataLen
						Data[I] = Stream.ReadFloat()
					Next
					DataPak.AddDataFloatArray(Name, Data)
				Case TDP_DOUBLEARRAY
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					Local DataLen:Int = Stream.ReadInt()
					Local Data:Double[DataLen]
					For Local I:Int = 0 To DataLen
						Data[I] = Stream.ReadDouble()
					Next
					DataPak.AddDataDoubleArray(Name, Data)
				Case TDP_BANKARRAY
					Local NameLen:Int = Stream.ReadInt()
					Local Name:String = Stream.ReadString(NameLen)
					Local DataLen:Int = Stream.ReadInt()
					Local Data:TBank[DataLen]
					For Local I:Int = 0 To DataLen
						Local SubDataLen:Int = Stream.ReadInt()
						Data[I] = TBank.Create(SubDataLen)
						Stream.ReadBytes(Data[I].Buf(),SubDataLen)
					Next
					DataPak.AddDataBankArray(Name, Data)
				Case TDP_OPENCONTAINER
					TDataPak.__FromStreamHandler(Stream, DataPak)
				Case TDP_CLOSECONTAINER
					Return Null
				Default
					?Debug If TDP_ERRORLEVEL = 1 Then Throw TDataPakException.Create("Invalid Data in Stream, cannot open DataPak.")
					?
					Return Null
			End Select
		Wend
	End Function
End Type

Type TDataPakByte Extends TDataPakType
	Field sName:String
	Field vData:Byte
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:Byte);vData = Data;EndMethod
	Method GetData:Byte();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_BYTE
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_BYTE)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteByte(vData)
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 1
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakShort Extends TDataPakType
	Field sName:String
	Field vData:Short
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:Short);vData = Data;EndMethod
	Method GetData:Short();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_SHORT
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_SHORT)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteShort(vData)
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 2
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakInt Extends TDataPakType
	Field sName:String
	Field vData:Int
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:Int);vData = Data;EndMethod
	Method GetData:Int();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_INT
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_INT)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteInt(vData)
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 4
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakLong Extends TDataPakType
	Field sName:String
	Field vData:Long
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:Long);vData = Data;EndMethod
	Method GetData:Long();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_LONG
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_LONG)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteLong(vData)
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 8
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakFloat Extends TDataPakType
	Field sName:String
	Field vData:Float
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:Float);vData = Data;EndMethod
	Method GetData:Float();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_FLOAT
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_FLOAT)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteFloat(vData)
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 4
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakDouble Extends TDataPakType
	Field sName:String
	Field vData:Double
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:Double);vData = Data;EndMethod
	Method GetData:Double();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_DOUBLE
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_DOUBLE)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteDouble(vData)
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 8
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakString Extends TDataPakType
	Field sName:String
	Field vData:String
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:String);vData = Data;EndMethod
	Method GetData:String();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_STRING
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_STRING)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteInt(vData.Length)
		Stream.WriteString(vData)
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 4 + vData.Length
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakBank
	Field sName:String
	Field vData:TBank
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:TBank);vData = Data;EndMethod
	Method GetData:TBank();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_BANK
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_STRING)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteInt(vData.Capacity())
		Stream.WriteBytes(vData.Buf(), vData.Capacity() )
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 4 + vData.Capacity()
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type

Type TDataPakByteArray Extends TDataPakType
	Field sName:String
	Field vData:Byte[]
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:Byte[]);vData = Data;EndMethod
	Method GetData:Byte[]();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_BYTEARRAY
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_BYTE)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteInt(vData.Length)
		For Local I:Long = 0 To vData.Length
			Stream.WriteByte(vData[I])
		Next
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 4 + vData.Length
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakShortArray Extends TDataPakType
	Field sName:String
	Field vData:Short[]
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:Short[]);vData = Data;EndMethod
	Method GetData:Short[]();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_SHORTARRAY
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_BYTE)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteInt(vData.Length)
		For Local I:Long = 0 To vData.Length
			Stream.WriteShort(vData[I])
		Next
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 4 + vData.Length*2
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakIntArray Extends TDataPakType
	Field sName:String
	Field vData:Int[]
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:Int[]);vData = Data;EndMethod
	Method GetData:Int[]();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_INTARRAY
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_BYTE)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteInt(vData.Length)
		For Local I:Long = 0 To vData.Length
			Stream.WriteInt(vData[I])
		Next
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 4 + vData.Length*4
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakLongArray Extends TDataPakType
	Field sName:String
	Field vData:Long[]
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:Long[]);vData = Data;EndMethod
	Method GetData:Long[]();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_LONGARRAY
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_BYTE)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteInt(vData.Length)
		For Local I:Long = 0 To vData.Length
			Stream.WriteLong(vData[I])
		Next
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 4 + vData.Length*8
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakFloatArray Extends TDataPakType
	Field sName:String
	Field vData:Float[]
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:Float[]);vData = Data;EndMethod
	Method GetData:Float[]();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_LONGARRAY
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_BYTE)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteInt(vData.Length)
		For Local I:Long = 0 To vData.Length
			Stream.WriteFloat(vData[I])
		Next
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 4 + vData.Length*4
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakDoubleArray Extends TDataPakType
	Field sName:String
	Field vData:Double[]
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:Double[]);vData = Data;EndMethod
	Method GetData:Double[]();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_DOUBLEARRAY
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_BYTE)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteInt(vData.Length)
		For Local I:Long = 0 To vData.Length
			Stream.WriteDouble(vData[I])
		Next
	End Method
	Method GetTotalSize:Long()
		Return 1 + 4 + sName.Length + 4 + vData.Length*8
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakStringArray Extends TDataPakType
	Field sName:String
	Field vData:String[]
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:String[]);vData = Data;EndMethod
	Method GetData:String[]();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_STRINGARRAY
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_BYTE)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteInt(vData.Length)
		For Local I:Long = 0 To vData.Length
			Stream.WriteInt(vData[I].Length)
			Stream.WriteString(vData[I])
		Next
	End Method
	Method GetTotalSize:Long()
		Local Leng:Long = 1 + 4 + sName.Length + 4 + vData.Length*4
		For Local I:Long = 0 To vData.Length
			Leng :+ vData[I].Length
		Next
		Return Leng
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type
Type TDataPakBankArray
	Field sName:String
	Field vData:TBank[]
	
	Method SetName(Name:String);sName = Name;EndMethod
	Method GetName:String();Return sName;EndMethod
	Method SetData(Data:TBank[]);vData = Data;EndMethod
	Method GetData:TBank[]();Return vData;EndMethod
	
	Method GetType:Int()
		Return TDP_BANKARRAY
	End Method
	
	Method ToStream(Stream:TStream)
		Stream.WriteByte(TDP_STRING)
		Stream.WriteInt(sName.Length)
		Stream.WriteString(sName)
		Stream.WriteInt(vData.Length)
		For Local I:Long = 0 To vData.Length
			Stream.WriteInt(vData[I].Capacity())
			Stream.WriteBytes(vData[I].Buf(), vData[I].Capacity())
		Next
	End Method
	Method GetTotalSize:Long()
		Local Leng:Long = 1 + 4 + sName.Length + 4 + vData.Length*4
		For Local I:Long = 0 To vData.Length
			Leng :+ vData[I].Capacity()
		Next
		Return Leng
	End Method
	
	Method Destroy()
		sName = Null
		vData = Null
	End Method
End Type

Type TDataPakException
	Field Error:String
	Method ToString:String()
		Return Error
	End Method
	Function Create:TDataPakException(Error:String)
		Local TCE:TDataPakException = New TDataPakException
		TCE.Error = Error
		Return TCE
	End Function
End Type

Private

Function __SHA256$(in$)
  Local h0:Int = $6A09E667, h1:Int = $BB67AE85, h2:Int = $3C6EF372, h3:Int = $A54FF53A
  Local h4:Int = $510E527F, h5:Int = $9B05688C, h6:Int = $1F83D9AB, h7:Int = $5BE0CD19
  
  Local k:Int[] = [$428A2F98, $71374491, $B5C0FBCF, $E9B5DBA5, $3956C25B, $59F111F1,..
                $923F82A4, $AB1C5ED5, $D807AA98, $12835B01, $243185BE, $550C7DC3,..
                $72BE5D74, $80DEB1FE, $9BDC06A7, $C19BF174, $E49B69C1, $EFBE4786,..
                $0FC19DC6, $240CA1CC, $2DE92C6F, $4A7484AA, $5CB0A9DC, $76F988DA,..
                $983E5152, $A831C66D, $B00327C8, $BF597FC7, $C6E00BF3, $D5A79147,..
                $06CA6351, $14292967, $27B70A85, $2E1B2138, $4D2C6DFC, $53380D13,..
                $650A7354, $766A0ABB, $81C2C92E, $92722C85, $A2BFE8A1, $A81A664B,..
                $C24B8B70, $C76C51A3, $D192E819, $D6990624, $F40E3585, $106AA070,..
                $19A4C116, $1E376C08, $2748774C, $34B0BCB5, $391C0CB3, $4ED8AA4A,..
                $5B9CCA4F, $682E6FF3, $748F82EE, $78A5636F, $84C87814, $8CC70208,..
                $90BEFFFA, $A4506CEB, $BEF9A3F7, $C67178F2]

  Local intCount:Int = (((in$.length + 8) Shr 6) + 1) Shl 4
  Local data:Int[intCount]
  
  For Local c:Int=0 Until in$.length
    data[c Shr 2] = (data[c Shr 2] Shl 8) | (in$[c] & $FF)
  Next
  data[in$.length Shr 2] = ((data[in$.length Shr 2] Shl 8) | $80) Shl ((3 - (in$.length & 3)) Shl 3) 
  data[data.length - 2] = (Long(in$.length) * 8) Shr 32
  data[data.length - 1] = (Long(in$.length) * 8) & $FFFFFFFF
  
  For Local chunkStart:Int=0 Until intCount Step 16
    Local a:Int = h0, b:Int = h1, c:Int = h2, d:Int = h3, e:Int = h4, f:Int = h5, g:Int = h6, h:Int = h7

    Local w:Int[] = data[chunkStart..chunkStart + 16]
    w = w[..64]
    
    For Local i:Int=16 To 63
      w[i] = w[i - 16] + (Ror(w[i - 15], 7) ~ Ror(w[i - 15], 18) ~ (w[i - 15] Shr 3))..
            + w[i - 7] + (Ror(w[i - 2], 17) ~ Ror(w[i - 2], 19) ~ (w[i - 2] Shr 10))
    Next
    
    For Local i:Int=0 To 63
      Local t0:Int = (Ror(a, 2) ~ Ror(a, 13) ~ Ror(a, 22)) + ((a & b) | (b & c) | (c & a))
      Local t1:Int = h + (Ror(e, 6) ~ Ror(e, 11) ~ Ror(e, 25)) + ((e & f) | (~e & g)) + k[i] + w[i]
      
      h = g ; g = f ; f = e ; e = d + t1
      d = c ; c = b ; b = a ;  a = t0 + t1  
    Next
    
    h0 :+ a ; h1 :+ b ; h2 :+ c ; h3 :+ d
    h4 :+ e ; h5 :+ f ; h6 :+ g ; h7 :+ h
  Next
  
  Return (Hex(h0) + Hex(h1) + Hex(h2) + Hex(h3) + Hex(h4) + Hex(h5) + Hex(h6) + Hex(h7)).ToLower()  
End Function
Function Rol:Int(val:Int, shift:Int)
  Return (val Shl shift) | (val Shr (32 - shift))
End Function
Function Ror:Int(val:Int, shift:Int)
  Return (val Shr shift) | (val Shl (32 - shift))
End Function