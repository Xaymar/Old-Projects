SuperStrict
Import BRL.Socket
Import BRL.LinkedList
Import BRL.Stream
Import BRL.Bank
Import Xaymar.IOQueue

'----------------------------------------------------------------
'-- Packet Descriptors
'----------------------------------------------------------------

'-- Any Packet
'Off	Size	Desc
'  0	1		Packet Id

'-- Login
'Off	Size	Desc
'  1	2		Unique Id (Server) / UDP Port (Client)
'  3	2		Version
'  5	16		Name (Always 16B, Only allows bytes above 32)
' 21	4F		Initial Position X
' 25	4F		Initial Position Y
' 29	4F		Initial Position Z
' 33	4F		Initial Rotation X
' 37	4F		Initial Rotation Y
' 41	4F		Initial Rotation Z

'-- Logout
'Off	Size	Desc
'  1	2		Unique Id

'-- Kick
'Off	Size	Desc
'  1	2		Reason Length
'  3	^		Reason

'-- Data
'  1	2		Unique Id
'  3	1		Data Flags (See BNET_DATAFLAG_*)
'...	4F		Position X
'...	4F		Position Y
'...	4F		Position Z
'...	2		Rotation X
'...	2		Rotation Y
'...	2		Rotation Z
'...	2		Velocity X
'...	2		Velocity Y
'...	2		Velocity Z
'...	2		Aim X
'...	2		Aim Y

'-- Action (Yet Unsupported)



'----------------------------------------------------------------
'-- Constants
'----------------------------------------------------------------
	' Library Version
Const BNET_VERSION_MAJOR:Byte		 = 0
Const BNET_VERSION_MINOR:Byte		 = 1
	' Value Thresholds
Const BNET_THRESHOLD_POSITION:Float	 = 1.0
Const BNET_THRESHOLD_ROTATION:Float	 = 8.0
Const BNET_THRESHOLD_VELOCITY:Float	 = 5.0
Const BNET_THRESHOLD_AIM:Float		 = 1.0
	' Data Flags, used in BNetPacketData
Const BNET_DATAFLAG_POSITIONX:Byte	 = $00000001
Const BNET_DATAFLAG_POSITIONY:Byte	 = $00000010
Const BNET_DATAFLAG_POSITIONZ:Byte	 = $00000100
Const BNET_DATAFLAG_ROTATIONX:Byte	 = $00001000
Const BNET_DATAFLAG_ROTATIONY:Byte	 = $00010000
Const BNET_DATAFLAG_ROTATIONZ:Byte	 = $00100000
Const BNET_DATAFLAG_VELOCITY:Byte	 = $01000000
Const BNET_DATAFLAG_AIM:Byte		 = $10000000
	' Translateable Strings
Const BNET_KICK_SHUTDOWN:String		 = "~r~rKICK_SHUTDOWN"
Const BNET_KICK_SERVERFULL:String	 = "~r~rKICK_SERVERFULL"

