SuperStrict

' Modules and Includes -----------------------------------------------------------------------------------------------------------------------------------------
Framework BRL.Blitz
Import BRL.StandardIO
Import BRL.Timer
Import BRL.Max2D
Import BRL.GLMax2D
Import BRL.Event
Import BRL.EventQueue
Import PUB.FreeProcess
Import MaxGUI.MaxGUI
Import MaxGUI.Drivers

	' Libraries
Import "TGUI.bmx"
Import "Max2DExtended.bmx"

' Resources -----------------------------------------------------------------------------------------------------------------------------------------------------
	' Binary Resources
Incbin "GFX/LNC/GUIxSTYLE.png"
Incbin "SFX/M/launcher.ogg"

' Initialization ------------------------------------------------------------------------------------------------------------------------------------------------
SetGraphicsDriver(GLMax2DDriver())

	' Create Timers to reduce CPU load.
Global tmrRender:TTimer	 = CreateTimer(20)		' One for rendering the GUI
Global tmrUpdate:TTimer	 = CreateTimer(100)		' And one for working.

	' Create the Window and Canvas for the Patcher, set the Graphics Driver to OpenGL and enable polled input.
Global gdgPatcher:TGadget			 = CreateWindow("Sirius Online", 0, 0, 720, 460, Desktop(), WINDOW_CLIENTCOORDS | WINDOW_CENTER | WINDOW_HIDDEN)
Global gdgCanvas:TGadget			 = CreateCanvas(0, 0, ClientWidth(gdgPatcher), ClientHeight(gdgPatcher), gdgPatcher)
SetGraphics(gdgCanvas)				' Change OpenGL Context to the Canvas.
EnablePolledInput(gdgCanvas)		' Enable input handling for the Canvas.

DebugLog TShader.CheckCompatability()
End

Rem
' Load our GUI Style and split it into the required images.
Global guiStyle:TPixmap			 = LoadPixmap("GFX/LNC/GUIxSTYLE.png")
If guiStyle = Null Then guiStyle = LoadPixmap("incbin::GFX/LNC/GUIxSTYLE.png")

Global guiWindowBackground:TImage	 = LoadImage(guiStyle.Window(0, 0, 32, 32), FILTEREDIMAGE)
Global guiWindowShade:TImage[]		 = New TImage[2]
guiWindowShade[0]					 = LoadImage(guiStyle.Window(32, 0, 16, 16), FILTEREDIMAGE)
guiWindowShade[1]					 = LoadImage(guiStyle.Window(32, 16, 16, 16), FILTEREDIMAGE)
Global guiWindowTitleBar:TImage[]	 = New TImage[2]
guiWindowTitleBar[0]				 = LoadImage(guiStyle.Window(48, 0, 16, 16), FILTEREDIMAGE)
guiWindowTitleBar[1]				 = LoadImage(guiStyle.Window(48, 16, 16, 16), FILTEREDIMAGE)
Global guiSymbols:TImage[]			 = New TImage[8]
guiSymbols[0]						 = LoadImage(guiStyle.Window(64, 0, 16, 16), FILTEREDIMAGE)		'Close
guiSymbols[1]						 = LoadImage(guiStyle.Window(80, 0, 16, 16), FILTEREDIMAGE)		'Minimize
guiSymbols[2]						 = LoadImage(guiStyle.Window(96, 0, 16, 16), FILTEREDIMAGE)		'Maximize
guiSymbols[3]						 = LoadImage(guiStyle.Window(112, 0, 16, 16), FILTEREDIMAGE)	'Restore
guiSymbols[4]						 = LoadImage(guiStyle.Window(64, 16, 16, 16), FILTEREDIMAGE)
guiSymbols[5]						 = LoadImage(guiStyle.Window(80, 16, 16, 16), FILTEREDIMAGE)
guiSymbols[6]						 = LoadImage(guiStyle.Window(96, 16, 16, 16), FILTEREDIMAGE)
guiSymbols[7]						 = LoadImage(guiStyle.Window(112, 16, 16, 16), FILTEREDIMAGE)
Global guiButtonNormal:TImage[]		 = New TImage[3]
guiButtonNormal[0]					 = LoadImage(guiStyle.Window(0, 32, 16, 16), FILTEREDIMAGE)
guiButtonNormal[1]					 = LoadImage(guiStyle.Window(0, 48, 16, 16), FILTEREDIMAGE)
guiButtonNormal[2]					 = LoadImage(guiStyle.Window(0, 64, 16, 16), FILTEREDIMAGE)
Global guiButtonShaped:TImage[]		 = New TImage[3]
guiButtonShaped[0]					 = LoadImage(guiStyle.Window(16, 32, 16, 16), FILTEREDIMAGE)
guiButtonShaped[1]					 = LoadImage(guiStyle.Window(16, 48, 16, 16), FILTEREDIMAGE)
guiButtonShaped[2]					 = LoadImage(guiStyle.Window(16, 64, 16, 16), FILTEREDIMAGE)
Global guiProgressBar:TImage[]		 = New TImage[3]
guiProgressBar[0]					 = LoadImage(guiStyle.Window(16, 80, 16, 16), FILTEREDIMAGE)
guiProgressBar[1]					 = LoadImage(guiStyle.Window(16, 96, 16, 16), FILTEREDIMAGE)
guiProgressBar[2]					 = LoadImage(guiStyle.Window(16, 112, 16, 16), FILTEREDIMAGE)
EndRem

	' Show the Patcher Window
