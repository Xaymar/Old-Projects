Type Tradelane
	Field P1.TVector, P2.TVector, Dir.TVector
	Field From$, Target$
	Field FromSpot.Spotmark, TargetSpot.Spotmark
	
	Field Gates.TList, Lasers.TList
	Field RingsTop.TList, RingsBot.TList
	Field RingsInnerTop.TList, RingsInnerBot.TList
	Field TubesTop.TList, TubesBot.TList
	
	Field PivotTubes
	Field PivotRings
	
	Field MeshA, MeshB, OBBa, OBBb
	Field XTubeA, XTubeB
	Field Range
End Type

Const Tradelane_Speed#		= 50.0
Const Tradelane_SpeedForce#	= 0.125
Const Tradelane_Force#		= 0.1

Const Tradelane_GateDistance# = 5000.0
Const Tradelane_Offset# = 144
Const Tradelane_Size# = 115

Function CreateTradelane(StartX,StartY,StartZ, EndX,EndY,EndZ, From$, Target$)
	Local T.Tradelane = New Tradelane
	Local TempVec.TVector
	
	; Information
	T\From = From:T\Target = Target
	
	; Start, End and Direction Vector
	T\P1	= TVector_Create(StartX, StartY, StartZ)
	T\P2	= TVector_Create(EndX, EndY, EndZ)
	TempVec	= TVector_Subtract(T\P2, T\P1)
	T\Dir	= TVector_Normalize(TempVec):Delete TempVec
	Local Range# = TVector_Distance(T\P1, T\P2)
	
	; Initialize LinkedLists
	T\Gates			= TList_Create()
	T\Lasers		= TList_Create()
	T\RingsTop		= TList_Create()
	T\RingsBot		= TList_Create()
	T\RingsInnerTop	= TList_Create()
	T\RingsInnerBot	= TList_Create()
	T\TubesTop		= TList_Create()
	T\TubesBot		= TList_Create()
	
	T\PivotRings = CreatePivot(PVTxEFFECT)
	T\PivotTubes = CreatePivot(PVTxEFFECT)
	
	; Create Spots
	T\FromSpot.Spotmark = CreateSpot(T\P1\X, T\P1\Y, T\P1\Z, 21, (From + ">>>" + Target))
	T\FromSpot.Spotmark = CreateSpot(T\P2\X, T\P2\Y, T\P2\Z, 21, (Target + ">>>" + From))
	
	; Create OBB Collisions
	TVector_Angle(T\Dir)
	Local TX#, TY#, TZ#
	TX = (T\P1\X + T\P2\X) / 2.0
	TY = (T\P1\Y + T\P2\Y) / 2.0
	TZ = (T\P1\Z + T\P2\Z) / 2.0
	
	Local TVecUp.TVector = TVector_Rotate(T\Dir, -90, 0, 0)
	T\OBBa=CreateOBB(TX, TY, TZ, TVector_Pitch-90, TVector_Yaw, 0, 115, 115, Range/2)
	T\OBBb=CreateOBB(TX, TY, TZ, TVector_Pitch-90, TVector_Yaw, 0, 115, 115, Range/2)	MoveEntity T\OBBa, 0, Tradelane_Offset, 0
	MoveEntity T\OBBb, 0, -Tradelane_Offset, 0
	Delete TVecUp
	
	; Create Gates, Sprites and Rings
	Local Count = Ceil(Range / Tradelane_GateDistance)
	Local Stp# = Range / Count
	For n = 0 To Count
		; Calculate Position
		TempVec = TVector_MultiplyScalar(T\Dir, n*Stp)
		Local Pos.TVector = TVector_Add(T\P1, TempVec)
		
		; Create Mesh
		Local Mesh = CopyEntity(TLxMSH, PVTxNORMAL)
		PositionEntity Mesh, Pos\X, Pos\Y, Pos\Z
		AlignToVector Mesh, T\Dir\X, T\Dir\Y, T\Dir\Z, 0, 1
		RotateEntity Mesh, EntityPitch(Mesh, 1), EntityYaw(Mesh, 1), 0, 1
		EntityAutoFade Mesh,14500,15000
		TList_AddLast(T\Gates, Mesh)
		
		; Create Laser
		Local Laser = CopyEntity(TLxMSX, Mesh)
		EntityAutoFade Laser,9500,10000
		TList_AddLast(T\Lasers, Laser)
		
		; Create Rings
		Local RingTop = CopyEntity(TLxENT, Mesh)
		Local RingBot = CopyEntity(TLxENT, Mesh)
		MoveEntity RingTop, 0,  Tradelane_Offset, 0
		MoveEntity RingBot, 0, -Tradelane_Offset, 0
		ScaleSprite RingTop, 125, 125:SpriteViewMode RingTop, 2:EntityColor RingTop,  64, 198, 255:EntityAlpha RingTop, 0.5:EntityAutoFade RingTop, 4500, 5000:EntityBlend RingTop, 3:EntityFX RingTop, 1+16
		ScaleSprite RingBot, 125, 125:SpriteViewMode RingBot, 2:EntityColor RingBot, 255,   0,   0:EntityAlpha RingBot, 0.5:EntityAutoFade RingBot, 4500, 5000:EntityBlend RingBot, 3:EntityFX RingBot, 1+16
		TList_AddLast(T\RingsTop, RingTop)
		TList_AddLast(T\RingsBot, RingBot)
		
		Local RingInTop = CopyEntity(TLxENT2, Mesh)
		Local RingInBot = CopyEntity(TLxENT2, Mesh)
		MoveEntity RingInTop, 0,  Tradelane_Offset, 0
		MoveEntity RingInBot, 0, -Tradelane_Offset, 0
		ScaleSprite RingInTop, 125, 125:SpriteViewMode RingInTop, 2:EntityAlpha RingInTop, 0.5:EntityAutoFade RingInTop, 4500, 5000:EntityBlend RingInTop, 3:EntityFX RingInTop, 1+16
		ScaleSprite RingInBot, 125, 125:SpriteViewMode RingInBot, 2:EntityAlpha RingInBot, 0.5:EntityAutoFade RingInBot, 4500, 5000:EntityBlend RingInBot, 3:EntityFX RingInBot, 1+16
		TList_AddLast(T\RingsInnerTop, RingInTop)
		TList_AddLast(T\RingsInnerBot, RingInBot)
		
		; Create Refraction Tubes
		If n < Count
			Local Tube
			Tube = CreateCylinder(24, 0, RingInTop)
			ScaleEntity Tube, Tradelane_Size, Stp/2, Tradelane_Size
			TurnEntity Tube, -90, 0, 0
			MoveEntity Tube, 0,  -Stp/2, 0
			EntityParent Tube, T\PivotTubes
			EntityAutoFade Tube, 5000, 10000
			EntityFX Tube, 1+8+16
			EntityTexture Tube,StarBTex,0,0
			EntityTexture Tube,ProjectTex,0,1
			EntityAlpha Tube, 0.35
			TList_AddLast(T\TubesTop, Tube)
			
			Tube = CreateCylinder(24, 0, RingInBot)
			ScaleEntity Tube, Tradelane_Size, Stp/2, Tradelane_Size
			TurnEntity Tube, 90, 0, 0
			MoveEntity Tube, 0,  Stp/2, 0
			EntityParent Tube, T\PivotTubes
			EntityAutoFade Tube, 5000, 10000
			EntityFX Tube, 1+8+16
			EntityTexture Tube,StarBTex,0,0
			EntityTexture Tube,ProjectTex,0,1
			EntityAlpha Tube, 0.35
			TList_AddLast(T\TubesBot, Tube)
		EndIf
		
		; Delete remaining temporary data
		Delete Pos:Delete TempVec
	Next