'----------------------------------------------------------------
'-- Types
'----------------------------------------------------------------
Type BNetServer
		' Size of Buffer for Network Packets, 64KB should be enough for now.
	Const BUFFER_SIZE:Int			 = (64 * 1024)
		' How many updates should we send out each minute?
	Const DATA_COUNT:Int			 = 150
		' How many keyframes should we send out each minute?
	Const DATA_COUNT_KEYFRAME:Int	 = 3
	
	'----------------------------------------------------------------
	'-- Globals
	'----------------------------------------------------------------
	Global Data_Time:Int			 = 60000 / BNetServer.DATA_COUNT
	Global Data_Time_KeyFrame:Int	 = 60000 / BNetServer.DATA_COUNT_KEYFRAME
	
	'----------------------------------------------------------------
	'-- Variables / Members
	'----------------------------------------------------------------
		' Sockets
	Field m_TCPSocket:TSocket				 = Null
	Field m_UDPSocket:TSocket				 = Null
		' Queue
	Field m_TCPQueue:TIOQueue				 = Null
	Field m_UDPQueue:TIOQueue				 = Null
		' Packet Buffer
	Field m_Buffer:TBank					 = Null
		' Players
	Field m_Players:TList					 = Null'New TList
	Field m_UniqueIdToPlayer:BNetPlayer[]	 = Null'New BNetPlayer[65536]
	Field m_UDPPortToPlayers:TList[]		 = Null'New TList[65536]
	
		' Temporary Variables
	Field mt_LastUpdateTime:Long			 = 0
	
	'----------------------------------------------------------------
	'-- Functions / Methods
	'----------------------------------------------------------------
	Function Create:BNetServer()
		Return (New BNetServer)
	End Function
	
		' Constructor
	Method New()
			' Create packet queues.
		m_TCPQueue				 = New TIOQueue
		m_UDPQueue				 = New TIOQueue
			' Create Buffer
		m_Buffer				 = CreateBank(BUFFER_SIZE)
		
			' Create Player list.
		m_Players				 = New TList
			' Create UID to Player mapping.
		m_UniqueIdToPlayer		 = New BNetPlayer[65536]
			' Create UDP Port to Players list.
		m_UDPPortToPlayers		 = New TList[65536]
	EndMethod
	
		' Destructor
	Method Destroy()
			' Close sockets and delete remaining Data.
		Close()
		
			' Destroy packet queues.
		If m_UDPQueue Then
			m_UDPQueue.Destroy()
			m_UDPQueue = Null
		EndIf
		If m_TCPQueue Then
			m_TCPQueue.Destroy()
			m_TCPQueue = Null
		EndIf
		
			' Destroy buffer
		m_Buffer.Delete()
		m_Buffer = Null
		
			' Destroy UDP-Port to players list.
		If m_UDPPortToPlayers Then
			For Local l_UDPPort:Short = 0 To 65535
				If m_UDPPortToPlayers[l_UDPPort] Then
					m_UDPPortToPlayers[l_UDPPort].Clear()
					m_UDPPortToPlayers[l_UDPPort] = Null
				EndIf
			Next
			m_UDPPortToPlayers = Null
		EndIf
		
			' Destroy UID to Player mapping.
		If m_UniqueIdToPlayer Then 
			m_UniqueIdToPlayer = Null
		EndIf
		
			' Destroy Player list.
		If m_Players Then
			m_Players.Clear()
			m_Players = Null
		EndIf
	EndMethod
	
		' Open the Server so that players can connect.
		' @return: <Bool> True for success, otherwise False.
	Method Open:Int(p_TCPPort:Short, p_UDPPort:Short, p_Backlog:Int = 8)
			' Create missing Sockets.
		If Not m_TCPSocket Then m_TCPSocket = TSocket.CreateTCP()
		If Not m_UDPSocket Then m_UDPSocket = TSocket.CreateUDP()
		
			' Try binding the TCP Socket to the given port.
		If m_TCPSocket.Bind(p_TCPPort) Then
			m_TCPSocket.SetTCPNoDelay(True)
			m_TCPSocket.Listen(p_Backlog)
				' Try binding the UDP Socket to the given port.
			If m_UDPSocket.Bind(p_UDPPort) Then
				Return True
			EndIf
		EndIf
		
			' Otherwise just undo everything.
		Close()
		Return False
	EndMethod
	
		' Close the Server and disconnect all players.
	Method Close()
			' Kick all remaining players and destroy their data.
		For Local l_Player:BNetPlayer = EachIn m_Players
			l_Player.Kick("Shutting Down")
			l_Player.Destroy()
			l_Player = Null
		Next
		m_Players.Clear()
		
			' Clear Packet queues.
		m_TCPQueue.Clear()
		m_UDPQueue.Clear()
		
			' Close and destroy sockets.
		If m_UDPSocket Then
			m_UDPSocket.Close()
			m_UDPSocket = Null
		EndIf
		If m_TCPSocket Then
			m_TCPSocket.Close()
			m_TCPSocket = Null
		EndIf
	EndMethod
	
		' Update everything.
	Method Update()
		If m_TCPSocket And m_UDPSocket Then
				' Try to accept new players.
			Local l_NewPlayer:TSocket = m_TCPSocket.Accept(0)
			While l_NewPlayer <> Null
				
				Local l_UniqueId:Short
				If GetUnusedUniqueId(l_UniqueId) = True Then
					Local l_Player:BNetPlayer = BNetPlayer.Create(l_NewPlayer, l_UniqueId)
					
					m_UniqueIdToPlayer[lt_UniqueId] = l_Player
					m_Players.AddLast(l_Player)
				Else
					l_NewPlayer.Close()
				EndIf
				
					' Get next player.
				l_NewPlayer = m_TCPSocket.Accept(0)
			Wend
			
				' Retrieve UDP Packets and assign them to Players. (UDP is connectionless)
			Local l_Packet:BNetPacket	 = Null
			While Local l_RecvSize:Int = m_UDPSocket.Recv(m_Buffer._buf, m_Buffer._size)
			
				' Update all players.
			For Local l_Player:BNetPlayer = EachIn m_Players
				l_Player.Update()
			Next
		EndIf
	EndMethod
	
	Rem
		bbdoc: Tries to find an unused UniqueId and writes it into p_UniqueId.
		returns: <Byte> Successful
	EndRem
	Method GetUnusedUniqueId:Byte(p_UniqueId:Short Var)
		For Local l_UniqueId:Short = 0 To 65535
			If m_UniqueIdToPlayer[l_UniqueId] = Null Then
				p_UniqueId = l_UniqueId
				Return True
			EndIf
		Next
		Return False
	EndMethod
EndType

