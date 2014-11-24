;----------------------------------------------------------------
;-- Types
;----------------------------------------------------------------
Type TList
	Field FirstEntry.TListEntry
	Field LastEntry.TListEntry
	
	Field Iterator.TListEntry
End Type

Type TListEntry
	Field Value%
	
	Field PreviousEntry.TListEntry
	Field NextEntry.TListEntry
End Type

;----------------------------------------------------------------
;-- Functions
;----------------------------------------------------------------
Function TList_Create.TList()
	Local lList.TList = New TList
	
	lList\FirstEntry = Null
	lList\LastEntry = Null
	lList\Iterator = Null
	
	Return lList
End Function

Function TList_Destroy(pList.TList)
	If pList = Null Then RuntimeError "TList: Tried to destroy non-existing list."
	
	; Delete all entries
	pList\Iterator = pList\FirstEntry
	While pList\Iterator <> Null
		Local lNextEntry.TListEntry = pList\Iterator\NextEntry
		Delete pList\Iterator
		pList\Iterator = lNextEntry
	Wend
End Function

Function TList_Reset(pList.TList)
	If pList = Null Then RuntimeError "TList: Tried to reset non-existing list."
	
	pList\Iterator = Null
End Function

Function TList_First%(pList.TList)
	If pList = Null Then RuntimeError "TList: Tried to iterate non-existing list."
	
	pList\Iterator = pList\FirstEntry
	If pList\Iterator <> Null Then Return pList\Iterator\Value
End Function

Function TList_Last%(pList.TList)
	If pList = Null Then RuntimeError "TList: Tried to iterate non-existing list."
	
	pList\Iterator = pList\LastEntry
	If pList\Iterator <> Null Then Return pList\Iterator\Value
End Function

Function TList_Next%(pList.TList)
	If pList = Null Then RuntimeError "TList: Tried to iterate non-existing list."
	
	If pList\Iterator <> Null Then
		pList\Iterator = pList\Iterator\NextEntry
	Else
		pList\Iterator = pList\FirstEntry
	EndIf
	If pList\Iterator <> Null Then Return pList\Iterator\Value
End Function

Function TList_Previous%(pList.TList)
	If pList = Null Then RuntimeError "TList: Tried to iterate non-existing list."
	
	If pList\Iterator <> Null Then
		pList\Iterator = pList\Iterator\PreviousEntry
	Else
		pList\Iterator = pList\LastEntry
	EndIf
	If pList\Iterator <> Null Then Return pList\Iterator\Value
End Function

Function TList_HasFirst(pList.TList)
	If pList = Null Then RuntimeError "TList: Tried to iterate non-existing list."
	
	Return (pList\FirstEntry <> Null)
End Function

Function TList_HasLast(pList.TList)
	If pList = Null Then RuntimeError "TList: Tried to iterate non-existing list."
	
	Return (pList\LastEntry <> Null)
End Function

Function TList_HasNext(pList.TList)
	If pList = Null Then RuntimeError "TList: Tried to iterate non-existing list."
	
	If pList\Iterator <> Null Then
		Return (pList\Iterator\NextEntry <> Null)
	Else
		Return (pList\FirstEntry <> Null)
	EndIf
End Function

Function TList_HasPrevious(pList.TList)
	If pList = Null Then RuntimeError "TList: Tried to iterate non-existing list."
	
	If pList\Iterator <> Null Then
		Return (pList\Iterator\PreviousEntry <> Null)
	Else
		Return (pList\LastEntry <> Null)
	EndIf
End Function

Function TList_AddFirst(pList.TList, Value%)
	If pList = Null Then RuntimeError "TList: Tried to insert into non-existing list."
	
	Local lEntry.TListEntry = New TListEntry
	lEntry\Value = Value
	
	If pList\FirstEntry <> Null Then
		lEntry\NextEntry = pList\FirstEntry
		lEntry\NextEntry\PreviousEntry = lEntry
	EndIf
	
	pList\FirstEntry = lEntry
	If pList\LastEntry = Null Then pList\LastEntry = lEntry
End Function

Function TList_AddLast(pList.TList, Value%)
	If pList = Null Then RuntimeError "TList: Tried to insert into non-existing list."

	Local lEntry.TListEntry = New TListEntry
	lEntry\Value = Value
	
	If pList\LastEntry <> Null Then
		lEntry\PreviousEntry = pList\LastEntry
		lEntry\PreviousEntry\NextEntry = lEntry
	EndIf
	
	If pList\FirstEntry = Null pList\FirstEntry = lEntry
	pList\LastEntry = lEntry
End Function

