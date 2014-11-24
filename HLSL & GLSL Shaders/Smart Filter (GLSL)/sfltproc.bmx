Rem
	Texture Preprocessor For SmartFilter Shader
EndRem

Framework brl.blitz
Import brl.pixmap
Import brl.pngloader
Import brl.jpgloader
Import brl.bmploader
Import brl.system
Import brl.standardio
Import brl.retro

Print "----------------------------------------------"
Print "SmartFilter Texture Preprocessor (cc-by-sa-nc)"
Print "  (c) LevelNull 2011"
Print "  Licensed under CC-BY-SA-NC"
Print "----------------------------------------------"
Print ""

Global stImageSource:String	= ""
Global stImageTarget:String	= ""
Global inImageBias:Int		= 0.0
Global inStateColors:Int[14]
	inStateColors[0]		= $FF000000
	inStateColors[1]		= $FF131313
	inStateColors[2]		= $FF262626
	inStateColors[3]		= $FF393939
	inStateColors[4]		= $FF4C4C4C
	inStateColors[5]		= $FF5F5F5F
	inStateColors[6]		= $FF727272
	inStateColors[7]		= $FF858585
	inStateColors[8]		= $FF989898
	inStateColors[9]		= $FFABABAB
	inStateColors[10]		= $FFBEBEBE
	inStateColors[11]		= $FFD1D1D1
	inStateColors[12]		= $FFE4E4E4
	inStateColors[13]		= $FFF7F7F7

Local i:Int=1
While i < AppArgs.Length
	If (Chr(AppArgs[i][0]) = "-") Then
		Local command:String = Right(AppArgs[i],AppArgs[i].length-1)
		Select command
			Case "i","input"
				i :+ 1
				stImageSource = AppArgs[i]
			Case "b","bias"
				i :+ 1
				inImageBias = Int(AppArgs[i])
			Case "o","output"
				i :+ 1
				stImageTarget = AppArgs[i]
			Case "h","help"
				Print "SmartFilter Texture Preprocessor Help"
				Print "-------------------------------------"
				Print " -i -input (string)      Set the input file to read from"
				Print " -o -output (string)     Set the output file to write to"
				Print " -b -bias (0-255)        Set the bias at which borders are detected"
				Print " -h -help                Shows this help"
				Print "example:"
				Print AppArgs[0]+" -i mytex.png -o mytex_sflt.png -b 0.05"
				End
			Default
				Print "Unknown command '"+command+"'."
		End Select
	Else
		Print "  Unknown parameter: '"+AppArgs[i]+"'."
	EndIf
	i :+ 1
Wend

