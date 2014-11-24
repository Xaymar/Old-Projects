SuperStrict

'--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
Framework BRL.Blitz
Import BRL.Threads
Import BRL.Timer

Import "BlitzNet.bmx"
'--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

	' Fix GC weirdness
OnEnd(GCCollect)

Local Server:BNetServer = New BNetServer
Server.Open(27000, 27001, 8)
While
	Server.Update()
Wend
Server.Close()



'----------------------------------------------------------------
'-- Types
'----------------------------------------------------------------
'Type Server
'	Field m_Server:BNetServer
'End Type

Rem
Const VersionMajor:Byte = 0
Const VersionMinor:Byte = 40
Const Net_VersionMajor:Byte = 0
Const Net_VersionMinor:Byte = 1

' Network Initiation
Global InfoSocket:TSocket = TSocket.CreateTCP()
Global DataSocket:TSocket = TSocket.CreateUDP()

' Set up Information Socket.
Const SOCKET_INFO_PORT:Short = 27000
Const SOCKET_DATA_PORT:Short = 27001
If InfoSocket.Bind(SOCKET_INFO_PORT) Then
	InfoSocket.SetTCPNoDelay(True)
Else
	InfoSocket.Close()
	Print "[ERR] Unable to bind Information Socket to port " + SOCKET_INFO_PORT + ". Make sure it is not in use."
	End
EndIf
InfoSocket.Listen(128)

' Set up Data Socket.
If Not DataSocket.Bind(SOCKET_DATA_PORT) Then
	DataSocket.Close()
	Print "[ERR] Unable to bind Data Socket to port " + SOCKET_DATA_PORT + ". Make sure it is not in use."
	End
EndIf
'END OF: Network Initiation

' Timers
Const DataTicks:Int		 = 5
Local l_LoopTimer:TTimer = TTimer.Create(120)

' Network Packet Buffer
Const NetPacketBufferSize:Int			 = 64
Local NetPacketBuffer:TBank				 = TBank.Create(NetPacketBufferSize)
Local NetPacketBufferStream:TBankStream	 = TBankStream.Create(NetPacketBuffer)
Local NetPacketQueueTCP:TList		 = (New TList)
Local NetPacketQueueTCPSwap:TList	 = (New TList)
Local NetPacketQueueUDP:TList		 = (New TList)
Local NetPacketQueueUDPSwap:TList	 = (New TList)

