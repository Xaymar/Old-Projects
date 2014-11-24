'Function DebugProgressCB:Int(data:Object,dltotal:Double,dlnow:Double,ultotal:Double,ulnow:Double)
'	DebugLog "dltotal="+Int(dltotal)+",dlnow="+Int(dlnow)+",ultotal="+Int(ultotal)+",ulnow="+Int(ulnow)
'EndFunction


Rem
Type TTaskPatchCheck Extends TTaskPatch
	Field m_stFile:String
	Field m_stHash:String
	Field l_bValid:Byte = True
	
	Method Run()
		' Retrieve Hash
		Local stHash:String = FileMD5(m_stFile).ToUpper();l_fProgress = 0.5
		
		' Compare Hash
		If stHash <> m_stHash Then
			l_bValid = False
			oPatchListMutex.Lock()
			oPatchList.AddLast(m_stFile)
			oPatchListMutex.Unlock()
		EndIf
		l_fProgress = 1.0
	EndMethod
	
	Function Create:TTask(File:String, Hash:String)
		Local oTask:TTaskPatchCheck = New TTaskPatchCheck
		oTask.m_stFile = File
		oTask.m_stHash = Hash
		Return oTask
	EndFunction
EndType

Type TTaskPatchFile Extends TTaskPatch
	Field m_stFile:String
	
	Method Run()
		Local oCurl:TCurlEasy = New TCurlEasy
		oCurl.setOptInt(CURLOPT_FOLLOWLOCATION, 1)
		oCurl.setOptString(CURLOPT_USERAGENT, "Sirius Online Launcher")		
		oCurl.setOptString(CURLOPT_REFERER, TPatcher_Server)
		oCurl.setOptString(CURLOPT_URL, TPatcher_Server + m_stFile)
		oCurl.setWriteString()
		
		
	EndMethod
	
	Function Create:TTask(File:String)
		Local oTask:TTaskPatchFile = New TTaskPatchFile
		oTask.m_stFile = File
		Return oTask
	EndFunction
EndType

Rem
Import BRL.LinkedList

Const TPatcher_MaxParallelTasks:Int = 4
Const TPatcher_Server:String = "http://sirius-online.us.to/patch/"

Type TPatcher
	' TCurlMulti Instance for non-blocking IO.
	Field oCurlMulti:TCurlMulti = TCurlMulti.Create()
	
	' List of available tasks and active tasks.
	Field oTasks:TList				 = (New TList)
	Field oTaskSlots:TTaskSlot[]	 = New TTaskSlot[TPatcher_MaxParallelTasks]
	Field oTasksDone:TList			 = (New TList)
	
	Method New()
		For Local i:Int = 0 Until TPatcher_MaxParallelTasks
			oTaskSlots[i].oCurl = oCurlMulti.newEasy()
		next
		
		oTasks.AddLast(TTask_PatchInfo.Create())
	EndMethod
	
	' Main loop for TPatcher
	Method Perform()
		Local runningHandles:Int
		
		If oTaskSlots[0].oTask <> Null or oTaskSlots[1].oTask <> Null or oTaskSlots[2].oTask <> Null or oTaskSlots[3].oTask <> Null Then
			Local iResult:Int = CURLM_OK
			Repeat
				iResult = oCurlMulti.multiPerform(runningHandles)
				
				For Local slot:Int = 0 Until TPatcher_MaxParallelTasks
					If oTaskSlots[slot].oTask = Null And oTasks.Count() > 0 Then
						oTaskSlots[slot].oTask = TTask(oTasks.RemoveLast())
						oTaskSlots[slot].oTask.Initialize(Self, oTaskSlots[slot].oCurl)
					ElseIf oTaskSlots[slot].oTask <> Null Then
						If oTaskSlots[slot].oTask.bComplete = True Then
							oTasksDone.AddLast(oTaskSlots[slot].oTask)
							oTaskSlots[slot].oTask = Null
							
							' Restore TCurlEasy to useable state.
							oCurlMulti.multiRemove(oTaskSlots[slot].oCurl)
							oTaskSlots[slot].oCurl.cleanup()
							oCurlMulti.multiAdd(oTaskSlots[slot].oCurl)
						EndIf
					Endif
				Next
			Until iResult <> CURLM_CALL_MULTI_PERFORM
		Else
			oCurlMulti.cleanup()
		EndIf
		
	EndMethod
