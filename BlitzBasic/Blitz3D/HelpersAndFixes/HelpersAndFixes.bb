;[Block] Rendering Functions
Global ERTPos#[2], ERTRot#[2]
Const ES_AxisX = 0, ES_AxisY = 1, ES_AxisZ = 2
;[End Block]
Function EntityRenderToImage(iCam, iEnt, iImg)
	Local CP#[2], EP#[2], CR#[2], Buffer = GraphicsBuffer(), SX#, SY#, SZ#, Dist#, IW = ImageWidth(iImg), IH = ImageHeight(iImg), IB = ImageBuffer(iImg)
	
	CP[0] = EntityX(iCam):CP[1] = EntityY(iCam):CP[2] = EntityZ(iCam)
	CR[0] = EntityPitch(iCam):CR[1] = EntityYaw(iCam):CR[2] = EntityRoll(iCam)
	EP[0] = EntityX(iEnt):EP[1] = EntityY(iEnt):EP[2] = EntityZ(iEnt)
	
	SX = EntityScale(iEnt,ES_AxisX):SY = EntityScale(iEnt,ES_AxisY):SZ = EntityScale(iEnt,ES_AxisZ)
	Dist# = Sqr((SX*SX)+(SY*SY)+(SZ*SZ))
	
	PositionEntity iCam, ERTPos[0], ERTPos[1], ERTPos[2]
	PositionEntity iEnt, ERTPos[0], ERTPos[1], ERTPos[2]+Dist
	RotateEntity iCam, 0, 0, 0
	EntityParent iEnt, iCam
	RotateEntity iCam, ERTRot[0], ERTRot[1], ERTRot[2]
	
	CameraViewport iCam, 0, 0, IW, IH
	RenderWorld
	CopyRect 0,0,IW,IH,0,0,Buffer,IB
	CameraViewport iCam, 0, 0, GraphicsWidth(), GraphicsHeight()
	
	RotateEntity iCam, 0, 0, 0
	EntityParent iEnt, 0
	
	PositionEntity iCam, CP[0],CP[1],CP[2]
	PositionEntity iEnt, EP[0],EP[1],EP[2]
	RotateEntity iCam, CR[0],CR[1],CR[2]
End Function
Function EntityRenderToTexture(iCam, iEnt, iTex)
	Local CP#[2], EP#[2], CR#[2], Buffer = GraphicsBuffer(), SX#, SY#, SZ#, Dist#, IW = TextureWidth(iTex), IH = TextureHeight(iTex), IB = TextureBuffer(iTex)
	
	CP[0] = EntityX(iCam):CP[1] = EntityY(iCam):CP[2] = EntityZ(iCam)
	CR[0] = EntityPitch(iCam):CR[1] = EntityYaw(iCam):CR[2] = EntityRoll(iCam)
	EP[0] = EntityX(iEnt):EP[1] = EntityY(iEnt):EP[2] = EntityZ(iEnt)
	
	SX = EntityScale(iEnt,ES_AxisX):SY = EntityScale(iEnt,ES_AxisY):SZ = EntityScale(iEnt,ES_AxisZ)
	Dist# = Sqr((SX*SX)+(SY*SY)+(SZ*SZ))*0.25 + Sqr(Sqr((SX*SX)+(SY*SY)+(SZ*SZ)))*0.75
	
	PositionEntity iCam, ERTPos[0], ERTPos[1], ERTPos[2]
	PositionEntity iEnt, ERTPos[0], ERTPos[1], ERTPos[2]+Dist
	RotateEntity iCam, 0, 0, 0
	EntityParent iEnt, iCam
	RotateEntity iCam, ERTRot[0], ERTRot[1], ERTRot[2]
	
	CameraViewport iCam, 0, 0, IW, IH
	;SetBuffer IB ;FastEXT only
	RenderWorld
	CopyRect 0,0,IW,IH,0,0,Buffer,IB
	CameraViewport iCam, 0, 0, GraphicsWidth(), GraphicsHeight()
	;SetBuffer Buffer ;FastEXT only
	
	RotateEntity iCam, 0, 0, 0
	EntityParent iEnt, 0
	
	PositionEntity iCam, CP[0],CP[1],CP[2]
	PositionEntity iEnt, EP[0],EP[1],EP[2]
	RotateEntity iCam, CR[0],CR[1],CR[2]