ShowGadget gdgPatcher

	' GUI Values
Const guiTitleHeight:Int			 = 24
Global guiWidth:Int					 = ClientWidth(gdgPatcher)
Global guiHeight:Int				 = ClientHeight(gdgPatcher)
Global guiActive:Int = True


' Main Loop -----------------------------------------------------------------------------------------------------------------------------------------------------
Repeat
	WaitEvent()
	
	Select EventSource()
		Case tmrRender
			SetGraphics CanvasGraphics(gdgCanvas);Cls
			
			Rem
			' Draw GUI
			SetColor(255, 255, 255); SetAlpha(1.0); SetBlend(SOLIDBLEND); SetScale(1.0, 1.0); SetRotation(0.0)
				' Title Bar and Title
			DrawTiledImage guiWindowTitleBar[guiActive], 0, 0, guiWidth, guiTitleHeight, 0, 0, 16, 16,,,,4,4,4,4, True, MODE_SCALEUPX | MODE_SCALEUPY
			DrawText(GadgetText(gdgPatcher), 4, guiTitleHeight / 2 - (12 / 2))
			
				' Content Background
			DrawSubImageRectEx guiWindowBackground, 0, guiTitleHeight, guiWidth, guiHeight - guiTitleHeight, 0, 0, 32, 32,,,, MODE_REPEATX | MODE_REPEATY
			SetBlend SHADEBLEND
			DrawTiledImage guiContent[guiActive], 0, guiTitleHeight, guiWidth, guiHeight - guiTitleHeight, 0, 0, 16, 16,,,, 4, 4, 4, 4, True, MODE_REPEATX | MODE_REPEATY | MODE_SCALEUPX | MODE_SCALEUPY
			EndRem
			
			
			
			' Show the now rendered GUI
			Flip False
		Case gdgPatcher
			Select EventID()
				Case EVENT_WINDOWMODE, EVENT_WINDOWSIZE
					' Update GUI Values {
					guiWidth = ClientWidth(gdgPatcher)
					guiHeight = ClientWidth(gdgPatcher)
					' }
			End Select
	End Select
Forever
End











Rem
' Files
Import "Patcher.bmx"

'=====> Constants
?Win32
Const LAUNCHER_NAME:String = "Launcher.exe"
?Linux
Const LAUNCHER_NAME:String = "Launcher.bin"
?MacOS
Const LAUNCHER_NAME:String = "Launcher.app"
?

'=====> Parse Arguments
Global stProgram:String			 = AppArgs[0]
Global stProgramPath:String		 = ExtractDir(AppArgs[0])
Global stProgramName:String		 = StripDir(AppArgs[0])
Global bPatchLauncher:Byte		 = False
Global iPatchRetries:Int		 = 30

