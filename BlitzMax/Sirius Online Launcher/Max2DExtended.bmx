SuperStrict

Import BRL.Max2D
Import BRL.TextStream
Import PUB.OpenGL
Import PUB.Glew

Const MODE_REPEATX:Byte		 = $00000001	' Repeat.
Const MODE_REPEATY:Byte		 = $00000010
Const MODE_MIRRORX:Byte		 = $00000100	' Mirror every second repeat.
Const MODE_MIRRORY:Byte		 = $00001000
Const MODE_SCALEUPX:Byte	 = $00010000	' Scale up to fit the destination area if the source doesn't fit exactly in.
Const MODE_SCALEUPY:Byte	 = $00100000
Const MODE_SCALEDOWNX:Byte	 = $01000000	' Similar to MODE_SCALEUP*, except that it scales down to fit.
Const MODE_SCALEDOWNY:Byte	 = $10000000
Function DrawSubImageRectEx:Int(img:TImage, ..
		dstX:Float, dstY:Float, dstW:Float, dstH:Float, ..
		srcX:Float, srcY:Float, srcW:Float, srcH:Float, ..
		hndX:Float=0, hndY:Float=0, frame:Int=0, bFlags:Byte=0)
	Assert img <> Null Else "Image does Not exist"
	
	Local bRepeatX:Byte		 = ((bFlags & MODE_REPEATX) <> 0)
	Local bRepeatY:Byte		 = ((bFlags & MODE_REPEATY) <> 0)
	Local bMirrorX:Byte		 = ((bFlags & MODE_MIRRORX) <> 0)
	Local bMirrorY:Byte		 = ((bFlags & MODE_MIRRORY) <> 0)
	Local bScaleUpX:Byte	 = ((bFlags & MODE_SCALEUPX) <> 0)
	Local bScaleUpY:Byte	 = ((bFlags & MODE_SCALEUPY) <> 0)
	Local bScaleDownX:Byte	 = ((bFlags & MODE_SCALEDOWNX) <> 0)
	Local bScaleDownY:Byte	 = ((bFlags & MODE_SCALEDOWNY) <> 0)
	
	Local lXRepeat:Float = 1, lXScale:Float = 1
	If bRepeatX Then
		lXRepeat = dstW / srcW
		
		If bScaleUpX Then
			lXRepeat = Floor(lXRepeat)
			lXScale = (dstW / srcW) / lXRepeat
		ElseIf bScaleDownX Then
			lXRepeat = Ceil(lXRepeat)
			lXScale = (dstW / srcW) / lXRepeat
		EndIf
	Else
		lXScale = dstW / srcW
	EndIf
	
	Local lYRepeat:Float = 1, lYScale:Float = 1
	If bRepeatY Then
		lYRepeat = dstH / srcH
		
		If bScaleUpY Then
			lYRepeat = Floor(lYRepeat)
			lYScale = (dstH / srcH) / lYRepeat
		ElseIf bScaleDownY Then
			lYRepeat = Ceil(lYRepeat)
			lYScale = (dstH / srcH) / lYRepeat
		EndIf
	Else
		lYScale = dstH / srcH
	EndIf
	
	Local lXSize:Float = srcW * lXScale, lYSize:Float = srcH * lYScale
	Local lM2DScaleXOrig:Float, lM2DScaleYOrig:Float
	Local lM2DScaleX:Float, lM2DScaleY:Float
	GetScale(lM2DScaleXOrig, lM2DScaleYOrig)
	GetScale(lM2DScaleX, lM2DScaleY)
	
		' Cache some variables, so that we don't waste CPU time
	Local lXRepeatFl:Int = Floor(lXRepeat), lXRepeatOverflow:Float = lXRepeat - lXRepeatFl
	Local lYRepeatFl:Int = Floor(lYRepeat), lYRepeatOverflow:Float = lYRepeat - lYRepeatFl
	For Local lYIndex:Int = 0 To lYRepeatFl
		Local lYSizeMul:Float = 1
		Local lYPos:Float = dstY + lYIndex * lYSize
		
		If bMirrorY Then lM2DScaleY :* -1
		If (lYIndex = lYRepeatFl) Then lYSizeMul = lYRepeatOverflow
		
		For Local lXIndex:Int = 0 To lXRepeatFl
			Local lXSizeMul:Float = 1
			Local lXPos:Float = dstX + lXIndex * lXSize
			
			If bMirrorX Then lM2DScaleX :* -1
			If (lXIndex = lXRepeatFl) Then lXSizeMul = lXRepeatOverflow
			
			SetScale lM2DScaleX, lM2DScaleY
			
			DrawSubImageRect(img, lXPos, lYPos, lXSize * lXSizeMul, lYSize * lYSizeMul, srcX, srcY, srcW * lXSizeMul, srcH * lYSizeMul, hndX, hndY, frame)
		Next
	Next
	
	SetScale(lM2DScaleXOrig, lM2DScaleYOrig)
EndFunction

