Type TrueMotion
	Field Camera%	= 0
	Field Mesh%		= 0
End Type

Function TrueMotion_Create.TrueMotion(Camera%)
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
	
	; Create Mesh
	tInstance\Mesh = CreateMesh(tInstance\Camera)
	sSurface = CreateSurface(tInstance\Mesh)
	AddVertex(sSurface, -1,  1, 0, 0, 0)
	AddVertex(sSurface,  1,  1, 0, 1, 0)
	AddVertex(sSurface,  1, -1, 0, 1, 1)
	AddVertex(sSurface, -1, -1, 0, 0, 1)
	AddTriangle(sSurface, 0, 1, 2)
	AddTriangle(sSurface, 0, 2, 3)
	
	EntityOrder(tInstance\Mesh, -1)
	EntityColor(tInstance\Mesh, 0, 0, 0)
	PositionEntity(tInstance\Mesh, 0, 0, 1)
	HideEntity(tInstance\Mesh)
	
	Return tInstance
End Function

; Call this before after you have done your changes. It is a good practice to only let this effect affect nearby entities.
Function TrueMotion_RenderWorld(tInstance.TrueMotion, Steps%=12)
	ShowEntity(tInstance\Mesh)
	EntityAlpha(tInstance\Mesh, 1.0/Steps)
	CameraClsMode(tInstance\Camera, 0, 1)
	For curStep = 0 To Steps - 1
		RenderWorld curStep/Float(Steps)
	Next
	HideEntity(tInstance\Mesh)
	RenderWorld 1
	CameraClsMode(tInstance\Camera, 1, 1)
	CaptureWorld
End Function