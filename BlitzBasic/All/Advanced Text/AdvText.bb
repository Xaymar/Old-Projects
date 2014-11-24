;AdvText Function
Dim SplittedString$(0)
Dim sTextLines$(0,1)
Global SplitCount, AdvText_X, AdvText_Y, AdvText_Width, AdvText_Height
Function AdvText(iX, iY, sText$, iCenterX#=0, iCenterY#=0, fLineSpace#=1)
	Local iTextLines = 1, iLine = 0
	
	AdvText_X = 0:AdvText_Y = 0:AdvText_Width = 0:AdvText_Height = 0
	
	SplitString(sText, Chr(10))
	iTextLines = SplitCount-1
	
	Dim sTextLines$(iTextLines,1)
	For iLine = 0 To iTextLines
		sTextLines(iLine, 0) = SplittedString$(iLine)
	Next
	
	;Parse the Text so that we get only the visible text into the second slot.
	Local iLastClr = 1, tabLength = 0
	For iLine = 0 To iTextLines
		iLastClr = 1
		For iPos = 1 To Len(sTextLines(iLine,0))
			sChar$ = Mid(sTextLines(iLine,0), iPos, 1)
			If sChar = "|" And (Mid(sTextLines(iLine,0), iPos+1, 1) = "f" Or Mid(sTextLines(iLine,0), iPos+1, 1) = "b")
				sTextLines(iLine,1) = sTextLines(iLine,1) + Mid(sTextLines(iLine,0),iLastClr,iPos-iLastClr)
				iLastClr = iPos+8
			ElseIf sChar = Chr(9)
				sTextLines(iLine,1) = sTextLines(iLine,1) + Mid(sTextLines(iLine,0),iLastClr,iPos-iLastClr)
				iLastClr = iPos
				
				;Remove Tab Character
				sTextLines(iLine,0) = Left(sTextLines(iLine,0), iPos-1) + Right(sTextLines(iLine,0), Len(sTextLines(iLine,0))-iPos)
				
				tabLength = (4-(Len(sTextLines(iLine,1)) Mod 4))
 				For iTabSpace = 1 To tabLength
					sTextLines(iLine,0) = Left(sTextLines(iLine,0), iPos-1) + " " + Right(sTextLines(iLine,0), Len(sTextLines(iLine,0))-iPos+1)
				Next
			ElseIf sChar = Chr(11)
				sTextLines(iLine,1) = sTextLines(iLine,1) + Mid(sTextLines(iLine,0),iLastClr,iPos-iLastClr)
				iLastClr = iPos
				
				;Remove Tab Character
				sTextLines(iLine,0) = Left(sTextLines(iLine,0), iPos-1) + Right(sTextLines(iLine,0), Len(sTextLines(iLine,0))-iPos)
				
				tabLength = (8-(Len(sTextLines(iLine,1)) Mod 8))
				For iTabSpace = 1 To tabLength
					sTextLines(iLine,0) = Left(sTextLines(iLine,0), iPos-1) + " " + Right(sTextLines(iLine,0), Len(sTextLines(iLine,0))-iPos+1)
				Next
			ElseIf iPos = Len(sTextLines(iLine,0))
				sTextLines(iLine,1) = sTextLines(iLine,1) + Mid(sTextLines(iLine,0),iLastClr,iPos-iLastClr+1)
			EndIf
		Next
		; Return width.
		If (AdvText_Width < StringWidth(sTextLines(iLine,1))) Then AdvText_Width = StringWidth(sTextLines(iLine,1))
	Next
	
	; Return Height
	If (AdvText_Height < (FontHeight()*(iTextLines+1)*fLineSpace)) Then AdvText_Height = (FontHeight()*(iTextLines+1)*fLineSpace)
	
	;Real Text Processing
	Local iRedO = ColorRed(), iGreenO = ColorGreen(), iBlueO = ColorBlue() ; Original Foreground Color
	Local iRed = ColorRed(), iGreen = ColorGreen(), iBlue = ColorBlue()	;Foreground
	Local iBGRed = -1, iBGGreen = -1, iBGBlue = -1						;Background
	Local icX, icY, icsX, scText$, iRealPos
	For iLine = 0 To iTextLines
		sText$ = sTextLines(iLine,0)
		icsX = -(StringWidth(sTextLines(iLine,1))*0.5)*iCenterX
		icX = icsX
		icY = ( -(FontHeight()*(iTextLines+1)*0.5*fLineSpace*iCenterY) + (iLine * FontHeight() * fLineSpace) )
		
		; Return X and Y starting point.
		If (AdvText_X > iX + icX) Then AdvText_X = iX + icX
		If (AdvText_Y > iY + icY) Then AdvText_Y = iY + icY
		
		iRealPos = 1
		For iPos = 1 To Len(sText)
			sChar$ = Mid(sText, iPos, 1)
			If sChar = "|" And Mid(sText, iPos+1, 1) = "f"		;Foreground Change
				If iBGRed > -1 And iBGGreen > -1 And iBGBlue > -1
					Color iBGRed, iBGGreen, iBGBlue
					Rect iX+icX, iY+icY, StringWidth(scText), FontHeight()*fLineSpace
				EndIf
				Color iRed, iGreen, iBlue
				Text iX+icX, iY+icY, scText$
				
				icX = icX + StringWidth(scText$)
				scText = ""
				
				If Mid(sText, iPos+2, 2) = "-1"
					iRed = iRedO
					iGreen = iGreenO
					iBlue = iBlueO
				Else
					iRed = HexB(Mid(sText, iPos+2, 2))
					iGreen = HexB(Mid(sText, iPos+4, 2))
					iBlue = HexB(Mid(sText, iPos+6, 2))
				EndIf
				
				sText = Left(sText, iPos)+Mid(sText, iPos+8)
			ElseIf sChar = "|" And Mid(sText, iPos+1, 1) = "b"	;Background Change
				If iBGRed > -1 And iBGGreen > -1 And iBGBlue > -1
					Color iBGRed, iBGGreen, iBGBlue
					Rect iX+icX, iY+icY, StringWidth(scText), FontHeight()*fLineSpace
				EndIf
				Color iRed, iGreen, iBlue
				Text iX+icX, iY+icY, scText$
				
				icX = icX + StringWidth(scText$)
				scText = ""
				
				If Mid(sText, iPos+2, 2) = "-1"
					iBGRed = -1
					iBGGreen = -1
					iBGBlue = -1
				Else
					iBGRed = HexB(Mid(sText, iPos+2, 2))
					iBGGreen = HexB(Mid(sText, iPos+4, 2))
					iBGBlue = HexB(Mid(sText, iPos+6, 2))
				EndIf
				sText = Left(sText, iPos)+Mid(sText, iPos+8)
			Else
				scText$ = scText$ + sChar$
				iRealPos = iRealPos + 1
			EndIf
			
			If iPos = Len(sText)
				If iBGRed > -1 And iBGGreen > -1 And iBGBlue > -1
					Color iBGRed, iBGGreen, iBGBlue
					Rect iX+icX, iY+icY, StringWidth(scText), FontHeight()*fLineSpace
				EndIf
				Color iRed, iGreen, iBlue
				Text iX+icX, iY+icY, scText
				scText = ""
			EndIf
		Next
		scText$ = ""
	Next
End Function

Function HexB#(Hexzahl$)
	Local Integer_Result#
	If Left$(Hexzahl$,1)="$" Then Hexzahl$=Mid$(Hexzahl$,2)
	For i=1 To Len(Hexzahl$)
		tmp1$=Upper$(Mid$(Hexzahl$,i,1)):tmp2=tmp1$
		If tmp2=0 And tmp1$<>"0" Then tmp2=Asc(tmp1$)-55
		Integer_Result=Integer_Result*16:Integer_Result=Integer_Result+tmp2
	Next
	Return Integer_Result
End Function

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
;~IDEal Editor Parameters:
;~C#Blitz3D