Repeat
	Local l_LoopTime:Int = MilliSecs()
	
	' Check for data on InfoSockets {
		' Check for new connections. {
		Local l_NewSocket:TSocket
		Repeat
			l_NewSocket = InfoSocket.Accept(0)
			
			If l_NewSocket <> Null Then
				Local l_NewPlayer:TPlayer = TPlayer.Create(l_NewSocket)
				If l_NewPlayer <> Null Then
					Print RSet(l_NewPlayer.m_UniqueId, 6) + ": Connected from " + DottedIP(l_NewSocket.RemoteIp()) + ":" + l_NewSocket.RemotePort() + "."
				Else
					l_NewSocket.Close()
					Print "Server: New player tried connecting, but we are full."
				EndIf
			EndIf
		Until l_NewSocket = Null
		' }
		
		' Check if a pregame client sent his login information {
		For Local l_PGPlayer:TPlayer = EachIn TPlayer.PreGame
			If l_PGPlayer.m_InfoSocket.Connected() = False Then
				Print RSet(l_PGPlayer.m_UniqueId, 6) + ": Closed connection."
				l_PGPlayer.Destroy();Continue
			EndIf
			
			While l_PGPlayer.m_InfoSocket.ReadAvail() > 0
				Local l_ReadLength:Int = l_PGPlayer.m_InfoSocket.Recv(NetPacketBuffer._buf, 1);NetPacketBufferStream.Seek(0)
				If l_ReadLength = 1
					Local l_PacketId:Byte = NetPacketBufferStream.ReadByte()
					Select l_PacketId
						Case TInfoLogin.Id
							l_PGPlayer.m_InfoSocket.Recv(NetPacketBuffer._buf, TInfoLogin.Size);NetPacketBufferStream.Seek(0)
							Local l_InfoLogin:TInfoLogin = TInfoLogin(TInfoLogin.Read(NetPacketBufferStream))
							
							If l_InfoLogin Then 
								' Retrieve Information from Packet
								l_PGPlayer.m_Name = l_InfoLogin.m_Name
								l_PGPlayer.m_Position[0] = l_InfoLogin.m_Position[0]
								l_PGPlayer.m_Position[1] = l_InfoLogin.m_Position[1]
								l_PGPlayer.m_Position[2] = l_InfoLogin.m_Position[2]
								
								' Create UDP Socket for Client.
								l_PGPlayer.m_DataSocket = TSocket.CreateUDP()
								l_PGPlayer.m_DataSocket.Connect(l_PGPlayer.m_InfoSocket.RemoteIp(), l_InfoLogin.m_PunchPort)
								
								' Send information to this client (order is important!).
								Local l_InfoLogin:TInfoLogin = (New TInfoLogin)
								l_InfoLogin.m_UniqueId = l_PGPlayer.m_UniqueId
								l_InfoLogin.m_Name = l_PGPlayer.m_Name
								
								'Local l_InfoUpdate:TInfoUpdate = (New TInfoUpdate)
								'l_InfoUpdate.m_UniqueId = l_PGPlayer.m_UniqueId
								'l_InfoUpdate.m_Name = l_PGPlayer.m_Name
								
								'NetPacketBufferStream.Seek(0)
								'Local l_Size:Int = l_InfoUpdate.Write(NetPacketBufferStream)
								'l_PGPlayer.m_InfoSocket.Send(NetPacketBuffer._buf, l_Size)
								
								' Add Player to ingame list.
								TPlayer.PreGame.Remove(l_PGPlayer)
								TPlayer.InGame.AddLast(l_PGPlayer)
								Print RSet(l_PGPlayer.m_UniqueId, 6) + ": Logged in at " + l_PGPlayer.m_Position[0] + ":" + l_PGPlayer.m_Position[1] + ":" + l_PGPlayer.m_Position[2] + "."
							Else
								Print RSet(l_PGPlayer.m_UniqueId, 6) + ": CheckSum did not match with ours, Client is probably outdated."
								l_PGPlayer.Destroy();Exit
							EndIf
					EndSelect
				Else
					Print RSet(l_PGPlayer.m_UniqueId, 6) + ": Unable to read first byte, socket might be corrupted. Dropping client."
					l_PGPlayer.Destroy();Exit
				EndIf
			Wend
			
			' If we still have no login packet, check if they are supposed to be dropped by timeout.
			If l_PGPlayer.m_InfoSocket And (l_LoopTime - l_PGPlayer.m_Time) > 5000 Then
				Print RSet(l_PGPlayer.m_UniqueId, 6) + ": Did not login, dropped."
				l_PGPlayer.Destroy();Continue
			EndIf
		Next
		' }
		
		' Check if an ingame client sent packets. {
		While DataSocket.ReadAvail() > 0
			Local l_ReadLength:Int = DataSocket.Recv(NetPacketBuffer._buf, 1);NetPacketBufferStream.Seek(0)
			If l_ReadLength = 1
				Local l_PacketId:Byte = NetPacketBufferStream.ReadByte()
				
				Select l_PacketId
					Case TDataUpdate.Id
						l_ReadLength = DataSocket.Recv(NetPacketBuffer._buf, NetPacketBufferSize)
						Local l_DataUpdate:TDataUpdate = TDataUpdate(TDataUpdate.Read(NetPacketBufferStream))
						
						Local l_Player:TPlayer = TPlayer.UIDToPlayer[l_DataUpdate.m_UniqueId]
						If l_Player And l_Player.m_DataSocket Then ' Player exists and is logged in.
							If DataSocket.RemoteIp() = l_Player.m_DataSocket.RemoteIp() And DataSocket.RemotePort() = l_Player.m_DataSocket.RemotePort() Then ' It is the same player.
								If l_DataUpdate.m_Changed & TPlayer.CHANGED_POSX <> 0 Then l_Player.m_Position[0] = l_DataUpdate.m_Position[0]
								If l_DataUpdate.m_Changed & TPlayer.CHANGED_POSY <> 0 Then l_Player.m_Position[1] = l_DataUpdate.m_Position[1]
								If l_DataUpdate.m_Changed & TPlayer.CHANGED_POSZ <> 0 Then l_Player.m_Position[2] = l_DataUpdate.m_Position[2]
								If l_DataUpdate.m_Changed & TPlayer.CHANGED_ROTX <> 0 Then l_Player.m_Rotation[0] = l_DataUpdate.m_Rotation[0]
								If l_DataUpdate.m_Changed & TPlayer.CHANGED_ROTY <> 0 Then l_Player.m_Rotation[1] = l_DataUpdate.m_Rotation[1]
								If l_DataUpdate.m_Changed & TPlayer.CHANGED_ROTZ <> 0 Then l_Player.m_Rotation[2] = l_DataUpdate.m_Rotation[1]
								If l_DataUpdate.m_Changed & TPlayer.CHANGED_VEL <> 0 Then
									l_Player.m_Velocity[0] = l_DataUpdate.m_Velocity[0]
									l_Player.m_Velocity[1] = l_DataUpdate.m_Velocity[1]
									l_Player.m_Velocity[2] = l_DataUpdate.m_Velocity[2]
								EndIf
							Else
								Print "Server: Recieved update for another player from " + DottedIP(DataSocket.RemoteIp()) + ":" + DataSocket.RemotePort() + "."
							EndIf
						EndIf
				EndSelect
			EndIf
		Wend
		' }
		
		' Check if an ingame client sent packets or packets need sending. {
		For Local l_IGPlayer:TPlayer = EachIn TPlayer.InGame
			Local l_Skip:Byte = False
			If l_IGPlayer = Null Then Print "Fatal Error: Ran into ingame client, which apparently doesn't exist. Skipping."; Continue
			
			' Check if the Player still has his TCP Socket open, otherwise drop him.
			If l_IGPlayer.m_InfoSocket.Connected() = False Then
				Print RSet(l_IGPlayer.m_UniqueId, 6) + ": Closed connection."
				
				' Create InfoLogout packet.
				Local l_InfoLogout:TInfoLogout = New TInfoLogout
				l_InfoLogout.m_UniqueId = l_IGPlayer.m_UniqueId
				
				' Remove the player from nearby other players.
				NetPacketBufferStream.Seek(0)
				Local l_Size:Int = l_InfoLogout.Write(NetPacketBufferStream)
				For Local l_IGPlayerKnown:TPlayer = EachIn l_IGPlayer.__KnownPlayers:TList
					l_IGPlayerKnown.m_InfoSocket.Send(NetPacketBuffer._buf, l_Size)
					l_IGPlayerKnown.__KnownPlayers.Remove(l_IGPlayer)
				Next
				
				l_IGPlayer.Destroy();Continue
			EndIf
			
			' TCP: Do we have any incoming data from this player?
			While l_IGPlayer.m_InfoSocket.ReadAvail() > 0
				Local l_ReadLength:Int = l_IGPlayer.m_InfoSocket.Recv(NetPacketBuffer._buf, 1);NetPacketBufferStream.Seek(0)
				If l_ReadLength = 1
					Local l_PacketId:Byte = NetPacketBufferStream.ReadByte()
					NetPacketBufferStream.Seek(0)
					
					Select l_PacketId
						Case TInfoLogin.Id
							l_IGPlayer.m_InfoSocket.Recv(NetPacketBuffer._buf, TInfoLogin.Size)
						Case TInfoLogout.Id
							l_IGPlayer.m_InfoSocket.Recv(NetPacketBuffer._buf, TInfoLogout.Size)
							
							Local l_InfoLogout:TInfoLogout = TInfoLogout(TInfoLogout.Read(NetPacketBufferStream))
							l_InfoLogout.m_UniqueId = l_IGPlayer.m_UniqueId
							
							' Send information to other clients.
							NetPacketQueueTCPSwap.AddLast(l_InfoLogout)
							
							Print RSet(l_IGPlayer.m_UniqueId, 6) + ": Logged out."
							l_IGPlayer.Destroy();l_Skip = True;Exit
					EndSelect
				EndIf
			Wend
			If l_Skip = True Then Continue
			
			' TCP: Do we have any data for this player? If so then send it out.
			If NetPacketQueueTCP.Count() > 0 Then
				For Local l_Packet:TNetPacket = EachIn NetPacketQueueTCP
					NetPacketBufferStream.Seek(0)
					l_IGPlayer.m_InfoSocket.Send(NetPacketBuffer._buf, l_Packet.Write(NetPacketBufferStream))
				Next
			EndIf
			
			' UDP: If a DataUpdate is needed, send it.
			If l_IGPlayer.m_TicksOnline Mod DataTicks Then
				For Local l_IGPlayer2:TPlayer = EachIn TPlayer.InGame
					If l_IGPlayer2 = l_IGPlayer Then Continue
					
					If l_IGPlayer.IsInRange(l_IGPlayer2) Then ' Is this player even in range? If yes, continue
						If Not l_IGPlayer.IsKnown(l_IGPlayer2) Then ' This client is new to him, need to introduce him first.
							Local l_InfoLogin:TInfoLogin = (New TInfoLogin)
							l_InfoLogin.m_UniqueId = l_IGPlayer2.m_UniqueId
							l_InfoLogin.m_Name = l_IGPlayer2.m_Name
							l_InfoLogin.m_Position[0] = l_IGPlayer2.m_Position[0]
							l_InfoLogin.m_Position[1] = l_IGPlayer2.m_Position[1]
							l_InfoLogin.m_Position[2] = l_IGPlayer2.m_Position[2]
							l_InfoLogin.m_Rotation[0] = l_IGPlayer2.m_Rotation[0]
							l_InfoLogin.m_Rotation[1] = l_IGPlayer2.m_Rotation[1]
							l_InfoLogin.m_Rotation[2] = l_IGPlayer2.m_Rotation[2]
							
							NetPacketBufferStream.Seek(0)
							l_IGPlayer.m_InfoSocket.Send(NetPacketBuffer._buf, l_InfoLogin.Write(NetPacketBufferStream))
						EndIf
						
						Local l_DataUpdate:TDataUpdate = l_IGPlayer.GetDataUpdate(l_IGPlayer)
						If l_DataUpdate Then
							NetPacketBufferStream.Seek(0)
							l_IGPlayer.m_DataSocket.Send(NetPacketBuffer._buf, l_DataUpdate.Write(NetPacketBufferStream))
						EndIf
					Else
						If l_IGPlayer.IsKnownEx(l_IGPlayer2) Then ' This client is known to him, need to remove him.
							Local l_InfoLogout:TInfoLogout = (New TInfoLogout)
							l_InfoLogout.m_UniqueId = l_IGPlayer2.m_UniqueId
							
							NetPacketBufferStream.Seek(0)
							l_IGPlayer.m_InfoSocket.Send(NetPacketBuffer._buf, l_InfoLogout.Write(NetPacketBufferStream))
						EndIf
					EndIf
				Next
			EndIf
			
			' UDP: Do we have any data for this player? If so then send it out.
			If NetPacketQueueUDP.Count() > 0 Then
				For Local l_Packet:TNetPacket = EachIn NetPacketQueueUDP
					NetPacketBufferStream.Seek(0)
					l_IGPlayer.m_DataSocket.Send(NetPacketBuffer._buf, l_Packet.Write(NetPacketBufferStream))
				Next
			EndIf
			
			l_IGPlayer.m_TicksOnline :+ 1
		Next
		' }
	' }
	
	
	' Swap Network Packet Queue.
	NetPacketQueueTCP.Swap(NetPacketQueueTCPSwap)
	NetPacketQueueUDP.Swap(NetPacketQueueUDPSwap)
	NetPacketQueueTCPSwap.Clear()
	NetPacketQueueUDPSwap.Clear()
	
	l_LoopTimer.Wait()