Type BNetPlayer
	'----------------------------------------------------------------
	'-- Variables / Members
	'----------------------------------------------------------------
		' Sockets
	Field m_TCPSocket:TSocket				 = Null
	Field m_UDPSocket:TSocket				 = Null
		' Queue
	Field m_TCPQueue:TIOQueue				 = Null
	Field m_UDPQueue:TIOQueue				 = Null
		' Player Information
	Field m_UniqueId:Short					 = 0
	Field m_Name:String						 = Null
	Field m_Version:Short					 = 0
	Field m_Position:Vector3F				 = Null
	Field m_Rotation:Vector3F				 = Null
	Field m_Velocity:Vector3F				 = Null
	Field m_Aim:Vector2F					 = Null
		' Historical Player Information (Used to calculate Data Flags)
	Field m_OldPosition:Vector3F			 = Null
	Field m_OldRotation:Vector3F			 = Null
	Field m_OldVelocity:Vector3F			 = Null
	Field m_OldAim:Vector2F					 = Null
	
		' Boolean Information
	Field mt_IsConnected:Byte				 = False
	Field mt_IsLoggedIn:Byte				 = False
	
	'----------------------------------------------------------------
	'-- Functions / Methods
	'----------------------------------------------------------------
	Function Create:BNetPlayer()
		Return (New BNetPlayer)
	End Function
	
		' Constructor
	Method New()
			' Create packet queues.
		m_TCPQueue		 = TIOQueue.Create()
		m_UDPQueue		 = TIOQueue.Create()
		
			' Create player information.
		m_Position		 = New Vector3F
		m_Rotation		 = New Vector3F
		m_Velocity		 = New Vector3F
		m_Aim			 = New Vector3F
		
			' Create historical player information.
		m_OldPosition	 = New Vector3F
		m_OldRotation	 = New Vector3F
		m_OldVelocity	 = New Vector3F
		m_OldAim		 = New Vector2F
	EndMethod
	
		' Destructor
	Method Destroy()
			' Destroy packet queues.
		If m_UDPQueue Then
			m_UDPQueue.Clear()
			m_UDPQueue = Null
		EndIf
		If m_TCPQueue Then
			For Local p_Packet:BNetPacket = EachIn m_TCPQueue.GetInQueue()
				
			Next
			m_TCPQueue.Clear()
			m_TCPQueue = Null
		EndIf
		
			' Destroy player information.
		m_Aim		 = Null
		m_Velocity	 = Null
		m_Rotation	 = Null
		m_Position	 = Null
		
			' Destroy historical player information.
		m_OldAim = Null
		m_OldVelocity = Null
		m_OldRotation = Null
		m_OldPosition = Null
		
			' Disconnect the player.
		Disconnect()
	EndMethod
	
	Method Connect(p_TCPSocket:TSocket, p_UniqueId:Short)
		m_TCPSocket = p_TCPSocket
		m_UniqueId = p_UniqueId
		
		m_IsConnected = True
	EndMethod
	
	Method Disconnect()
			' Close and destroy sockets.
		If m_UDPSocket Then
			m_UDPSocket.Close()
			m_UDPSocket = Null
		EndIf
		If m_TCPSocket Then
			m_TCPSocket.Close()
			m_TCPSocket = Null
		EndIf
		
		m_IsConnected = False
	EndMethod
	
	Method Login(p_LoginPacket:BNetPacketLogin)
		If m_TCPSocket Then
				' Create UDP Socket so that we can send data, and not only retrieve.
			m_UDPSocket = TSocket.CreateUDP()
			m_UDPSocket.Connect(m_TCPSocket.RemoteIp(), p_LoginPacket.m_UDPPort)
			
			m_IsLoggedIn = True
		EndIf
	EndMethod
	
	Method Logout()
		If m_TCPSocket Then
				' Create Logout Packet and add it to the TCPQueue
			Local l_Packet:BNetPacketLogout = BNetPacketLogout.Create()
			l_Packet.setUniqueId(m_UniqueId)
			m_TCPQueue.PushOut(l_Packet)
			
			m_UDPSocket.Close()
			m_UDPSocket = Null
			
			m_IsLoggedIn = False
		EndIf
	EndMethod
	
	Method Kick(p_Reason:String)
		If m_TCPSocket Then
			Local l_Packet:BNetPacketKick = BNetPacketKick.Create()
			l_Packet.setReason(p_Reason)
			m_TCPQueue.PushOut(l_Packet)
		EndIf
	EndMethod
	
	Method Update()
		
	EndMethod

	'----------------------------------------------------------------
	'-- Getters / Setters
	'----------------------------------------------------------------
	Method getPosition:Vector3F()
			' Return a clone of this Vector without bypassing GC.
		Local l_Position:Vector3F = New Vector3F
		MemCopy(Byte Ptr(l_Position), Byte Ptr(m_Position), SizeOf(m_Position))
		Return l_Position
	EndMethod
	
	Method setPosition(p_Position:Vector3F)
		MemCopy(Byte Ptr(m_OldPosition), Byte Ptr(m_Position), SizeOf(m_Position))
		MemCopy(Byte Ptr(m_Position), Byte Ptr(p_Position), SizeOf(m_Position))
	EndMethod
	
	Method getRotation:Vector3F()
			' Return a clone of this Vector without bypassing GC.
		Local l_Rotation:Vector3F = New Vector3F
		MemCopy(Byte Ptr(l_Rotation), Byte Ptr(m_Rotation), SizeOf(m_Rotation))
		Return l_Rotation
	EndMethod
	
	Method setRotation(p_Rotation:Vector3F)
		MemCopy(Byte Ptr(m_OldRotation), Byte Ptr(m_Rotation), SizeOf(m_Rotation))
		MemCopy(Byte Ptr(m_Rotation), Byte Ptr(p_Rotation), SizeOf(m_Rotation))
	EndMethod
	
	Method getVelocity:Vector3F()
			' Return a clone of this Vector without bypassing GC.
		Local l_Velocity:Vector3F = New Vector3F
		MemCopy(Byte Ptr(l_Velocity), Byte Ptr(m_Velocity), SizeOf(m_Velocity))
		Return l_Velocity
	EndMethod
	
	Method setVelocity(p_Velocity:Vector3F)
		MemCopy(Byte Ptr(m_OldVelocity), Byte Ptr(m_Velocity), SizeOf(m_Velocity))
		MemCopy(Byte Ptr(m_Velocity), Byte Ptr(p_Velocity), SizeOf(m_Velocity))
	EndMethod
	
	Method getAim:Vector2F()
			' Return a clone of this Vector without bypassing GC.
		Local l_Aim:Vector3F = New Vector2F
		MemCopy(Byte Ptr(l_Aim), Byte Ptr(m_Aim), SizeOf(m_Aim))
		Return l_Aim
	EndMethod
	
	Method setAim(p_Velocity:Vector2F)
		MemCopy(Byte Ptr(m_OldAim), Byte Ptr(m_Aim), SizeOf(m_Aim))
		MemCopy(Byte Ptr(m_Aim), Byte Ptr(p_Aim), SizeOf(m_Aim))
	EndMethod