For Local stArgument:String = EachIn AppArgs[1..]
	Local stArgName:String, stArgValue:String
	
	' Skip arguments not starting with a dash
	If stArgument[0..1] <> "-" Then Continue
	
	Local iValuePos:Int = stArgument.Find("=")
	If iValuePos >= 0 Then
		stArgName = stArgument[1..iValuePos]
		stArgValue = stArgument[iValuePos..]
	Else
		stArgName = stArgument[1..]
		stArgValue = "1"
	EndIf
	
	Select stArgName
		Case "patch"
			bPatchLauncher = Byte(stArgValue)
		Case "retry"
			iPatchRetries = Int(stArgValue)
	End Select
Next

'=====> Use parsed Arguments

'--> Patch the Launcher
If bPatchLauncher = True Then
	Local iRetryCount:Int = 0
	Local oRetryTimer:TTimer = TTimer.Create(10)
	
	' Copy the newly downloaded Launcher file
	If stProgramName <> LAUNCHER_NAME Then 
		If FileType(LAUNCHER_NAME) = FILETYPE_FILE Then 
			Repeat
				oRetryTimer.Wait()
				iRetryCount :+ 1
			Until (DeleteFile(LAUNCHER_NAME) = True) or (iRetryCount > iPatchRetries)
		EndIf
		
		If FileType(LAUNCHER_NAME) = 0 Then
			Repeat
				oRetryTimer.Wait()
				iRetryCount :+ 1
			Until (CopyFile(stProgramName, LAUNCHER_NAME) = True) or (iRetryCount > iPatchRetries)
			
			If FileType(LAUNCHER_NAME) = FILETYPE_FILE Then
				Local oProcess:TProcess = CreateProcess(LAUNCHER_NAME)
				TProcess.ProcessList.Remove oProcess
				End
			Else
				Notify("Error: Failed to replace Launcher", True)
				End
			EndIf
		Else
			Notify("Error: Failed to delete old Launcher", True)
			End
		EndIf
	EndIf
EndIf

' Resources: Create Variables that store resources.
Global riMainBackground:Int,	imMainBackground:TImage;LoadResource("GFX/LNC/LAUxPATxB.png",, imMainBackground, riMainBackground)
Global riMainForeground:Int,	imMainForeground:TImage;LoadResource("GFX/LNC/LAUxPATxA.png",, imMainForeground, riMainForeground)
Global riProgressBar:Int, 		imProgressBar:TImage;	LoadResource("GFX/LNC/BARxPROGRESS.png",, imProgressBar, riProgressBar)
Global riButtonClose:Int,		imButtonClose:TImage;	LoadResource("GFX/LNC/BTNxCLOSE.png",, imButtonClose, riButtonClose)
Global riButtonAccount:Int, 	imButtonAccount:TImage;	LoadResource("GFX/LNC/BTNxACCOUNT.png",, imButtonAccount, riButtonAccount)
Global riButtonWebsite:Int, 	imButtonWebsite:TImage;	LoadResource("GFX/LNC/BTNxWEBSITE.png",, imButtonWebsite, riButtonWebsite)
Global riButtonSupport:Int, 	imButtonSupport:TImage;	LoadResource("GFX/LNC/BTNxSUPPORT.png",, imButtonSupport, riButtonSupport)
Global riButtonEULATOS:Int, 	imButtonEULATOS:TImage;	LoadResource("GFX/LNC/BTNxEULATOS.png",, imButtonEULATOS, riButtonEULATOS)
Global riButtonCheck:Int,		imButtonCheck:TImage;	LoadResource("GFX/LNC/BTNxCHECK.png",, imButtonCheck, riButtonCheck)
Global riButtonPatch:Int, 		imButtonPatch:TImage;	LoadResource("GFX/LNC/BTNxPATCH.png",, imButtonPatch, riButtonPatch)
Global riButtonPlay:Int,		imButtonPlay:TImage;	LoadResource("GFX/LNC/BTNxPLAY.png",, imButtonPlay, riButtonPlay)
Global riButtonRestart:Int,		imButtonRestart:TImage;	LoadResource("GFX/LNC/BTNxRESTART.png",, imButtonRestart, riButtonRestart)
Global rfSmallFont:Int,			fnSmallFont:TImageFont;	LoadFontResource("GFX/FNT/TranscencsGames.otf", 10,, fnSmallFont, rfSmallFont)
Global rfMediumFont:Int,		fnMediumFont:TImageFont;LoadFontResource("GFX/FNT/TranscencsGames.otf", 12,, fnMediumFont, rfMediumFont)
Global rfBigFont:Int,			fnBigFont:TImageFont;	LoadFontResource("GFX/FNT/TranscencsGames.otf", 14,, fnBigFont, rfBigFont)