Until False
DataSocket.Close()
InfoSocket.Close()
End

'--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
Type TPlayer
	'! Array that tells us if an Id is in use. Toggle the bit you were using if an object is created or dies.
	Global UniqueIds:Byte[]		 = (New Byte[8192])
	Global UIDToPlayer:TPlayer[] = (New TPlayer[65536])
	'! List of active players
	Global PreGame:TList = (New TList)
	Global InGame:TList	 = (New TList)
	
	' Constants {
		Const NAME_LENGTH:Int = 16
		
		Const CHANGED_POSX:Byte	 = $00000001
		Const CHANGED_POSY:Byte	 = $00000010
		Const CHANGED_POSZ:Byte	 = $00000100
		Const CHANGED_ROTX:Byte	 = $00001000
		Const CHANGED_ROTY:Byte	 = $00010000
		Const CHANGED_ROTZ:Byte	 = $00100000
		Const CHANGED_VEL:Byte	 = $01000000
		Const CHANGED_SHOOT:Byte = $10000000
	' }
	
	' Identification
	Field m_UniqueId:Short
	Field m_Name:Byte[]
	
	' Ship Data
	Field m_Changed:Byte
	Field m_Position:Float[]
	Field m_Rotation:Short[]
	Field m_Velocity:Short[]
	
	' Network Data
	Field m_InfoSocket:TSocket
	Field m_DataSocket:TSocket
	Field m_Time:Int
	Field m_TicksOnline:Int
	
	' Initialize and Create new Players {
	Method New()
		' Some Network Data
		m_Time		  = MilliSecs()
		m_TicksOnline = 0
		
		' Identification
		m_Name		 = New Byte[TPlayer.NAME_Length]
		m_UniqueID	 = 0
		
		' Ship Data
		m_Position = New Float[3]
		m_Rotation = New Short[3]
		m_Velocity = New Short[3]
		
		TPlayer.PreGame.AddLast(Self)
	EndMethod
	
	Function Create:TPlayer(p_InfoSocket:TSocket)
		' Make sure the Socket is still alive.
		If p_InfoSocket <> Null And p_InfoSocket.Connected() Then
			Local l_Player:TPlayer = (New TPlayer)
			
			' Find a Unique ID that is not yet in use.
			Local l_UniqueId:Int = 0
			While l_UniqueId < 8192
				Local l_SubUniqueId:Byte = 0
				While l_SubUniqueId < 8
					If (TPlayer.UniqueIds[l_UniqueId] & (1 shl l_SubUniqueId)) = 0 Then
						' Mark UniqueId as used.
						TPlayer.UniqueIds[l_UniqueId] :| (1 shl l_SubUniqueId)
						l_Player.m_UniqueId = l_UniqueId * 8 + l_SubUniqueId
						
						' Exit Loops.
						l_UniqueId = 65535;l_SubUniqueId = 7
					EndIf
					
					l_SubUniqueId :+ 1
				Wend
				
				l_UniqueId :+ 1
			Wend
			
			If l_UniqueId = 65536 Then ' We have a Unique Id if we hit the unsigned limit for Short.
				TPlayer.UIDToPlayer[l_Player.m_UniqueId] = l_Player
				
				l_Player.m_InfoSocket = p_InfoSocket
				Return l_Player
			EndIf
		EndIf
	EndFunction
	' }
	
	' Deinitalize and Destroy old Players {
	Method Destroy()
		' Mark UniqueId as unused.
		TPlayer.UniqueIds[Int(m_UniqueId / 8)] :& ~(1 Shl (m_UniqueId Mod 8))
		TPlayer.UIDToPlayer[m_UniqueId] = Null
		
		' Try and remove from both Player lists.
		TPlayer.PreGame.Remove(Self)
		TPlayer.InGame.Remove(Self)
		
		' Close remaining Sockets.
		If m_InfoSocket Then m_InfoSocket.Close()
		If m_DataSocket Then m_DataSocket.Close()
		
		' Destroy Data
		m_Name = Null
		m_Position = Null
		m_Rotation = Null
		m_Velocity = Null
		m_InfoSocket = Null
		m_DataSocket = Null
	EndMethod
	' }
	
	' Update {
	Method Update(p_Multiplier:Float)
		Local m_OPositionX:Float = m_Position[0]
		Local m_OPositionY:Float = m_Position[1]
		Local m_OPositionZ:Float = m_Position[2]
		
		m_Position[0] :+ m_Velocity[0] * p_Multiplier
		m_Position[1] :+ m_Velocity[1] * p_Multiplier
		m_Position[2] :+ m_Velocity[2] * p_Multiplier
		
		If m_Position[0] <> m_OPositionX Then m_Changed :| TPlayer.CHANGED_POSX
		If m_Position[1] <> m_OPositionY Then m_Changed :| TPlayer.CHANGED_POSY
		If m_Position[2] <> m_OPositionZ Then m_Changed :| TPlayer.CHANGED_POSZ
	EndMethod
	' }
	
	' Packets {
	Const Range:Int = 3000
	Method IsInRange:Byte(p_Player:TPlayer)
		Local l_Distance:Float = Abs(p_Player.m_Position[0] - m_Position[0]) + Abs(p_Player.m_Position[1] - m_Position[1]) + Abs(p_Player.m_Position[2] - m_Position[2])
		If l_Distance < TPlayer.Range Then Return True
		Return False
	EndMethod
	
	Field __KnownPlayers:TList = New TList
	Method IsKnown:Byte(p_Player:TPlayer)
		If Not __KnownPlayers.Contains(p_Player) Then
			__KnownPlayers.AddLast(p_Player)
			Return False
		EndIf
		Return True
	EndMethod
	
	Method IsKnownEx:Byte(p_Player:TPlayer)
		If __KnownPlayers.Contains(p_Player) Then
			__KnownPlayers.Remove(p_Player)
			Return True
		EndIf
		Return False
	EndMethod
	
	Method GetDataUpdate:TDataUpdate(p_Player:TPlayer)
		Local l_Distance:Float = Abs(p_Player.m_Position[0] - m_Position[0]) + Abs(p_Player.m_Position[1] - m_Position[1]) + Abs(p_Player.m_Position[2] - m_Position[2])
		If l_Distance < TPlayer.Range And m_Changed Then
			Local l_DataUpdate:TDataUpdate = New TDataUpdate
			Local l_Changed:Byte = $00000000
			
			l_Changed = m_Changed & (TPlayer.CHANGED_POSX | TPlayer.CHANGED_POSY | TPlayer.CHANGED_POSZ)
			If l_Distance < 2000 Then l_Changed = m_Changed & (TPlayer.CHANGED_ROTX | TPlayer.CHANGED_ROTY | TPlayer.CHANGED_ROTZ)
			If l_Distance < 1000 Then l_Changed = m_Changed & TPlayer.CHANGED_VEL
			
			l_DataUpdate.m_UniqueId = m_UniqueId
			l_DataUpdate.m_Changed = m_Changed
			l_DataUpdate.m_Position[0] = m_Position[0]
			l_DataUpdate.m_Position[1] = m_Position[1]
			l_DataUpdate.m_Position[2] = m_Position[2]
			l_DataUpdate.m_Rotation[0] = m_Rotation[0]
			l_DataUpdate.m_Rotation[1] = m_Rotation[1]
			l_DataUpdate.m_Rotation[2] = m_Rotation[2]
			l_DataUpdate.m_Velocity[0] = m_Velocity[0]
			l_DataUpdate.m_Velocity[1] = m_Velocity[1]
			l_DataUpdate.m_Velocity[2] = m_Velocity[2]
			
			Return l_DataUpdate
		EndIf
		Return Null
	EndMethod
	' }