End Type

'----------------------------------------------------------------
'-- Packet Types
'----------------------------------------------------------------
Type BNetPacket
	'----------------------------------------------------------------
	'-- Constants
	'----------------------------------------------------------------
		' Packet Ids, for identification of each one.
	Const ID_LOGIN:Byte		 = 0
	Const ID_LOGOUT:Byte	 = 1
	Const ID_KICK:Byte		 = 2
	Const ID_DATA:Byte		 = 3
	Const ID_ACTION:Byte	 = 4
	Const ID_INVALID:Byte	 = 255
	
	'----------------------------------------------------------------
	'-- Variables / Members
	'----------------------------------------------------------------
	Field m_Id:Byte
	
	'----------------------------------------------------------------
	'-- Functions / Methods
	'----------------------------------------------------------------
	Function CreateFromId:BNetPacket(l_PacketId:Byte)
			' Need to know the type of the packet to create
		Select l_PacketId
			Case ID_LOGIN
				Return BNetPacketLogin.Create(p_Buffer)
			Case ID_LOGOUT
				Return BNetPacketLogout.Create(p_Buffer)
			Case ID_KICK
				Return BNetPacketKick.Create(p_Buffer)
			Case ID_DATA
				Return BNetPacketData.Create(p_Buffer)
			Case ID_ACTION
				Return BNetPacketAction.Create(p_Buffer)
		End Select
		
			' If we don't have such a type, just do nothing.
		Return Null
	End Function
	
	Function CreateFromBuffer:BNetPacket(p_Buffer:TBank)
		Local l_PacketId:Byte = p_Buffer.PeekByte(0)
		
		Return Create(l_PacketId)
	End Function
	
		' Constructor
	Method New()
			' This Packet is Invalid.
		m_Id = ID_INVALID
	EndMethod
	
		' Destructor
	Method Destroy()
	EndMethod
	
		' Size of Packet
	Method GetSize:Int()
		Return 1
	EndMethod
	
		' Read the packet data from the buffer.
	Method Read:Int(p_Buffer:TBank)
		Return GetSize()
	EndMethod
	
		' Write the Packet data to the Buffer.
	Method Write:Int(p_Buffer:TBank)
			' Write Packet Id
		p_Buffer.PokeByte(0, m_Id)
		
		Return GetSize()
	EndMethod
End Type

Type BNetPacketLogin Extends BNetPacket
	'----------------------------------------------------------------
	'-- Variables / Members
	'----------------------------------------------------------------
		' UniqueId is sent by Server, UDPPort is sent by Client
	Field m_UnqiueId:Short, m_UDPPort:Short Ptr
		' Version the Client sent us.
	Field m_Version:Short
		' Name of the Client, truncated.
	Field m_Name:String
		' Initial Position and Rotation.
	Field m_Position:Vector3F
	Field m_Rotation:Vector3F
	
	'----------------------------------------------------------------
	'-- Functions / Methods
	'----------------------------------------------------------------
	Function Create:BNetPacket(p_Buffer:TBank)
		Local l_Packet:BNetPacketLogin = (New BNetPacketLogin)
		l_Packet.Read(p_Buffer)
		Return l_Packet
	End Function
	
		' Constructor
	Method New()
			' This is a Login packet.
		m_Id = BNetPacket.ID_LOGIN
		
			' UDPPort and UniqueId share the same Space in the Packet.
		m_UDPPort	 = VarPtr(m_UniqueId)
		
			' Create vector objects for position and rotation.
		m_Position	 = New Vector3F
		m_Rotation	 = New Vector3F
	EndMethod
	
		' Destructor
	Method Destroy()
			' Destroy vector objects.
		m_Rotation	 = Null
		m_Position	 = Null
		
			' Remove memory link for UDPPort so GC doesn't get confused.
		m_UDPPort	 = Null
	EndMethod
	
		' Size of Packet
	Method GetSize:Int()
		Return Super.GetSize() + 44
	EndMethod
	
		' Read the packet data from the buffer.
	Method Read:Int(p_Buffer:TBank)
		Local l_Offset:Int = Super.Read(p_Buffer)
		
			' Read UDPPort for punchthrough.
		m_UDPPort	 = p_Buffer.PeekShort(l_Offset)
		m_Version	 = p_Buffer.PeekShort(l_Offset + 2)
			' Read Player Name
		m_Name		 = ""
		For Local l_NamePos:Int = 0 Until 16
			Local l_Char:Byte = p_Buffer.PeekByte(l_Offset + 4 + l_NamePos)
			If l_Char < 32 Then Exit
			m_Name :+ Chr(l_Char)
		Next
			' Read initial position and rotation
		m_Position.X = p_Buffer.PeekFloat(l_Offset + 20)
		m_Position.Y = p_Buffer.PeekFloat(l_Offset + 24)
		m_Position.Z = p_Buffer.PeekFloat(l_Offset + 28)
		m_Rotation.X = p_Buffer.PeekFloat(l_Offset + 32)
		m_Rotation.Y = p_Buffer.PeekFloat(l_Offset + 36)
		m_Rotation.Z = p_Buffer.PeekFloat(l_Offset + 40)
		
		Return GetSize()
	EndMethod
	
		' Write the packet data to the buffer.
	Method Write:Int(p_Buffer:TBank)
		Local l_Offset:Int = Super.Write(p_Buffer)
		
			' Write chosen UniqueId
		p_Buffer.PokeShort(l_Offset, m_UniqueId)
		p_Buffer.PokeShort(l_Offset + 2, m_Version)
			' Write Player Name
		Local l_NameLength:Int		 = m_Name.length
		Local l_NameBytes:Byte Ptr	 = m_Name.ToCString()
		For Local l_NamePos:Int = 0 Until 16
				' Write name and automatically write 0 to the unused space.
			p_Buffer.PokeByte(l_Offset + 4 + l_NamePos, l_NameBytes[Min(l_NamePos, l_NameLength)])
		Next
		MemFree l_NameBytes
			' Write initial position and rotation
		p_Buffer.PokeFloat(l_Offset + 20, m_Position.X)
		p_Buffer.PokeFloat(l_Offset + 24, m_Position.Y)
		p_Buffer.PokeFloat(l_Offset + 28, m_Position.Z)
		p_Buffer.PokeFloat(l_Offset + 32, m_Rotation.X)
		p_Buffer.PokeFloat(l_Offset + 36, m_Rotation.Y)
		p_Buffer.PokeFloat(l_Offset + 40, m_Rotation.Z)
		
		Return GetSize()
	EndMethod

	'----------------------------------------------------------------
	'-- Getters / Setters
	'----------------------------------------------------------------
	Method getUniqueId:Short()
		Return m_UniqueId
	EndMethod
	
	Method setUniqueId(p_UniqueId:Short)
		m_UniqueId = p_UniqueId
	EndMethod
	
	Method getUDPPort:Short()
		Return m_UDPPort
	EndMethod
	
	Method setUDPPort(p_UDPPort:Short)
		m_UDPPort = p_UDPPort
	EndMethod
	
	Method getVersion:Byte[]()
		Return Byte[][m_Version Shr 8 & 255, m_Version & 255]
	EndMethod
	
	Method setVersion(p_Major:Byte, p_Minor:Byte)
		m_Version = p_Major Shl 8 + p_Minor
	EndMethod
	
	Method getName:String()
		Return m_Name
	EndMethod
	
	Method setName(p_Name:String)
		m_Name = p_Name
	EndMethod
	
	Method getPosition:Vector3F()
			' Return a clone of this Vector without bypassing GC.
		Local l_Position:Vector3F = New Vector3F
		MemCopy(Byte Ptr(l_Position), Byte Ptr(m_Position), SizeOf(m_Position))
		Return l_Position
	EndMethod
	
	Method setPosition(p_Position:Vector3F)
		MemCopy(Byte Ptr(m_Position), Byte Ptr(p_Position), SizeOf(m_Position))
	EndMethod
	
	Method getRotation:Vector3F()
			' Return a clone of this Vector without bypassing GC.
		Local l_Rotation:Vector3F = New Vector3F
		MemCopy(Byte Ptr(l_Rotation), Byte Ptr(m_Rotation), SizeOf(m_Rotation))
		Return l_Rotation
	EndMethod
	
	Method setRotation(p_Rotation:Vector3F)
		MemCopy(Byte Ptr(m_Rotation), Byte Ptr(p_Rotation), SizeOf(m_Rotation))
	EndMethod