' Buttons
Const BTN_STT_NORMAL:Int = 0, BTN_STT_HOVER:Int = 1, BTN_STT_DOWN:Int = 2, BTN_STT_ACTION:Int = 3
Global stButtonClose:Int	 = BTN_STT_NORMAL
Global stButtonAccount:Int	 = BTN_STT_NORMAL
Global stButtonWebsite:Int	 = BTN_STT_NORMAL
Global stButtonSupport:Int	 = BTN_STT_NORMAL
Global stButtonEULATOS:Int	 = BTN_STT_NORMAL
Global stButtonCheck:Int	 = BTN_STT_NORMAL
Global stButtonPatch:Int	 = BTN_STT_NORMAL
Global stButtonPlay:Int		 = BTN_STT_NORMAL
Global stButtonRestart:Int	 = BTN_STT_NORMAL

' PatchLog
Global oPatchLog:TList = New TList

' Patcher
Global oPatcher:TPatcher = TPatcher.Create()
ShowGadget(gdPatcherWindow)
EndRem



Rem
'=====> Mainloop
Local relMouseX:Int, relMouseY:Int, bDragging:Byte
Repeat
	WaitEvent()
	Select EventSource()
		Case tmTimer ' Core timer that is basically our main loop.
			'=====> User-input
			Local MX:Int = MouseX(), MY:Int = MouseY()
			Local MD1:Int = MouseDown(1), MH1:Int = MouseHit(1)
			
			stButtonClose = GetButtonState(stButtonClose, 684, 0, 24, 18, MX, MY, MD1)
			stButtonWebsite = GetButtonState(stButtonWebsite, 618, 330, 96, 16, MX, MY, MD1)
			stButtonSupport = GetButtonState(stButtonSupport, 618, 348, 96, 16, MX, MY, MD1)
			stButtonAccount = GetButtonState(stButtonAccount, 618, 366, 96, 16, MX, MY, MD1)
			stButtonEULATOS = GetButtonState(stButtonEULATOS, 618, 384, 96, 16, MX, MY, MD1)
			
			' React on the close button by sending a WindowCloseEvent
			If stButtonClose = BTN_STT_ACTION Then PostEvent(TEvent.Create(EVENT_WINDOWCLOSE, gdPatcherWindow, 0, 0, 0, 0, Null))
			If stButtonWebsite = BTN_STT_ACTION Then OpenURL("http://www.sirius.vektor-studios.com/")
			If stButtonSupport = BTN_STT_ACTION Then OpenURL("http://www.sirius.vektor-studios.com/#contact")
			If stButtonAccount = BTN_STT_ACTION Then OpenURL("http://www.sirius.vektor-studios.com/")
			If stButtonEULATOS = BTN_STT_ACTION Then OpenURL("http://www.sirius.vektor-studios.com/")
			
			' State based checking
			Select oPatcher.GetState()
				Case TPatcher.STATE_PATCHINFO
					stButtonCheck = GetButtonState(stButtonCheck, 312, 441, 96, 16, MX, MY, MD1)
					If stButtonCheck = BTN_STT_ACTION Then oPatcher.Advance()
				Case TPatcher.STATE_PREPATCH
					stButtonPatch = GetButtonState(stButtonPatch, 312, 441, 96, 16, MX, MY, MD1)
					If stButtonPatch = BTN_STT_ACTION Then oPatcher.Advance()
				Case TPatcher.STATE_COMPLETE
					stButtonPlay = GetButtonState(stButtonPlay, 312, 441, 96, 16, MX, MY, MD1)
					If stButtonPlay = BTN_STT_ACTION Then oPatcher.Advance()
				Case TPatcher.STATE_LAUNCHER
					stButtonRestart = GetButtonState(stButtonRestart, 312, 441, 96, 16, MX, MY, MD1)
					If stButtonRestart = BTN_STT_ACTION Then oPatcher.Advance()
			EndSelect
			
			' Window Dragging
			If bDragging = False And (MH1 = True And (MY >= 0 And MY < 32) And (MX >= 0 And MX < 720)) Then
				bDragging = True
				relMouseX = MX;relMouseY = MY
			ElseIf bDragging = True And MD1 = True Then
				SetGadgetShape(gdPatcherWindow, GadgetX(gdPatcherWindow) - (relMouseX - MX), GadgetY(gdPatcherWindow) - (relMouseY - MY), 720, 460)
				relMouseX = MX + (relMouseX - MX);relMouseY = MY + (relMouseY - MY) 'Magic o.o - I don't understand why this works this way, but not when using :+ notation.
			Else
				bDragging = False
			EndIf
			
			'=====> Rendering
			SetGraphics CanvasGraphics(gdRenderCanvas)
			Cls
			
			' Draw Back- & Foreground
			SetBlend ALPHABLEND;SetMaskColor 0, 0, 0
			SetColor 255, 255, 255;SetAlpha 1.0
			SetOrigin 0, 0;SetTransform 0, 1, 1
			If imMainBackground <> Null Then DrawImage(imMainBackground, 0, 0)
			If imMainForeground <> Null Then DrawImage(imMainForeground, 0, 0)
			
			' Window Close Button
			DrawButtonState(stButtonClose, 684, 0, 24, 18, imButtonClose, "X")
			
			' Buttons
			DrawButtonState(stButtonWebsite, 618, 330, 96, 16, imButtonWebsite, "Website")
			DrawButtonState(stButtonSupport, 618, 348, 96, 16, imButtonSupport, "Support")
			DrawButtonState(stButtonAccount, 618, 366, 96, 16, imButtonAccount, "Account")
			DrawButtonState(stButtonEULATOS, 618, 384, 96, 16, imButtonEULATOS, "EULA/TOS")
			
			' State based drawing.
			Select oPatcher.GetState()
				Case TPatcher.STATE_PATCHINFO
					DrawButtonState(stButtonCheck, 312, 441, 96, 16, imButtonCheck, "Check")
				Case TPatcher.STATE_PREPATCH
					DrawButtonState(stButtonPatch, 312, 441, 96, 16, imButtonPatch, "Patch")
				Case TPatcher.STATE_COMPLETE
					DrawButtonState(stButtonPlay, 312, 441, 96, 16, imButtonPlay, "Play")
				Case TPatcher.STATE_LAUNCHER
					DrawButtonState(stButtonRestart, 312, 441, 96, 16, imButtonRestart, "Restart")
				Case TPatcher.STATE_PATCHINFO, TPatcher.STATE_CHECKING, TPatcher.STATE_PATCHING
					DrawProgressBar(8, 442, 704, 12, oPatcher.GetProgress(), imProgressBar)
			EndSelect
			
			' Draw Patcher Tasklog
			SetBlend ALPHABLEND;SetMaskColor 0, 0, 0
			SetColor 255, 255, 255;SetAlpha 0.66;SetImageFont fnSmallFont
			SetOrigin 0, 0;SetTransform 0, 1, 1
			SetViewport 11, 332, 600, 100
			
			Local iLogNum:Int = 1, oLogEntry:TLink = oPatcher.m_oTaskList.FirstLink()
			While (iLogNum < 9) And (oLogEntry <> Null)
				Local stLine:String = String(oLogEntry.Value())
				If stLine[0..1] = "n" Then SetColor 204, 244, 255
				If stLine[0..1] = "e" Then SetColor 255, 222, 204
				If stLine[0..1] = "g" Then SetColor 204, 255, 204
				If stLine[0..1] = "h" Then SetColor 222, 222, 255
				DrawText stLine[1..], 11, 430 - iLogNum * 12
				
				oLogEntry = oLogEntry.NextLink()
				iLogNum :+ 1
			Wend
			SetViewport 0, 0, 720, 460; SetColor 255,255,255; SetImageFont Null
			
			Flip False
		Case tmPatch
			'=====> Patcher
			oPatcher.Update()
			
			If oPatcher.GetShutdown() <> Null Then
				Local oProcess:TProcess = CreateProcess(oPatcher.GetShutdown())
				TProcess.ProcessList.Remove oProcess
				PostEvent(TEvent.Create(EVENT_WINDOWCLOSE, gdPatcherWindow, 0, 0, 684, 0, Null))
			EndIf
		Case tmResource
			LoadResource("GFX/LNC/LAUxPATxB.png",, imMainBackground, riMainBackground)
			LoadResource("GFX/LNC/LAUxPATxA.png",, imMainForeground, riMainForeground)
			LoadResource("GFX/LNC/BARxPROGRESS.png",, imProgressBar, riProgressBar)
			LoadResource("GFX/LNC/BTNxCLOSE.png",, imButtonClose, riButtonClose)
			LoadResource("GFX/LNC/BTNxACCOUNT.png",, imButtonAccount, riButtonAccount)
			LoadResource("GFX/LNC/BTNxWEBSITE.png",, imButtonWebsite, riButtonWebsite)
			LoadResource("GFX/LNC/BTNxSUPPORT.png",, imButtonSupport, riButtonSupport)
			LoadResource("GFX/LNC/BTNxEULATOS.png",, imButtonEULATOS, riButtonEULATOS)
			LoadResource("GFX/LNC/BTNxCHECK.png",, imButtonCheck, riButtonCheck)
			LoadResource("GFX/LNC/BTNxPATCH.png",, imButtonPatch, riButtonPatch)
			LoadResource("GFX/LNC/BTNxPLAY.png",, imButtonPlay, riButtonPlay)
			LoadFontResource("GFX/FNT/TranscencsGames.otf", 10,, fnSmallFont, rfSmallFont)
			LoadFontResource("GFX/FNT/TranscencsGames.otf", 12,, fnMediumFont, rfMediumFont)
			LoadFontResource("GFX/FNT/TranscencsGames.otf", 14,, fnBigFont, rfBigFont)
		Case gdPatcherWindow
			Select EventID()
				Case EVENT_WINDOWCLOSE
					'=====> End the patcher.
					End
			End Select
	EndSelect