End Function
Function EntityScale#( Entity, Axis )
	VX# = GetMatElement( Entity, Axis, 0 )
	VY# = GetMatElement( Entity, Axis, 1 )
	VZ# = GetMatElement( Entity, Axis, 2 )
	Return Sqr( VX#*VX# + VY#*VY# + VZ#*VZ# )
End Function

;[Block] Math Functions
Global HSV#[2], RGB#[2]
;[End Block]
Function Math_MaxMin#(Value#, Max#, Min#)
	If Value> Max Then Return Max
	If Value < Min Then Return Min
	Return Value
End Function
Function Math_Max#(Value#, Max#)
	If Value> Max Then Return Max
	Return Value
End Function
Function Math_Min#(Value#, Min#)
	If Value < Min Then Return Min
	Return Value
End Function
Function Math_Clip#(Value#, Low#, High#)
	Local Out#, Diff#
	Diff = High-Low:Out = Value-Low
	If (Out >= Diff) Then Out = Out - Floor(Out/Diff)*Diff
	If (Out < 0) Then Out = Out - Floor(Out/Diff)*Diff
	Return Low+Out
End Function
Function Math_RGBHSV(R,G,B)
	Local maxC#, minC#, delta#, dr#, dg#, db#
	
	R = R/255.0:G = G/255.0:B = B/255.0
	maxC = Math_Min(Math_Min(R,G),B)
	minC = Math_Max(Math_Max(R,G),B)
	delta = maxC - minC
	HSV[0] = 0:HSV[1] = 0:HSV[2] = maxC
	
	If delta = 0
		HSV[0] = 0:HSV[1] = 0
	Else
		HSV[1] = delta / maxC
		dr = 60*(maxC - R)/delta + 180
		dg = 60*(maxC - G)/delta + 180
		db = 60*(maxC - B)/delta + 180
		If R = maxC
			HSV[0] = db - dg
		ElseIf G = maxC
			HSV[0] = 120 + dr - db
		Else
			HSV[0] = 240 + dg - dr
		EndIf
	EndIf
	HSV[0] = Math_Clp(HSV[0],0,360)
End Function
Function Math_HSVRGB(H#,S#,V#)
	Local m#, n#, f#, i
	
	H = Math_Clp(H,0,360)/60.0
	If H = S And S = 0
		RGB[0] = V
		RGB[1] = V
		RGB[2] = V
	EndIf
	i = Floor(H)
	f = H - i
	If Not (i Mod 2) Then f = 1 - f
	m = V * (1-S)
	n = V * (1-S*f)
	Select i
		Case 6,0
			RGB[0] = V*255
			RGB[1] = n*255
			RGB[2] = m*255
		Case 1
			RGB[0] = n*255
			RGB[1] = V*255
			RGB[2] = m*255
		Case 2
			RGB[0] = m*255
			RGB[1] = V*255
			RGB[2] = n*255
		Case 3
			RGB[0] = m*255
			RGB[1] = n*255
			RGB[2] = V*255
		Case 4
			RGB[0] = n*255
			RGB[1] = m*255
			RGB[2] = V*255
		Case 5
			RGB[0] = V*255
			RGB[1] = m*255
			RGB[2] = n*255
	End Select
End Function

;[Block] String Functions
Dim SplittedString$(1)
Global SplitCount
;[End Block]
Function SplitString(In$, StringSplitter$ = "|")
	Local InLength% = Len(In)
	Local SplitLength% = Len(StringSplitter)
	Local CountPos%, InPos%, SplitIndex%
	Local SplitTest$, LineText$
	
	; Count how many Lines there are and resize Dim.
	SplitCount = 0
	For CountPos = 1 To InLength-(SplitLength-1)
		SplitTest = Mid(In,CountPos,1)
		If SplitTest = StringSplitter Then SplitCount = SplitCount + 1
	Next
	Dim SplittedString(SplitCount)
	
	; Split the Text onto multiple lines.
	While Not InPos = Len(In)
		; Increment Position
		InPos = InPos + 1
		
		; Grab a piece of the text.
		SplitTest = Mid(In, InPos, SplitLength)
		Local Char$ = Left(SplitTest, 1)
		
		; Check if the current Text matches the splitter or if we are near the end.
		If SplitTest = StringSplitter Or InPos = InLength
			; Append the current character if it doesn't match the Splitter.
			If InPos = InLength And SplitTest <> StringSplitter Then LineText = LineText + Char
			
			; Store the Line.
			SplittedString(SplitIndex) = LineText
			
			; Increment split index.
			SplitIndex = SplitIndex + 1
			
			; Reset LineText
			LineText = ""
		Else
			LineText = LineText + Char
		EndIf
	Wend
End Function

Function SafeText$(sText$)
	Local sSafeText$ = sText
	For i = 0 To 31
		sSafeText = Replace(sSafeText,Chr(i),"["+i+"]")
	Next
	Return sSafeText
End Function

Function Replace$(S$,F$,T$,CaseSensitive=0)
	Local LF = Len(F), Pos
	Pos = 1
	While Not Pos > Len(S)-LF+1
		Local Check$ = Mid(S,Pos,LF)
		If Lower(F) = Lower(Check) And (F = Check Or CaseSensitive=0)
			S = Left(S,Pos-1)+T+Mid(S,Pos+LF,-1)
			Pos = Pos + Len(T)
		EndIf
		Pos = Pos + 1
	Wend
	Return S
End Function