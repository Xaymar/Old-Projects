SuperStrict

Import xaymar.datapak

Module xaymar.config
ModuleInfo "License: Public Domain"
ModuleInfo "Original Author: Michael Dirks <support@levelnull.de>"
ModuleInfo "Purpose: Easily read and write configuration files by using xaymar.datapak."

Type TConfig
	Field __MainDP:TDataPak
	Field __CurrentDP:TDataPak
	
	Method CreateConfig(name:String="Master")
		__CurrentDP = Null
		If __MainDP <> Null
			__MainDP.Destroy()
			__MainDP = Null
		End If
		__MainDP = TDataPak.Create(name)
	End Method
	Method OpenConfig(url:String,pwd:String="")
		__CurrentDP = Null
		If __MainDP <> Null
			__MainDP.Destroy()
			__MainDP = Null
		End If
		__MainDP = TDataPak.FromFile(url,pwd)
	End Method
	Method SaveConfig(url:String,pwd:String="")
		__MainDP.ToFile(url, TDP_FLAG_COMPRESSED | TDP_FLAG_PASSWORDED, pwd)
	End Method
	Method CloseConfig()
		__CurrentDP = Null
		__MainDP.Destroy()
		__MainDP = Null
	End Method
	Method IsConfigOpen:Int()
		Return (__MainDP <> Null)
	End Method
	
	Method CreateGroup(name:String)
		If __MainDP <> Null
			Local TVarDP:TDataPakType[] = Null
			If __MainDP.GetDataByName(name) <> Null Then
				TVarDP = __MainDP.GetDataByName(name)
			End If
			If TVarDP <> Null And TVarDP[0].GetType() = TDP_CONTAINER
				__CurrentDP = TDataPak(TVarDP[0])
			Else
				__CurrentDP = __MainDP.AddDataContainer(name)
			End If
		Else
			Throw TConfigException.Create("No Config open.")
		End If
	End Method
	Method OpenGroup(name:String)
		If __MainDP <> Null
			Local TVarDP:TDataPakType[] = Null
			If __MainDP.GetDataByName(name) <> Null Then
				TVarDP = __MainDP.GetDataByName(name)
			End If
			If TVarDP <> Null And TVarDP[0].GetType() = TDP_CONTAINER
				__CurrentDP = TDataPak(TVarDP[0])
			Else
				Throw TConfigException.Create("Group '"+name+"' does not exist.")
			End If
		Else
			Throw TConfigException.Create("No Config open.")
		End If
	End Method
	Method CloseGroup()
		__CurrentDP = Null
	End Method
	Method IsGroupOpen:Int()
		Return (__CurrentDP <> Null)
	End Method
	
	Method GetGroupByte:Byte(name:String)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_BYTE
				Return TDataPakByte(TVarDP).GetData()
			Else
				Throw TConfigException.Create("Byte '"+name+"' does not exist.")
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method GetGroupShort:Short(name:String)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_SHORT
				Return TDataPakShort(TVarDP).GetData()
			Else
				Throw TConfigException.Create("Short '"+name+"' does not exist.")
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method GetGroupInt:Int(name:String)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_INT
				Return TDataPakInt(TVarDP).GetData()
			Else
				Throw TConfigException.Create("Int '"+name+"' does not exist.")
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method GetGroupLong:Long(name:String)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_LONG
				Return TDataPakLong(TVarDP).GetData()
			Else
				Throw TConfigException.Create("Long '"+name+"' does not exist.")
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method GetGroupFloat:Float(name:String)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_FLOAT
				Return TDataPakFloat(TVarDP).GetData()
			Else
				Throw TConfigException.Create("Float '"+name+"' does not exist.")
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method GetGroupDouble:Double(name:String)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_DOUBLE
				Return TDataPakDouble(TVarDP).GetData()
			Else
				Throw TConfigException.Create("Double '"+name+"' does not exist.")
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method GetGroupString:String(name:String)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_STRING
				Return TDataPakString(TVarDP).GetData()
			Else
				Throw TConfigException.Create("String '"+name+"' does not exist.")
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	
	Method GetGroupByteEx:Byte(name:String, def:Byte)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_BYTE
				Return TDataPakByte(TVarDP).GetData()
			Else
				Return def
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method GetGroupShortEx:Short(name:String, def:Short)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_SHORT
				Return TDataPakShort(TVarDP).GetData()
			Else
				Return def
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method GetGroupIntEx:Int(name:String, def:Int)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_INT
				Return TDataPakInt(TVarDP).GetData()
			Else
				Return def
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method GetGroupLongEx:Long(name:String, def:Long)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_LONG
				Return TDataPakLong(TVarDP).GetData()
			Else
				Return def
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method GetGroupFloatEx:Float(name:String, def:Float)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_FLOAT
				Return TDataPakFloat(TVarDP).GetData()
			Else
				Return def
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method GetGroupDoubleEx:Double(name:String, def:Double)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_DOUBLE
				Return TDataPakDouble(TVarDP).GetData()
			Else
				Return def
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method GetGroupStringEx:String(name:String, def:String)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_STRING
				Return TDataPakString(TVarDP).GetData()
			Else
				Return def
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	
	Method SetGroupByte(name:String,data:Byte)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_BYTE
				TDataPakByte(TVarDP).SetData(data)
			Else
				__CurrentDP.AddDataByte(name,data)
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method SetGroupShort(name:String,data:Short)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_SHORT
				TDataPakShort(TVarDP).SetData(data)
			Else
				__CurrentDP.AddDataShort(name,data)
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method SetGroupInt(name:String,data:Int)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_INT
				TDataPakInt(TVarDP).SetData(data)
			Else
				__CurrentDP.AddDataInt(name,data)
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method SetGroupLong(name:String,data:Long)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_LONG
				TDataPakLong(TVarDP).SetData(data)
			Else
				__CurrentDP.AddDataLong(name,data)
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method SetGroupFloat(name:String,data:Float)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_FLOAT
				TDataPakFloat(TVarDP).SetData(data)
			Else
				__CurrentDP.AddDataFloat(name,data)
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method SetGroupDouble(name:String,data:Double)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_DOUBLE
				TDataPakDouble(TVarDP).SetData(data)
			Else
				__CurrentDP.AddDataDouble(name,data)
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
	Method SetGroupString(name:String,data:String)
		If __CurrentDP <> Null
			Local TVarDP:TDataPakType = Null, TVarDPA:TDataPakType[] = __CurrentDP.GetDataByName(name)
			If TVarDPA.Length > 0 Then TVarDP = TVarDPA[0]
			If TVarDP <> Null And TVarDP.GetType() = TDP_STRING
				TDataPakString(TVarDP).SetData(data)
			Else
				__CurrentDP.AddDataString(name,data)
			End If
		Else
			Throw TConfigException.Create("No Group selected.")
		End If
	End Method
End Type

Type TConfigException
	Field Error:String
	Method ToString:String()
		Return Error
	End Method
	Function Create:TConfigException(Error:String)
		Local TCE:TConfigException = New TConfigException
		TCE.Error = Error
		Return TCE
	End Function
End Type