Forever

'=====> Functions
Function PointInsideRect:Byte(PX:Int, PY:Int, RX:Int, RY:Int, RW:Int, RH:Int)
	If PX >= RX And PX < RX + RW Then
		If PY >= RY And PY < RY + RH Then
			Return True
		EndIf
	EndIf
	Return False
EndFunction

Function GetButtonState:Byte(BS:Int, BX:Int, BY:Int, BW:Int, BH:Int, MX:Int, MY:Int, MB:Int)
	Local retVal:Int = BS
	If PointInsideRect(MX, MY, BX, BY, BW, BH) = True Then
		If BS = BTN_STT_NORMAL Then
			retVal = BTN_STT_HOVER
		ElseIf BS = BTN_STT_HOVER And MB = True Then
			retVal = BTN_STT_DOWN
		ElseIf BS = BTN_STT_DOWN And MB = False Then
			retVal = BTN_STT_ACTION
		ElseIf BS = BTN_STT_ACTION Then
			retVal = BTN_STT_HOVER
		EndIf
	Else
		retVal = BTN_STT_NORMAL
	EndIf
	Return retVal
EndFunction

Function DrawBorder(X:Int, Y:Int, W:Int, H:Int)
	DrawLine X,		Y,		X+W,	Y
	DrawLine X+W,	Y,		X+W,	Y+H
	DrawLine X+W,	Y+H,	X,		Y+H
	DrawLine X,		Y+H,	X,		Y