End Function

Function UpdateTradelane()
	;set test on
	TLxTST=0
	
	; Create Camera Vector
	Local vCam.TVector = New TVector
	Local vGate.TVector = New TVector
	Local vTmp.TVector = Null
	Local vTmp2.TVector = Null
	vCam\X = EntityX(cCamera, 1)
	vCam\Y = EntityY(cCamera, 1)
	vCam\Z = EntityZ(cCamera, 1)
	
	For TL.Tradelane = Each Tradelane
		; Rings
		TList_Reset(TL\RingsTop):TList_Reset(TL\RingsInnerTop)
		TList_Reset(TL\RingsBot):TList_Reset(TL\RingsInnerBot)
		TList_Reset(TL\Gates)
		While TList_HasNext(TL\Gates)
			Local RT	= TList_Next(TL\RingsTop)
			Local RTI	= TList_Next(TL\RingsInnerTop)
			Local RB	= TList_Next(TL\RingsBot)
			Local RBI	= TList_Next(TL\RingsInnerBot)
			Local Gate	= TList_Next(TL\Gates)
			
			TurnEntity RT, 0, 0, -.1
			TurnEntity RB, 0, 0,  .1
			
			vGate\X = EntityX(Gate, 1)
			vGate\Y = EntityY(Gate, 1)
			vGate\Z = EntityZ(Gate, 1)
			
			vTmp = TVector_Subtract(vGate, vCam)
			vTmp2 = TVector_Normalize(vTmp):Delete vTmp
			
			Local Dot# = TVector_Dot(TL\Dir, vTmp2):Delete vTmp2
			If Dot >= 0 Then
				EntityColor RT,  64, 198, 255
				EntityColor RB, 255,   0,   0
			Else
				EntityColor RB,  64, 198, 255
				EntityColor RT, 255,   0,   0
			EndIf
		Wend
		
		; Traveling
		If EntityInOBB(TL\OBBa,pvShip)
			AlignToVector pvShip, TL\Dir\X, TL\Dir\Y, TL\Dir\Z, 0, Tradelane_Force
			ShipSpeedZ = (ShipSpeedZ * (1-Tradelane_SpeedForce)) + (Tradelane_Speed * Tradelane_SpeedForce)
			TLxTST=1
		EndIf
		If EntityInOBB(TL\OBBB,pvShip)
			AlignToVector pvShip, -TL\Dir\X, -TL\Dir\Y, -TL\Dir\Z, 0, Tradelane_Force
			ShipSpeedZ = (ShipSpeedZ * (1-Tradelane_SpeedForce)) + (Tradelane_Speed * Tradelane_SpeedForce)
			TLxTST=1
		EndIf
	Next
	Delete vCam:Delete vGate
	
	PositionTexture StarBTex, Sin(MilliSecs() / 10000.0), (MilliSecs() / 10000.0) Mod 1
	
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D