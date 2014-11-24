Global LoD_Dist_Lv0# = 32, LoD_Dist_Lv1# = 64, LoD_Dist_Lv2# = 128, LoD_Dist_Lv3# = 256, LoD_Dist_Lv4# = 512
Global LoD_Range# = 8

Type LoDEntity
	Field Pivot%  = 0
	
	; Levels
	Field LoDs%[5]
	
	; Temporary Values
	Field Visible%[5]
	Field Fade#[5]
End Type

Function LoD_Initialize(Dist_Range# = 8, Dist_Lv0# = 32, Dist_Lv1# = 64, Dist_Lv2# = 128, Dist_Lv3# = 256, Dist_Lv4# = 512)
	LoD_Range = Dist_Range
	
	; Distance Limits
	LoD_Dist_Lv0 = Dist_Lv0
	LoD_Dist_Lv1 = Dist_Lv1
	LoD_Dist_Lv2 = Dist_Lv2
	LoD_Dist_Lv3 = Dist_Lv3
	LoD_Dist_Lv4 = Dist_Lv4
End Function

Function LoD_Create.LoDEntity(Lv0, Lv1, Lv2, Lv3, Lv4)
	Local tInstance.LoDEntity = New LoDEntity
	tInstance\Pivot = CreatePivot()
	
	; Copy Entities
	tInstance\LoDs[0] = CopyEntity(Lv0, tInstance\Pivot):HideEntity tInstance\LoDs[0]
	tInstance\LoDs[1] = CopyEntity(Lv1, tInstance\Pivot):HideEntity tInstance\LoDs[1]
	tInstance\LoDs[2] = CopyEntity(Lv2, tInstance\Pivot):HideEntity tInstance\LoDs[2]
	tInstance\LoDs[3] = CopyEntity(Lv3, tInstance\Pivot):HideEntity tInstance\LoDs[3]
	tInstance\LoDs[4] = CopyEntity(Lv4, tInstance\Pivot):HideEntity tInstance\LoDs[4]
	
	Return tInstance
End Function

Function LoD_EntityAlpha(tInstance.LoDEntity, Alpha#, Level=-1)
	If (Level = -1) Then
		EntityAlpha tInstance\LoDs[0], Alpha
		EntityAlpha tInstance\LoDs[1], Alpha
		EntityAlpha tInstance\LoDs[2], Alpha
		EntityAlpha tInstance\LoDs[3], Alpha
		EntityAlpha tInstance\LoDs[4], Alpha
	Else
		EntityAlpha tInstance\LoDs[Level], Alpha
	EndIf
End Function

Function LoD_EntityBlend(tInstance.LoDEntity, Mode%, Level=-1)
	If (Level = -1) Then
		EntityBlend tInstance\LoDs[0], Mode
		EntityBlend tInstance\LoDs[1], Mode
		EntityBlend tInstance\LoDs[2], Mode
		EntityBlend tInstance\LoDs[3], Mode
		EntityBlend tInstance\LoDs[4], Mode
	Else
		EntityBlend tInstance\LoDs[Level], Mode
	EndIf
End Function

Function LoD_EntityColor(tInstance.LoDEntity, Red#, Green#, Blue#, Level=-1)
	If (Level = -1) Then
		EntityColor tInstance\LoDs[0], Red, Green, Blue
		EntityColor tInstance\LoDs[1], Red, Green, Blue
		EntityColor tInstance\LoDs[2], Red, Green, Blue
		EntityColor tInstance\LoDs[3], Red, Green, Blue
		EntityColor tInstance\LoDs[4], Red, Green, Blue
	Else
		EntityColor tInstance\LoDs[Level], Red, Green, Blue
	EndIf
End Function

Function LoD_EntityFX(tInstance.LoDEntity, FX%, Level=-1)
	If (Level = -1) Then
		EntityFX tInstance\LoDs[0], FX
		EntityFX tInstance\LoDs[1], FX
		EntityFX tInstance\LoDs[2], FX
		EntityFX tInstance\LoDs[3], FX
		EntityFX tInstance\LoDs[4], FX
	Else
		EntityFX tInstance\LoDs[Level], FX
	EndIf
End Function

Function LoD_EntityShininess(tInstance.LoDEntity, Shininess#, Level=-1)
	If (Level = -1) Then
		EntityShininess tInstance\LoDs[0], Shininess
		EntityShininess tInstance\LoDs[1], Shininess
		EntityShininess tInstance\LoDs[2], Shininess
		EntityShininess tInstance\LoDs[3], Shininess
		EntityShininess tInstance\LoDs[4], Shininess
	Else
		EntityShininess tInstance\LoDs[Level], Shininess
	EndIf
End Function

Function LoD_EntityTexture(tInstance.LoDEntity, Texture%, Frame%=0, Index%=0, Level=-1)
	If (Level = -1) Then
		EntityTexture tInstance\LoDs[0], Texture, Frame, Index
		EntityTexture tInstance\LoDs[1], Texture, Frame, Index
		EntityTexture tInstance\LoDs[2], Texture, Frame, Index
		EntityTexture tInstance\LoDs[3], Texture, Frame, Index
		EntityTexture tInstance\LoDs[4], Texture, Frame, Index
	Else
		EntityTexture tInstance\LoDs[Level], Texture, Frame, Index
	EndIf
End Function

Function LoD_EntityLoD(tInstance.LoDEntity)
	For Level = 0 To 4
		If (tInstance\Visible[Level]) Then Return Level
	Next
	Return 5
End Function

Function LoD_Update(eCamera, bDoFadeOut=False)
	For tInstance.LoDEntity = Each LoDEntity
		Local Dist# = EntityDistance(eCamera, tInstance\Pivot)
		
		; LoD Level 1 (Very High)
		If (Dist > 0) And ((bDoFadeOut And (Dist < LoD_Dist_Lv0 + LoD_Range)) Or (bDoFadeOut = False And (Dist < LoD_Dist_Lv0))) Then
			If tInstance\Visible[0] = False Then tInstance\Visible[0] = True:ShowEntity tInstance\LoDs[0]
			If bDoFadeOut = True And (Dist > LoD_Dist_Lv0) Then EntityAlpha tInstance\LoDs[0], 1.0 - (Dist - LoD_Dist_Lv0) / LoD_Range
		Else
			If tInstance\Visible[0] = True Then tInstance\Visible[0] = False:HideEntity tInstance\LoDs[0]
		EndIf
		
		; LoD Level 2 (High)
		If (Dist > LoD_Dist_Lv0) And ((bDoFadeOut And (Dist < LoD_Dist_Lv1 + LoD_Range)) Or (bDoFadeOut = False And (Dist < LoD_Dist_Lv1))) Then
			If tInstance\Visible[1] = False Then tInstance\Visible[1] = True:ShowEntity tInstance\LoDs[1]
			If bDoFadeOut = True And (Dist > LoD_Dist_Lv1) Then EntityAlpha tInstance\LoDs[1], 1.0 - (Dist - LoD_Dist_Lv1) / LoD_Range
		Else
			If tInstance\Visible[1] = True Then tInstance\Visible[1] = False:HideEntity tInstance\LoDs[1]
		EndIf
		
		; LoD Level 3 (Normal)
		If (Dist > LoD_Dist_Lv1) And ((bDoFadeOut And (Dist < LoD_Dist_Lv2 + LoD_Range)) Or (bDoFadeOut = False And (Dist < LoD_Dist_Lv2))) Then
			If tInstance\Visible[2] = False Then tInstance\Visible[2] = True:ShowEntity tInstance\LoDs[2]
			If bDoFadeOut = True And (Dist > LoD_Dist_Lv2) Then EntityAlpha tInstance\LoDs[2], 1.0 - (Dist - LoD_Dist_Lv2) / LoD_Range
		Else
			If tInstance\Visible[2] = True Then tInstance\Visible[2] = False:HideEntity tInstance\LoDs[2]
		EndIf
		
		; LoD Level 4 (Low)
		If (Dist > LoD_Dist_Lv2) And ((bDoFadeOut And (Dist < LoD_Dist_Lv3 + LoD_Range)) Or (bDoFadeOut = False And (Dist < LoD_Dist_Lv3))) Then
			If tInstance\Visible[3] = False Then tInstance\Visible[3] = True:ShowEntity tInstance\LoDs[3]
			If bDoFadeOut = True And (Dist > LoD_Dist_Lv3) Then EntityAlpha tInstance\LoDs[3], 1.0 - (Dist - LoD_Dist_Lv3) / LoD_Range
		Else
			If tInstance\Visible[3] = True Then tInstance\Visible[3] = False:HideEntity tInstance\LoDs[3]
		EndIf
		
		; LoD Level 5 (Very Low) (Usually a Sprite)
		If (Dist > LoD_Dist_Lv3) And ((bDoFadeOut And (Dist < LoD_Dist_Lv4 + LoD_Range)) Or (bDoFadeOut = False And (Dist < LoD_Dist_Lv4))) Then
			If tInstance\Visible[4] = False Then tInstance\Visible[4] = True:ShowEntity tInstance\LoDs[4]
			If bDoFadeOut = True And (Dist > LoD_Dist_Lv4) Then EntityAlpha tInstance\LoDs[4], 1.0 - (Dist - LoD_Dist_Lv4) / LoD_Range
		Else
			If tInstance\Visible[4] = True Then tInstance\Visible[4] = False:HideEntity tInstance\LoDs[4]
		EndIf
	Next
End Function