EndType

' Basic Network Packet
Type TNetPacket
	Field m_UniqueId:Short
	
	Function Read:TNetPacket(p_Stream:TStream)
	EndFunction
	
	Method Write(p_Stream:TStream)
	EndMethod
EndType

Type TInfoLogin Extends TNetPacket
	Const Id:Byte		 = 0
	Const Size:Int		 = 34
	Global Version:Short	 = Net_VersionMajor Shl 8 + Net_VersionMinor
	
	Field m_Name:Byte[]		 = New Byte[TPlayer.NAME_LENGTH]
	Field m_Position:Float[] = New Float[3]
	Field m_Rotation:Float[] = New Float[3]
	Field m_PunchPort:Short
	
	Function Read:TNetPacket(p_Stream:TStream)
		Local l_UPDPort:Short = p_Stream.ReadShort()
		Local l_Version:Short = p_Stream.ReadShort()
		
		If l_Version = TInfoLogin.Version Then
			Local l_Packet:TInfoLogin = New TInfoLogin
			p_Stream.ReadBytes(l_Packet.m_Name, TPlayer.NAME_LENGTH)
			l_Packet.m_Position[0] = p_Stream.ReadFloat()
			l_Packet.m_Position[1] = p_Stream.ReadFloat()
			l_Packet.m_Position[2] = p_Stream.ReadFloat()
			l_Packet.m_Rotation[0] = p_Stream.ReadFloat()
			l_Packet.m_Rotation[1] = p_Stream.ReadFloat()
			l_Packet.m_Rotation[2] = p_Stream.ReadFloat()
			l_Packet.m_PunchPort = l_UDPPort
			Return l_Packet
		EndIf
	EndFunction
	
	Method Write:Int(p_Stream:TStream)
		Local l_Pos:Int = p_Stream.Pos()
		p_Stream.WriteByte(TInfoLogin.Id)
		p_Stream.WriteShort(m_UniqueId)
		p_Stream.WriteShort(TInfoLogin.Version)
		p_Stream.WriteBytes(m_Name, TPlayer.NAME_LENGTH)
		p_Stream.WriteFloat(m_Position[0])
		p_Stream.WriteFloat(m_Position[1])
		p_Stream.WriteFloat(m_Position[2])
		p_Stream.WriteFloat(m_Rotation[0])
		p_Stream.WriteFloat(m_Rotation[1])
		p_Stream.WriteFloat(m_Rotation[2])
		Return p_Stream.Pos() - l_Pos
	EndMethod
