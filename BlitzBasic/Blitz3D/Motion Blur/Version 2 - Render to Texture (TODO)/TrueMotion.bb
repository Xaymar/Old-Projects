Const TRUEMOTION_STEPS_MAX	= 32

Type TrueMotion
	Field Camera%	= 0
	
	; Settings
	Field Steps%	= 6
	Field SizeW%	= 128
	Field SizeH%	= 128
	
	; Core Stuff
	Field Texture%	= 0
	Field Mesh%		= 0
End Type

Function TrueMotion_Create.TrueMotion(Camera%, Steps%=12);, SizeW%=128, SizeH%=128, Steps%=5)
	;Create TrueMotion Instance
	tInstance.TrueMotion = New TrueMotion
	
	; Camera can't be Null or invalid
	If Camera = 0 Then
		RuntimeError "TrueMotion: Camera is Null."
	Else
		If EntityClass(Camera) <> "Camera" Then
			RuntimeError "TrueMotion: Camera is not of type <Camera>."
		Else
			tInstance\Camera = Camera
		EndIf
	EndIf
	
	; Limit <Steps> into 1-TRUEMOTION_STEPS_MAX to prevent too high values.
	If Steps < 1 Then
		tInstance\Steps = 1
	ElseIf Steps > TRUEMOTION_STEPS_MAX Then
		tInstance\Steps = TRUEMOTION_STEPS_MAX
	Else
		tInstance\Steps = Steps
	EndIf
	
	; Limit <SizeW/H> to be 2^n and still below GraphicsWidth and -Height.
	If SizeW < 1 Then SizeW = 1
	If SizeW > GraphicsWidth() Then SizeW = GraphicsWidth()
	tInstance\SizeW = 2^Floor(Log(SizeW)/Log(2))
	If SizeH < 1 Then SizeH = 1
	If SizeH > GraphicsWidth() Then SizeH = GraphicsHeight()
	tInstance\SizeH = 2^Floor(Log(SizeH)/Log(2))
	
	; Create Texture
	tInstance\Texture = CreateTexture(tInstance\SizeW, tInstance\SizeH, 305, tInstance\Steps)
	
	; Create Mesh
	tInstance\Mesh = CreateMesh(tInstance\Camera)
	sSurface = CreateSurface(tInstance\Mesh)
	AddVertex(sSurface, -1,  1, 0, 0, 0)
	AddVertex(sSurface,  1,  1, 0, 1, 0)
	AddVertex(sSurface,  1, -1, 0, 1, 1)
	AddVertex(sSurface, -1, -1, 0, 0, 1)
	AddTriangle(sSurface, 0, 1, 2)
	AddTriangle(sSurface, 0, 2, 3)
	
	EntityFX(tInstance\Mesh, 8)
	EntityTexture(tInstance\Mesh, tInstance\Texture, 0)
	EntityOrder(tInstance\Mesh, -1)
	EntityColor(tInstance\Mesh, 0, 0, 0)
	EntityAlpha(tInstance\Mesh, (1.0/(tInstance\Steps)))
	PositionEntity(tInstance\Mesh, 0, 0, 1)
	HideEntity(tInstance\Mesh)
	
	Return tInstance
End Function

; Call this before after you have done your changes. It is a good practice to only let this effect affect nearby entities.
Function TrueMotion_RenderWorld(tInstance.TrueMotion)
	HideEntity(tInstance\Mesh)
	CameraClsMode(tInstance\Camera, 1, 1)
	For curStep = 0 To tInstance\Steps - 1
		RenderWorld curStep/Float(tInstance\Steps)
	Next
	ShowEntity(tInstance\Mesh)
	RenderWorld 1
	CaptureWorld
End Function