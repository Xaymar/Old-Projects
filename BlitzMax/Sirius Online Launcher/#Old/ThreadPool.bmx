SuperStrict

Import BRL.Threads
Import BRL.LinkedList
Import BRL.StandardIO

Const DEF_THREADPOOL_MINTHREADS:Int = 1
Const DEF_THREADPOOL_MAXTHREADS:Int = 4

Type TThreadPool
	' Threads
	Field m_oThreads:TList	 = New TList
	Field m_iMinThreads:Int	 = DEF_THREADPOOL_MINTHREADS
	Field m_iMaxThreads:Int	 = DEF_THREADPOOL_MAXTHREADS
	
	' Task Queue
	Field m_oTaskList:TList		 = New TList
	Field m_oTaskMutex:TMutex	 = TMutex.Create()
	
	' Construction
	Function Create:TThreadPool(iMinThreads:Int = DEF_THREADPOOL_MINTHREADS, iMaxThreads:Int = DEF_THREADPOOL_MAXTHREADS)
		Local oThreadPool:TThreadPool = New TThreadPool
		oThreadPool.SetLimits(iMinThreads, iMaxThreads)
		Return oThreadPool
	EndFunction
	
	' Destruction
	Method Destroy()
		For Local oThreadWorker:TThreadWorker = EachIn m_oThreads
			oThreadWorker.ForceDestroy()
		Next
		m_oThreads.Clear()
		m_oTaskList.Clear()
	EndMethod
	
	' Change the thread limits, for example when the game configuration changes. Effective immediately or after 
	Method SetLimits(iMinThreads:Int = DEF_THREADPOOL_MINTHREADS, iMaxThreads:Int = DEF_THREADPOOL_MAXTHREADS)
		m_iMinThreads = iMinThreads
		If (iMaxThreads < iMinThreads) Then iMaxThreads = iMinThreads
		m_iMaxThreads = iMaxThreads
	EndMethod
	
	' Add new Task to the queue.
	Method AddTask(oTask:TTask)
		m_oTaskMutex.Lock()
		m_oTaskList.AddLast(oTask)
		m_oTaskMutex.Unlock()
	EndMethod
	
	' Distributes work across threads, destroys and spawns threads
	Method Update:Int()
		Local iThreadCount:Int = m_oThreads.Count()
		
		' Spawn more Threads if we don't have at least m_iMinThreads
		If iThreadCount < m_iMinThreads Then
			Local iSpawnCount:Int = m_iMinThreads - iThreadCount
			For Local spawn:Int = 1 To iSpawnCount
				m_oThreads.AddLast(New TThreadWorker)
				iThreadCount :+ 1
			Next
		EndIf
		
		For Local oThreadWorker:TThreadWorker = EachIn m_oThreads
			' Loosely enforce m_iMaxThreads by destroying unused Threads.
			If iThreadCount > m_iMaxThreads And oThreadWorker.m_oAvailable.TryLock() = True Then
				If oThreadWorker.m_oTask = Null Then
					oThreadWorker.m_oAvailable.Unlock()
					oThreadWorker.Destroy()
					m_oThreads.Remove(oThreadWorker)
					iThreadCount :- 1
					Exit
				EndIf
			EndIf
			
			' Distribute available work.
			m_oTaskMutex.Lock()
			If m_oTaskList.Count() > 0 And oThreadWorker.m_oAvailable.TryLock() = True Then
				oThreadWorker.m_oAvailable.Unlock()
				Local oTask:TTask = TTask(m_oTaskList.RemoveFirst())
				If oThreadWorker.AssignTask(oTask) = False Then m_oTaskList.AddLast(oTask)
			EndIf
			m_oTaskMutex.Unlock()
			
			' Destroy Threads that have been waiting too long on Work, given that they are unneeded.
			If iThreadCount > m_iMinThreads And oThreadWorker.m_oAvailable.TryLock() = True Then
				oThreadWorker.m_oAvailable.Unlock()
				If oThreadWorker.m_oTask = Null And (MilliSecs() - oThreadWorker.m_lTaskTime) > 5000 Then
					m_oThreads.Remove(oThreadWorker)
					oThreadWorker.Destroy()
					iThreadCount :- 1
				EndIf
			EndIf
		Next
		
		' Spawn more Threads if we did not hit m_iMaxThreads and there is still work left.
		m_oTaskMutex.Lock()
		If m_oTaskList.Count() > 0 And iThreadCount < m_iMaxThreads Then
			Local iSpawnCount:Int = Min(m_oTaskList.Count(), m_iMaxThreads - iThreadCount)
			For Local spawn:Int = 1 To iSpawnCount
				m_oThreads.AddLast(New TThreadWorker)
				iThreadCount :+ 1
			Next
		EndIf
		m_oTaskMutex.Unlock()
		
		Return iThreadCount
	EndMethod

	Method CountTasks:Int()
		Local retVal:Int = 0
		m_oTaskMutex.Lock()
		retVal = m_oTaskList.Count()
		m_oTaskMutex.Unlock()
		Return retVal
	EndMethod
	
	Method CountActiveTasks:Int()
		Local retVal:Int = 0
		For Local oThreadWorker:TThreadWorker = EachIn m_oThreads
			oThreadWorker.m_oAvailable.Lock()
			If oThreadWorker.m_oTask <> Null Then retVal :+ 1
			oThreadWorker.m_oAvailable.Unlock()
		Next
		Return retVal
	EndMethod
