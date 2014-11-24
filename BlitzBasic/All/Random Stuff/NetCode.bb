;----------------------------------------------------------------
;-- Packet Descriptors
;----------------------------------------------------------------

;-- Any Packet
;Off	Size	Desc
;  0	1		Packet Id

;-- Login
;Off	Size	Desc
;  1	2		Unique Id (Server) / UDP Port (Client)
;  3	2		Version
;  5	16		Name (Always 16B, Only allows bytes above 32)
; 21	4F		Initial Position X
; 25	4F		Initial Position Y
; 29	4F		Initial Position Z
; 33	4F		Initial Rotation X
; 37	4F		Initial Rotation Y
; 41	4F		Initial Rotation Z

;-- Logout
;Off	Size	Desc
;  1	2		Unique Id

;-- Kick
;Off	Size	Desc
;  1	2		Reason Length
;  3	^		Reason

;-- Data
;  1	2		Unique Id
;  3	1		Data Flags (See BNET_DATAFLAG_*)
;...	4F		Position X
;...	4F		Position Y
;...	4F		Position Z
;...	2		Rotation X
;...	2		Rotation Y
;...	2		Rotation Z
;...	2		Velocity X
;...	2		Velocity Y
;...	2		Velocity Z
;...	2		Aim X
;...	2		Aim Y

;-- Action (Yet Unsupported)

;----------------------------------------------------------------
;-- Constants
;----------------------------------------------------------------
	; BlitzNet Version
Const BNET_VERSION_MAJOR%	 = 0
Const BNET_VERSION_MINOR%	 = 1
	; Size of Buffer for Network Packets, 64KB should be enough for now.
Const BNET_BUFFER_SIZE%		 = (64 * 1024)
	; Flags for data changes. Only when the bit is set this data is available.
Const BNET_DATAFLAG_POSX	 = $00000001
Const BNET_DATAFLAG_POSY	 = $00000010
Const BNET_DATAFLAG_POSZ	 = $00000100
Const BNET_DATAFLAG_ROTX	 = $00001000
Const BNET_DATAFLAG_ROTY	 = $00010000
Const BNET_DATAFLAG_ROTZ	 = $00100000
Const BNET_DATAFLAG_VELOCITY = $01000000
Const BNET_DATAFLAG_AIM		 = $10000000
	; Packet Ids, for identification of each one.
Const BNET_PACKET_LOGIN	 = 0
Const BNET_PACKET_LOGOUT = 1
Const BNET_PACKET_KICK	 = 2
Const BNET_PACKET_DATA	 = 3
Const BNET_PACKET_ACTION = 4
	; How many updates should we send out each minute?
Const BNET_DATA_COUNT			 = 150
	; How many keyframes should we send out each minute?
Const BNET_DATA_COUNT_KEYFRAME	 = 3
	; How much has the value to change to be considered different?
Const BNET_DATA_THRESHOLD_POS#	 = 1.0
Const BNET_DATA_THRESHOLD_ROT#	 = 1.0
Const BNET_DATA_THRESHOLD_VEL#	 = 1.0
Const BNET_DATA_THRESHOLD_AIM#	 = 1.0

;----------------------------------------------------------------
;-- Globals
;----------------------------------------------------------------
	; Connections to Sector Servers.
Global BNet_Sector_TCP%			 = 0
Global BNet_Sector_UDP%			 = 0
	; Our UniqueId and Player instance so we know which Player is us.
Global BNet_UniqueId%			 = -1
Global BNet_Player.BNetPlayer	 = Null
	; Buffer for Network Packets.
Global BNet_Buffer%				 = CreateBank(BNET_BUFFER_SIZE)
	; Set to true when Kicked from the server, or similar.
Global BNet_Kick%				 = False
Global BNet_Kick_Reason$		 = "Not Kicked"
	; Update Times
