Const CfgMeshToRender$		= "D:\Projekte\Blitz3D\Sirius\GFX\ENV\VINxROIDx001.3ds"
Const CfgDiffuseMap$		= "D:\Projekte\Blitz3D\Sirius\GFX\ENV\VINxROIDx001DIFF.png"
Const CfgDiffuseFlags%		= (1+256+512)
Const CfgNormalMap$			= "D:\Projekte\Blitz3D\Sirius\GFX\ENV\VINxROIDx001NORM.png"
Const CfgNormalFlags		= (1+256+512)

Const CfgFrameSizeX			= 128
Const CfgFrameSizeY			= 128
Const CfgPitchMin#			= -90
Const CfgPitchMax#			=  90
Const CfgPitchFrames		= 5
Const CfgYawMin#			= 0
Const CfgYawMax#			= 360
Const CfgYawFrames			= 16
Const CfgDistance#			= 120

; Includes
Include "FreeImage.bb"
Include "FastExt.bb"
Include "../Advanced Text (Library)/AdvText.bb"
Include "Impostor.bb"

; Set up 3D Scene
Graphics3D 1024, 1024, 32, 2:InitExt()
SetBuffer_ BackBuffer()

; Set up Camera
Global CameraPivot			= CreatePivot()
Global Camera				= CreateCamera(CameraPivot)
MoveEntity Camera, 0, 0, -CfgDistance

AmbientLight 255, 255, 255

; Set up Impostor data
Local ImpostorMesh%			= LoadMesh(CfgMeshToRender)
Local ImpostorDiffuse%		= LoadTexture(CfgDiffuseMap, CfgDiffuseFlags)
Local ImpostorNormal%		= LoadTexture(CfgNormalMap, CfgNormalFlags)

; Set up final image
Local DiffuseSheet%			= CreateImage(CfgFrameSizeX, CfgFrameSizeY, CfgYawFrames * CfgPitchFrames)
Local NormalSheet%			= CreateImage(CfgFrameSizeX, CfgFrameSizeY, CfgYawFrames * CfgPitchFrames)

; Set up render target
;Const RTSize% = 2048
Local RenderTarget% = CreateTexture(CfgFrameSizeX, CfgFrameSizeY, 1+256+FE_RENDER+FE_ZRENDER)
CameraViewport Camera, 0, 0, CfgFrameSizeX, CfgFrameSizeY

; Render
SetBuffer TextureBuffer(RenderTarget)
Local YawStep# = (CfgYawMax - CfgYawMin) / CfgYawFrames
Local PitchStep# = (CfgPitchMax - CfgPitchMin) / CfgPitchFrames
Local Yaw, Pitch
For Yaw = 0 To CfgYawFrames - 1
	Local RealYaw# = CfgYawMin + (YawStep * Yaw)
	For Pitch = 0 To CfgPitchFrames - 1
		Local RealPitch# = CfgPitchMin + (PitchStep * Pitch)
		Local Frame = Pitch * CfgYawFrames + Yaw
		
		RotateEntity CameraPivot, RealPitch, RealYaw, 0, 1
		
		; Render Diffuse
		Cls:EntityTexture ImpostorMesh, ImpostorDiffuse:RenderWorld:CopyRectStretch(0, 0, CfgFrameSizeX, CfgFrameSizeY, 0, 0, CfgFrameSizeX, CfgFrameSizeY, TextureBuffer(RenderTarget), ImageBuffer(DiffuseSheet, Frame))
		; Render Normal
		Cls:EntityTexture ImpostorMesh, ImpostorNormal:RenderWorld:CopyRectStretch(0, 0, CfgFrameSizeX, CfgFrameSizeY, 0, 0, CfgFrameSizeX, CfgFrameSizeY, TextureBuffer(RenderTarget), ImageBuffer(NormalSheet, Frame))
	Next
Next
SetBuffer BackBuffer()
FreeTexture RenderTarget

Local Stream = WriteFile("Preview.imp")
WriteShort Stream, CfgFrameSizeX
WriteShort Stream, CfgFrameSizeY
WriteByte Stream, CfgPitchFrames
WriteFloat Stream, CfgPitchMin
WriteFloat Stream, CfgPitchMax
WriteByte Stream, CfgYawFrames
WriteFloat Stream, CfgYawMin
WriteFloat Stream, CfgYawMax
CloseFile(Stream)
Stream = WriteFile("PreviewNormal.imp")
WriteShort Stream, CfgFrameSizeX
WriteShort Stream, CfgFrameSizeY
WriteByte Stream, CfgPitchFrames
WriteFloat Stream, CfgPitchMin
WriteFloat Stream, CfgPitchMax
WriteByte Stream, CfgYawFrames
WriteFloat Stream, CfgYawMin
WriteFloat Stream, CfgYawMax
CloseFile(Stream)


