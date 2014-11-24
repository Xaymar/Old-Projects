;--------------------------------------------
; Example
;--------------------------------------------
Include "../Advanced Text (Library)/AdvText.bb"
Include "TiledSprite.bb"

Graphics3D 1280,720,32,2
SetBuffer BackBuffer()

; Background
Local FPlane = CreatePlane(16)
RotateEntity FPlane, 270, 0, 0
PositionEntity FPlane, 0, 0, 256, True
EntityColor FPlane, 128, 128, 128

Local BPlane = CreatePlane(16)
RotateEntity BPlane, 90, 0, 0
PositionEntity BPlane, 0, 0, -256, True
EntityColor BPlane, 0, 128, 190

Local LPlane = CreatePlane(16)
RotateEntity LPlane, 90, 90, 0
PositionEntity LPlane, 256, 0, 0, True
EntityColor LPlane, 0, 128, 190

Local RPlane = CreatePlane(16)
RotateEntity RPlane, 90, 270, 0
PositionEntity RPlane, -256, 0, 0, True
EntityColor RPlane, 0, 128, 190

Local DPlane = CreatePlane(16)
RotateEntity DPlane, 0, 0, 0
PositionEntity DPlane, 0, -256, 0, True
EntityColor DPlane, 0, 128, 0

Local UPlane = CreatePlane(16)
RotateEntity UPlane, 180, 0, 0
PositionEntity UPlane, 0, 256, 0, True
EntityColor UPlane, 0, 128, 190

; Camera
Local eCameraPitch# = 0
Local eCameraYaw# = 0
Local eCameraZoom# = 1
Local eCameraFOV# = 90.0
Local eCameraFOVValue# = Tan( eCameraFOV / 2.0 )

Local eCameraCenter = CreatePivot()
Local eCamera = CreateCamera(eCameraCenter)
Local eCameraLight = CreateLight(3, eCamera)

PositionEntity eCamera, 0, 0, -eCameraZoom, False
CameraRange eCamera, 0.1, 1024
CameraZoom eCamera, 1.0 / eCameraFOVValue
LightConeAngles eCameraLight, 10, 60
LightColor eCameraLight, 64, 128, 255

; Sprite Texture Creation
Global InvTex = LoadTexture("TiledSprite_Test01.fw.png", 1+2+8+16+32+256)
Global InvBrush = CreateBrush(255.0, 255.0, 255.0)
BrushTexture InvBrush, InvTex, 0, 0
BrushFX InvBrush, 1+4+8

; Main Mesh
Global InvMesh = CreateMesh(eCamera)
PositionEntity InvMesh, 0, 0, 1
EntityFX InvMesh, 1+4+8
; Scale the Mesh according to ScreenWidth and FOV(Tan(FOV/2.0)).
Local InvMeshScale# = (1 / (GraphicsWidth()/2.0)) * eCameraFOVValue
ScaleEntity InvMesh, InvMeshScale, InvMeshScale, 1, True

;Hint: Order of Creation is Important for Sprites not sharing a mesh or a surface. If only sharing the surface, order of fill is important.

; Foreground Sprite
Local InvFGSpriteBuilder.TSpriteBuilder = TSpriteBuilder_Create()
TSpriteBuilder_Mesh(InvFGSpriteBuilder, InvMesh)
TSpriteBuilder_Brush(InvFGSpriteBuilder, InvBrush)
TSpriteBuilder_BrushSize(InvFGSpriteBuilder, 256, 256)
TSpriteBuilder_Scale(InvFGSpriteBuilder, 0.5, 0.5)
TSpriteBuilder_Padding(InvFGSpriteBuilder, 0, 0, 128, 128)
TSpriteBuilder_Border(InvFGSpriteBuilder, 32, 32, 32, 32)
TSpriteBuilder_BorderScale(InvFGSpriteBuilder, 0.5, 0.5)
Local InvFGSprite.TSprite = TSprite_Create(InvFGSpriteBuilder)

; Inverted Gradient Sprite
Local InvGradientSpriteBuilder.TSpriteBuilder = TSpriteBuilder_Create()
TSpriteBuilder_Mesh(InvGradientSpriteBuilder, InvMesh)
TSpriteBuilder_Brush(InvGradientSpriteBuilder, InvBrush)
TSpriteBuilder_BrushSize(InvGradientSpriteBuilder, 256, 256)
TSpriteBuilder_Scale(InvGradientSpriteBuilder, 0.5, 0.5)
TSpriteBuilder_Padding(InvGradientSpriteBuilder, 128, 128, 0, 0)
TSpriteBuilder_Border(InvGradientSpriteBuilder, 32, 32, 32, 32)
TSpriteBuilder_BorderScale(InvGradientSpriteBuilder, 0.5, 0.5)
Local InvGradientSprite.TSprite = TSprite_Create(InvGradientSpriteBuilder)

