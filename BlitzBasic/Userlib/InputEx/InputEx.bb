;----------------------------------------------------------------
;-- Userlib
;----------------------------------------------------------------
;.lib "User32.dll"
;User32_FindWindow%(class$, title$):"FindWindowA"
;User32_GetActiveWindow%():"GetActiveWindow"
;User32_GetCursorPosition%(point*):"GetCursorPos"
;User32_ScreenToClient%(hwnd%, point*):"ScreenToClient"
;User32_MapVirtualKeyEx%(code%, mapType%, dwhkl%):"MapVirtualKeyExA"
;User32_GetAsyncKeyState%(vkey%):"GetAsyncKeyState"
;
;.lib " "
;InputEx_Init()
;InputEx_Update()
;InputEx_VKeyTime%(VirtualKey%)
;InputEx_VKeyDownEx%(VirtualKey%)
;InputEx_VKeyDown%(VirtualKey%)
;InputEx_VKeyHitEx%(VirtualKey%)
;InputEx_VKeyHit%(VirtualKey%)
;InputEx_KeyTime%(ScanCode%)
;InputEx_KeyDownEx%(ScanCode%)
;InputEx_KeyDown%(ScanCode%)
;InputEx_KeyHitEx%(ScanCode%)
;InputEx_KeyHit%(ScanCode%)
;InputEx_MouseTime%(Button%)
;InputEx_MouseDownEx%(Button%)
;InputEx_MouseDown%(Button%)
;InputEx_MouseHitEx%(Button%)
;InputEx_MouseHit%(Button%)
;KeyTime%(Key%)
;KeyDownEx%(Key%)
;KeyHitEx%(Key%)
;MouseTime%(Button%)
;MouseDownEx%(Button%)
;MouseHitEx%(Button%)
;----------------------------------------------------------------

;----------------------------------------------------------------
;-- Types
;----------------------------------------------------------------
Type Point
	Field X,Y
End Type
;----------------------------------------------------------------

;----------------------------------------------------------------
;-- Globals
;----------------------------------------------------------------
Global InputEx_Window = SystemProperty("AppHWND"), InputEx_ForMe = True
Global InputEx_Mouse.Point = New Point
Global InputEx_Width = GraphicsWidth()
Global InputEx_Height = GraphicsHeight()

Dim InputEx_State(256)
Dim InputEx_StateTime(256)
Dim InputEx_StateUpdates(256)
Dim InputEx_Hits(256)
Dim InputEx_VSCAsVK(256)
;----------------------------------------------------------------

;----------------------------------------------------------------
;-- Functions
;----------------------------------------------------------------
Function InputEx_Init(applicationTitle$="")
;@desc: Call this when your program starts to allow InputEx to work.
	;If Not InputEx_Window Then InputEx_Window = User32_FindWindow("Blitz Runtime Class", applicationTitle)
	InputEx_Window = SystemProperty("AppHWND")
	User32_GetCursorPosition(InputEx_Mouse)
	User32_ScreenToClient(InputEx_Window, InputEx_Mouse)
	InputEx_ForMe = (User32_GetActiveWindow() = InputEx_Window)
	If Not ((InputEx_Mouse\X >= 0) And (InputEx_Mouse\Y >= 0) And (InputEx_Mouse\X < GraphicsWidth()) And (InputEx_MouseY < GraphicsHeight())) Then InputEx_ForMe = False
	
	For VSC = 0 To 255
		InputEx_VSCAsVK(VSC) = User32_MapVirtualKeyEx(VSC, 1, 0)
	Next
End Function

Function InputEx_SetResolution(Width, Height)
	InputEx_Width = Width
	InputEx_Height = Height
End Function

