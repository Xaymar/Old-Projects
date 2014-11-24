SuperStrict

Import BaH.libcurl
Import BRL.Bank
Import BRL.FileSystem
Import BRL.LinkedList
Import BRL.StandardIO
Import BRL.Stream
Import BRL.Timer
Import "Clock.c"
Import "Digest.bmx"

Type TPatcher
	Const STATE_DEFAULT:Byte	 = 0
	Const STATE_PATCHINFO:Byte	 = 1
	Const STATE_CHECKING:Byte	 = 2
	Const STATE_PREPATCH:Byte	 = 3
	Const STATE_PATCHING:Byte	 = 4
	Const STATE_COMPLETE:Byte	 = 5
	Const STATE_LAUNCHER:Byte	 = 6
	Const SSTATE_ENTER:Byte	 = 0
	Const SSTATE_WORK:Byte	 = 1
	Const SSTATE_LEAVE:Byte	 = 2
	Const SERVER:String	 = "http://sirius-online.us.to/patch/"
	
	' State-based Patcher
	Field m_bState:Byte		 = TPatcher.STATE_DEFAULT
	Field m_bSubState:Byte	 = TPatcher.SSTATE_ENTER
	
	' lib/cURL is used for downloading of data.
	Field m_oCurlMulti:TCurlMulti	 = Null
	Field m_oCurl:TCurlEasy			 = Null
	Field m_fCurlProgress:Float
	
	' State: All
	Field m_oTaskList:TList		 = New TList
	Field m_fProgress:Float		 = 0
	
	' State: Patchinfo, Checking, Patching
	Field m_oFileHashList:TList	 = New TList
	Field m_iFileCount:Int	 = 0
	
	' State: Checking, Patching
	Field m_oFileLoader:TAsyncLoader = New TAsyncLoader
	Field m_oFileHashPair:TFileHash	 = Null
	
	' State: Patching
	Field m_oFileList:TList = New TList
	Field m_oFile:String, m_oFileDL:String
	Field m_oFileStream:TStream
	
	' Construction
	Method New()
	EndMethod
	
	Function Create:TPatcher()
		Local oPatcher:TPatcher = New TPatcher
		Return oPatcher
	EndFunction
	
	' Deconstruction
	Method Destroy()
		m_oCurlMulti.multiCleanup()
		m_oCurl.cleanup()
	EndMethod
	
	
	' Basically the Main Loop of the Patcher, handles everything. Call GetShutdown:String() if you want to know when the patcher is telling you to run an executable and close.
	Method Update()
		' Check 
		Select m_bState
			Case TPatcher.STATE_DEFAULT
				If m_bSubState = TPatcher.SSTATE_ENTER Then
					m_oCurlMulti = TCurlMulti.Create()
					m_oCurl = m_oCurlMulti.newEasy()
					m_oCurl.setOptInt(CURLOPT_FOLLOWLOCATION, 1)
					m_oCurl.setOptInt(CURLOPT_CRLF, False)
					m_oCurl.setOptString(CURLOPT_USERAGENT, "Sirius Online Launcher")		
					m_oCurl.setOptString(CURLOPT_REFERER, TPatcher.SERVER)
					m_oCurl.setOptString(CURLOPT_URL, TPatcher.SERVER + "info")
					m_oCurl.setProgressCallback(TPatcher.ProgressCallback, Self)
					m_oCurl.setWriteString()
					m_oTaskList.AddFirst("n[WAIT] Getting patch information...")
					
					m_bSubState = TPatcher.SSTATE_WORK
				EndIf
				
				If m_bSubState = TPatcher.SSTATE_WORK Then
					Local iResult:Int = Perform()
					
					m_fProgress = m_fCurlProgress
					m_oTaskList.RemoveFirst()
					m_oTaskList.AddFirst("g[" + RSet(String(Int(m_fProgress * 100)), 3) + "%] Retrieving patch information...")
					
					If iResult = CURLM_OK Then
						If m_fCurlProgress = 1.0 And m_iCurlMultiHandles = 0 Then
							m_bSubState = TPatcher.SSTATE_LEAVE
						EndIf
					Else
						m_oTaskList.RemoveFirst()
						m_oTaskList.AddFirst("g[FAIL] Retrieving patch information... ("+CurlError(iResult)+")")
						SetState(TPatcher.STATE_COMPLETE)
					EndIf
				EndIf
				
				If m_bSubState = TPatcher.SSTATE_LEAVE Then
					m_oCurlMulti.multiRemove(m_oCurl)
					m_oCurlMulti.multiCleanup()
					m_oCurlMulti = Null
					
					Local oCurlRCode:Int = m_oCurl.getInfo().responseCode()
					Local stResult:String = m_oCurl.toString()
					m_oCurl.cleanup()
					m_oCurl = Null
					
					If oCurlRCode <> 404 Then
						m_oFileHashList.Clear()
						If stResult.length > 0 Then
							Local stFileHashArr:String[] = stResult.Split("~n")
							For Local stFileHashPair:String = EachIn stFileHashArr
								If stFileHashPair.length > 0 And stFileHashPair[0..1] <> "#" Then ' Ignore Comments
									Local stPairArr:String[] = stFileHashPair.Split(":")
									m_oFileHashList.AddLast(TFileHash.Create(stPairArr[0], stPairArr[1]))
								EndIf
							Next
							m_iFileCount = m_oFileHashList.Count()
							m_oFileList.Clear()
							SetState(TPatcher.STATE_PATCHINFO) ' Got Patchinfo, and it had information.
							m_oTaskList.RemoveFirst()
							m_oTaskList.AddFirst("g[ OK ] Retrieving patch information... done.")
						Else
							SetState(TPatcher.STATE_COMPLETE)
							m_oTaskList.RemoveFirst()
							m_oTaskList.AddFirst("e[FAIL] Retrieving patch information... invalid information file.")
						EndIf
					Else
						SetState(TPatcher.STATE_COMPLETE)
						m_oTaskList.RemoveFirst()
						m_oTaskList.AddFirst("e[FAIL] Retrieving patch information... file not found.")
					EndIf
				EndIf
			Case TPatcher.STATE_CHECKING
				If m_bSubState = TPatcher.SSTATE_ENTER Then ' Used to load a new File
					m_oFileHashPair = TFileHash(m_oFileHashList.RemoveFirst())
					If m_oFileHashPair <> Null Then
						m_oFileLoader.Initialize(m_oFileHashPair.stName)
						m_oTaskList.AddFirst("n[WAIT] Checking '" + m_oFileHashPair.stName + "'...")
						
						m_bSubState = TPatcher.SSTATE_WORK
					Else
						m_bSubState = TPatcher.SSTATE_LEAVE
					EndIf
				EndIf
				
				If m_bSubState = TPatcher.SSTATE_WORK Then
					If m_oFileLoader.m_bComplete = 0 Then
						m_oFileLoader.Process(2, 1024)
						m_fProgress = ((m_iFileCount - (m_oFileHashList.Count() + 1)) / Float(m_iFileCount)) + m_oFileLoader.m_fProgress * (1.0 / m_iFileCount)
						m_oTaskList.RemoveFirst()
						m_oTaskList.AddFirst("g[" + RSet(String(Int(m_fProgress * 100)), 3) + "%] Checking '" + m_oFileHashPair.stName + "'...")
					Else
						If m_oFileLoader.m_bComplete = 1
							If m_oFileLoader.m_oBank <> Null Then
								Local stHash:String = MD5Bank(m_oFileLoader.m_oBank).ToUpper()
								If stHash <> m_oFileHashPair.stHash Then
									m_oFileList.AddLast(m_oFileHashPair.stName)
									m_oTaskList.RemoveFirst()
									m_oTaskList.AddFirst("h[ OK ] Checking '" + m_oFileHashPair.stName + "'... requires update.")
								Else
									m_oTaskList.RemoveFirst()
									m_oTaskList.AddFirst("g[ OK ] Checking '" + m_oFileHashPair.stName + "'... up to date.")
								EndIf
							Else
								m_oTaskList.RemoveFirst()
								m_oTaskList.AddFirst("e[FAIL] Checking '" + m_oFileHashPair.stName + "'... (File in use?)")
							EndIf
						Else
							m_oFileList.AddLast(m_oFileHashPair.stName)
							m_oTaskList.RemoveFirst()
							m_oTaskList.AddFirst("h[ OK ] Checking '" + m_oFileHashPair.stName + "'... missing.")
						EndIf
						m_bSubState = TPatcher.SSTATE_ENTER
						m_oFileLoader.Cleanup()
					EndIf
				EndIf
				
				If m_bSubState = TPatcher.SSTATE_LEAVE Then
					If m_oFileList.Count() > 0 Then
						m_iFileCount = m_oFileList.Count()
						SetState(TPatcher.STATE_PREPATCH)
					Else
						SetState(TPatcher.STATE_COMPLETE)
					EndIf
				EndIf
			Case TPatcher.STATE_PATCHING
				If m_bSubState = TPatcher.SSTATE_ENTER Then
					Local m_oEntry:Object = m_oFileList.RemoveFirst()
					If m_oEntry <> Null Then
						m_oFile = String(m_oEntry);m_oFileDL = m_oFile + ".part"
						
						CreateDir(ExtractDir(m_oFile), True)
						m_oFileStream = WriteFile(m_oFileDL)
						If m_oFileStream <> Null Then
							m_oCurlMulti = TCurlMulti.Create()
							m_oCurl = m_oCurlMulti.newEasy()
							m_oCurl.setOptInt(CURLOPT_FOLLOWLOCATION, 1)
							m_oCurl.setOptInt(CURLOPT_CRLF, False)
							m_oCurl.setOptString(CURLOPT_USERAGENT, "Sirius Online Launcher")		
							m_oCurl.setOptString(CURLOPT_REFERER, TPatcher.SERVER + "info")
							m_oCurl.setOptString(CURLOPT_URL, TPatcher.SERVER + m_oFile)
							m_oCurl.setWriteStream(m_oFileStream)
							m_oCurl.setProgressCallback(TPatcher.ProgressCallback, Self)
							m_oTaskList.AddFirst("n[WAIT] Downloading '" + m_oFile + "'...")
							
							m_bSubState = TPatcher.SSTATE_WORK
						EndIf
					Else
						SetState(TPatcher.STATE_DEFAULT)
					EndIf
				EndIf
				
				If m_bSubState = TPatcher.SSTATE_WORK Then
					Local iResult:Int = Perform()
					m_fProgress = ((m_iFileCount - (m_oFileList.Count() + 1)) / Float(m_iFileCount)) + m_fCurlProgress * (1.0 / m_iFileCount)
					If iResult = CURLM_OK Then
						m_oFileStream.Flush()
						If m_fCurlProgress = 1.0 And m_iCurlMultiHandles = 0 Then
							m_oCurlMulti.multiRemove(m_oCurl)
							m_oCurlMulti.multiCleanup()
							
							Local oCurlRCode:Int = m_oCurl.getInfo().responseCode()
							m_oCurl.cleanup()
							
							' Cleanup(1)
							m_oCurl = Null
							m_oCurlMulti = Null
							m_oFileStream.Close()
							m_oFileStream = Null
							
							If oCurlRCode <> 404 Then 
								m_oTaskList.RemoveFirst()
								m_oTaskList.AddFirst("g[ OK ] Downloading '" + m_oFile + "'...")
								
								' Rename files
								If m_oFile <> "Launcher.exe" Then
									RenameFile(m_oFile, m_oFile + ".old")
									If RenameFile(m_oFileDL, m_oFile) Then
										DeleteFile(m_oFile + ".old")
									Else
										RenameFile(m_oFile + ".old", m_oFile)
									EndIf
								Else' If we patched the launcher, tell the parent program to start the new one.
									RenameFile(m_oFileDL, "-" + m_oFile)
									m_oFileList.Clear() ' Clear the list to be sure
									SetState(TPatcher.STATE_LAUNCHER)
								EndIf
								
							Else
								DeleteFile(m_oFileDL)
								m_oTaskList.RemoveFirst()
								m_oTaskList.AddFirst("e[FAIL] Downloading '" + m_oFile + "'... file not found.")
							EndIf
							
							'Cleanup (2)
							m_oFileDL = Null
							m_oFile = Null
							
							If m_oFileList.Count() = 0 Then
								m_bSubState = TPatcher.SSTATE_LEAVE
							Else
								m_bSubState = TPatcher.SSTATE_ENTER
							EndIf
						Else
							m_oTaskList.RemoveFirst()
							m_oTaskList.AddFirst("n[" + RSet(String(Int(m_fCurlProgress * 100)), 3) + "%] Downloading '" + m_oFile + "'...")
						EndIf
					Else
						m_oFileStream.Close()
						m_oTaskList.RemoveFirst()
						m_oTaskList.AddFirst("e[FAIL] Downloading '" + m_oFile + "'... " + CurlError(iResult))
					EndIf
				EndIf
				
				If m_bSubState = TPatcher.SSTATE_LEAVE And m_bState = TPatcher.STATE_PATCHING Then
					SetState(TPatcher.STATE_DEFAULT)
				EndIf
		EndSelect
	EndMethod
	
	Method Advance()
		Select m_bState
			Case TPatcher.STATE_PATCHINFO
				SetState(TPatcher.STATE_CHECKING)
			Case TPatcher.STATE_PREPATCH
				SetState(TPatcher.STATE_PATCHING)
			Case TPatcher.STATE_COMPLETE
				m_stProcessToRun = "ExeFile.exe"
			Case TPatcher.STATE_LAUNCHER
				m_stProcessToRun = "-Launcher.exe 1"
		EndSelect
	EndMethod
	
	Field m_iCurlMultiHandles:Int, m_bCurlMultiState:Byte = 0
	Method Perform:Int()
		Local iResult:Int = 0
		
		iResult = MultiPerform()
		Select m_bCurlMultiState
			Case 0, 2
				m_bCurlMultiState = (m_bCurlMultiState + 1) Mod 3
			Case 1
				If m_oCurlMulti.multiSelect(0.1) <> -1 And m_iCurlMultiHandles Then
					iResult = MultiPerform()
					If m_iCurlMultiHandles = 0 Then m_bCurlMultiState = 2
				EndIf
		EndSelect
		
		Return iResult
	EndMethod
	
	Method MultiPerform:Int()
		Local iResult:Int = m_oCurlMulti.multiPerform(m_iCurlMultiHandles)
		While iResult = CURLM_CALL_MULTI_PERFORM 
			iResult = m_oCurlMulti.multiPerform(m_iCurlMultiHandles)
		Wend
		Return iResult
	EndMethod
	
	' State Management
	Method SetState(bState:Byte)
		m_bState = bState
		m_bSubState = TPatcher.SSTATE_ENTER
		m_fProgress = 0.0
	EndMethod
	
	Method GetState:Byte()
		Return m_bState
	EndMethod
	
	' Information
	Method GetProgress:Float()
		Return m_fProgress
	EndMethod
	

	
	Field m_stProcessToRun:String = NUll
	Method GetShutdown:String()
		Return m_stProcessToRun
	EndMethod
	' Callbacks
	Function ProgressCallback:Int(oPatcherObj:Object, dDownloadTotal:Double, dDownloadNow:Double, dUploadTotal:Double, dUploadNow:Double)
		If dDownloadTotal = 0 And dDownloadNow = 0 And dUploadTotal = 0 And dUploadNow = 0 Then TPatcher(oPatcherObj).m_fCurlProgress = 0.0;Return 0
		TPatcher(oPatcherObj).m_fCurlProgress = (dDownloadNow / dDownloadTotal)
	EndFunction