Function DrawTiledImage:Int(img:TImage, dstX:Float, dstY:Float, dstW:Float, dstH:Float, srcX:Float, srcY:Float, srcW:Float, srcH:Float, hndX:Float=0, hndY:Float=0, frame:Int=0, lft:Float, top:Float, rgt:Float, btm:Float, bBackground:Byte=True, bFlags:Byte=MODE_REPEATX | MODE_REPEATY)
	DrawSubImageRect(img, dstX, dstY, lft, top, srcX, srcY, lft, top, hndX, hndY, frame)
	DrawSubImageRect(img, dstX + dstW - rgt, dstY, rgt, top, srcX + srcW - rgt, srcY, rgt, top, hndX, hndY, frame)
	DrawSubImageRect(img, dstX, dstY + dstH - btm, lft, top, srcX, srcY + srcH - btm, lft, btm, hndX, hndY, frame)
	DrawSubImageRect(img, dstX + dstW - rgt, dstY + dstH - btm, rgt, top, srcX + srcW - rgt, srcY + srcH - btm, rgt, btm, hndX, hndY, frame)
	
		' Left
	DrawSubImageRectEx(img, dstX, dstY + top, lft, dstH - top - btm, srcX, srcY + top, lft, srcH - top - btm, hndX, hndY, frame, bFlags)
		' Right
	DrawSubImageRectEx(img, dstX + dstW - rgt, dstY + top, rgt, dstH - top - btm, srcX + srcW - rgt, srcY + top, rgt, srcH - top - btm, hndX, hndY, frame, bFlags)
		' Top
	DrawSubImageRectEx(img, dstX + lft, dstY, dstW - lft - rgt, top, srcX + lft, srcY, srcW - lft - rgt, top, hndX, hndY, frame, bFlags)
		' Bottom
	DrawSubImageRectEx(img, dstX + lft, dstY + dstH - btm, dstW - lft - rgt, btm, srcX + lft, srcY + srcH - btm, srcW - lft - rgt, btm, hndX, hndY, frame, bFlags)
	
	If bBackground Then DrawSubImageRectEx(img, dstX + lft, dstY + top, dstW - lft - rgt, dstH - top - btm, srcX + lft, srcY + top, srcW - lft - rgt, srcH - top - btm, hndX, hndY, frame, bFlags)
End Function