EndType

Type TThreadWorker
	Field m_oThread:TThread
	' Used to make the Thread sleep and work.
	Field m_oAvailable:TMutex
	Field m_oCondVar:TCondVar
	' Contains Task and the time it was assigned at.
	Field m_oTask:TTask
	Field m_lTaskTime:Long
	Field d_iCount:Int
	
	' Construction
	Method New()
		m_oAvailable = TMutex.Create()
		m_oCondVar	 = TCondVar.Create()
		m_oThread	 = TThread.Create(Execute, Self)
		m_lTaskTime	 = MilliSecs()
	EndMethod
	
	' Destruction
	Method Destroy()
		While AssignTask(oTaskTerminate) = False
			Delay 10
		Wend
		m_oThread.Wait() 'broken?
		m_oThread.Detach()
	EndMethod
	
	Method ForceDestroy()
		m_oThread.Detach()
	EndMethod
	
	' Task Management
	Method AssignTask:Int(oTask:TTask)
		If m_oAvailable.TryLock() = True And m_oTask = Null Then
			m_oTask		 = oTask
			m_lTaskTime	 = MilliSecs()
			m_oAvailable.Unlock()
			m_oCondVar.Signal()
			Return True
		EndIf
		Return False
	EndMethod
	
	' Thread Wrapper Function
	Function Execute:Object(Data:Object)
		Local oThreadWorker:TThreadWorker = TThreadWorker(Data)
		
		Local oTask:TTask = Null
		oThreadWorker.m_oAvailable.Lock()
		Repeat
			oThreadWorker.m_oCondVar.Wait(oThreadWorker.m_oAvailable)
			
			If oThreadWorker.m_oTask <> Null Then
				oThreadWorker.d_iCount :+ 1
				oTask = oThreadWorker.m_oTask
				oThreadWorker.m_oAvailable.Unlock()
				oTask.Run()
				oThreadWorker.m_oAvailable.Lock()
				oThreadWorker.m_oTask = Null
			EndIf
		Until oTask = oTaskTerminate
		oThreadWorker.m_oAvailable.Unlock()
	EndFunction
EndType

Type TTask Abstract
	' Called when a thread is working on this item.
	Method Run()
	EndMethod
EndType

Type TTaskTerminate Extends TTask
EndType
Global oTaskTerminate:TTask = New TTaskTerminate

Type TTaskPtr Extends TTask
	Field m_pFunc:Object(data:Object)
	Field m_pData:Object
	Field m_pReturn:Object
	
	' Construction
	Function Create:TTask(pFunc:Object(data:Object), pData:Object)
		Local oTWPtr:TTaskPtr = New TTaskPtr
		oTWPtr.SetFunction(pFunc)
		oTWPtr.SetData(pData)
		Return oTWPtr
	EndFunction
	
	' Called when a thread is working on this item.
	Method Run()
		m_pReturn = m_pFunc(m_pData)
	EndMethod
	
	' Set the Function executed by Run()
	Method SetFunction(pFunc:Object(data:Object))
		m_pFunc = pFunc
	EndMethod
	
	' Set the Data passed to the Function.
	Method SetData(pData:Object)
		m_pData = pData
	EndMethod
	
	' Retrieve the return value of the executed Function
	Method GetReturn:Object()
		Return m_pReturn
	EndMethod
EndType

Global PrintMutex:TMutex = TMutex.Create()
Function _Print(Str:String)
	PrintMutex.Lock()
	Print(Str)
	PrintMutex.Unlock()
EndFunction