Global BNet_Data_Time			 = 60000 / BNET_DATA_COUNT
Global BNet_Data_Time_KeyFrame	 = 60000 / BNET_DATA_COUNT_KEYFRAME
Global BNet_Data_LastUpdate		 = 0
Global BNet_Data_LastKeyFrame	 = 0
	; Array for all known players. Makes access a bit faster.
Dim BNet_Players.BNetPlayer(65535)

;----------------------------------------------------------------
;-- Types
;----------------------------------------------------------------
Type BNetPlayer
	Field Name$		 = "Invalid Player"
	
		; Position, Rotation, Velocity and Aim
	Field PositionX#, PositionY#, PositionZ#
	Field RotationX#, RotationY#, RotationZ#
	Field VelocityX#, VelocityY#, VelocityZ#
	Field AimX#, AimY#
	
		; Internal Data
	Field m_UniqueId%
End Type

;----------------------------------------------------------------
;-- Functions
;----------------------------------------------------------------
Function BNet_Initialize()
	TCPTimeouts 100, 100
End Function

Function BNet_Connect(Ip$ = "127.0.0.1", IPort% = 27000, DPort = 27001)
	BNet_Kick%			 = False
	BNet_Kick_Reason$	 = "Not Kicked"
	
	BNet_Sector_TCP = OpenTCPStream(Ip, IPort)
	If BNet_Sector_TCP Then
		BNet_Sector_UDP = CreateUDPStream()
		If BNet_Sector_UDP Then
			Return True
		Else
			CloseTCPStream BNet_Sector_TCP
			BNet_Sector_UDP = 0
			BNet_Sector_TCP = 0
		EndIf
	Else
		BNet_Sector_UDP = 0
		BNet_Sector_TCP = 0
	EndIf
	Return False
End Function

Function BNet_Disconnect()
	If BNet_UniqueId > -1 Then
			; Remove all existing players
		For Player.BNetPlayer = Each BNetPlayer
			Local UniqueId = Player\m_UniqueId
			
			CB_BNet_DeletePlayer(UniqueId, Player)
			
			Delete BNet_Players(UniqueId):BNet_Players(UniqueId) = Null
		Next
		
			; Logout
		BNet_Logout()
		BNet_UniqueId = -1
	EndIf
	If BNet_Sector_UDP Then CloseUDPStream(BNet_Sector_UDP):BNet_Sector_UDP = 0
	If BNet_Sector_TCP Then CloseTCPStream(BNet_Sector_TCP):BNet_Sector_TCP = 0
End Function

Function BNet_Connected()
	If BNet_Sector_TCP = 0 Then Return False 
	If BNet_Sector_UDP = 0 Then Return False
	If Eof(BNet_Sector_TCP) <> 0 Then Return False
	
	Return True
End Function