EndType

Type TFileHash
	Field stName:String, stHash:String
	
	Function Create:TFileHash(stName:String, stHash:String)
		Local oFH:TFileHash = New TFileHash
		oFH.stName = stName
		oFH.stHash = stHash
		Return oFH
	EndFunction
EndType

Type TAsyncLoader
	Field m_oBank:TBank
	Field m_oStream:TStream, m_iFileSize:Int
	Field m_fProgress:Float, m_bComplete:Byte
	
	Method Initialize(stFile:String)
		If FileType(stFile) = FILETYPE_FILE Then
			m_oStream = ReadFile(stFile)
			If m_oStream <> Null Then
				m_iFileSize = FileSize(stFile)
				m_oBank = CreateBank(m_iFileSize)
				If m_oBank <> Null Then
					m_fProgress = 0.0
					m_bComplete = False
				Else
					m_oStream.Close()
				EndIf
			Else
				m_fProgress = 1.0
				m_bComplete = -2
			EndIf
		Else
			m_fProgress = 1.0
			m_bComplete = -2
		EndIf
	EndMethod
	
	Method Process(iMs:Float, iBufferSize:Int = 1024)
		Local iClockStart:Int = getClock()
		If (m_oStream <> Null And m_oBank <> Null) And m_oStream.Eof() = False Then
			Local iClockNow:Int = getClock()
			Repeat
				Local iRemaining:Int = Min(m_iFileSize - m_oStream.Pos(), iBufferSize)
				m_oBank.Read(m_oStream, m_oStream.Pos(), iRemaining) 
				
				iClockNow = getClock()
			Until m_oStream.Eof() = True or getClockDiff(iClockStart, iClockNow) > (iMs / 1000.0 * getClocksPerSecond())
			
			m_fProgress = m_oStream.Pos() / Float(m_iFileSize)
			m_bComplete = m_oStream.Eof()
			If m_oStream.Eof() = True Then
				m_oStream.Close()
				m_oStream = Null
			EndIf
		ElseIf m_bComplete = 0 Then
			m_fProgress = 1.0
			m_bComplete = -1
		EndIf
	EndMethod
	
	Method Cleanup()
		m_oBank = Null
	EndMethod
EndType




Function DebugClock(Text:String)
	DebugLog getClock() + ":" + Text
EndFunction

Extern "c"
Function getClocksPerSecond:Int()="getClocksPerSecond_"
Function getClock:Int()="getClock_"
Function getClockDiff:Int(clockStart:Int, clockEnd:Int)="getClockDiff_"
EndExtern