Function TList_InsertBefore(pList.TList, Value%)
	If pList = Null Then RuntimeError "TList: Tried to insert into non-existing list."
	If pList\Iterator = Null Then RuntimeError "TList: TList: Tried to change non-existing list entry."
	
	Local lEntry.TListEntry = New TListEntry
	lEntry\Value = Value
	
	If pList\Iterator\PreviousEntry <> Null Then
		lEntry\PreviousEntry = pList\Iterator\PreviousEntry
		pList\Iterator\PreviousEntry\NextEntry = lEntry
	Else
		pList\FirstEntry = lEntry
	EndIf
	pList\Iterator\PreviousEntry = lEntry
	
	If pList\Iterator\NextEntry = Null Then
		pList\LastEntry = lEntry
	EndIf
	lEntry\NextEntry = pList\Iterator
End Function

Function TList_InsertAfter(pList.TList, Value%)
	If pList = Null Then RuntimeError "TList: Tried to insert into non-existing list."
	If pList\Iterator = Null Then RuntimeError "TList: TList: Tried to change non-existing list entry."
	
	Local lEntry.TListEntry = New TListEntry
	lEntry\Value = Value
	
	If pList\Iterator\NextEntry <> Null Then
		lEntry\NextEntry = pList\Iterator\NextEntry 
		pList\Iterator\NextEntry\PreviousEntry = lEntry
	Else
		pList\LastEntry = lEntry
	EndIf
	pList\Iterator\NextEntry = lEntry
	
	If pList\Iterator\PreviousEntry = Null Then
		pList\FirstEntry = lEntry
	EndIf
	lEntry\PreviousEntry = pList\Iterator
End Function

Function TList_Replace(pList.TList, Value%)
	If pList = Null Then RuntimeError "TList: Tried to insert into non-existing list."
	If pList\Iterator = Null Then RuntimeError "TList: TList: Tried to change non-existing list entry."
	
	pList\Iterator\Value = Value
End Function

Function TList_DeleteFirst(pList.TList)
	If pList = Null Then RuntimeError "TList: Tried to insert into non-existing list."
	If pList\FirstEntry = Null Then RuntimeError "TList: TList: Tried to change non-existing list entry."
	
	Local lEntry.TListEntry = pList\FirstEntry 
	pList\FirstEntry\NextEntry\PreviousEntry = Null
	pList\FirstEntry = pList\FirstEntry\NextEntry
	Delete lEntry
	
	If pList\FirstEntry <> Null Then Return pList\FirstEntry\Value
End Function

Function TList_DeleteLast(pList.Tlist)
	If pList = Null Then RuntimeError "TList: Tried to insert into non-existing list."
	If pList\LastEntry = Null Then RuntimeError "TList: TList: Tried to change non-existing list entry."
	
	Local lEntry.TListEntry = pList\LastEntry
	pList\LastEntry\PreviousEntry\NextEntry = Null
	pList\LastEntry = pList\LastEntry\PreviousEntry
	Delete lEntry
	
	If pList\LastEntry <> Null Then Return pList\LastEntry\Value
End Function

Function TList_Delete(pList.TList)
	If pList = Null Then RuntimeError "TList: Tried to insert into non-existing list."
	If pList\Iterator = Null Then RuntimeError "TList: TList: Tried to change non-existing list entry."
	
	If pList\FirstEntry = pList\Iterator Then pList\FirstEntry = pList\Iterator\NextEntry
	If pList\LastEntry = pList\Iterator Then pList\LastEntry = pList\Iterator\PreviousEntry
	
	If pList\Iterator\NextEntry <> Null Then pList\Iterator\NextEntry\PreviousEntry = pList\Iterator\PreviousEntry
	If pList\Iterator\PreviousEntry <> Null Then pList\Iterator\PreviousEntry\NextEntry = pList\Iterator\NextEntry
	
	Local lEntry.TListEntry = pList\Iterator
	pList\Iterator = pList\Iterator\NextEntry
	Delete lEntry
	
	If pList\Iterator <> Null Then Return pList\Iterator\Value
End Function

Function TList_DeleteValue(pList.TList, Value%)
	If pList = Null Then RuntimeError "TList: Tried to insert into non-existing list."
	
	If pList\FirstEntry <> Null Then
		Local lEntry.TListEntry = pList\FirstEntry
		While lEntry <> Null
			If lEntry\Value = Value Then
				If lEntry\PreviousEntry <> Null Then lEntry\PreviousEntry\NextEntry = lEntry\NextEntry
				If lEntry\NextEntry <> Null Then lEntry\NextEntry\PreviousEntry = lEntry\PreviousEntry
				
				If pList\FirstEntry = lEntry Then pList\FirstEntry = lEntry\NextEntry
				If pList\LastEntry = lEntry Then pList\LastEntry = lEntry\PreviousEntry
				
				Local lDelEntry.TListEntry = lEntry
				lEntry = lEntry\NextEntry
				Delete lDelEntry
			Else
				lEntry = lEntry\NextEntry
			EndIf
		Wend
	EndIf
End Function