EndFunction

Function DrawButtonState(BS:Int, BX:Int, BY:Int, BW:Int, BH:Int, imButton:TImage, Text:String = "")
	SetBlend ALPHABLEND; SetAlpha 1.0; SetColor 255, 255, 255
	SetTransform 0, 1, 1;SetOrigin 0, 0; SetImageFont Null
	Select BS
		Case BTN_STT_NORMAL
			If imButton <> Null Then
				DrawSubImageRect(imButton, BX, BY, BW, BH, 0, 0, BW, BH, 0, 0, 0)
			Else
				SetColor 0, 0, 0;DrawRect BX, BY, BW, BH
				SetColor 255, 255, 255;DrawBorder(BX,BY,BW,BH)
				DrawText Text, BX + BW/2 - TextWidth(Text)/2, BY + BH/2 - TextHeight(Text)/2
			EndIf
		Case BTN_STT_HOVER
			If imButton <> Null Then
				DrawSubImageRect(imButton, BX, BY, BW, BH, 0, BH, BW, BH, 0, 0, 0)
			Else
				SetColor 0, 0, 0;DrawRect BX, BY, BW, BH
				SetColor 204, 225, 255;DrawBorder(BX,BY,BW,BH)
				DrawText Text, BX + BW/2 - TextWidth(Text)/2, BY + BH/2 - TextHeight(Text)/2
			EndIf
		Case BTN_STT_DOWN, BTN_STT_ACTION
			If imButton <> Null Then
				DrawSubImageRect(imButton, BX, BY, BW, BH, 0, BH*2, BW, BH, 0, 0, 0)
			Else
				SetColor 0, 0, 0;DrawRect BX, BY, BW, BH
				SetColor 102, 123, 188;DrawBorder(BX,BY,BW,BH)
				DrawText Text, BX + BW/2 - TextWidth(Text)/2, BY + BH/2 - TextHeight(Text)/2
			EndIf
	EndSelect