Function SaveAnimImageSheet(Image%, File$, FramesX%, FramesY%)
	Local x, y, img = CreateImage(ImageWidth(Image) * FramesX, ImageHeight(Image) * FramesY)
	For x = 0 To FramesX - 1
		For y = 0 To FramesY - 1
			Local frame = y * FramesX + x
			CopyRect 0, 0, ImageWidth(Image), ImageHeight(Image), x * ImageWidth(Image), y * ImageHeight(Image), ImageBuffer(Image, frame), ImageBuffer(img)
		Next
	Next
	FiSaveImage(img, File)
End Function

SaveAnimImageSheet(DiffuseSheet, "Preview.png", CfgYawFrames, CfgPitchFrames)
SaveAnimImageSheet(NormalSheet, "PreviewNormal.png", CfgYawFrames, CfgPitchFrames)

Global ControlHelp1$
ControlHelp1$ = ControlHelp1$ + "Controls:" + Chr(10)
ControlHelp1$ = ControlHelp1$ + Chr(9) + "1 - Mode: Draw Sheet" + Chr(10)
ControlHelp1$ = ControlHelp1$ + Chr(9) + "2 - Mode: Draw Frame" + Chr(10)
ControlHelp1$ = ControlHelp1$ + Chr(9) + "3 - Mode: 3D Preview" + Chr(10)
Global ControlHelp2$
ControlHelp2$ = ControlHelp2$ + "Sheet Mode Controls:" + Chr(10)
ControlHelp2$ = ControlHelp2$ + Chr(9) + "Q - Sheet: Diffuse" + Chr(10)
ControlHelp2$ = ControlHelp2$ + Chr(9) + "W - Sheet: Normal" + Chr(10)
Global ControlHelp3$
ControlHelp3$ = ControlHelp3$ + "Frame Mode Controls:" + Chr(10)
ControlHelp3$ = ControlHelp3$ + Chr(9) + "Q - Sheet: Diffuse" + Chr(10)
ControlHelp3$ = ControlHelp3$ + Chr(9) + "W - Sheet: Normal" + Chr(10)
ControlHelp3$ = ControlHelp3$ + Chr(9) + "A - Previous Frame" + Chr(10)
ControlHelp3$ = ControlHelp3$ + Chr(9) + "D - Next Frame" + Chr(10)
Global ControlHelp4$
ControlHelp4$ = ControlHelp4$ + "3D Mode Controls:" + Chr(10)
ControlHelp4$ = ControlHelp4$ + Chr(9) + "Q - Sheet: Diffuse" + Chr(10)
ControlHelp4$ = ControlHelp4$ + Chr(9) + "W - Sheet: Normal" + Chr(10)
ControlHelp4$ = ControlHelp4$ + Chr(9) + "Mouse - Rotate Camera" + Chr(10)

Local Timer = CreateTimer(30)

Const DrawModeSheet% = 0
Const DrawModeFrame% = 1
Const DrawMode3D% = 2
Local DrawMode%	= DrawModeSheet

Local ModeSheet_Image%	= DiffuseSheet
Local ModeSheet_XOff%	= 0
Local ModeSheet_YOff%	= 0

Local ModeFrame_Image%	= DiffuseSheet
Local ModeFrame_Frame%	= 0


Local Mode3D_DiffImp.Impostor	= Impostor_Load("Preview.imp")
Local Mode3D_NormImp.Impostor	= Impostor_Load("PreviewNormal.imp")
Local Mode3D_Pitch#				= 0
Local Mode3D_Yaw#				= 0
Local Mode3D_Pivot				= CreatePivot()
Local Mode3D_Camera				= CreateCamera(Mode3D_Pivot)
MoveEntity Mode3D_Camera, 0, 0, -100
EntityTexture ImpostorMesh, ImpostorDiffuse:HideEntity Mode3D_NormImp\Pivot

CameraViewport Camera, 0, 256, 512, 512
CameraViewport Mode3D_Camera, 512, 256, 512, 512
MoveEntity Mode3D_DiffImp\Mesh, 1000, 0, 0
MoveEntity Mode3D_NormImp\Mesh, 1000, 0, 0
ScaleEntity Mode3D_DiffImp\Mesh, CfgDistance, CfgDistance, 1
ScaleEntity Mode3D_NormImp\Mesh, CfgDistance, CfgDistance, 1
MoveEntity Mode3D_Pivot, 1000, 0, 0
CameraRange Camera, 0.1, 500
CameraRange Mode3D_Camera, 0.1, 500

;Local tc = CreateCube(Mode3D_Pivot)
EntityTexture Mode3D_DiffImp\Mesh, Mode3D_DiffImp\Sheet, 0, 0

