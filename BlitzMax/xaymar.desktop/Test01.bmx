SuperStrict

Framework BRL.StandardIO
Import Xaymar.Desktop
Import Pub.DirectX
Import Pub.Win32
Import MaxGUI.Win32MaxGUIEx

Extern "win32"
	Function GetLastError() = "GetLastError@0"
EndExtern

Global d3dx9Lib:Int = LoadLibraryA( "d3dx9" )
DebugLog GetLastError()
If d3dx9Lib Then
	Global D3DXAssembleShader:Int(pSrcData:Byte Ptr, SrcDataLen:Int, pDefines:Byte Ptr, pInclude:Byte Ptr, FLAGS:Int, ppShader:ID3DXBuffer Var, ppErrorMsgs:ID3DXBuffer Var)"win32"=GetProcAddress( d3dx9Lib,"D3DXAssembleShader" )
	Global D3DXSaveSurfaceToFile:Int(pConstString:Byte Ptr, DestFormat:Int, pSurface:Byte Ptr, pSrcPalette:Byte Ptr, pSrcRect:Byte Ptr)"win32"=GetProcAddress( d3dx9Lib, "D3DXSaveSurfaceToFile" )
EndIf

Local l_Displays:TList = GetDisplayList()
For Local l_Display:Rect = EachIn l_Displays
	Print "[" + l_Display.Left + ", " + l_Display.Top + "]:[" + l_Display.Right + ", " + l_Display.Bottom + "]"
Next
Local l_Display:Rect = Rect(l_Displays.ValueAtIndex(0))

Local l_D3D:IDirect3D9				 = Direct3DCreate9( 32 )
If Not l_D3D Then
	Print "0x00000001"
Else
	Local l_D3DCaps:D3DCAPS9			 = New D3DCAPS9
	If l_D3D.GetDeviceCaps(D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, l_D3DCaps) < 0
		Print "0x00000002"
	Else
		Local l_D3DPresentParam:D3DPRESENT_PARAMETERS	 = New D3DPRESENT_PARAMETERS
		l_D3DPresentParam.BackBufferWidth		 = (l_Display.Right - l_Display.Left)
		l_D3DPresentParam.BackBufferHeight		 = (l_Display.Bottom - l_Display.Top)
		l_D3DPresentParam.BackBufferCount		 = 1
		l_D3DPresentParam.BackBufferFormat		 = D3DFMT_UNKNOWN
		l_D3DPresentParam.MultiSampleType		 = D3DMULTISAMPLE_NONE
		l_D3DPresentParam.SwapEffect			 = D3DSWAPEFFECT_DISCARD
		l_D3DPresentParam.hDeviceWindow			 = Null
		l_D3DPresentParam.Windowed				 = True
		l_D3DPresentParam.Flags					 = D3DPRESENTFLAG_LOCKABLE_BACKBUFFER | D3DPRESENTFLAG_VIDEO
		l_D3DPresentParam.PresentationInterval	 = D3DPRESENT_INTERVAL_IMMEDIATE
		
		Local l_WindowName:String = "Null"
		Local l_hWnd:Int = CreateWindowExW(0, TWindowsGUIDriver.ClassName(), l_WindowName, 0, 0, 0, 1, 1, Null, Null, Null, Null)
		If l_hWnd = 0 Then
			Print GetLastError()
			Print "0x00000003"
		Else
			Local l_D3DDevice:IDirect3DDevice9	 = Null
			If l_D3D.CreateDevice(D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, l_hWnd, D3DCREATE_SOFTWARE_VERTEXPROCESSING, l_D3DPresentParam, l_D3DDevice) < 0
				Print "0x00000004"
			Else
				Local l_DesktopRT:IDirect3DSurface9	 = Null
				Local l_RecordRT:IDirect3DSurface9	 = Null
				
				If l_D3DDevice.GetRenderTarget(0, l_DesktopRT) < 0
					Print "0x00000005"
				Else
					Local l_D3DDisplayMode:D3DDISPLAYMODE = New D3DDISPLAYMODE
					If l_D3D.GetAdapterDisplayMode(D3DADAPTER_DEFAULT, l_D3DDisplayMode) < 0
						Print "0x00000006"
					Else
						If l_D3DDevice.CreateOffscreenPlainSurface(l_D3DDisplayMode.Width, l_D3DDisplayMode.Height, l_D3DDisplayMode.Format, D3DPOOL_SYSTEMMEM, l_RecordRT, Null) < 0
							Print "0x00000007"
						Else
							If l_D3DDevice.GetRenderTargetData(l_DesktopRT, l_RecordRT) < 0
								Print "0x00000008"
							Else
								Local File:String = "C:\Dokumente und Einstellungen\Teilnehmer\Desktop\" + MilliSecs() + ".png"
								Local FileCStr:Byte Ptr = File.ToCString()
								Print D3DXSaveSurfaceToFile(FileCStr, 3, Byte Ptr(l_RecordRT), Null, Null)
								If D3DXSaveSurfaceToFile(FileCStr, 3, Byte Ptr(l_RecordRT), Null, Null) <> 0
									Print "0x00000009"
								Else
									Print "Success?"
								EndIf
								MemFree FileCStr
							EndIf
							l_RecordRT.Release_
						EndIf
						l_D3DDisplayMode = Null
					EndIf
					l_DesktopRT.Release_
				EndIf
			EndIf
			DestroyWindow l_hWnd
		EndIf
	EndIf
	l_D3D.Release_
EndIf


Rem
   //copy the render target to the destination surface.
   hr = Device->GetRenderTargetData(pRenderTarget, pDestTarget);
   //save its contents to a bitmap file.
   hr = D3DXSaveSurfaceToFile(file,
                              D3DXIFF_BMP,
                              pDestTarget,
                              NULL,
                              NULL);