End Type

Type BNetPacketLogout Extends BNetPacket
	'----------------------------------------------------------------
	'-- Variables / Members
	'----------------------------------------------------------------
		' UniqueId of the Client logging out.
	Field m_UnqiueId:Short
	
	'----------------------------------------------------------------
	'-- Functions / Methods
	'----------------------------------------------------------------
	Function Create:BNetPacket(p_Buffer:TBank)
		Return (New BNetPacketLogin).Read(p_Buffer)
	End Function
	
		' Constructor
	Method New()
			' This is a Login packet.
		m_Id = BNetPacket.ID_LOGIN
		
			' UDPPort and UniqueId share the same Space in the Packet.
		m_UDPPort	 = VarPtr(m_UniqueId)
		
			' Create vector objects for position and rotation.
		m_Position	 = New Vector3F
		m_Rotation	 = New Vector3F
	EndMethod
	
		' Destructor
	Method Destroy()
			' Destroy vector objects.
		m_Rotation	 = Null
		m_Position	 = Null
		
			' Remove memory link for UDPPort so GC doesn't get confused.
		m_UDPPort	 = Null
	EndMethod
	
		' Size of Packet
	Method GetSize:Int()
		Return Super.GetSize() + 44
	EndMethod
	
		' Read the packet data from the buffer.
	Method Read:Int(p_Buffer:TBank)
		Local l_Offset:Int = Super.Read(p_Buffer)
		
		m_UniqueId = p_Buffer.PeekShort(l_Offset)
		
		Return GetSize()
	EndMethod
	
		' Write the Packet data to the Buffer.
	Method Write:Int(p_Buffer:TBank)
		Local l_Offset:Int = Super.Write(p_Buffer)
		
			' Write UniqueId
		p_Buffer.PokeShort(l_Offset, m_UniqueId)
		
		Return GetSize()
	EndMethod
	
	'----------------------------------------------------------------
	'-- Getters / Setters
	'----------------------------------------------------------------
	Method getUniqueId:Short()
		Return m_UniqueId
	EndMethod
	
	Method setUniqueId(p_UniqueId:Short)
		m_UniqueId = p_UniqueId
	EndMethod
End Type

