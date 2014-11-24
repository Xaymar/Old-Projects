Const ImpostorVariant = 0

Type Impostor
	Field Frames%
	Field FrameWidth%, FrameHeight%
	Field PitchFrames%, PitchMin#, PitchMax#
	Field YawFrames%, YawMin#, YawMax#
	Field Sheet%
	
	Field Pivot%, Parent%
	Field Mesh%, Surface%
	
	Field YawStep#, PitchStep#
End Type

Function Impostor_Init.Impostor(Parent%=0)
	Local Instance.Impostor = New Impostor
	Instance\Pivot = CreatePivot(Parent)
	Instance\Mesh = CreateMesh(Instance\Pivot)
	Instance\Surface = CreateSurface(Instance\Mesh)
	Local V0,V1,V2,V3
	V0 = AddVertex(Instance\Surface, -1, 1, 0, 1, 0, 0)
	V1 = AddVertex(Instance\Surface,  1, 1, 0, 0, 0, 0)
	V2 = AddVertex(Instance\Surface, -1, -1, 0, 1, 1, 0)
	V3 = AddVertex(Instance\Surface,  1, -1, 0, 0, 1, 0)
	AddTriangle Instance\Surface, V0, V2, V1
	AddTriangle Instance\Surface, V1, V2, V3
	Return Instance
End Function

Function Impostor_Load.Impostor(Path$, Flags%=1+4+16+32+256+512, Parent%=0)
	If FileType(Path) <> 1 Then
		RuntimeError "Impostor: Given <Path$> is not a file."
	Else
		Local Stream = ReadFile(Path)
		If Stream = 0 Then
			RuntimeError "Impostor: Unable to open given <Path$>."
		Else
			Local Instance.Impostor = Impostor_Init(Parent)
			
			Instance\FrameWidth		= ReadShort(Stream)
			Instance\FrameHeight	= ReadShort(Stream)
			Instance\PitchFrames	= ReadByte(Stream)
			Instance\PitchMin		= ReadFloat(Stream)
			Instance\PitchMax		= ReadFloat(Stream)
			Instance\YawFrames		= ReadByte(Stream)
			Instance\YawMin			= ReadFloat(Stream)
			Instance\YawMax			= ReadFloat(Stream)
			
			Local BaseName$ = Impostor_StripExtension(Path)
			Instance\Sheet	= LoadAnimTexture(BaseName + "png", Flags, Instance\FrameWidth, Instance\FrameHeight, 0, Instance\PitchFrames * Instance\YawFrames)
			If Instance\Sheet = 0 Then RuntimeError "Impostor: Unable to open texture for given <Path$>."
			
			Instance\YawStep = (Instance\YawMax - Instance\YawMin) / Instance\YawFrames
			Instance\PitchStep = (Instance\PitchMax - Instance\PitchMin) / Instance\PitchFrames
			
			Return Instance
		EndIf
	EndIf
End Function

Function Impostor_Create.Impostor(Mesh%, Diffuse%)
	
	
	
End Function

Function Impostor_Update(Camera%)
	Local Instance.Impostor = Null
	For Instance = Each Impostor
		Impostor_UpdateSingle(Camera, Instance)
	Next
End Function

Function Impostor_UpdateSingle(Camera%, Impostor.Impostor)
	; Calculate current Yaw frame and Pitch frame.
	PointEntity Impostor\Mesh, Camera, 0
	
	Local Yaw# = Impostor_Math_MaxMin(EntityYaw(Impostor\Mesh), Impostor\YawMax, Impostor\YawMin) - Impostor\YawMin
	Local Pitch# = Impostor_Math_MaxMin(EntityPitch(Impostor\Mesh), Impostor\PitchMax, Impostor\PitchMin) - Impostor\PitchMin
	
	Local YawFrame = Int(Yaw / Impostor\YawStep)
	Local PitchFrame = Floor(Pitch / Impostor\PitchStep)
	DebugLog YawFrame
	EntityTexture Impostor\Mesh, Impostor\Sheet, PitchFrame * Impostor\YawFrames + YawFrame, 0
	EntityFX Impostor\Mesh, 16
	RotateEntity Impostor\Mesh, Impostor\PitchMin - PitchFrame * Impostor\PitchStep, 180 + Impostor\YawMin + YawFrame * Impostor\YawStep, 0
End Function

Function Impostor_StripExtension$(Path$)
	Local RPath$ = Path$
	For temp_Pos = Len(Path)-1 To 1 Step -1
		If Mid(Path, temp_Pos, 1) = "."
			RPath = Left(Path, temp_Pos)
			Exit
		EndIf
	Next
	Return RPath
End Function

Function Impostor_Math_MaxMin#(Value#, Max#, Min#)
	If Value> Max Then Return Max
	If Value < Min Then Return Min
	Return Value
End Function
Function Impostor_Math_Max#(Value#, Max#)
	If Value> Max Then Return Max
	Return Value
End Function
Function Impostor_Math_Min#(Value#, Min#)
	If Value < Min Then Return Min
	Return Value
End Function
Function Impostor_Math_Clip#(Value#, Low#, High#)
	Local Out#, Diff#
	Diff = High-Low:Out = Value-Low
	If (Out >= Diff) Then Out = Out - Floor(Out/Diff)*Diff
	If (Out < 0) Then Out = Out - Floor(Out/Diff)*Diff
	Return Low+Out
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D