While Not KeyHit(1)
	Cls
	
	; 3D - Render
	If DrawMode = DrawMode3D
		Impostor_Update(Mode3D_Camera)
		;EntityTexture Mode3D_DiffImp\Mesh, Mode3D_DiffImp\Sheet, 0, 0
		WireFrame KeyDown(57)
		RenderWorld
	EndIf
	
	; 2D - Rener
	AdvText 0, 0, ControlHelp1, 0, 0, 1
	
	; Show Mode specific data.
	Select DrawMode
		Case DrawModeSheet
			; Control
			If MouseDown(1) And MouseHit(1) Then
				MoveMouse MouseX(), MouseY()
			ElseIf MouseDown(1)
				ModeSheet_XOff = ModeSheet_XOff + MouseXSpeed()
				ModeSheet_YOff = ModeSheet_YOff + MouseYSpeed()
			EndIf
			
			If KeyHit(16) Then ModeSheet_Image = DiffuseSheet
			If KeyHit(17) Then ModeSheet_Image = NormalSheet
			
			; Render
			Local rw = ImageWidth(ModeSheet_Image) * CfgYawFrames
			Local rh = ImageHeight(ModeSheet_Image) * CfgPitchFrames
			For p = 0 To CfgPitchFrames - 1
				For y = 0 To CfgYawFrames - 1
					DrawImage ModeSheet_Image, GraphicsWidth() / 2 - rw / 2 + ModeSheet_XOff + ImageWidth(ModeSheet_Image) * y, GraphicsHeight() / 2 - rh / 2 + ModeSheet_YOff + ImageHeight(ModeSheet_Image) * p, y + p * CfgYawFrames
				Next
			Next
			
			; Show Help
			AdvText 0, 50, ControlHelp2, 0, 0, 1
		Case DrawModeFrame
			; Control
			If KeyHit(16) Then ModeFrame_Image = DiffuseSheet
			If KeyHit(17) Then ModeFrame_Image = NormalSheet
			If KeyHit(30) Then ModeFrame_Frame = ModeFrame_Frame - 1
			If KeyHit(32) Then ModeFrame_Frame = ModeFrame_Frame + 1
			
			If ModeFrame_Frame < 0 Then ModeFrame_Frame = CfgPitchFrames * CfgYawFrames - 1
			If ModeFrame_Frame = (CfgPitchFrames * CfgYawFrames) Then ModeFrame_Frame = 0
			
			; Render
			DrawImage ModeFrame_Image, GraphicsWidth() / 2 - ImageWidth(ModeFrame_Image) / 2, GraphicsHeight() / 2 - ImageHeight(ModeFrame_Image) / 2, ModeFrame_Frame
			
			; Show Help
			AdvText 0, 50, ControlHelp3, 0, 0, 1
		Case DrawMode3D
			; Control
			If KeyHit(16) Then
				HideEntity Mode3D_NormImp\Pivot
				ShowEntity Mode3D_DiffImp\Pivot
				EntityTexture ImpostorMesh, ImpostorDiffuse
			EndIf
			If KeyHit(17) Then
				ShowEntity Mode3D_NormImp\Pivot
				HideEntity Mode3D_DiffImp\Pivot
				EntityTexture ImpostorMesh, ImpostorNormal
			EndIf
			
			If MouseDown(1) And MouseHit(1)
				MoveMouse 512, 384
			ElseIf MouseDown(1)
				Mode3D_Pitch = Math_MaxMin(Mode3D_Pitch + MouseYSpeed() / 15.0, 90, -90)
				Mode3D_Yaw = Mode3D_Yaw + MouseXSpeed() / 15.0
				RotateEntity Mode3D_Pivot, Mode3D_Pitch, Mode3D_Yaw, 0, 1
				RotateEntity CameraPivot, Mode3D_Pitch, Mode3D_Yaw, 0, 1
				MoveMouse 512, 384
			EndIf
			
			If MouseDown(2) And MouseHit(2)
				MoveMouse 512, 384
			ElseIf MouseDown(2)
				MoveEntity Mode3D_Camera, 0, 0, (MouseXSpeed() - MouseYSpeed()) / 15.0
				MoveMouse 512, 384
			EndIf
			
			; Show Help
			AdvText 0, 50, ControlHelp4, 0, 0, 1
	End Select
	
	; Switch Mode Hotkeys
	If KeyHit(2) Then DrawMode = DrawModeSheet
	If KeyHit(3) Then DrawMode = DrawModeFrame
	If KeyHit(4) Then DrawMode = DrawMode3D
	
	Flip 0
	WaitTimer Timer
Wend

Function Math_MaxMin#(Value#, Max#, Min#)
	If Value> Max Then Return Max
	If Value < Min Then Return Min
	Return Value
End Function
Function Math_Max#(Value#, Max#)
	If Value> Max Then Return Max
	Return Value
End Function
Function Math_Min#(Value#, Min#)
	If Value < Min Then Return Min
	Return Value
End Function
Function Math_Clip#(Value#, Low#, High#)
	Local Out#, Diff#
	Diff = High-Low:Out = Value-Low
	If (Out >= Diff) Then Out = Out - Floor(Out/Diff)*Diff
	If (Out < 0) Then Out = Out - Floor(Out/Diff)*Diff
	Return Low+Out
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D