If (stImageSource <> "") And (stImageTarget <> "") Then
	Local obImageSource:TPixmap = LoadPixmap(stImageSource)
	If obImageSource <> Null
		Local inSourceSize:Int[2]
			inSourceSize[0]=obImageSource.width
			inSourcesize[1]=obImageSource.height
		Local inTargetSize:Int[2]
			inTargetSize[0]=inSourceSize[0]
			inTargetSize[1]=inSourceSize[1]
		Print "  Loaded Input Image (w"+inSourceSize[0]+",h"+inSourceSize[1]+") into memory."
		Local obImageTarget:TPixmap  = CreatePixmap(inTargetSize[0],inTargetSize[1],PF_RGBA8888,4)
		If obImageTarget <> Null
			Print "  Created Output Image (w"+inTargetSize[0]+",h"+inTargetSize[1]+") in memory."
			obImageTarget.ClearPixels($00000000)
			
			Local prog:Int = MilliSecs()
			Local lprog:Int = MilliSecs()
			For Local x:Int = 0 Until inTargetSize[0]
				Local sourceX:Int = clip(x,0,inSourceSize[0]-1)
				Local sourceX2:Int = clip(x+1,0,inSourceSize[0]-1)
				For Local y:Int = 0 Until inTargetSize[1]
					Local sourceY:Int = clip(y,0,inSourceSize[1]-1)
					Local sourceY2:Int = clip(y+1,0,inSourceSize[1]-1)
					
					Local pxlTL:ColorVec4 = New ColorVec4.fromInt(obImageSource.ReadPixel(sourceX,sourceY))
					Local pxlTR:ColorVec4 = New ColorVec4.fromInt(obImageSource.ReadPixel(sourceX2,sourceY))
					Local pxlBL:ColorVec4 = New ColorVec4.fromInt(obImageSource.ReadPixel(sourceX,sourceY2))
					Local pxlBR:ColorVec4 = New ColorVec4.fromInt(obImageSource.ReadPixel(sourceX2,sourceY2))
					
					pxlTL.r :* pxlTL.a/255.0;pxlTL.g :* pxlTL.a/255.0;pxlTL.b :* pxlTL.a/255.0
					pxlTR.r :* pxlTR.a/255.0;pxlTR.g :* pxlTR.a/255.0;pxlTR.b :* pxlTR.a/255.0
					pxlBL.r :* pxlBL.a/255.0;pxlBL.g :* pxlBL.a/255.0;pxlBL.b :* pxlBL.a/255.0
					pxlBR.r :* pxlBR.a/255.0;pxlBR.g :* pxlBR.a/255.0;pxlBR.b :* pxlBR.a/255.0
					
					Local rStates:Byte[] = statesAvaible(pxlTL.r,pxlTR.r,pxlBL.r,pxlBR.r,inImageBias)
					Local gStates:Byte[] = statesAvaible(pxlTL.g,pxlTR.g,pxlBL.g,pxlBR.g,inImageBias)
					Local bStates:Byte[] = statesAvaible(pxlTL.b,pxlTR.b,pxlBL.b,pxlBR.b,inImageBias)
					Local aStates:Byte[] = statesAvaible(pxlTL.a,pxlTR.a,pxlBL.a,pxlBR.a,inImageBias)
					
					For Local w:Int = 13 To 0 Step -1
						If (rStates[w]+gStates[w]+bStates[w]+aStates[w] >= 2) Then
							obImageTarget.WritePixel(x,y,inStateColors[w])
						EndIf
					Next
					
					prog = MilliSecs()
					If prog-lprog > 250 Then
						Print "  Progress: "+Int(Floor((x*inTargetSize[1]+y)/Float((inTargetSize[0])*(inTargetSize[1]))*100.0))+"% [x"+x+",y"+y+"]"
						lprog = prog
					EndIf 
				Next
			Next
			Print "  Progress: 100% [x"+(inTargetSize[0]-1)+",y"+(inTargetSize[1]-1)+"]"
			
			Print "  Saving Output Image to file '"+stImageTarget+"'."
			SavePixmapPNG(obImageTarget,stImageTarget,9)
			
			obImageTarget = Null
			Print "  Unloaded Output Image from memory."
		Else
			Print "  Failes to create Output Image. Is enough RAM avaible?"
		EndIf
		obImageSource = Null
		Print "  Unloaded Input Image from memory."
	Else
		Print "  Can not open input image. Are you sure it's a proper bmp,png or jpg?"
	EndIf
Else
	Print "  Missing -i or -o. Please double-check your command."
EndIf

Function clip:Float(in:Float, low:Float, high:Float)
	Local out:Float, diff:Float
	diff = high-low+1;out = in-low
	If (out >= diff) Then out :- Floor(out/diff)*diff
	If (out < 0) Then out :- Floor(out/diff)*diff
	Return (out+low)
End Function

