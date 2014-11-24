SuperStrict

?Threaded
Import brl.threads
?
Import brl.linkedlist

Module xaymar.resource
ModuleInfo "License: Public Domain"
ModuleInfo "Original Author: Michael Dirks <support@levelnull.de>"
ModuleInfo "Purpose: Load your Files the better way! (Threading supported)"

Const TRM_SAVED:Int			= -2
Const TRM_SAVING:Int		= -1
Const TRM_NONE:Int			= +0
Const TRM_LOADING:Int		= +1
Const TRM_LOADED:Int		= +2
Const TRM_ERROR:Int			= $FF
Const TRM_ALL:Int			= $FE

Const TRM_LS_ERROR:Int		= 0
Const TRM_LS_SUCCESS:Int	= 1
Const TRM_LS_NORESOURCE:Int	= 2

Type TRLException
	Method ToString:String()
		Return "An Error appeared during Loading/Saving the Object."
	End Method
End Type
Type TResourceManager
	'Members
	Field _ResList:TList						= New TList
	Field _ResourceLoaderFunc(Res:TResource)
	Field _ResourceSaverFunc(Res:TResource)
	
	'Members: Stats
	Field _SavingRes:Int, _SavedRes:Int
	Field _TotalRes:Int, _ErrorRes:Int
	Field _LoadingRes:Int, _LoadedRes:Int
		
	'Threaded Members
	?Threaded Field _ResMutex:TMutex			= CreateMutex()
	?
	
	'Methods
	Method New();SetLoaderFunc(TResourceLoaderFunc);SetSaverFunc(TResourceSaverFunc);End Method
	Method Remove()
		?Threaded _ResMutex.Lock()
		?
		_ResList.Clear()
		_ResList = Null
		?Threaded _ResMutex.Unlock()
		_ResMutex.Close()
		_ResMutex = Null
		?
	End Method
	
	Method SetLoaderFunc(LoaderFunc:Byte Ptr)
		_ResourceLoaderFunc = LoaderFunc
	End Method
	Method SetSaverFunc(SaverFunc:Byte Ptr)
		_ResourceSaverFunc = SaverFunc
	End Method
	
	Method Update()
		?Threaded _ResMutex.Lock()
		?
		_TotalRes = _ResList.Count()
		_LoadingRes = 0;_SavingRes = 0
		_LoadedRes = 0;_SavedRes = 0
		_ErrorRes = 0;
		
		For Local _Res:TResource = EachIn _ResList
			Select _Res._State
				Case TRM_LOADING
					_LoadingRes :+ 1
				Case TRM_LOADED
					_LoadedRes :+ 1
				Case TRM_SAVING
					_SavingRes :+ 1
				Case TRM_SAVED
					_SavedRes :+ 1
				Case TRM_NONE
				Default
					_ErrorRes :+ 1
			End Select
		Next
		?Threaded _ResMutex.Unlock()
		?
	End Method
	Method GetCount:Int(Which:Int)
		Select Which
			Case TRM_NONE
				Return _TotalRes-_LoadedRes-_LoadingRes-_SavedRes-_SavingRes-_ErrorRes
			Case TRM_LOADING
				Return _LoadingRes
			Case TRM_LOADED
				Return _LoadedRes
			Case TRM_SAVING
				Return _SavingRes
			Case TRM_SAVED
				Return _SavedRes
			Case TRM_ERROR
				Return _ErrorRes
			Case TRM_ALL
				Return _TotalRes
			Default
				Return 0
		End Select
	EndMethod
	
	Method AddResource(Res:TResource)
		?Threaded _ResMutex.Lock()
		?
		_ResList.AddLast(Res)
		Res._State = TRM_NONE
		?Threaded _ResMutex.Unlock()
		?
	End Method
	Method RemoveResourceName(Name:String)
		?Threaded _ResMutex.Lock()
		?
		For Local _Res:TResource = EachIn _ResList
			If _Res.Name = Name Then
				_ResList.Remove(_Res)
				_Res = Null
			End If
		Next
		?Threaded _ResMutex.Unlock()
		?
	End Method
	Method RemoveResource(Res:TResource)
		?Threaded _ResMutex.Lock()
		?
		_ResList.Remove(Res)
		?Threaded _ResMutex.Unlock()
		?
	End Method
	Method ClearResource(Which:Int=TRM_ALL)
		?Threaded _ResMutex.Lock()
		?
		_ResList.Clear()
		?Threaded _ResMutex.Unlock()
		?
	End Method
	
	Method GetResourceName:TResource(Name:String, Which:Int=TRM_ALl)
		Local _rRes:TResource
		?Threaded _ResMutex.Lock()
		?
		For Local _Res:TResource = EachIn _ResList
			If _Res.Name = Name And (_Res._State = Which Or Which = TRM_ALL)
				_rRes = _Res
				Exit
			EndIf
		Next
		?Threaded _ResMutex.Unlock()
		?
		Return _rRes
	End Method
	Method GetResourcesName:TResource[](Name:String, Which:Int=TRM_ALL)
		Local _rList:TList = New TList
		?Threaded _ResMutex.Lock()
		?
		For Local _Res:TResource = EachIn _ResList
			If _Res.Name = Name And (_Res._State = Which Or Which = TRM_ALL)
				_rList.AddLast(_Res)
			EndIf
		Next
		?Threaded _ResMutex.Unlock()
		?
		Return TResource[](_rList.ToArray())
	End Method
	Method GetResource:TResource(Which:Int=TRM_ALl)
		Local _rRes:TResource
		?Threaded _ResMutex.Lock()
		?
		For Local _Res:TResource = EachIn _ResList
			If (_Res._State = Which Or Which = TRM_ALL)
				_rRes = _Res
				Exit
			EndIf
		Next
		?Threaded _ResMutex.Unlock()
		?
		Return _rRes
	End Method
	Method GetResources:TResource[](Which:Int=TRM_ALL)
		Local _rList:TList = New TList
		?Threaded _ResMutex.Lock()
		?
		For Local _Res:TResource = EachIn _ResList
			If (_Res._State = Which Or Which = TRM_ALL)
				_rList.AddLast(_Res)
			EndIf
		Next
		?Threaded _ResMutex.Unlock()
		?
		Return TResource[](_rList.ToArray())
	End Method
	
	Method LoadResource:Int(Which:Int=TRM_NONE)
		Local _Res:TResource = GetResource(Which), _rVal:Int
		If _Res = Null
			Return TRM_LS_NORESOURCE
		Else
			?Threaded _ResMutex.Lock()
			?
			_Res._State = TRM_LOADING
			?Threaded _ResMutex.Unlock()
			?
			
			_ResourceLoaderFunc(_Res)
			
			?Threaded _ResMutex.Lock()
			?
			If _Res.Exception = Null And _Res.Resource <> Null
				_Res._State = TRM_LOADED
				_rVal = TRM_LS_SUCCESS
			Else
				If _Res.Exception = Null
					_Res.Exception = New TRLException
				End If
				_Res._State = TRM_ERROR
				_rVal = TRM_LS_ERROR
			End If
			?Threaded _ResMutex.Unlock()
			?
			Return _rVal
		End If
	End Method
	Method SaveResource:Int(Which:Int=TRM_NONE)
		Local _Res:TResource = GetResource(Which), _rVal:Int
		If _Res = Null
			Return TRM_LS_NORESOURCE
		Else
			?Threaded _ResMutex.Lock()
			?
			_Res._State = TRM_SAVING
			?Threaded _ResMutex.Unlock()
			?
			
			_ResourceSaverFunc(_Res)
			
			?Threaded _ResMutex.Lock()
			?
			If _Res.Exception = Null
				_Res._State = TRM_SAVED
				_rVal = TRM_LS_SUCCESS
			Else
				_Res._State = TRM_ERROR
				_rVal = TRM_LS_ERROR
			End If
			?Threaded _ResMutex.Unlock()
			?
			Return _rVal
		End If
	End Method
End Type
Type TResource
	Field _State:Int
	
	Field Name:String, File:String
	Field Resource:Object, Exception:Object
	
	Method New();EndMethod
	Method Set(Name:String, File:String)
		Self.Name		= Name
		Self.File		= File
		Self.Resource	= Null
		Self.Exception	= Null
	End Method
	Method Remove()
		Self.Resource = Null
		Self.Exception = Null
		Self.File = Null
		Self.Name = Null
	End Method
	
	Method _Load();End Method
	Method _Save();End Method
End Type

Function TResourceLoaderFunc(Res:TResource)
	Try
		Res._Load()
	Catch E:Object
		Res.Exception = E
		Res.Resource = Null
	EndTry
End Function
Function TResourceSaverFunc(Res:TResource)
	Try
		Res._Save()
	Catch E:Object
		Res.Exception = E
		Res.Resource = Null
	EndTry
End Function