; Gradient Sprite
Local GradientSpriteBuilder.TSpriteBuilder = TSpriteBuilder_Create()
TSpriteBuilder_Mesh(GradientSpriteBuilder, InvMesh)
TSpriteBuilder_Brush(GradientSpriteBuilder, InvBrush)
TSpriteBuilder_BrushSize(GradientSpriteBuilder, 256, 256)
TSpriteBuilder_Scale(GradientSpriteBuilder, 0.5, 0.5)
TSpriteBuilder_Padding(GradientSpriteBuilder, 0, 128, 128, 0)
TSpriteBuilder_Border(GradientSpriteBuilder, 32, 32, 32, 32)
TSpriteBuilder_BorderScale(GradientSpriteBuilder, 0.5, 0.5)
Local GradientSprite.TSprite = TSprite_Create(GradientSpriteBuilder)

; Background Sprite
Local InvBGSpriteBuilder.TSpriteBuilder = TSpriteBuilder_Create()
TSpriteBuilder_Mesh(InvBGSpriteBuilder, InvMesh, 0, True)
TSpriteBuilder_Brush(InvBGSpriteBuilder, InvBrush)
TSpriteBuilder_BrushSize(InvBGSpriteBuilder, 256, 256)
TSpriteBuilder_Scale(InvBGSpriteBuilder, 0.5, 0.5)
TSpriteBuilder_Padding(InvBGSpriteBuilder, 128, 0, 0, 128)
TSpriteBuilder_Border(InvBGSpriteBuilder, 16, 16, 16, 16)
TSpriteBuilder_BorderScale(InvBGSpriteBuilder, 0.5, 0.5)
Local InvBGSprite.TSprite = TSprite_Create(InvBGSpriteBuilder)

; Other
Global FPSTimer = CreateTimer(30)
Local SpriteFillFlags = TSPRITE_BORDER_LFT + TSPRITE_BORDER_RGT + TSPRITE_BORDER_TOP + TSPRITE_BORDER_BTM
Local X#=256,Y#=256,W#=128,H#=128, Change = True

