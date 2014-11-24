;----------------------------------------------------------------
;-- Types
;----------------------------------------------------------------
Type BU_Rectangle
	Field X,Y
	Field X2,Y2
End Type

Type BU_Point
	Field X,Y
End Type
;----------------------------------------------------------------

;----------------------------------------------------------------
;-- Global
;----------------------------------------------------------------
Global Utility_Rect.BU_Rectangle = New BU_Rectangle
Global Utility_Point.BU_Point = New BU_Point
Global Utility_PrivateProfileBuffer = CreateBank(65535)
;----------------------------------------------------------------

;----------------------------------------------------------------
;-- Functions
;----------------------------------------------------------------
Function Utility_LockPointerToWindow(hwnd=0)
	If hwnd = 0 Then
		Utility_Rect\X = 0
		Utility_Rect\Y = 0
		Utility_Rect\X2 = BUApi_GetSystemMetrics(78)
		Utility_Rect\Y2 = BUApi_GetSystemMetrics(79)
		BUApi_ClipCursor(Utility_Rect)
	Else
		;Grab TopLeft
		Utility_Point\X = 0
		Utility_Point\Y = 0
		BUApi_ClientToScreen(hwnd, Utility_Point)
		Utility_Rect\X = Utility_Point\X
		Utility_Rect\Y = Utility_Point\Y
		
		;Grab BottomRight
		Utility_Point\X = GraphicsWidth()
		Utility_Point\Y = GraphicsHeight()
		BUApi_ClientToScreen(hwnd, Utility_Point)
		Utility_Rect\X2 = Utility_Point\X
		Utility_Rect\Y2 = Utility_Point\Y
		
		BUApi_ClipCursor(Utility_Rect)
	EndIf
End Function

Function Utility_BorderlessWindowmode(Title$="", MonitorId=0)
	Local hWnd = SystemProperty("AppHwnd")
	If hWnd = 0 Then hWnd = BUApi_FindWindow("Blitz Runtime Class", Title)
	If hWnd = 0 Then RuntimeError("Unable to create borderless window.")
	
	Utility_EnumerateDisplays()
	Local dispCnt = Utility_GetDisplayCount()
	If MonitorId < 0 Then MonitorId = 0
	If MonitorId >= dispCnt Then MonitorId = dispCnt -1
	
	Local rct.BU_Rectangle = New BU_Rectangle
	Utility_GetDisplay(MonitorId, rct)
	
	BUApi_SetWindowLong hWnd, -16, $01000000
	BUApi_SetWindowPos hWnd, 0, rct\X, rct\Y, rct\X2, rct\Y2, 64
End Function

Function Utility_GetIniString$(File$, Section$, Key$, Def$)
	Local wLen% = BUApi_GetPrivateProfileString(Section, Key, Def, Utility_PrivateProfileBuffer, 65535, File)
	If wLen > 0 Then
		Local wOut$ = ""
		Local wPos = 1
		While (wPos < wLen)
			wOut = wOut + Chr(PeekByte(Utility_PrivateProfileBuffer, wPos - 1))
			wPos=wPos+1
		Wend
		Return wOut
	EndIf
End Function

Function Utility_SetIniString(File$, Section$, Key$, Value$)
	Return (BUApi_SetPrivateProfileString(Section, Key, Value, File) = 1)
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D