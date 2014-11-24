;VectorMath.bb

Type TVector
	Field X#,Y#,Z#
End Type

Global VectorForward.TVector	= TVector_Create( 0,  0,  1)
Global VectorBackward.TVector	= TVector_Create( 0,  0, -1)
Global VectorLeft.TVector		= TVector_Create(-1,  0,  0)
Global VectorRight.TVector		= TVector_Create( 1,  0,  0)
Global VectorUp.TVector			= TVector_Create( 0,  1,  0)
Global VectorDown.TVector		= TVector_Create( 0, -1,  0)

Function TVector_Create.TVector(X#,Y#,Z#)
	Local R.TVector = New TVector
	R\X = X
	R\Y = Y
	R\Z = Z
	Return R
End Function
Function TVector_Copy.TVector(A.TVector)
	Return TVector_Create(A\X,A\Y,A\Z)
End Function

Function TVector_Add.TVector(A.TVector, B.TVector)
	Local R.TVector = New TVector
	R\X = A\X + B\X
	R\Y = A\Y + B\Y
	R\Z = A\Z + B\Z
	Return R
End Function
Function TVector_AddScalar.TVector(A.TVector, B#)
	Local R.TVector = New TVector
	R\X = A\X + B
	R\Y = A\Y + B
	R\Z = A\Z + B
	Return R
End Function

Function TVector_Subtract.TVector(A.TVector, B.TVector)
	Local R.TVector = New TVector
	R\X = A\X - B\X
	R\Y = A\Y - B\Y
	R\Z = A\Z - B\Z
	Return R
End Function
Function TVector_SubtractScalar.TVector(A.TVector, B#)
	Local R.TVector = New TVector
	R\X = A\X - B
	R\Y = A\Y - B
	R\Z = A\Z - B
	Return R
End Function

Function TVector_Multiply.TVector(A.TVector, B.TVector)
	Local R.TVector = New TVector
	R\X = A\X * B\X
	R\Y = A\Y * B\Y
	R\Z = A\Z * B\Z
	Return R
End Function
Function TVector_MultiplyScalar.TVector(A.TVector, B#)
	Local R.TVector = New TVector
	R\X = A\X * B
	R\Y = A\Y * B
	R\Z = A\Z * B
	Return R
End Function

Function TVector_Divide.TVector(A.TVector, B.TVector)
	Local R.TVector = New TVector
	R\X = A\X / B\X
	R\Y = A\Y / B\Y
	R\Z = A\Z / B\Z
	Return R
End Function
Function TVector_DivideScalar.TVector(A.TVector, B#)
	Local R.TVector = New TVector
	R\X = A\X / B
	R\Y = A\Y / B
	R\Z = A\Z / B
	Return R
End Function

Function TVector_Normalize.TVector(A.TVector, MultiPass%=True)
	Local R1.TVector, R.TVector
	R = TVector_DivideScalar(A, TVector_Length(A))
	If MultiPass Then
		R1 = R
		R = TVector_DivideScalar(R1, TVector_Length(R1))
		Delete R1
	EndIf
	Return R
End Function
Function TVector_Rotate.TVector(A.TVector, Pitch#, Yaw#, Roll#)
	Local M1.TVector = TVector_Create(Cos(Roll) * Cos(Yaw), -Sin(Roll), Sin(Yaw))
	Local M2.TVector = TVector_Create(Sin(Roll), Cos(Roll) * Cos(Pitch), -Sin(Pitch))
	Local M3.TVector = TVector_Create(-Sin(Yaw), Sin(Pitch), Cos(Yaw) * Cos(Pitch))
	
	Local R.TVector = New TVector
	R\X = (A\X * M1\X) + (A\Y * M1\Y) + (A\Z * M1\Z)
	R\Y = (A\X * M2\X) + (A\Y * M2\Y) + (A\Z * M2\Z)
	R\Z = (A\X * M3\X) + (A\Y * M3\Y) + (A\Z * M3\Z)
	Delete M1:Delete M2:Delete M3:Return R
End Function
Function TVector_RotateAround.TVector(A.TVector, B.TVector, Pitch#, Yaw#, Roll#)
	Local R1.TVector = TVector_Subtract(A, B)
	Local R2.TVector = TVector_Rotate(R1, Pitch, Yaw, Roll)
	Local R3.TVector = TVector_Add(R2, B)
	Delete R1:Delete R2:Return R3
End Function
Function TVector_RotateAroundScalar.TVector(A.TVector, X#, Y#, Z#, Pitch#, Yaw#, Roll#)
	Local B.TVector = TVector_Create(X,Y,Z)
	Local R.TVector = TVector_RotateAround(A, B, Pitch, Yaw, Roll)
	Delete B:Return R
End Function

Function TVector_Dot#(A.TVector, B.TVector)
	Return ((A\X*B\X)+(A\Y*B\Y)+(A\Z*B\Z))
End Function
Function TVector_Cross.TVector(A.TVector, B.TVector)
	Local R.TVector = New TVector
	R\X = (A\Y*B\Z) - (A\Z*B\Y)
	R\Y = (A\Z*B\X) - (A\X*B\Z)
	R\Z = (A\X*B\Y) - (A\Y*B\X)
	Return R
End Function

Global TVector_Pitch#, TVector_Yaw#
Function TVector_Angle(A.TVector) ; X
	TVector_Pitch = VectorPitch(A\X, A\Y, A\Z);-ATan2(A\Y, Sqr((A\X*A\X) + (A\Z*A\Z))) + 90
	TVector_Yaw# = VectorYaw(A\X, A\Y, A\Z);ATan2(A\Z, A\X) - 90
End Function

Function TVector_PitchFrom#(A.TVector, B.TVector) ; X
	;Return ATan2(A\Y-B\Y, A\Z-B\Z)
	Return VectorPitch(A\X-B\X, A\Y-B\Y, A\Z-B\Z)
End Function
Function TVector_YawFrom#(A.TVector, B.TVector) ; Y
	;Return ATan2(A\Z-B\Z, A\X-B\X)
	Return VectorYaw(A\X-B\X, A\Y-B\Y, A\Z-B\Z)
End Function
;Function TVector_RollFrom#(A.TVector, B.TVector) ; Z
;	Return ATan2(A\Y-B\Y, A\X-B\X)
;End Function

Function TVector_Length#(A.TVector)
	Return Sqr((A\X*A\X)+(A\Y*A\Y)+(A\Z*A\Z))
End Function
Function TVector_Distance#(A.TVector, B.TVector)
	Local X# = (A\X-B\X)
	Local Y# = (A\Y-B\Y)
	Local Z# = (A\Z-B\Z)
	Return Sqr(X*X+Y*Y+Z*Z)
End Function

Function TVector_ToString$(A.TVector)
	Return "{X:"+A\X+";Y:"+A\Y+";Z:"+A\Z+"}"
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D