Global Logger_Stream

Function Logger_Initialize(File$)
	File = "Logs/" + File
	CreateDir("./Logs/")
	If FileType("./Logs/") <> 2 Then RuntimeError("Unable To create Log File."+Chr(10)+" If the program is running in a protected directory,"+Chr(10)+" consider running it as an administrator.")
	
	Logger_Stream = OpenFile(File)
	If Logger_Stream = 0
		Logger_Stream = WriteFile(File)
		If Logger_Stream = 0 Then RuntimeError("Unable to create log file."+Chr(10)+" If the program is running in a protected directory,"+Chr(10)+" consider running it as an administrator.")
	EndIf
End Function

Function Logger_Info(Module$, Message$)
	WriteLine Logger_Stream, "[" + CurrentTime() + "] [Info] " + Module + ": " + Message
End Function

Function Logger_Warning(Module$, Message$)
	WriteLine Logger_Stream, "[" + CurrentTime() + "] [Warn] " + Module + ": " + Message
End Function

Function Logger_Error(Module$, Message$)
	WriteLine Logger_Stream, "[" + CurrentTime() + "] [Errr] " + Module + ": " + Message
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D