EndFunction

Function DrawProgressBar(X:Int, Y:Int, W:Int, H:Int, fProgress:Float, imProgressBar:TImage)
	SetBlend ALPHABLEND; SetAlpha 1.0; SetColor 255, 255, 255
	SetTransform 0, 1, 1;SetOrigin 0, 0
	If imProgressBar = Null Then
		SetColor 255, 255, 255;DrawBorder(X,Y,W,H)
		SetColor 0, 0, 0;DrawRect X+1,Y+1,W-2,H-2
		SetColor 204, 225, 255;SetAlpha 0.75 + Sin(MilliSecs()/5) * 0.25;DrawRect X, Y, W * fProgress, H
	Else
		DrawSubImageRect(imProgressBar, X, Y, W, H, 0, 0, W, H, 0, 0, 0)
		SetAlpha 0.75 + Sin(MilliSecs()/5) * 0.25
		Local iLength:Int = (W-6) * fProgress
		DrawSubImageRect(imProgressBar, X,           Y, 3,       H, 0,   H, 3,       H, 0, 0, 0)
		DrawSubImageRect(imProgressBar, X+3,         Y, iLength, H, 3,   H, iLength, H, 0, 0, 0)
		DrawSubImageRect(imProgressBar, X+3+iLength, Y, 3,       H, W-3, H, 3,       H, 0, 0, 0)
	EndIf
EndFunction

Function LoadResource(URL:String, Flags:Int = - 1, ImagePtr:TImage Var, InfoPtr:Int Var)
	Local iModTime:Int = FileTime(URL)
	If iModTime <> InfoPtr Then
		ImagePtr = LoadImage(URL, Flags)
		InfoPtr = iModTime
	EndIf
EndFunction

Function LoadFontResource(URL:String, Size:Int, Style:Int = SMOOTHFONT, FontPtr:TImageFont Var, InfoPtr:Int Var)
	Local iModTime:Int = FileTime(URL)
	If iModTime <> InfoPtr Then
		FontPtr = LoadImageFont(URL, Size, Style)
		InfoPtr = iModTime
	EndIf
EndFunction
EndRem