EndType

Type TInfoLogout Extends TNetPacket
	Const Id:Byte	 = 1
	Const Size:Int	 = 2
	
	Function Read:TNetPacket(p_Stream:TStream)
		Local l_Packet:TInfoLogout = New TInfoLogout
		Return l_Packet
	EndFunction
	
	Method Write:Int(p_Stream:TStream)
		Local l_Pos:Int = p_Stream.Pos()
		p_Stream.WriteByte(TInfoLogout.Id)
		p_Stream.WriteShort(m_UniqueId)
		Return p_Stream.Pos() - l_Pos
	EndMethod
EndType

Type TInfoUpdate Extends TNetPacket
	Const Id:Byte	 = 2
	Const Size:Int	 = 18
	
	Field m_Name:Byte[] = New Byte[TPlayer.NAME_LENGTH]
	
	Function Read:TNetPacket(p_Stream:TStream)
		Local l_Packet:TInfoUpdate = New TInfoUpdate
		l_Packet.m_UniqueId = p_Stream.ReadShort() ~ p_Stream.ReadShort()
		p_Stream.ReadBytes(l_Packet.m_Name, TPlayer.NAME_LENGTH)
		Return l_Packet
	EndFunction
	
	Method Write:Int(p_Stream:TStream)
		Local l_Pos:Int = p_Stream.Pos()
		p_Stream.WriteByte(TInfoUpdate.Id)
		p_Stream.WriteShort(m_UniqueId)
		p_Stream.WriteBytes(m_Name, TPlayer.NAME_LENGTH)
		Return p_Stream.Pos() - l_Pos
	EndMethod