Type BNetPacketKick Extends BNetPacket
	'----------------------------------------------------------------
	'-- Variables / Members
	'----------------------------------------------------------------
	Field m_Reason:String
	
	'----------------------------------------------------------------
	'-- Functions / Methods
	'----------------------------------------------------------------
	Function Create:BNetPacket(p_Buffer:TBank)
		Return (New BNetPacketKick).Read(p_Buffer)
	End Function
	
		' Constructor
	Method New()
			' This is a Login packet.
		m_Id = BNetPacket.ID_KICK
		
			' Create String object for Reason.
		m_Reason = ""
	EndMethod
	
		' Destructor
	Method Destroy()
			' Remove String object for Reason
		m_Reason = Null
	EndMethod
	
		' Size of Packet
	Method GetSize:Int()
		Return Super.GetSize() + 2 + m_Reason.length
	EndMethod
	
		' Read the packet data from the buffer.
	Method Read:Int(p_Buffer:TBank)
		Local l_Offset:Int = Super.Read(p_Buffer)
		
		Local l_ReasonLength:Short = p_Buffer.PeekShort(l_Offset)
		For Local l_ReasonPos:Short = 0 Until l_ReasonLength
			m_Reason = Chr(p_Buffer.PeekByte(l_Offset + 2 + l_ReasonPos))
		Next
		
		Return GetSize()
	EndMethod
	
		' Write the Packet data to the Buffer.
	Method Write:Int(p_Buffer:TBank)
		Local l_Offset:Int = Super.Write(p_Buffer)
		
			' Write reason length
		Local l_ReasonLength:Short	 = m_Reason.length
		p_Buffer.PokeShort(l_Offset, l_ReasonLength)
			' Write kick reason.
		Local l_ReasonBytes:Byte Ptr = m_Reason.ToCString()
		For Local l_ReasonPos:Short = 0 Until l_ReasonLength
			p_Buffer.PokeByte(l_Offset + 2 + l_ReasonPos, l_ReasonBytes[l_ReasonPos])
		Next
		MemFree l_ReasonBytes
		
		Return GetSize()
	EndMethod
	
	'----------------------------------------------------------------
	'-- Getters / Setters
	'----------------------------------------------------------------
	Method getReason:String()
		Return m_Reason
	EndMethod
	
	Method setReason(p_Reason:String)
		m_Reason = p_Reason
	EndMethod
End Type

