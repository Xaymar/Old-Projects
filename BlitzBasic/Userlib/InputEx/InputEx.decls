.lib "User32.dll"
User32_FindWindow%(class$, title$):"FindWindowA"
User32_GetActiveWindow%():"GetActiveWindow"
User32_GetCursorPosition%(point*):"GetCursorPos"
User32_ScreenToClient%(hwnd%, point*):"ScreenToClient"
User32_MapVirtualKeyEx%(code%, mapType%, dwhkl%):"MapVirtualKeyExA"
User32_GetAsyncKeyState%(vkey%):"GetAsyncKeyState"

.lib " "
InputEx_Init()
InputEx_Update()
InputEx_VKeyTime%(VirtualKey%)
InputEx_VKeyDownEx%(VirtualKey%)
InputEx_VKeyDown%(VirtualKey%)
InputEx_VKeyHitEx%(VirtualKey%)
InputEx_VKeyHit%(VirtualKey%)
InputEx_KeyTime%(ScanCode%)
InputEx_KeyDownEx%(ScanCode%)
InputEx_KeyDown%(ScanCode%)
InputEx_KeyHitEx%(ScanCode%)
InputEx_KeyHit%(ScanCode%)
InputEx_MouseTime%(Button%)
InputEx_MouseDownEx%(Button%)
InputEx_MouseDown%(Button%)
InputEx_MouseHitEx%(Button%)
InputEx_MouseHit%(Button%)
KeyTime%(Key%)
KeyDownEx%(Key%)
KeyHitEx%(Key%)
MouseTime%(Button%)
MouseDownEx%(Button%)
MouseHitEx%(Button%)