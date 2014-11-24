'Xaymar.DataPak
Import xaymar.datapak
Type TDataPakResource Extends TResource
	Field Password:String
	Field Compressed:Int
	Field Stream:TStream
	
	Method SetDataPak(Name:String, URL:Object, Password:String="", Compressed:Int=False)
		Self.Password = Password
		Self.Compressed = Compressed
		If TStream(URL) <> Null Then
			Self.Stream = TStream(URL)
			Set(Name, "")
		ElseIf String(URL) <> Null Then
			Self.Stream = Null
			Set(Name, String(URL))
		EndIf
	End Method
	
	Method _Load()
		If Stream = Null
			Self.Resource = TDataPak.FromFile(Self.File, Self.Password)
		Else
			Self.Resource = TDataPak.FromStream(Self.Stream)
		End If
	End Method
	Method _Save()
		Local _Flags:Byte
		If Self.Password <> "" Then _Flags :+ TDP_FLAG_PASSWORDED
		If Self.Compressed <> 0 Then _Flags :+ TDP_FLAG_COMPRESSED
		If Stream = Null
			TDataPak(Self.Resource).ToFile(Self.File, _Flags, Self.Password)
		Else
			TDataPak(Self.Resource).ToStream(Self.Stream)
		End If
	End Method
End Type