Function InputEx_Update()
;@desc: Call this once per frame to update InputExs values.
	Local InputEx_StateNew
	Local InputEx_Time = MilliSecs()
	
	User32_GetCursorPosition(InputEx_Mouse)
	User32_ScreenToClient(InputEx_Window, InputEx_Mouse)
	InputEx_ForMe = (User32_GetActiveWindow() = InputEx_Window)
	If Not ((InputEx_Mouse\X >= 0) And (InputEx_Mouse\Y >= 0) And (InputEx_Mouse\X < InputEx_Width) And (InputEx_Mouse\Y < InputEx_Height)) Then InputEx_ForMe = False
	;If InputEx_ForMe Then ;Are those signals even for us?
	For VK = 0 To 255
		InputEx_StateNew = (User32_GetAsyncKeyState(VK) <> 0)
			
			; Generic Update Structure
			If (InputEx_StateNew = 1) And (InputEx_State(VK) = 0) Then
				InputEx_Hits(VK) = InputEx_Hits(VK) + 1 ; Register as new key hit.
				InputEx_State(VK) = 1 ; Set State to down.
				InputEx_StateUpdates(VK) = 0 ; Reset updatecount.
				InputEx_StateTime(VK) = InputEx_Time ; Set time at which the state changed.
			ElseIf (InputEx_StateNew = 0) And (InputEx_State(VK) = 1) Then
				InputEx_State(VK) = 0 ; Set State to up.
				InputEx_StateUpdates(VK) = 0 ; Reset Updatecount.
				InputEx_StateTime(VK) = InputEx_Time ; Set time at which the state changed.
			Else
				If (InputEx_State(VK) = 1) Then
					InputEx_StateUpdates(VK) = InputEx_StateUpdates(VK) + 1 ; Increase updatecount because button is down.
				Else
					InputEx_StateUpdates(VK) = InputEx_StateUpdates(VK) - 1 ; Decrease updatecount because button is up.
				EndIf
			EndIf
		Next
	;Else ;No
		For VK = 0 To 255
			InputEx_State(VK) = 0
			InputEx_StateTime(VK) = InputEx_Time
		Next
	;EndIf
	;Some may ask why i didn't put the If into the loop, this is the answer:
	;If I put it outside the loop, it's one less task for the CPU to do for every iteration. Thus increasing speed.
	;If I put it inside the loop, it's one more task for the CPU to do for every iteration. Thus decreasing speed.
End Function

Function InputEx_VKeyTime(VirtualKey)
;@desc: This tells you when the last state of the key was recieved in milliseconds.
;@returns: Time in milliseconds when the state of the key was registered.
	Return InputEx_StateTime(VirtualKey)
End Function

Function InputEx_VKeyDownEx(VirtualKey)
;@desc: This tells you the amount of updates a key has been down for(positive) or the amount of updates a key has been up for(negative).
;@returns: Updates the key has been down for. 
	Return InputEx_StateUpdates(VirtualKey)
End Function

Function InputEx_VKeyDown(VirtualKey)
;@desc: This tells you if a key is down or not.
;@returns: The keys state.
	Return InputEx_State(VirtualKey)
End Function

Function InputEx_VKeyHitEx(VirtualKey, Reduce=1)
;@desc: This tells you the amount of hits a key has recieved, while reducing the amount by <Reduce>.
;@returns: How many times the key has been hit.
	Local Hits = InputEx_Hits(VirtualKey)
	InputEx_Hits(VirtualKey) = InputEx_Hits(VirtualKey) - Reduce
	Return Hits
End Function

Function InputEx_VKeyHit(VirtualKey)
;@desc: This tells you the amount of hits a key has recieved since the last call and setting the amount to zero.
;@returns: How many times the key has been hit.
	Local Hits = InputEx_Hits(VirtualKey)
	InputEx_Hits(VirtualKey) = 0
	Return Hits
End Function

Function InputEx_KeyTime(ScanCode)
;@desc: See [InputEx_VKeyTime].
	Return InputEx_VKeyTime(InputEx_VSCAsVK(ScanCode))
End Function

Function InputEx_KeyDownEx(ScanCode)
;@desc: See [InputEx_VKeyDownEx].
	Return InputEx_VKeyDownEx(InputEx_VSCAsVK(ScanCode))
End Function

Function InputEx_KeyDown(ScanCode)
;@desc: See [InputEx_VKeyDown].
	Return InputEx_VKeyDown(InputEx_VSCAsVK(ScanCode))
End Function

Function InputEx_KeyHitEx(ScanCode)
;@desc: See [InputEx_VKeyHitEx].
	Return InputEx_VKeyHitEx(InputEx_VSCAsVK(ScanCode))
End Function

Function InputEx_KeyHit(ScanCode)
;@desc: See [InputEx_VKeyHit].
	Return InputEx_VKeyHit(InputEx_VSCAsVK(ScanCode))
End Function

