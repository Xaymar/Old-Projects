Include "Impostor.bb"

Graphics3D 1024, 768, 32, 2
SetBuffer BackBuffer()
Local Timer = CreateTimer(60)

;Camera
Local CamPivot = CreatePivot()
Local Cam = CreateCamera(CamPivot)
MoveEntity Cam, 0, 0, -20

; Impostor
Local MyImp.Impostor = Impostor_Load("Cube.imp")
;EntityFX MyImp\Mesh, 16
MoveEntity MyImp\Mesh, 0, 0, 0
ScaleEntity MyImp\Mesh, 10, 10, 10

; Base Cube
Local Flr = CreateCube()
MoveEntity Flr, 0, -10, 0
ScaleEntity Flr, 10, .001, 10
EntityColor Flr, 51, 51, 51

While Not KeyHit(1)
	Cls
	WireFrame KeyDown(2)
	If MouseDown(1) And MouseHit(1)
		MoveMouse 512, 384
	ElseIf MouseDown(1)
		RotateEntity CamPivot, EntityPitch(CamPivot) + MouseYSpeed() / 15.0, EntityYaw(CamPivot) + MouseXSpeed() / 15.0, 0, 1
		MoveMouse 512, 384
	EndIf
	
	If MouseDown(2) And MouseHit(2)
		MoveMouse 512, 384
	ElseIf MouseDown(2)
		MoveEntity Cam, 0, 0, (MouseXSpeed() - MouseYSpeed()) / 15.0
		MoveMouse 512, 384
	EndIf
	
	Impostor_Update(Cam)
	RenderWorld
	
	Flip 0:WaitTimer Timer
Wend
;~IDEal Editor Parameters:
;~C#Blitz3D