EndType

Type TTaskSlot
	Field oTask:TTask
	Field oCurl:TCurlEasy
EndType

Type TTask Abstract
	Field oPatcher:TPatcher	 = Null
	Field stName:String 	 = "Unknown"
	Field bComplete:Byte	 = False
	Field fProgress:Float	 = 0.0
	
	Method Initialize(oPatcher:TPatcher, oCurl:TCurlEasy)
		Self.oPatcher = oPatcher
		oCurl.setWriteCallback(_HandleWriteCallback, Self)
		oCurl.setProgressCallback(_HandleProgressCallback, Self)
	EndMethod
	
	Method HandleProgressCallback:Int(dltotal:Double, dlnow:Double, ultotal:Double, ulnow:Double)
		fProgress = dlnow / dltotal
		If dlnow = dltotal Then bComplete = True
	EndMethod
	Method HandleWriteCallback:Int(buffer:Byte Ptr, size:Int)
	EndMethod
	
	Function _HandleProgressCallback:Int(Data:Object, dltotal:Double, dlnow:Double, ultotal:Double, ulnow:Double)
		Local Task:TTask = TTask(Data)
		Return Task.HandleProgressCallback(dltotal, dlnow, ultotal, ulnow)
	EndFunction
	Function _HandleWriteCallback(buffer:Byte Ptr, size:Int, Data:Object)
		Local Task:TTask = TTask(Data)
		Return Task.HandleWriteCallback(buffer, size)
	EndFunction
EndType

Type TTask_PatchInfo Extends TTask
	Method New()
		Self.stName = "Retrieving Patch Information"
	EndMethod
	
	Method Initialize(oPatcher:TPatcher, oCurl:TCurlEasy)
		Super.Initialize(oPatcher, oCurl)
		oCurl.setOptInt(CURLOPT_FOLLOWLOCATION, 1)
		oCurl.setOptString(CURLOPT_USERAGENT, "Sirius Online Launcher")		
		oCurl.setOptString(CURLOPT_REFERER, TPatcher_Server)
		oCurl.setOptString(CURLOPT_URL, TPatcher_Server + "info")
		oCurl.setWriteString()
	EndMethod
	Method HandleProgressCallback:Int(dltotal:Double, dlnow:Double, ultotal:Double, ulnow:Double)
		Super.HandleProgressCallback()
		
		If bComplete = True Then
			
		EndIf
	EndMethod
	
	Function Create:TTask()
		Return (New TTask_PatchInfo)
	EndFunction
EndType

Type TTask_File Extends TTask
	Field stHash:String
	Field stFile:String

	Method Initialize(oPatcher:TPatcher, oCurl:TCurlEasy)
		Super.Initialize(oPatcher, oCurl)
		oCurl.setOptInt(CURLOPT_FOLLOWLOCATION, 1)
		oCurl.setOptString(CURLOPT_USERAGENT, "Sirius Online Launcher")		
		oCurl.setOptString(CURLOPT_REFERER, TPatcher_Server + "info")
		
		'Local oFileStream:TStream = 
		oCurl.setWriteString()
	EndMethod
	
	Method HandleProgressCallback:Int(dltotal:Double, dlnow:Double, ultotal:Double, ulnow:Double)
		Super.HandleProgressCallback()
	EndMethod
	
	Function Create:TTask(File:String, Hash:String)
		Local oTask:TTask_File = New TTask_File
		oTask.stHash = Hash
		oTask.stFile = File
		oCurl.setOptString(CURLOPT_URL, TPatcher_Server + oTask.File)
		Return oTask
	EndFunction
EndType
EndRem