Function statesAvaible:Byte[](TL:Byte,TR:Byte,BL:Byte,BR:Byte,Threshold:Byte)
	Local states:Byte[] = New Byte[14]
	
	If (_AT(TL,TR,Threshold) = False) And (_AT(TL,BL,Threshold) = False) And ..
		(_AT(BL,BR,Threshold) = False) And (_AT(TR,BR,Threshold) = False) Then
		states[0] = True
	EndIf
	
	'Lines
	If (_AT(TL,TR,Threshold) = False) And (_AT(TL,BL,Threshold) = True) And ..
		(_AT(BL,BR,Threshold) = False) And (_AT(TR,BR,Threshold) = True) Then
		states[1] = True
	EndIf
	If (_AT(TL,TR,Threshold) = True) And (_AT(TL,BL,Threshold) = False) And ..
		(_AT(BL,BR,Threshold) = True) And (_AT(TR,BR,Threshold) = False) Then
		states[2] = True
	EndIf
	
	'Edges
	If (_AT(TL,TR,Threshold) = False) And (_AT(TL,BL,Threshold) = False) And ..
		(_AT(BL,BR,Threshold) = True) And (_AT(TR,BR,Threshold) = True) Then
		states[3] = True
	EndIf
	If (_AT(TL,TR,Threshold) = False) And (_AT(TL,BL,Threshold) = True) And ..
		(_AT(BL,BR,Threshold) = True) And (_AT(TR,BR,Threshold) = False) Then
		states[4] = True
	EndIf
	If (_AT(TL,TR,Threshold) = True) And (_AT(TL,BL,Threshold) = True) And ..
		(_AT(BL,BR,Threshold) = False) And (_AT(TR,BR,Threshold) = False) Then
		states[5] = True
	EndIf
	If (_AT(TL,TR,Threshold) = True) And (_AT(TL,BL,Threshold) = False) And ..
		(_AT(BL,BR,Threshold) = False) And (_AT(TR,BR,Threshold) = True) Then
		states[6] = True
	EndIf
	
	'T-Edge
	If (_AT(TL,TR,Threshold) = True) And (_AT(TL,BL,Threshold) = True) And ..
		(_AT(BL,BR,Threshold) = False) And (_AT(TR,BR,Threshold) = True) Then
		states[7] = True
	EndIf
	If (_AT(TL,TR,Threshold) = False) And (_AT(TL,BL,Threshold) = True) And ..
		(_AT(BL,BR,Threshold) = True) And (_AT(TR,BR,Threshold) = True) Then
		states[8] = True
	EndIf
	If (_AT(TL,TR,Threshold) = True) And (_AT(TL,BL,Threshold) = True) And ..
		(_AT(BL,BR,Threshold) = True) And (_AT(TR,BR,Threshold) = False) Then
		states[9] = True
	EndIf
	If (_AT(TL,TR,Threshold) = True) And (_AT(TL,BL,Threshold) = False) And ..
		(_AT(BL,BR,Threshold) = True) And (_AT(TR,BR,Threshold) = True) Then
		states[10] = True
	EndIf
	If (_AT(TL,TR,Threshold) = True) And (_AT(TL,BL,Threshold) = False) And ..
		(_AT(BL,BR,Threshold) = True) And (_AT(TR,BR,Threshold) = True) Then
		states[10] = True
	EndIf
	
	'Diagonal
	If (_AT(TL,TR,Threshold) = True) And (_AT(TL,BL,Threshold) = True) And ..
		(_AT(BL,BR,Threshold) = True) And (_AT(TR,BR,Threshold) = True) And ..
		(_AT(TL,BR,Threshold) = False) Then
		states[11] = True
	EndIf
	If (_AT(TL,TR,Threshold) = True) And (_AT(TL,BL,Threshold) = True) And ..
		(_AT(BL,BR,Threshold) = True) And (_AT(TR,BR,Threshold) = True) And ..
		(_AT(TR,BL,Threshold) = False) Then
		states[12] = True
	EndIf
	If states[11] = True And states[12] = True Then
		If (Abs(TL-BR) < Abs(TR-BL)) Then
			states[12] = False
		Else
			states[11] = False
		EndIf
	EndIf
	
	'All
	If (_AT(TL,TR,Threshold) = True) And (_AT(TL,BL,Threshold) = True) And ..
		(_AT(BL,BR,Threshold) = True) And (_AT(TR,BR,Threshold) = True) And ..
		(_AT(TL,BR,Threshold) = True) And (_AT(TR,BL,Threshold) = True) Then
		states[13] = True
	EndIf
	
	Return states
End Function

Function _AT:Byte(A:Byte,B:Byte,T:Byte)
	If (Abs(A-B) > T) Then Return True
	Return False
End Function

Type ColorVec4
	Field r:Byte
	Field g:Byte
	Field b:Byte
	Field a:Byte
	
	Method fromInt:ColorVec4(value:Int)
		r = value Shl 8 Shr 24
		g = value Shl 16 Shr 24
		b = value Shl 24 Shr 24
		a = value Shr 24
		Return Self
	End Method
End Type