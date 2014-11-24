'Xaymar.Resource:    Test 01
'                      Loading of Images and Sounds
SuperStrict

Import xaymar.resource
Import xaymar.brlaudio
Import xaymar.brlmax2d
Import brl.audio
Import brl.max2d

Graphics(800,600,0,60)
SetVirtualResolution(100,100)

Global MyResource:TResourceManager = New TResourceManager

Local imgDir:String[] = LoadDir("data/img/")
For Local fileImg:String = EachIn imgDir
	Local _Res:TImageResource = New TImageResource
	_Res.SetImage(fileImg, "data/img/"+fileImg)
	MyResource.AddResource(_Res)
Next

Local sndDir:String[] = LoadDir("data/snd/")
For Local fileSnd:String = EachIn sndDir
	Local _Res:TSoundResource = New TSoundResource
	_Res.SetSound(fileSnd, "data/snd/"+fileSnd)
	MyResource.AddResource(_Res)
Next

Local _ExitLoad:Int		= False
Repeat
	Cls
	
	Local _BarPerc:Float = Float(MyResource.GetCount(TRM_LOADED)) / Float(MyResource.GetCount(TRM_ALL))
	Local _BarPercLoad:Float = Float(MyResource.GetCount(TRM_LOADING)) / Float(MyResource.GetCount(TRM_ALL))
	Local _BarPercError:Float = Float(MyResource.GetCount(TRM_ERROR)) / Float(MyResource.GetCount(TRM_ALL))
	Local _BarColorMod:Float = 0.5+Sin( Float(MilliSecs()) / Float(1000/360) )*0.5
	
	SetBlend ALPHABLEND
	SetColor 102+153*_BarColorMod, 102+153*_BarColorMod, 102+153*_BarColorMod
	DrawLine( 5, 90, 95, 90)
	DrawLine( 5, 90,  5, 95)
	DrawLine( 5, 95, 95, 95)
	DrawLine(95, 90, 95, 95)
	
	SetAlpha 0.9+0.1*_BarColorMod
	SetColor 51, 255, 51
	DrawRect( 6, 91, 89*_BarPerc, 4)
	
	SetAlpha 0.8+0.2*_BarColorMod
	SetColor 255, 255, 51
	DrawRect( 6+89*_BarPerc, 91, 89*_BarPercLoad, 4)
	
	SetAlpha 0.7+0.3*_BarColorMod
	SetColor 255, 51, 51
	DrawRect( 6+89*_BarPerc+89*_BarPercLoad, 91, 89*_BarPercError, 4)
	
	SetColor 255,255,255
	SetAlpha 1.0*_BarColorMod*_BarPerc
	DrawRect( 5.5, 90.5, 90, 5)
	
	
	SetAlpha 0.8
	SetVirtualResolution(GraphicsWidth(),GraphicsHeight())
	DrawText("Complete %:"+Int(_BarPerc*100),0,0)
	DrawText("Loading %:"+Int(_BarPercLoad*100),0,15)
	DrawText("Errored %:"+Int(_BarPercError*100),0,30)
	
	If MyResource.GetResource(TRM_NONE) <> Null
		DrawText(MyResource.GetResource(TRM_NONE).File,800*0.05,600*0.80)
	Else
		DrawText("Done, Press any key to continue...",800*0.05,600*0.80)
	EndIf
	SetVirtualResolution(100,100)
	
	Flip
	
	Local LRCode:Int = MyResource.LoadResource()
	MyResource.Update()			'Update Count
	
	If AppTerminate()
		End
	End If
	If LRCode = TRM_LS_NORESOURCE
		For Local I:Int = 0 To 255
			If KeyDown(I) = True
				_ExitLoad = True
			End If
		Next
	End If
Until _ExitLoad = True

SetVirtualResolution(GraphicsWidth(),GraphicsHeight())

Print "Resource List:"
For Local _Res:TResource = EachIn MyResource.GetResources(TRM_ALL)
	Select _Res._State
		Case TRM_LOADED, TRM_SAVED
			Print "  SUCCESS: ["+_Res.Name+"]"+_Res.File
		Case TRM_LOADING, TRM_SAVING
			Print "  STUCK: ["+_Res.Name+"]"+_Res.File
		Case TRM_ERROR, TRM_NONE
			Print "  FAILED: ["+_Res.Name+"]"+_Res.File + " //"+_Res.Exception.ToString()
	End Select
Next
End