Type BNetPacketData Extends BNetPacket
	'----------------------------------------------------------------
	'-- Variables / Members
	'----------------------------------------------------------------
	Field m_UniqueId:Short
	Field m_Flags:Byte
	Field m_Position:Vector3F
	Field m_Rotation:Vector3F
	Field m_Velocity:Vector3F
	Field m_Aim:Vector2F
	
	'----------------------------------------------------------------
	'-- Functions / Methods
	'----------------------------------------------------------------
	Function Create:BNetPacket(p_Buffer:TBank)
		Return (New BNetPacketData).Read(p_Buffer)
	End Function
	
		' Constructor
	Method New()
			' This is a Login packet.
		m_Id = BNetPacket.ID_DATA
		
			' Create Vector objects for storage
		m_Position	 = New Vector3F
		m_Rotation	 = New Vector3F
		m_Velocity	 = New Vector3F
		m_Aim		 = New Vector2F
		
			' Set Default Data
		m_UniqueId	 = 0
		m_Flags		 = 0
	EndMethod
	
		' Destructor
	Method Destroy()
			' Release vector object references
		m_Position	 = Null
		m_Rotation	 = Null
		m_Velocity	 = Null
		m_Aim		 = Null
	EndMethod
	
		' Size of Packet
	Method GetSize:Int()
		Local l_Size:Int = 1
		
		If m_Flags & BNET_DATAFLAG_POSITIONX Then l_Size :+ 4
		If m_Flags & BNET_DATAFLAG_POSITIONY Then l_Size :+ 4
		If m_Flags & BNET_DATAFLAG_POSITIONZ Then l_Size :+ 4
		If m_Flags & BNET_DATAFLAG_ROTATIONX Then l_Size :+ 2
		If m_Flags & BNET_DATAFLAG_ROTATIONY Then l_Size :+ 2
		If m_Flags & BNET_DATAFLAG_ROTATIONZ Then l_Size :+ 2
		If m_Flags & BNET_DATAFLAG_VELOCITY Then l_Size :+ 6
		If m_Flags & BNET_DATAFLAG_AIM Then l_Size :+ 4
		
		Return Super.GetSize() + l_Size
	EndMethod

		' Read the packet data from the buffer.
	Method Read:Int(p_Buffer:TBank)
		Local l_Offset:Int = Super.Read(p_Buffer)
		
		m_Flags = p_Buffer.PeekByte(l_Offset);l_Offset :+ 1
		If m_Flags & BNET_DATAFLAG_POSITIONX Then
			m_Position.X = p_Buffer.PeekFloat(l_Offset)
			l_Offset :+ 4
		EndIf
		If m_Flags & BNET_DATAFLAG_POSITIONY Then
			m_Position.Y = p_Buffer.PeekFloat(l_Offset)
			l_Offset :+ 4
		EndIf
		If m_Flags & BNET_DATAFLAG_POSITIONZ Then
			m_Position.Z = p_Buffer.PeekFloat(l_Offset)
			l_Offset :+ 4
		EndIf
		If m_Flags & BNET_DATAFLAG_ROTATIONX Then
			m_Rotation.X = p_Buffer.PeekShort(l_Offset) / 65536.0 * 360.0
			l_Offset :+ 2
		EndIf
		If m_Flags & BNET_DATAFLAG_ROTATIONY Then
			m_Rotation.Y = p_Buffer.PeekShort(l_Offset) / 65536.0 * 360.0
			l_Offset :+ 2
		EndIf
		If m_Flags & BNET_DATAFLAG_ROTATIONZ Then
			m_Rotation.Z = p_Buffer.PeekShort(l_Offset) / 65536.0 * 360.0
			l_Offset :+ 2
		EndIf
		If m_Flags & BNET_DATAFLAG_VELOCITY Then
			m_Rotation.X = (p_Buffer.PeekShort(l_Offset + 0) / 65536.0) * 256.0
			m_Rotation.Y = (p_Buffer.PeekShort(l_Offset + 2) / 65536.0) * 256.0
			m_Rotation.Z = (p_Buffer.PeekShort(l_Offset + 4) / 65536.0) * 256.0
			l_Offset :+ 6
		EndIf
		If m_Flags & BNET_DATAFLAG_AIM Then
			m_Rotation.X = (p_Buffer.PeekShort(l_Offset + 0) / 65536.0) * 360.0
			m_Rotation.Y = (p_Buffer.PeekShort(l_Offset + 2) / 65536.0) * 360.0
			l_Offset :+ 4
		EndIf
		
		Return GetSize()
	EndMethod
	
		' Write the Packet data to the Buffer.
	Method Write:Int(p_Buffer:TBank)
		Local l_Offset:Int = Super.Write(p_Buffer)
		
		If m_Flags & BNET_DATAFLAG_POSITIONX Then
			p_Buffer.PeekFloat(l_Offset, m_Position.X)
			l_Offset :+ 4
		EndIf
		If m_Flags & BNET_DATAFLAG_POSITIONY Then
			p_Buffer.PeekFloat(l_Offset, m_Position.Y)
			l_Offset :+ 4
		EndIf
		If m_Flags & BNET_DATAFLAG_POSITIONZ Then
			p_Buffer.PeekFloat(l_Offset, m_Position.Z)
			l_Offset :+ 4
		EndIf
		If m_Flags & BNET_DATAFLAG_ROTATIONX Then
			p_Buffer.PokeShort(l_Offset, Int((m_Rotation.X / 360.0) * 65536))
			l_Offset :+ 2
		EndIf
		If m_Flags & BNET_DATAFLAG_ROTATIONY Then
			p_Buffer.PokeShort(l_Offset, Int((m_Rotation.Y / 360.0) * 65536))
			l_Offset :+ 2
		EndIf
		If m_Flags & BNET_DATAFLAG_ROTATIONZ Then
			p_Buffer.PokeShort(l_Offset, Int((m_Rotation.Z / 360.0) * 65536))
			l_Offset :+ 2
		EndIf
		If m_Flags & BNET_DATAFLAG_VELOCITY Then
			p_Buffer.PokeShort(l_Offset, Int((m_Velocity.X / 256.0) * 65536))
			p_Buffer.PokeShort(l_Offset, Int((m_Velocity.Y / 256.0) * 65536))
			p_Buffer.PokeShort(l_Offset, Int((m_Velocity.Z / 256.0) * 65536))
			l_Offset :+ 6
		EndIf
		If m_Flags & BNET_DATAFLAG_AIM Then
			p_Buffer.PokeShort(l_Offset, Int((m_Aim.X / 360.0) * 65536))
			p_Buffer.PokeShort(l_Offset, Int((m_Aim.Y / 360.0) * 65536))
			l_Offset :+ 4
		EndIf
		
		Return GetSize()
	EndMethod
	
	'----------------------------------------------------------------
	'-- Getters / Setters
	'----------------------------------------------------------------
	Method getUniqueId:Short()
		Return m_UniqueId
	EndMethod
	
	Method setUniqueId(p_UniqueId:Short)
		m_UniqueId = p_UniqueId
	EndMethod
	
	Method getFlags:Byte()
		Return m_Flags
	EndMethod
	
	Method setFlags(p_Flags:Byte)
		m_Flags = p_Flags
	EndMethod
	
	Method getPosition:Vector3F()
			' Return a clone of this Vector without bypassing GC.
		Local l_Position:Vector3F = New Vector3F
		MemCopy(Byte Ptr(l_Position), Byte Ptr(m_Position), SizeOf(m_Position))
		Return l_Position
	EndMethod
	
	Method setPosition(p_Position:Vector3F)
		MemCopy(Byte Ptr(m_Position), Byte Ptr(p_Position), SizeOf(m_Position))
	EndMethod
	
	Method getRotation:Vector3F()
			' Return a clone of this Vector without bypassing GC.
		Local l_Rotation:Vector3F = New Vector3F
		MemCopy(Byte Ptr(l_Rotation), Byte Ptr(m_Rotation), SizeOf(m_Rotation))
		Return l_Rotation
	EndMethod
	
	Method setRotation(p_Rotation:Vector3F)
		MemCopy(Byte Ptr(m_Rotation), Byte Ptr(p_Rotation), SizeOf(m_Rotation))
	EndMethod
	
	Method getVelocity:Vector3F()
			' Return a clone of this Vector without bypassing GC.
		Local l_Velocity:Vector3F = New Vector3F
		MemCopy(Byte Ptr(l_Velocity), Byte Ptr(m_Velocity), SizeOf(m_Velocity))
		Return l_Velocity
	EndMethod
	
	Method setVelocity(p_Velocity:Vector3F)
		MemCopy(Byte Ptr(m_Velocity), Byte Ptr(p_Velocity), SizeOf(m_Velocity))
	EndMethod
	
	Method getAim:Vector2F()
			' Return a clone of this Vector without bypassing GC.
		Local l_Aim:Vector3F = New Vector2F
		MemCopy(Byte Ptr(l_Aim), Byte Ptr(m_Aim), SizeOf(m_Aim))
		Return l_Aim
	EndMethod
	
	Method setAim(p_Aim:Vector2F)
		MemCopy(Byte Ptr(m_Aim), Byte Ptr(p_Aim), SizeOf(m_Aim))
	EndMethod
	
	Method setFromPlayer(p_Player:BNetPlayer Var)
		setUniqueId(p_Player.m_UniqueId)
		setPosition(p_Player.m_Position)
		setRotation(p_Player.m_Rotation)
		setVelocity(p_Player.m_Velocity)
		setAim(p_Player.m_Aim)
		
		' Calculate Flags
		m_Flags = 0
		If Abs(p_Player.m_Position.X - p_Player.m_OldPosition.X) > BNET_THRESHOLD_POSITION Then m_Flags :| BNET_DATAFLAG_POSITIONX
		If Abs(p_Player.m_Position.Y - p_Player.m_OldPosition.Y) > BNET_THRESHOLD_POSITION Then m_Flags :| BNET_DATAFLAG_POSITIONY
		If Abs(p_Player.m_Position.Z - p_Player.m_OldPosition.Z) > BNET_THRESHOLD_POSITION Then m_Flags :| BNET_DATAFLAG_POSITIONZ
		If Abs(p_Player.m_Rotation.X - p_Player.m_OldRotation.X) > BNET_THRESHOLD_ROTATION Then m_Flags :| BNET_DATAFLAG_ROTATIONX
		If Abs(p_Player.m_Rotation.Y - p_Player.m_OldRotation.Y) > BNET_THRESHOLD_ROTATION Then m_Flags :| BNET_DATAFLAG_ROTATIONY
		If Abs(p_Player.m_Rotation.Z - p_Player.m_OldRotation.Z) > BNET_THRESHOLD_ROTATION Then m_Flags :| BNET_DATAFLAG_ROTATIONZ
		If Abs(p_Player.m_Velocity.X - p_Player.m_OldVelocity.X) > BNET_THRESHOLD_VELOCITY Then m_Flags :| BNET_DATAFLAG_VELOCITY
		If Abs(p_Player.m_Velocity.Y - p_Player.m_OldVelocity.Y) > BNET_THRESHOLD_VELOCITY Then m_Flags :| BNET_DATAFLAG_VELOCITY
		If Abs(p_Player.m_Velocity.Z - p_Player.m_OldVelocity.Z) > BNET_THRESHOLD_VELOCITY Then m_Flags :| BNET_DATAFLAG_VELOCITY
		If Abs(p_Player.m_Aim.X - p_Player.m_OldAim.X) > BNET_THRESHOLD_AIM Then m_Flags :| BNET_DATAFLAG_AIM
		If Abs(p_Player.m_Aim.Y - p_Player.m_OldAim.Y) > BNET_THRESHOLD_AIM Then m_Flags :| BNET_DATAFLAG_AIM
	EndMethod
	
	Method setToPlayer(p_Player:BNetPlayer Var)
			' Don't update information for the wrong player.
		If m_UniqueId <> p_Player.m_UniqueId Then Return
		
			' Only update those parts that we recieved.
		If m_Flags & BNET_DATAFLAG_POSITIONX Then p_Player.m_OldPosition.X = p_Player.m_Position.X; p_Player.m_Position.X = m_Position.X
		If m_Flags & BNET_DATAFLAG_POSITIONY Then p_Player.m_OldPosition.Y = p_Player.m_Position.Y; p_Player.m_Position.Y = m_Position.Y
		If m_Flags & BNET_DATAFLAG_POSITIONZ Then p_Player.m_OldPosition.Z = p_Player.m_Position.Z; p_Player.m_Position.Z = m_Position.Z
		If m_Flags & BNET_DATAFLAG_ROTATIONX Then p_Player.m_OldRotation.X = p_Player.m_Rotation.X; p_Player.m_Rotation.X = m_Rotation.X
		If m_Flags & BNET_DATAFLAG_ROTATIONY Then p_Player.m_OldRotation.Y = p_Player.m_Rotation.Y; p_Player.m_Rotation.Y = m_Rotation.Y
		If m_Flags & BNET_DATAFLAG_ROTATIONZ Then p_Player.m_OldRotation.Z = p_Player.m_Rotation.Z; p_Player.m_Rotation.Z = m_Rotation.Z
		If m_Flags & BNET_DATAFLAG_VELOCITY Then
			p_Player.m_OldRotation.X = p_Player.m_Rotation.X
			p_Player.m_OldRotation.Y = p_Player.m_Rotation.Y
			p_Player.m_OldRotation.Z = p_Player.m_Rotation.Z
			
			p_Player.m_Rotation.X = m_Rotation.X
			p_Player.m_Rotation.Y = m_Rotation.Y
			p_Player.m_Rotation.Z = m_Rotation.Z
		EndIf
		If m_Flags & BNET_DATAFLAG_AIM Then
			p_Player.m_OldAim.X = p_Player.m_Aim.X
			p_Player.m_OldAim.Y = p_Player.m_Aim.Y
			
			p_Player.m_Aim.X = m_Aim.X
			p_Player.m_Aim.Y = m_Aim.Y
		EndIf
	EndMethod
End Type

Type BNetPacketAction Extends BNetPacket
	'----------------------------------------------------------------
	'-- Variables / Members
	'----------------------------------------------------------------
	
	'----------------------------------------------------------------
	'-- Functions / Methods
	'----------------------------------------------------------------
End Type

'----------------------------------------------------------------
'-- Storage Types
'----------------------------------------------------------------
Type Vector2F
	Field X:Float
	Field Y:Float
End Type

Type Vector3F Extends Vector2F
	Field Z:Float
End Type

Type Vector4F Extends Vector3F
	Field Z:Float
End Type