; Loop
While Not KeyHit(1)
	Local MsX = MouseX(), MsY = MouseY(), MsZ = MouseZ()
	Cls
	
	If Change = True Then
		TSprite_Fill(GradientSprite, -GraphicsWidth()/2 + X, -GraphicsHeight()/2 +Y, W, H, 0, SpriteFillFlags)
		TSprite_Fill(InvGradientSprite, -GraphicsWidth()/2 + X + 4, -GraphicsHeight()/2 +Y + 4, W - 8, H - 8, 0, SpriteFillFlags)
		Change = False
	EndIf
	
	WireFrame KeyDown(31)
	RenderWorld
	
	; Camera Movement
	If MouseDown(1)
		If MouseHit(1) Then
			MoveMouse GraphicsWidth()/2, GraphicsHeight()/2
		Else
			eCameraPitch = eCameraPitch + (MouseYSpeed()/4.0)
			eCameraYaw = eCameraYaw - (MouseXSpeed()/4.0)
		EndIf
		
		RotateEntity eCameraCenter, eCameraPitch, eCameraYaw, 0, True
	EndIf
	If MouseDown(2)
		If MouseHit(2) Then
			MoveMouse GraphicsWidth()/2, GraphicsHeight()/2
		Else
			eCameraZoom = eCameraZoom + (MouseYSpeed()/4.0) + (MouseXSpeed()/4.0)
			If eCameraZoom < 1.0 Then eCameraZoom = 1.0
		EndIf
		
		PositionEntity eCamera, 0, 0, -eCameraZoom, False
	EndIf
	
	; Position & Size
	If KeyDown(30) Then
		X = MsX
		Y = MsY
		Change = True
	EndIf
	
	If KeyDown(32) Then
		If MsX < X+8 Then X = MsX-8
		If MsY < Y+8 Then Y = MsY-8
		
		W = MsX - X
		H = MsY - Y
		If W < 16 Then W = 16
		If H < 16 Then H = 16
		
		Change = True
	EndIf
	
	; Update Status
	If KeyHit(2) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_BORDER_LFT:Change = True
	If KeyHit(3) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_BORDER_RGT:Change = True
	If KeyHit(4) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_BORDER_TOP:Change = True
	If KeyHit(5) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_BORDER_BTM:Change = True
	If KeyHit(16) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_BORDEROUT_LFT:Change = True
	If KeyHit(17) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_BORDEROUT_RGT:Change = True
	If KeyHit(18) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_BORDEROUT_TOP:Change = True
	If KeyHit(19) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_BORDEROUT_BTM:Change = True
	If KeyHit(6) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_ROUNDDOWN_HORZ:Change = True
	If KeyHit(7) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_ROUNDDOWN_VERT:Change = True
	If KeyHit(20) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_PARTIAL_HORZ:Change = True
	If KeyHit(21) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_PARTIAL_VERT:Change = True
	If KeyHit(34) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_PARTIALCUT_HORZ:Change = True
	If KeyHit(35) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_PARTIALCUT_VERT:Change = True
	If KeyHit(47) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_FORCESINGLE_HORZ:Change = True
	If KeyHit(48) Then SpriteFillFlags = SpriteFillFlags Xor TSPRITE_FORCESINGLE_VERT:Change = True
	
	; Show Boundaries
	If KeyDown(57)
		Color 255,128,0
		Rect X, Y, W, H, 0
		Color 0,128,255
		Rect X+8,Y+8,W-16,H-16,0
		Color 255,255,255
	EndIf
	
	; Draw Some shit
	Viewport X+8, Y+8, W-16, H-16
	Local GUIText$ = "This simple technique allows you to fast transparent backgrounds." + Chr(10)
	GUIText = GUIText + "I saw this being used in Unity3D to reduce drawcalls made and " + Chr(10)
	GUIText = GUIText + "thought it would be a good idea to enhance it in a prototype " + Chr(10)
	GUIText = GUIText + "before i reimplement it using new features." + Chr(10) + Chr(10)
	GUIText = GUIText + "Maybe this is already used in Draw3D or will be integrated now." + Chr(10)
	GUIText = GUIText + "Doesn't matter to me, have fun with this piece of code!" + Chr(10)
	AdvText(X+8, Y+8, GUIText)
	Viewport 0, 0, GraphicsWidth(), GraphicsHeight()
	
	; Draw Status
	If KeyDown(59) Then
		Local HelpText$ = 		"Key Function       Status" + Chr(10)
		HelpText = HelpText +	"-------------------------------" + Chr(10)
		HelpText = HelpText +   "LMB Move Camera: " + RSet(Int(eCameraPitch*10)/10.0, 8) + ", " + RSet(Int(eCameraYaw*10)/10.0, 8) + Chr(10)
		HelpText = HelpText +   "RMB Zoom Camera: " + RSet(Int(eCameraZoom*10)/10.0, 8) + Chr(10)
		HelpText = HelpText +	"Spc Show Boundaries" + Chr(10)
		HelpText = HelpText +	"A   Set Position" + Chr(10)
		HelpText = HelpText +	"D   Set Size" + Chr(10)
		HelpText = HelpText +	"S   Show Wireframe" + Chr(10)
		HelpText = HelpText +	"1   Border-Lft: "
		If SpriteFillFlags And TSPRITE_BORDER_LFT Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"2   Border-Rgt: "
		If SpriteFillFlags And TSPRITE_BORDER_RGT Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"3   Border-Top: "
		If SpriteFillFlags And TSPRITE_BORDER_TOP Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"4   Border-Btm: "
		If SpriteFillFlags And TSPRITE_BORDER_BTM Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"Q   BorderOut-Lft: "
		If SpriteFillFlags And TSPRITE_BORDEROUT_LFT Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"W   BorderOut-Rgt: "
		If SpriteFillFlags And TSPRITE_BORDEROUT_RGT Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"E   BorderOut-Top: "
		If SpriteFillFlags And TSPRITE_BORDEROUT_TOP Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"R   BorderOut-Btm: "
		If SpriteFillFlags And TSPRITE_BORDEROUT_BTM Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"5   RoundDown-Horz: "
		If SpriteFillFlags And TSPRITE_ROUNDDOWN_HORZ Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"6   RoundDown-Vert: "
		If SpriteFillFlags And TSPRITE_ROUNDDOWN_VERT Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"T   Partial-Horz: "
		If SpriteFillFlags And TSPRITE_PARTIAL_HORZ Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"Z   Partial-Vert: "
		If SpriteFillFlags And TSPRITE_PARTIAL_VERT Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"G   PartialCut-Horz: "
		If SpriteFillFlags And TSPRITE_PARTIALCUT_HORZ Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"H   PartialCut-Vert: "
		If SpriteFillFlags And TSPRITE_PARTIALCUT_VERT Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"V   ForceSingle-Horz: "
		If SpriteFillFlags And TSPRITE_FORCESINGLE_HORZ Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		HelpText = HelpText +	"B   ForceSingle-Vert: "
		If SpriteFillFlags And TSPRITE_FORCESINGLE_VERT Then HelpText = HelpText + "|f00FF00On|f-1-1-1" + Chr(10) Else HelpText = HelpText + "|fFF0000Off|f-1-1-1" + Chr(10)
		
		AdvText(10, 0, HelpText)
	Else
		Text 0, 0, "F1 to show Help"
	EndIf
	
	WaitTimer FPSTimer:Flip 0
Wend
;~IDEal Editor Parameters:
;~C#Blitz3D