Function InputEx_MouseTime(Button)
;@desc: See [InputEx_VKeyTime].
	Select Button
		Case 1,2
			Return InputEx_VKeyTime(Button)
		Case 3,4,5
			Return InputEx_VKeyTime(Button+1)
	End Select
End Function

Function InputEx_MouseDownEx(Button)
;@desc: See [InputEx_VKeyDownEx].
	Select Button
		Case 1,2
			Return InputEx_VKeyDownEx(Button)
		Case 3,4,5
			Return InputEx_VKeyDownEx(Button+1)
	End Select
End Function

Function InputEx_MouseDown(Button)
;@desc: See [InputEx_VKeyDown].
	Select Button
		Case 1,2
			Return InputEx_VKeyDown(Button)
		Case 3,4,5
			Return InputEx_VKeyDown(Button+1)
	End Select
End Function

Function InputEx_MouseHitEx(Button)
;@desc: See [InputEx_VKeyHitEx].
	Select Button
		Case 1,2
			Return InputEx_VKeyHitEx(Button)
		Case 3,4,5
			Return InputEx_VKeyHitEx(Button+1)
	End Select
End Function

Function InputEx_MouseHit(Button)
;@desc: See [InputEx_VKeyHit].
	Select Button
		Case 1,2
			Return InputEx_VKeyHit(Button)
		Case 3,4,5
			Return InputEx_VKeyHit(Button+1)
	End Select
End Function
;----------------------------------------------------------------

;----------------------------------------------------------------
;-- Helper Functions for ease of use.
;----------------------------------------------------------------
Function MouseTime(Button)
	Return InputEx_MouseTime(Button)
End Function

Function MouseDownEx(Button)
	Return InputEx_MouseDownEx(Button)
End Function

Function MouseDown(Button)
	Return InputEx_MouseDown(Button)
End Function

Function MouseHitEx(Button)
	Return InputEx_MouseHitEx(Button)
End Function

Function MouseHit(Button)
	Return InputEx_MouseHit(Button)
End Function

Function KeyTime(Key)
	Return InputEx_KeyTime(Key)
End Function

Function KeyDownEx(Key)
	Return InputEx_KeyHitEx(Key)
End Function

Function KeyDown(Key)
	Return InputEx_KeyDown(Key)
End Function

Function KeyHitEx(Key)
	Return InputEx_KeyHitEx(Key)
End Function

Function KeyHit(Key)
	Return InputEx_KeyHit(Key)
End Function
;----------------------------------------------------------------

;----------------------------------------------------------------
;-- Example
;----------------------------------------------------------------
;Graphics 400,300,32,2
;SetBuffer BackBuffer()
;User32_ShowWindow(SystemProperty("AppHWND"), 1)
;
;Local Behaviour
;
;InputEx_Init()
;While Not KeyDown(1)
;	InputEx_Update()
;	
;	Cls
;	
;	If KeyHit(2) Then Behaviour = 0
;	If KeyHit(3) Then Behaviour = 1
;	
;	Select Behaviour
;		Case 0
;			Color 255,204,204
;			Text 0, 0,"Behaviour: Normal (Press 2 to change to 'Extended')"
;			Text 0,15,"Mouse L:  "+MouseDown(1)+" "+MouseTime(1)
;			Text 0,30,"Mouse R:  "+MouseDown(2)+" "+MouseTime(2)
;			Text 0,45,"Mouse M:  "+MouseDown(3)+" "+MouseTime(3)
;			Text 0,60,"Mouse X1: "+MouseDown(4)+" "+MouseTime(4)
;			Text 0,75,"Mouse X2: "+MouseDown(5)+" "+MouseTime(5)
;		Case 1
;			Color 204,204,255
;			Text 0, 0,"Behaviour: Extended (Press 1 to change to 'Normal')"
;			Text 0,15,"Mouse L:  "+MouseDownEx(1)+" "+MouseTime(1)
;			Text 0,30,"Mouse R:  "+MouseDownEx(2)+" "+MouseTime(2)
;			Text 0,45,"Mouse M:  "+MouseDownEx(3)+" "+MouseTime(3)
;			Text 0,60,"Mouse X1: "+MouseDownEx(4)+" "+MouseTime(4)
;			Text 0,75,"Mouse X2: "+MouseDownEx(5)+" "+MouseTime(5)
;	End Select
;	
;	Flip
;Wend
;
;End
;----------------------------------------------------------------
;~IDEal Editor Parameters:
;~C#Blitz3D