Function BNet_Login(Name$, PosX# = 0, PosY# = 0, PosZ# = 0, RotX# = 0, RotY# = 0, RotZ# = 0)
	If BNet_Connected() And BNet_UniqueId = -1 Then
			; Packet Id: Login
		PokeByte BNet_Buffer, 0, BNET_PACKET_LOGIN
			; Write UDP Port
		PokeShort BNet_Buffer, 1, UDPStreamPort(BNet_Sector_UDP)
			; Write Client Version
		PokeShort BNet_Buffer, 3, BNET_VERSION_MAJOR Shl 8 + BNET_VERSION_MINOR
			; Write Name
		Local NameLength% = Len(Name)
		For NamePos = 1 To NameLength:PokeByte BNet_Buffer, (5 + NamePos - 1), Asc(Mid(Name, NamePos, 1)):Next
		For NamePos = NameLength To 16:PokeByte BNet_Buffer, (5 + NamePos - 1), 0:Next
			; Write Initial Position
		PokeFloat BNet_Buffer, 21, PosX
		PokeFloat BNet_Buffer, 25, PosY
		PokeFloat BNet_Buffer, 29, PosZ
			; Write Initial Rotation
		PokeFloat BNet_Buffer, 33, RotX
		PokeFloat BNet_Buffer, 37, RotX
		PokeFloat BNet_Buffer, 41, RotX
		
			; Write to Stream
		WriteBytes BNet_Buffer, BNet_Sector_TCP, 0, 47
		
		Return True
	Else
		Return False
	EndIf
End Function

Function BNet_Logout()
	If BNet_Connected() And BNet_UniqueId > -1 Then
			; Packet Id: Logout
		PokeByte BNet_Buffer, 0, BNET_PACKET_LOGOUT
			; Write our own UniqueId
		PokeShort BNet_Buffer, 1, BNet_UniqueId
		
			; Write to Stream
		WriteBytes BNet_Buffer, BNet_Sector_TCP, 0, 3
		
		Return True
	Else
		Return False
	EndIf
End Function

Function BNet_Update()
	Local UniqueId, PacketSize, PacketId, DataFlags, Offset, TempPlayer.BNetPlayer
	If BNet_Connected() Then
		; TCP
		While Not Eof(BNet_Sector_TCP) And ReadAvail(BNet_Sector_TCP) > 0
			PacketSize = ReadAvail(BNet_Sector_TCP)
			If PacketSize > BNET_BUFFER_SIZE Then PacketSize = BNET_BUFFER_SIZE
			ReadBytes(BNet_Buffer, BNet_Sector_TCP, 0, PacketSize)
			
			PacketId = PeekByte(BNet_Buffer, 0)
			Select PacketId
				Case BNET_PACKET_LOGIN
					UniqueId = PeekShort(BNet_Buffer, 1)

					BNet_Players(UniqueId) = New BNetPlayer
						; Read Name
					For NamePos = 1 To 16
						Local NameChar = PeekByte(BNet_Buffer, (5 + NamePos - 1))
						If NameChar < 32 Then Exit
						BNet_Players(UniqueId)\Name$ = BNet_Players(UniqueId)\Name$ + Chr(NameChar)
					Next
						; Read initital Position
					BNet_Players(UniqueId)\PositionX = PeekFloat(BNet_Buffer, 21)
					BNet_Players(UniqueId)\PositionY = PeekFloat(BNet_Buffer, 25)
					BNet_Players(UniqueId)\PositionZ = PeekFloat(BNet_Buffer, 29)
						; Read initial Rotation
					BNet_Players(UniqueId)\RotationX = PeekFloat(BNet_Buffer, 33)
					BNet_Players(UniqueId)\RotationY = PeekFloat(BNet_Buffer, 37)
					BNet_Players(UniqueId)\RotationZ = PeekFloat(BNet_Buffer, 41)
						; Assign Unique Id
					BNet_Players(UniqueId)\m_UniqueId = UniqueId
					
						; Assign local UniqueId if we don't have one.
					If BNet_UniqueId > -1 Then
						BNet_UniqueId = UniqueId
					Else
						CB_BNet_CreatePlayer(UniqueId, BNet_Players(BNet_UniqueId))
					EndIf
				Case BNET_PACKET_LOGOUT
					UniqueId = PeekShort(BNet_Buffer, 1)
					
						; A logout packet for ourselves will make us logout.
					If BNet_UniqueId = UniqueId Then
						BNet_Disconnect():Return True
					ElseIf BNet_Players(UniqueId) <> Null Then
						; Otherwise, it is another player removed from visible space.
						CB_BNet_DeletePlayer(Unique, BNet_Players(UniqueId))
						
						Delete BNet_Players(UniqueId):BNet_Players(UniqueId) = Null
					Else
						DebugLog "BNet: Logout for non-existing player."
					EndIf
				Case BNET_PACKET_KICK
					BNet_Kick = True
					
						; Read reason for Kick
					Local ReasonLength% = PeekShort(BNet_Buffer, 1)
					For ReasonPos = 1 To ReasonLength
						Local ReasonChar = PeekByte(BNet_Buffer, (3 + ReasonPos - 1))
						If ReasonChar < 32 Then Exit
						BNet_Kick_Reason = BNet_Kick_Reason + Chr(NameChar)
					Next
					
					BNet_Disconnect():Return True
			End Select
		Wend
		
		; UDP
		While Not Eof(BNet_Sector_UDP) And ReadAvail(BNet_Sector_UDP) > 0
			PacketSize = ReadAvail(BNet_Sector_UDP)
			If PacketSize > BNET_BUFFER_SIZE Then PacketSize = BNET_BUFFER_SIZE
			ReadBytes(BNet_Buffer, BNet_Sector_UDP, 0, PacketSize)
			
			PacketId = PeekByte(BNet_Buffer, 0)
			Select PacketId
				Case BNET_PACKET_DATA
					If BNet_UniqueId > -1 Then
						UniqueId = PeekShort(BNet_Buffer, 1)
						If BNet_Players(UniqueId) <> Null Then
							DataFlags = PeekShort(BNet_Buffer, 3)
							Offset% = 5
							
							If DataFlags And BNET_DATAFLAGS_POSX Then BNet_Players(UniqueId)\PositionX = PeekInt(BNet_Buffer, Offset):Offset = Offset + 4
							If DataFlags And BNET_DATAFLAGS_POSY Then BNet_Players(UniqueId)\PositionY = PeekInt(BNet_Buffer, Offset):Offset = Offset + 4
							If DataFlags And BNET_DATAFLAGS_POSZ Then BNet_Players(UniqueId)\PositionZ = PeekInt(BNet_Buffer, Offset):Offset = Offset + 4
							If DataFlags And BNET_DATAFLAGS_ROTX Then BNet_Players(UniqueId)\RotationX = BNet_FloatFromShort(PeekShort(BNet_Buffer, Offset)) * 360.0:Offset = Offset + 2
							If DataFlags And BNET_DATAFLAGS_ROTY Then BNet_Players(UniqueId)\RotationY = BNet_FloatFromShort(PeekShort(BNet_Buffer, Offset)) * 360.0:Offset = Offset + 2
							If DataFlags And BNET_DATAFLAGS_ROTZ Then BNet_Players(UniqueId)\RotationZ = BNet_FloatFromShort(PeekShort(BNet_Buffer, Offset)) * 360.0:Offset = Offset + 2
							If DataFlags And BNET_DATAFLAGS_VELOCITY Then
								BNet_Players(UniqueId)\VelocityX = BNet_FloatFromShort(PeekShort(BNet_Buffer, Offset)) * 255.0
								BNet_Players(UniqueId)\VelocityY = BNet_FloatFromShort(PeekShort(BNet_Buffer, Offset)) * 255.0
								BNet_Players(UniqueId)\VelocityZ = BNet_FloatFromShort(PeekShort(BNet_Buffer, Offset)) * 255.0
								
								Offset = Offset + 6
							EndIf
							If DataFlags And BNET_DATAFLAGS_AIM Then
								BNet_Players(UniqueId)\AimX = BNet_FloatFromShort(PeekShort(BNet_Buffer, Offset)) * 360.0
								BNet_Players(UniqueId)\AimY = BNet_FloatFromShort(PeekShort(BNet_Buffer, Offset)) * 360.0
								
								Offset = Offset + 4
							EndIf
							
								; Tell Client to update visual stuff
							CB_BNet_UpdatePlayer(UniqueId, BNet_Players(UniqueId))
						EndIf
					EndIf
;				Case BNET_PACKET_ACTION
;					If BNet_UniqueId > -1 Then
;						
;					EndIf
			End Select
		Wend
		
			; If we are logged in, tell the server our current data.
		If BNet_UniqueId > -1 Then
			Local Time = MilliSecs()
			
			; Send out whatever is needed.
			If Time - BNet_Data_LastKeyFrame > BNet_Data_Time_KeyFrame Then
					; Force Client to update Player object
				CB_BNet_SendUpdate(BNet_UniqueId, BNet_Player)
				
					; Packet Id
				PokeByte BNet_Buffer, 0, BNET_PACKET_DATA
					; Unique Id
				PokeShort BNet_Buffer, 1, BNet_UniqueId
					; Data Flags (KeyFrame always has this at 255, since it contains all data)
				PokeByte BNet_Buffer, 3, $11111111
					; Position
				PokeFloat BNet_Buffer, 4, BNet_Player\PositionX
				PokeFloat BNet_Buffer, 8, BNet_Player\PositionY
				PokeFloat BNet_Buffer,12, BNet_Player\PositionZ
					; Rotation
				PokeShort BNet_Buffer,16, BNet_ShortFromFloat(BNet_Player\RotationX / 360.0)
				PokeShort BNet_Buffer,18, BNet_ShortFromFloat(BNet_Player\RotationY / 360.0)
				PokeShort BNet_Buffer,20, BNet_ShortFromFloat(BNet_Player\RotationZ / 360.0)
					; Velocity
				PokeShort BNet_Buffer,22, BNet_ShortFromFloat(BNet_Player\VelocityX / 256.0)
				PokeShort BNet_Buffer,24, BNet_ShortFromFloat(BNet_Player\VelocityY / 256.0)
				PokeShort BNet_Buffer,26, BNet_ShortFromFloat(BNet_Player\VelocityZ / 256.0)
					; Aim
				PokeShort BNet_Buffer,28, BNet_ShortFromFloat(BNet_Player\AimX / 360.0)
				PokeShort BNet_Buffer,30, BNet_ShortFromFloat(BNet_Player\AimY / 360.0)
				
					; Send Packet
				WriteBytes BNet_Buffer, BNet_Sector_UDP, 0, 32
				
					; Swap Player objects.
				TempPlayer = BNet_Player
				BNet_Player = BNet_Players(BNet_UniqueId)
				BNet_Players(BNet_UnqiueId) = TempPlayer
				
					; Set last keyframe and update time to now.
				BNet_Data_LastKeyFrame = Time
				BNet_Data_LastUpdate = Time
			ElseIf Time - BNet_Data_LastUpdate > BNet_Data_Time Then
				DataFlags = 0
				Offset = 4
				
					; Force Client to update Player object
				CB_BNet_SendUpdate(BNet_UniqueId, BNet_Player)
				
					; Packet Id
				PokeByte BNet_Buffer, 0, BNET_PACKET_DATA
					; Unique Id
				PokeShort BNet_Buffer, 1, BNet_UniqueId
				
					; Position
				If Abs(BNet_Player\PositionX - BNet_Players(BNet_UniqueId)\PositionX) > BNET_DATA_THRESHOLD_POS Then
					DataFlags = DataFlags Or BNET_DATAFLAG_POSX
					PokeFloat BNet_Buffer, Offset, BNet_Player\PositionX:Offset = Offset + 4
				EndIf
				If Abs(BNet_Player\PositionY - BNet_Players(BNet_UniqueId)\PositionY) > BNET_DATA_THRESHOLD_POS Then
					DataFlags = DataFlags Or BNET_DATAFLAG_POSY
					PokeFloat BNet_Buffer, Offset, BNet_Player\PositionY:Offset = Offset + 4
				EndIf
				If Abs(BNet_Player\PositionZ - BNet_Players(BNet_UniqueId)\PositionZ) > BNET_DATA_THRESHOLD_POS Then
					DataFlags = DataFlags Or BNET_DATAFLAG_POSZ
					PokeFloat BNet_Buffer, Offset, BNet_Player\PositionZ:Offset = Offset + 4
				EndIf
					; Rotation
				If Abs(BNet_Player\RotationX - BNet_Players(BNet_UniqueId)\RotationX) > BNET_DATA_THRESHOLD_POS Then
					DataFlags = DataFlags Or BNET_DATAFLAG_ROTX
					PokeFloat BNet_Buffer, Offset, BNet_ShortFromFloat(BNet_Player\RotationX / 360.0):Offset = Offset + 2
				EndIf
				If Abs(BNet_Player\RotationY - BNet_Players(BNet_UniqueId)\RotationY) > BNET_DATA_THRESHOLD_POS Then
					DataFlags = DataFlags Or BNET_DATAFLAG_ROTY
					PokeFloat BNet_Buffer, Offset, BNet_ShortFromFloat(BNet_Player\RotationY / 360.0):Offset = Offset + 2
				EndIf
				If Abs(BNet_Player\RotationZ - BNet_Players(BNet_UniqueId)\RotationZ) > BNET_DATA_THRESHOLD_POS Then
					DataFlags = DataFlags Or BNET_DATAFLAG_ROTZ
					PokeFloat BNet_Buffer, Offset, BNet_ShortFromFloat(BNet_Player\RotationZ / 360.0):Offset = Offset + 2
				EndIf
					; Velocity
				If Abs(BNet_Player\VelocityX - BNet_Players(BNet_UniqueId)\VelocityX) + Abs(BNet_Player\VelocityY - BNet_Players(BNet_UniqueId)\VelocityY) + Abs(BNet_Player\VelocityZ - BNet_Players(BNet_UniqueId)\VelocityZ) > BNET_DATA_THRESHOLD_VEL Then
					DataFlags = DataFlags Or BNET_DATAFLAG_VELOCITY
					PokeFloat BNet_Buffer, Offset, BNet_ShortFromFloat(BNet_Player\VelocityX / 256.0):Offset = Offset + 2
					PokeFloat BNet_Buffer, Offset, BNet_ShortFromFloat(BNet_Player\VelocityY / 256.0):Offset = Offset + 2
					PokeFloat BNet_Buffer, Offset, BNet_ShortFromFloat(BNet_Player\VelocityZ / 256.0):Offset = Offset + 2
				EndIf
					; Aim
				If Abs(BNet_Player\AimX - BNet_Players(BNet_UniqueId)\AimX) + Abs(BNet_Player\AimY - BNet_Players(BNet_UniqueId)\AimY) > BNET_DATA_THRESHOLD_AIM Then
					DataFlags = DataFlags Or BNET_DATAFLAG_AIM
					PokeFloat BNet_Buffer, Offset, BNet_ShortFromFloat(BNet_Player\AimX / 360.0):Offset = Offset + 2
					PokeFloat BNet_Buffer, Offset, BNet_ShortFromFloat(BNet_Player\AimY / 360.0):Offset = Offset + 2
				EndIf
				
					; Data Flags
				PokeByte BNet_Buffer, 3, DataFlags
				
					; Send Packet
				WriteBytes BNet_Buffer, BNet_Sector_UDP, 0, Offset
				
					; Swap Player objects.
				TempPlayer = BNet_Player
				BNet_Player = BNet_Players(BNet_UniqueId)
				BNet_Players(BNet_UnqiueId) = TempPlayer
				
					; Set last update time to now.
				BNet_Data_LastUpdate = Time
			EndIf
		EndIf
	Else
		Return False
	EndIf
End Function

Function BNet_FloatFromShort#(Value%)
	Return BNet_Math_ClipF((Value / 65536.0), 0.0, 1.0)
End Function

Function BNet_ShortFromFloat%(Value#)
	Return BNet_Math_ClipF((Value * 65536.0), 0.0, 1.0)
End Function

Function BNet_Math_MinimumF#(Value#, Min#)
	If Value < Min Then Return Min
	Return Value
End Function

Function BNet_Math_MaximumF#(Value#, Max#)
	If Value > Max Then Return Max
	Return Value
End Function

Function BNet_Math_ClampF#(Value#, Min#, Max#)
	If Value < Min Then Return Min
	If Value > Max Then Return Max
	Return Value
End Function

Function BNet_Math_ClipF#(Value#, Min#, Max#)
	Local Out#, Diff#
	Diff = Max - Min:Out = Value - Min
	If (Out >= Diff) Or (Out < 0) Then Out = Out - (Floor(Out/Diff) * Diff)
	Return Min + Out
End Function

;----------------------------------------------------------------
;-- Callbacks
;----------------------------------------------------------------
;Function CB_BNet_CreatePlayer(UniqueId, Player.BNetPlayer)
;End Function

;Function CB_BNet_UpdatePlayer(UniqueId, Player.BNetPlayer)
;End Function

;Function CB_BNet_DeletePlayer(UniqueId, Player.BNetPlayer)
;End Function

;Function CB_BNet_SendUpdate(UniqueId, Player.BNetPlayer)
;End Function

;----------------------------------------------------------------
;-- Example
;----------------------------------------------------------------
Graphics3D 800, 600, 32, 2
SetBuffer BackBuffer()
SeedRnd MilliSecs()

Local FrameTimer = CreateTimer(60)
Local TimeToLogin = MilliSecs()

Dim Players.TPlayer(65535)
Local Player.TPlayer	 = Null

Global TestX#, TestY#, TestZ#
Global TestRX#, TestRY#, TestRZ#
Global TestVX#, TestVY#, TestVZ#
Global TestAX#, TestAY#
Local ShipVeloLR#, ShipVeloUD#, ShipVeloFB#
Const ShipMaxVeloLR# = 5.0, ShipMaxVeloUD# = 5.0, ShipMaxVeloFB = 30.0

; Init
TestX = Rnd(-2048, 2048)
TestY = Rnd(-2048, 2048)
TestZ = Rnd(-2048, 2048)
TestRX = Rnd(0, 360)
TestRY = Rnd(0, 360)
TestRZ = Rnd(0, 360)
TestVX = 0
TestVY = 0
TestVZ = 0
TestAX = 0
TestAY = 0


Local Cam = CreateCamera()
Local Conv = CreatePivot()

Print "[INF] Connecting..."
BNet_Initialize()
BNet_Connect()
If BNet_Connected() Then
	Print "[INF] Logging in..."
	BNet_Login("Test " + Rand(0, 65535), TestX, TestY, TestZ, TestRX, TestRY, TestRZ)
	
	Local LoopExit = False
	Local Load = False
	Repeat
		If Load = False And BNet_UniqueId = -1 And (MilliSecs() - TimeToLogin > 30000) Then
			Print "[ERR] Failed to login."
			LoopExit = True
		ElseIf Load = False And BNet_UniqueId > -1 Then
			Print "[INF] Logged in."
			
			Player = New TPlayer
			Player\Mesh = CreateCone()
			Player\Player = BNet_Player
			Players(BNet_UniqueId) = Player
			EntityParent Cam, Player\Mesh
			
			Load = True
		ElseIf Load = True And BNet_UniqueId > -1
			; Player Input
				; Velocity
			ShipVeloFB = BNet_Math_ClampF(ShipVeloFB + (KeyDown(17) - KeyDown(31)) * 2.5, -ShipMaxVeloFB, ShipMaxVeloFB)
			ShipVeloLR = BNet_Math_ClampF(ShipVeloLR + (KeyDown(32) - KeyDown(30)) * 1.5, -ShipMaxVeloLR, ShipMaxVeloLR)
			ShipVeloUD = BNet_Math_ClampF(ShipVeloUD + (KeyDown(19) - KeyDown(33)) * 1.5, -ShipMaxVeloUD, ShipMaxVeloUD)
				; Rotation
			If KeyDown(57) Then
				If KeyHit(57) Then MoveMouse 512, 384
				TestRX = TestRX + ((MouseX() / 512) - 1.0)
				TestRY = TestRY + ((MouseY() / 384) - 1.0)
			EndIf
			TestRZ = TestRZ + (KeyDown(18) - KeyDown(16)) * 1.0
			
			; Update Game
				; Slowly scale down Velocity.
			ShipVeloFB = ShipVeloFB * 0.9
			ShipVeloLR = ShipVeloLR * 0.8
			ShipVeloUD = ShipVeloUD * 0.8
				; Convert Local Velocity to Global Velocity
			TFormPoint ShipVeloLR, ShipVeloUD, ShipVeloFB, Player\Mesh, Conv
			TestVX = TFormedX()
			TestVY = TFormedY()
			TestVZ = TFormedZ()
				; Move player by Velocity.
			TestX = TestX + TestVX
			TestY = TestY + TestVY
			TestZ = TestZ + TestVZ
			
			; Draw Game
				; Update Local Mesh
			PositionEntity Player\Mesh, TestX, TestY, TestZ
			RotateEntity Player\Mesh, TestRX, TestRY, TestRZ
				; Update conversion point
			PositionEntity Conv, TestX, TestY, TestZ
			
			RenderWorld
			Flip 0
		EndIf
		
		If BNet_Update() Then
			LoopExit = True
		EndIf
		WaitTimer FrameTimer
	Until (LoopExit = True)
	If BNet_Kick = True Then Print "[INF] Kicked: " + BNet_Kick_Reason Else Print "Logging out."
	
	BNet_Disconnect()
Else
	Print "[ERR] Failed to connect." 
EndIf
End

Function CB_BNet_CreatePlayer(UniqueId, Player.BNetPlayer)
	DebugLog "[INF] New Player: " + Player\Name + "[" + UniqueId + "]"
	DebugLog "  Pos: " + Player\PositionX + ", " + Player\PositionY + ", " + Player\PositionZ
	DebugLog "  Rot: " + Player\RotationX + ", " + Player\RotationY + ", " + Player\RotationZ
	
	If Players(UniqueId) = Null Then Players(UniqueId) = New TPlayer
	If Players(UniqueId)\Mesh <> 0 Then Players(UniqueId)\Mesh = CreateCone()
	Players(UniqueId)\Player = Player
	
	PositionEntity Players(UniqueId)\Mesh, Player\PositionX, Player\PositionY, Player\PositionZ
	RotateEntity Players(UniqueId)\Mesh, Player\RotationX, Player\RotationY, Player\RotationZ
End Function

Function CB_BNet_UpdatePlayer(UniqueId, Player.BNetPlayer)
	If Players(UniqueId) <> Null Then
		DebugLog "[INF] Update Player: " + Player\Name + "[" + UniqueId + "]"
		DebugLog "  Pos: " + Player\PositionX + ", " + Player\PositionY + ", " + Player\PositionZ
		DebugLog "  Rot: " + Player\RotationX + ", " + Player\RotationY + ", " + Player\RotationZ
		
		PositionEntity Players(UniqueId)\Mesh, Player\PositionX, Player\PositionY, Player\PositionZ
		RotateEntity Players(UniqueId)\Mesh, Player\RotationX, Player\RotationY, Player\RotationZ
	Else
		DebugLog "[ERR] Player does not exist: " + Player\Name + "[" + UniqueId + "]"
	EndIf
End Function

Function CB_BNet_DeletePlayer(UniqueId, Player.BNetPlayer)
	If Players(UniqueId) <> Null Then
		DebugLog "[INF] Delete Player: " + UniqueId + "," + Player\Name
		
		FreeEntity Players(UniqueId)\Mesh
		Delete Players(UniqueId)
	Else
		DebugLog "[ERR] Player does not exist: " + Player\Name + "[" + UniqueId + "]"
	EndIf
End Function

Function CB_BNet_SendUpdate(UniqueId, Player.BNetPlayer)
	DebugLog "[INF] Update Self"
	
	Player\PositionX = TestX
	Player\PositionY = TestY
	Player\PositionZ = TestZ
	
	Player\VelocityX = TestVX
	Player\VelocityY = TestVY
	Player\VelocityZ = TestVZ
	
	Player\RotationX = TestRX
	Player\RotationY = TestRY
	Player\RotationZ = TestRZ
	
	Player\AimX = TestAX
	Player\AimY = TestAY
End Function

Type TPlayer
	Field Mesh
	
	Field Player.BNetPlayer
End Type