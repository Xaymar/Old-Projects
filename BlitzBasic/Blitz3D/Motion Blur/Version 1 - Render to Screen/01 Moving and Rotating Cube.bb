AppTitle "TrueMotion"
Include "TrueMotion.bb"

Graphics3D 1024,768,0,2
SetBuffer BackBuffer()
SeedRnd MilliSecs()

Local eCamera = CreateCamera()

Local tCube = CreateTexture(128,128)
SetBuffer TextureBuffer(tCube)
Color 255, 255, 0
Rect 0, 0, 64, 64
Rect 64, 64, 64, 64
Color 0, 127, 255
Rect 64, 0, 64, 64
Rect 0, 64, 64, 64

SetBuffer BackBuffer()

Local eCube = CreateCube()
PositionEntity eCube, 0, 0, 3
EntityTexture eCube, tCube

Local tInstance.TrueMotion = TrueMotion_Create(eCamera)

Timer = CreateTimer(60)

Global Msec
While Not KeyHit(1)
	Cls
	
	Msec = Msec + 10
	RotateEntity eCube, Cos(Msec/4.0)*30, 0, EntityRoll(eCube) + 16
	PositionEntity eCube, 0, Sin(Msec/4.0)*2, 4
	
	TrueMotion_RenderWorld(tInstance, 32)
	Flip
	WaitTimer(Timer)
Wend

End