Include "LevelOfDetail.bb"

Graphics3D 1024, 768, 0, 2
SetBuffer BackBuffer()
SeedRnd MilliSecs()

Timer = CreateTimer(60)
Local eCamera = CreateCamera()

LoD_Initialize()

Local L0 = CreateSphere(16):HideEntity L0
Local L1 = CreateSphere(8):HideEntity L1
Local L2 = CreateSphere(4):HideEntity L2
Local L3 = CreateSphere(2):HideEntity L3
Local L4 = CreateSprite():HideEntity L4

For X = -10 To 10
	For Y = -10 To 10
		For Z = -10 To 10
			Local tEnt.LoDEntity = LoD_Create(L0, L1, L2, L3, L4)
			PositionEntity tEnt\Pivot, X*3, Y*3, Z*3
			LoD_EntityColor tEnt, Rand(0, 255), Rand(0, 255), Rand(0, 255)
		Next
	Next
Next


While Not KeyHit(1)
	
	MoveEntity eCamera, KeyDown(32) - KeyDown(30), 0, KeyDown(17) - KeyDown(31)
	If MouseDown(1) Then RotateEntity eCamera, EntityPitch(eCamera) + MouseYSpeed()/4.0, EntityYaw(eCamera) -MouseXSpeed()/4.0, 0:MoveMouse 512,384
	
	WireFrame MouseDown(2)
	
	RenderWorld
	LoD_Update(eCamera)
	Text 0, 0, TrisRendered()
	Flip False
;	WaitTimer(Timer)
Wend