' Max2D Shader Support ---------------------------------------------------------
Type TShader
	' Functions ----------------------------------------------------------------
	Function Create:TShader()
		Return (New TShader)
	EndFunction
	
	Function CreateVertex:TShader(oVertexCode:String)
		Local oShader:TShader = TShader.Create()
		oShader.Load(oVertexCode, True)
		Return oShader
	EndFunction
	
	Function CreateFragement:TShader(oFragmentCode:String)
		Local oShader:TShader = TShader.Create()
		oShader.Load(oFragmentCode, True)
		Return oShader
	EndFunction
	
	Function CreateCombined:TShader(oVertexCode:String, oFragmentCode:String)
		Local oShader:TShader = TShader.Create()
		oShader.Load(oVertexCode, True)
		oShader.Load(oFragmentCode, False)
		Return oShader
	EndFunction
	
	Function CheckForErrors:Int(ShaderObject:Int, ErrorString:String Var, Compiled:Int = True)
		Local Successful:Int
		
		If Compiled Then
			glGetShaderiv (ShaderObject, GL_COMPILE_STATUS, Varptr Successful)
		Else
			glGetProgramiv(ShaderObject, GL_LINK_STATUS,    Varptr Successful)
		EndIf
		
		If Not Successful Then
			Local ErrorLength:Int
			glGetObjectParameterivARB(ShaderObject, GL_OBJECT_INFO_LOG_LENGTH_ARB, Varptr ErrorLength)
			
			Local Message:Byte Ptr = MemAlloc(ErrorLength), Dummy:Int
			
			glGetInfoLogARB(ShaderObject, ErrorLength, Varptr Dummy, Message)
			
			ErrorString = String.FromCString(Message)
			MemFree(Message)
			
			Return True
		EndIf
		
		Return False
	End Function
	
	Function CheckCompatability:Int()
		Local Extensions:String = String.FromCString(Byte Ptr glGetString(GL_EXTENSIONS))
		Local GLVersion:String  = String.FromCString(Byte Ptr glGetString(GL_VERSION))
		Local GLVersionInt:Int  = GLVersion[.. 3].Replace(".", "").ToInt()
		
		DebugLog Extensions
		If Extensions.Find("GL_ARB_shader_objects" ) >= 0 And ..
		   Extensions.Find("GL_ARB_vertex_shader"  ) >= 0 And ..
		   Extensions.Find("GL_ARB_fragment_shader") >= 0 or GLVersionInt >= 20 Then
			Return True
		EndIf
		
		Return False
	End Function
	
	' Variables ----------------------------------------------------------------
	Field iProgramObject:Int													' Shader Program Object
	Field tError:String														' Shader Compile Errors & Warnings
	
	' Members ------------------------------------------------------------------
	Method New()
		Self.iProgramObject = glCreateProgram()
	EndMethod
	
	Method Delete()
		glDeleteProgram(Self.iProgramObject)
	EndMethod
	
	'' Retrieves the error that happened during loading or linking.
	' @return <String> Error log retrieved from the 
	Method GetError:String()
		Return Self.tError
	EndMethod
	
	'' Loads a new shader and, if successful, attaches it to the program.
	' @param <String> tShaderCode A string containing the shader code.
	' @param <Bool> bIsVertexShader Is this code a vertex Shader
	' @return <Bool> Success
	Method Load:Byte(tShaderCode:String, bIsVertexShader:Byte = True)
		Local liShaderObject:Int
		
		If tShaderCode = Null Then Return False
		If tShaderCode.length = 0 Then Return False
		
			' Create a new Shader Object, either for Vertex or Fragment processing.
		If bIsVertexShader Then
			liShaderObject = glCreateShader(GL_VERTEX_SHADER)
		Else
			liShaderObject = glCreateShader(GL_FRAGMENT_SHADER)
		EndIf
		If liShaderObject = 0 Then Return False
		
			' Load the shader source into the compiler.
		Local lbShaderCodePtr:Byte Ptr = tShaderCode.ToCString()
		Local liShaderLength:Int = tShaderCode.length
		glShaderSource(liShaderObject, 1, VarPtr lbShaderCodePtr, VarPtr liShaderLength)
		MemFree(lbShaderCodePtr)
				
			' Compile the shader and check for errors
		glCompileShader(liShaderObject)
		If (TShader.CheckForErrors(liShaderObject, Self.tError, True) = False) Then
			glAttachShader(iProgramObject, liShaderObject)
			glDeleteShader(liShaderObject)
			Return True
		EndIf
		
		glDeleteShader(liShaderObject)
	EndMethod
	
	'' Links the attached shaders to the program, if successful.
	' @return <Bool> Success
	Method Link:Byte()
		glLinkProgramARB(Self.iProgramObject)
		Return (TShader.CheckForErrors(Self.iProgramObject, Self.tError, False) = False)
	EndMethod
	
	'' Enable the this program and disable all other programs.
	' @return <Bool> Success
	Method Enable:Byte()
		glUseProgramObjectARB(Self.iProgramObject)
		Return (TShader.CheckForErrors(Self.iProgramObject, Self.tError, False) = False)
	End Method
	
	'' Disable the all programs.
	' @return <Bool> Success
	Method Disable:Byte()
		glUseProgramObjectARB(0)
		Return (TShader.CheckForErrors(Self.iProgramObject, Self.tError, False) = False)
	End Method
	
	'' Uniform Management
	Method GetUniformLocation:Int(tUniform:String)
		Return glGetUniformLocationARB(Self.iProgramObject, tUniform)
	End Method
	
	Method SetUniform1F(tUniform:String, fValue1:Float)
		glUniform1f(GetUniformLocation(tUniform), fValue1)
	EndMethod
	Method SetUniform2F(tUniform:String, fValue1:Float, fValue2:Float)
		glUniform2f(GetUniformLocation(tUniform), fValue1, fValue2)
	EndMethod
	Method SetUniform3F(tUniform:String, fValue1:Float, fValue2:Float, fValue3:Float)
		glUniform3f(GetUniformLocation(tUniform), fValue1, fValue2, fValue3)
	EndMethod
	Method SetUniform4F(tUniform:String, fValue1:Float, fValue2:Float, fValue3:Float, fValue4:Float)
		glUniform4f(GetUniformLocation(tUniform), fValue1, fValue2, fValue3, fValue4)
	EndMethod
	
	Method SetUniform1I(tUniform:String, iValue1:Int)
		glUniform1i(GetUniformLocation(tUniform), iValue1)
	EndMethod
	Method SetUniform2I(tUniform:String, iValue1:Int, iValue2:Int)
		glUniform2i(GetUniformLocation(tUniform), iValue1, iValue2)
	EndMethod
	Method SetUniform3I(tUniform:String, iValue1:Int, iValue2:Int, iValue3:Int)
		glUniform3i(GetUniformLocation(tUniform), iValue1, iValue2, iValue3)
	EndMethod
	Method SetUniform4I(tUniform:String, iValue1:Int, iValue2:Int, iValue3:Int, iValue4:Int)
		glUniform4i(GetUniformLocation(tUniform), iValue1, iValue2, iValue3, iValue4)
	EndMethod
	
	Method SetUniform1UI(tUniform:String, iValue1:Int)
		glUniform1ui(GetUniformLocation(tUniform), iValue1)
	EndMethod
	Method SetUniform2UI(tUniform:String, iValue1:Int, iValue2:Int)
		glUniform2ui(GetUniformLocation(tUniform), iValue1, iValue2)
	EndMethod
	Method SetUniform3UI(tUniform:String, iValue1:Int, iValue2:Int, iValue3:Int)
		glUniform3ui(GetUniformLocation(tUniform), iValue1, iValue2, iValue3)
	EndMethod
	Method SetUniform4UI(tUniform:String, iValue1:Int, iValue2:Int, iValue3:Int, iValue4:Int)
		glUniform4ui(GetUniformLocation(tUniform), iValue1, iValue2, iValue3, iValue4)
	EndMethod
EndType