EndType

Type TDataUpdate Extends TNetPacket
	Const Id:Byte	 = 3
	Const Size:Int	 = 3
	
	Field m_Changed:Byte
	Field m_Position:Float[] = New Float[3]
	Field m_Rotation:Float[] = New Float[3]
	Field m_Velocity:Float[] = New Float[3]
	
	Function Read:TNetPacket(p_Stream:TStream)
		Local l_Packet:TDataUpdate = New TDataUpdate
		l_Packet.m_UniqueId = p_Stream.ReadShort()
		l_Packet.m_Changed = p_Stream.ReadByte()
		If l_Packet.m_Changed & TPlayer.CHANGED_POSX <> 0 Then l_Packet.m_Position[0] = p_Stream.ReadFloat()
		If l_Packet.m_Changed & TPlayer.CHANGED_POSY <> 0 Then l_Packet.m_Position[1] = p_Stream.ReadFloat()
		If l_Packet.m_Changed & TPlayer.CHANGED_POSZ <> 0 Then l_Packet.m_Position[2] = p_Stream.ReadFloat()
		If l_Packet.m_Changed & TPlayer.CHANGED_ROTX <> 0 Then l_Packet.m_Rotation[0] = Min(Max(p_Stream.ReadShort() / 32767.0, -1.0), 1.0) * 180.0
		If l_Packet.m_Changed & TPlayer.CHANGED_ROTY <> 0 Then l_Packet.m_Rotation[1] = Min(Max(p_Stream.ReadShort() / 32767.0, -1.0), 1.0) * 180.0
		If l_Packet.m_Changed & TPlayer.CHANGED_ROTZ <> 0 Then l_Packet.m_Rotation[2] = Min(Max(p_Stream.ReadShort() / 32767.0, -1.0), 1.0) * 180.0
		If l_Packet.m_Changed & TPlayer.CHANGED_VEL <> 0 Then
			l_Packet.m_Velocity[0] = Min(Max(p_Stream.ReadShort() / 32767.0, -1.0), 1.0) * 128.0
			l_Packet.m_Velocity[1] = Min(Max(p_Stream.ReadShort() / 32767.0, -1.0), 1.0) * 128.0
			l_Packet.m_Velocity[2] = Min(Max(p_Stream.ReadShort() / 32767.0, -1.0), 1.0) * 128.0
		EndIf
		Return l_Packet
	EndFunction
	
	Method Write:Int(p_Stream:TStream)
		Local l_Pos:Int = p_Stream.Pos()
		p_Stream.WriteByte(TDataUpdate.Id)
		p_Stream.WriteShort(m_UniqueId)
		p_Stream.WriteByte(m_Changed)
		If m_Changed & TPlayer.CHANGED_POSX <> 0 Then p_Stream.WriteFloat(m_Position[0])
		If m_Changed & TPlayer.CHANGED_POSY <> 0 Then p_Stream.WriteFloat(m_Position[1])
		If m_Changed & TPlayer.CHANGED_POSZ <> 0 Then p_Stream.WriteFloat(m_Position[2])
		If m_Changed & TPlayer.CHANGED_ROTX <> 0 Then p_Stream.WriteShort(Min(Max(m_Rotation[0] / 180.0, -1), 1) * 32767)
		If m_Changed & TPlayer.CHANGED_ROTY <> 0 Then p_Stream.WriteShort(Min(Max(m_Rotation[1] / 180.0, -1), 1) * 32767)
		If m_Changed & TPlayer.CHANGED_ROTZ <> 0 Then p_Stream.WriteShort(Min(Max(m_Rotation[2] / 180.0, -1), 1) * 32767)
		If m_Changed & TPlayer.CHANGED_VEL <> 0 Then
			p_Stream.WriteShort(Min(Max(m_Velocity[0] / 128.0, -1.0), 1.0) * 32767)
			p_Stream.WriteShort(Min(Max(m_Velocity[1] / 128.0, -1.0), 1.0) * 32767)
			p_Stream.WriteShort(Min(Max(m_Velocity[2] / 128.0, -1.0), 1.0) * 32767)
		EndIf
		Return p_Stream.Pos() - l_Pos
	EndMethod
EndType
'--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
EndRem