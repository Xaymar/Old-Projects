;----------------------------------------------------------------
;-- Constants
;----------------------------------------------------------------
Const ITEM_MAXIMUMID				 = 65535
	; Item Classes
Const ITEM_CLASS_WEAPON				 = 1
Const ITEM_CLASS_SHIELD				 = 2
Const ITEM_CLASS_ARMOR				 = 3
Const ITEM_CLASS_ENGINE				 = 4
Const ITEM_CLASS_POWERCORE			 = 5
Const ITEM_CLASS_RESOURCE			 = 6
Const ITEM_CLASS_UPGRADE			 = 7
Const ITEM_CLASS_MININGMODULE		 = 8
Const ITEM_CLASS_ELITIUM			 = 255
	; Return Codes
Const INVENTORY_RC_OK				 = 0
Const INVENTORY_RC_INVALIDID		 = -1
Const INVENTORY_RC_INVALIDAMOUNT	 = -2
Const INVENTORY_RC_UNKNOWNITEM		 = -3
Const INVENTORY_RC_ITEMTOOBIG		 = -4
Const INVENTORY_RC_ITEMNOTFOUND		 = -5

;----------------------------------------------------------------
;-- Types
;----------------------------------------------------------------
	; Item Definition
Type TItem
	Field ID%
	
	Field Name$			; Name of the Item
	Field Description$	; Description of the Item
	Field Image			; Handle of the Image for the Item
	
	Field Size%			; Size in cubic decimeters. Divide by 1000 to get the size in cube meters, which is displayed.
	Field Rarity%		; Rarity
	
	Field Class%		; Class of the Item.
	Field AttributeID0%
	Field AttributeID1%
	Field AttributeID2%
	Field AttributeID3%
	
	; Attributes for Class: Weapon
	;  - Short/255	Electric Damage	(Attribute 0 High)
	;  - Short/255	Physical Damage	(Attribute 0 Low)
	;  - Short/255	Rate of Fire	(Attribute 1 High)
	;  - Short		Energy			(Attribute 3 High)
	;  - Short/255	CPU				(Attribute 3 Low)
	; Attributes for Class: Shield
	;  - Int		Shield Maximum	(Attribute 0)
	;  - Short/255	Electric Resist	(Attribute 1 High)
	;  - Short/255	Physical Resist	(Attribute 1 Low)
	;  - Short		Recharge Amount	(Attribute 2 High)
	;  - Short		Recharge Rate	(Attribute 2 Low)
	;  - Short/255	CPU				(Attribute 3 Low)
	; Attributes for Class: Armor
	;  - Int		Armor Maximum	(Attribute 0)
	;  - Short/255	Speed Modifier	(Attribute 1 High)
	;  - Short/255	Turn Modifier	(Attribute 1 Low)
	;  - Short/255	Signature Mod.	(Attribute 2 High)
	; Attributes for Class: Engine
	;  - Short/255	Maximum Speed	(Attribute 0 High)
	;  - Short/255	Maximum Boost	(Attribute 0 Low)
	;  - Short		Trace Type		(Attribute 2 High)
	;  - Short/255	Trace Threshold (Attribute 2 Low)
	;  - Short		Energy			(Attribute 3 High)
	;  - Short/255	CPU				(Attribute 3 Low)
	; Attributes for Class: PowerCore
	;  - Int		Energy Maximum	(Attribute 0)
	;  - Short		Energy Amount	(Attribute 1 High)
	;  - Short		Energy Rate		(Attribute 1 Low)
	;  - Byte		Elec. Signature	(Attribute 2 High 1)
	;  - Byte		Scramble Str.	(Attribute 2 High 0)
	;  - Short/255	CPU				(Attribute 3 Low)
	; Attributes for Class: Resource
	;  - Byte		Processing Tier	(Attribute 0 Low 1)
	;  - Byte		Load Type		(Attribute 0 Low 0)
	;  - Int		Half-life Time	(Attribute 3) 
	; Attributes for Class: Upgrade
	;  - Byte/127	Armor Bonus		(Attribute 0 High 1)
	;  - Byte/127	Shield Bonus	(Attribute 0 High 0)
	;  - Byte/127	Speed Bonus		(Attribute 0 Low 1)
	;  - Byte/127	Cargo Bonus		(Attribute 0 Low 0)
	;  - Byte/127	Elec. Sig. Mod.	(Attribute 1 High 1)
	;  - Byte/127	Grav. Sig. Mod.	(Attribute 1 High 0)
	;  - Byte/127	Elec. Dmg. Mod.	(Attribute 1 Low 1)
	;  - Byte/127	Phys. Dmg. Mod.	(Attribute 1 Low 0)
	;  - Byte/127	El. ShRes Mod.	(Attribute 2 High 1)
	;  - Byte/127	Ph. ShRes Mod.	(Attribute 2 High 0)
	;  - Byte/127	El. ArRes Mod.	(Attribute 2 Low 1)
	;  - Byte/127	Ph. ArRes Mod.	(Attribute 2 Low 0)
	;  - Byte		Scramble Str.	(Attribute 3 High 1)
	;  - Byte/127	Yield Bonus		(Attribute 3 High 0)
	;  - Byte		Energy Max		(Attribute 3 Low 1)
	;  - Byte/127	CPU Modifier	(Attribute 3 Low 0)
	; Attributes for Class: Mining Module
	;  - 
	; Attributes for Class: Elitium
	;  - 
End Type
Dim Items.TItem(ITEM_MAXIMUMID)

	; Inventory
Type TInventory
	Field Size%		; Size in cubic meters. Divide by 1000 to get the size in cube meters, which is displayed.
	Field Used%		; Used space in cubic decimeters. Divide by 1000 to get the size in cube meters, which is displayed.

		; Array containing all Items using their ItemID as index.
	Field Items.TInventoryItem[ITEM_MAXIMUMID]
End Type

Type TInventoryItem
	Field Amount%			; How many of this Item are stored?
End Type

;----------------------------------------------------------------
;-- Functions
;----------------------------------------------------------------
	; Create a new inventory.
Function TInventory_Create.TInventory(Size%)
	If Size <= 0 Then RuntimeError "TInventory: Size is equal to or less than 0."
	
	Local lInventory.TInventory = New TInventory
	lInventory\Size = Size
	lInventory\Used = 0
	
	Return lInventory
End Function

	; Safely destroy an existing inventory.
Function TInventory_Destroy(pInventory.TInventory)
	If pInventory = Null Then RuntimeError "TInventory: Inventory does not exist."
	
	For lIndex = 0 To (ITEM_MAXIMUMID - 1)
		If pInventory\Items[lIndex] <> Null Then
			Delete pInventory\Items[lIndex]
			pInventory\Items[lIndex] = Null
		EndIf
	Next
	
	Delete pInventory
End Function

	; Retrieve the amount of items in the inventory with the given ItemID.
Function TInventory_GetItemAmount(pInventory.TInventory, ItemID%)
	If pInventory = Null Then RuntimeError "TInventory: Inventory does not exist."
	
	If ItemID >= 0 And ItemID < ITEM_MAXIMUMID Then
		If Items(ItemID) <> Null Then
			If pInventory\Items[ItemID] <> Null
				Return pInventory\Items[ItemID]\Amount
			Else
				Return INVENTORY_RC_ITEMNOTFOUND
			EndIf
		Else
			Return INVENTORY_RC_UNKNOWNITEM
		EndIf
	Else
		Return INVENTORY_RC_INVALIDID
	EndIf
End Function

	; Set the amount of items in the inventory with the given ItemID.
Function TInventory_SetItemAmount(pInventory.TInventory, ItemID%, Amount%)
	If pInventory = Null Then RuntimeError "TInventory: Inventory does not exist."
	If Amount < 0 Then Return INVENTORY_RC_INVALIDAMOUNT
	
	If ItemID >= 0 And ItemID < ITEM_MAXIMUMID Then
		If Items(ItemID) <> Null Then
			; Remove current Amount and Size from Inventory
			If pInventory\Items[ItemID] <> Null Then
				pInventory\Used = pInventory\Used - (Items(ItemID)\Size * pInventory\Items[ItemID]\Amount)
				If Amount = 0 Then
					Delete pInventory\Items[ItemID]
					pInventory\Items[ItemID] = Null
				EndIf					
			EndIf
			
			If Amount > 0
				If pInventory\Items[ItemID] = Null pInventory\Items[ItemID] = New TInventoryItem
				
				Local lAmount = Amount
				If pInventory\Used + (Items(ItemID)\Size * lAmount) > PInventory\Size Then lAmount = (pInventory\Size - pInventory\Used) / Items(ItemID)\Size
				
				pInventory\Items[ItemID]\Amount = lAmount
				pInventory\Used = pInventory\Used + (Items(ItemID)\Size * lAmount)
				
				Return lAmount
			Else
				Return INVENTORY_RC_OK
			EndIf
			If pInventory\Items[ItemID] <> Null Then lSize = lSize - (Items(ItemID)\Size * pInventory\Items[ItemID]\Amount)
		Else
			Return INVENTORY_RC_UNKNOWNITEM
		EndIf
	Else
		Return INVENTORY_RC_INVALIDID
	EndIf
End Function

	; Add an item to the inventory.
	;  Returns the amount of items added or a negative value (error code)
Function TInventory_AddItem(pInventory.TInventory, ItemID%, Amount%=1)
	If pInventory = Null Then RuntimeError "TInventory: Inventory does not exist."
	If Amount <= 0 Then Return INVENTORY_RC_INVALIDAMOUNT
	
	If ItemID >= 0 And ItemID < ITEM_MAXIMUMID Then
		If Items(ItemID ) <> Null Then
			Local lAmount = Amount
			
			If pInventory\Used + (lAmount * Items(ItemID)\Size) > pInventory\Size Then lAmount = (pInventory\Size - pInventory\Used) / Items(ItemID)\Size
			
			If lAmount > 0 Then
				If pInventory\Items[ItemID] = Null Then pInventory\Items[ItemID] = New TInventoryItem
				pInventory\Items[ItemID]\Amount = pInventory\Items[ItemID]\Amount + lAmount
				pInventory\Used = pInventory\Used + (lAmount * Items(ItemID)\Size)
			EndIf
			
			Return lAmount
		Else
			Return INVENTORY_RC_UNKNOWNITEM
		EndIf
	Else
		Return INVENTORY_RC_INVALIDID
	EndIf
End Function

	; Remove an item from the inventory.
	;  Returns the amount of items removed or a negative value (error code).
Function TInventory_RemoveItem(pInventory.TInventory, ItemID%, Amount%=1)
	If pInventory = Null Then RuntimeError "TInventory: Inventory does not exist."
	If Amount <= 0 Then Return INVENTORY_RC_INVALIDAMOUNT
	
	If ItemID >= 0 And ItemID < ITEM_MAXIMUMID Then
		If Items(ItemID) <> Null Then
			If pInventory\Items[ItemID] <> Null Then
				Local lAmount = pInventory\Items[ItemID]\Amount
				pInventory\Items[ItemID]\Amount = pInventory\Items[ItemID]\Amount - Amount
				
				If pInventory\Items[ItemID]\Amount <= 0 Then
					pInventory\Used = pInventory\Used - (lAmount * Items(ItemID)\Size)
						; Delete Item Entry (Reduces Memory Usage)
					Delete pInventory\Items[ItemID]:pInventory\Items[ItemID] = Null
						
					Return lAmount
				Else
					pInventory\Used = pInventory\Used - (Amount * Items(ItemID)\Size)
					
					Return Amount
				EndIf
			Else
				Return INVENTORY_RC_ITEMNOTFOUND
			EndIf
		Else
			Return INVENTORY_RC_UNKNOWNITEM
		EndIf
	Else
		Return INVENTORY_RC_INVALIDID
	EndIf
End Function

	; For when you edit the inventory without using the above functions.
Function TInventory_RecalculateUsedSpace(pInventory.TInventory)
	If pInventory = Null Then RuntimeError "TInventory: Inventory does not exist."
	
	pInventory\Used = 0
	For lIndex = 0 To (ITEM_MAXIMUMID - 1)
		If Items(lIndex) <> Null And pInventory\Items[lIndex] <> Null Then
			pInventory\Used = pInventory\Used + (pInventory\Items[lIndex]\Amount * Items(lIndex)\Size)
		EndIf
	Next
End Function

;----------------------------------------------------------------
;-- Extras - Require 'LinkedListEmulation.bb'!
;----------------------------------------------------------------
;Type TInventoryItemLink
;	Field ItemID%
;	Field Amount%
;End Type
;
;	; Retrieve a LinkedList containing all items in this inventory.
;	;  The returned list must be deleted using TInventory_DeleteItemList(...).
;	;	Modifications on this list can only be applied to the inventory using TInventory_SetItemList(...).
;Function TInventory_GetItemList.TList(pInventory.TInventory)
;	If pInventory = Null Then RuntimeError "TInventory: Inventory does not exist."
;	
;	Local lList.TList = TList_Create()
;	For lIndex = 0 To (ITEM_MAXIMUMID - 1)
;		If Items(lIndex) <> Null And pInventory\Items[lIndex] <> Null Then
;			Local lInvItemLink.TInventoryItemLink = New TInventoryItemLink
;			lInvItemLink\ItemID = lIndex
;			lInvItemLink\Amount = pInventory\Items[lIndex]\Amount
;			TList_AddLast(lInvItemLink)
;		EndIf
;	Next
;	
;	Return lList
;End Function
;
;Function TInventory_SetItemList(pInventory.TInventory, pList.TList)
;	If pInventory = Null Then RuntimeError "TInventory: Inventory does not exist."
;	If pList = Null Then RuntimeError "TInventory: List does not exist."
;	
;	pInventory\Used = 0
;	For lIndex = 0 To (ITEM_MAXIMUMID - 1)
;		If Items(lIndex) <> Null And pInventory\Items[lIndex] <> Null Then
;			Delete pInventory\Items[lIndex]
;			pInventory\Items[lIndex] = Null
;		EndIf
;	Next
;	
;	Local lInvItemLink.TInventoryItemLink = Object.TInventoryItemLink(TList_First(pList))
;	While lInvItemLink <> Null
;		If lInvItemLink\ItemID >= 0 And lInvItemLink\ItemID < ITEM_MAXIMUMID Then 
;			If Items(lInvItemLink\ItemID) <> Null And lInvItemLink\Amount > 0 Then
;				pInventory\Items[lInvItemLink\ItemID] = New TInventoryItem
;				pInventory\Items[lInvItemLink\ItemID]\Amount = lInvItemLink\Amount
;				
;				pInventory\Used = pInventory\Used + (lInvItemLink\Amount * Items(lInvItemLink\ItemID)\Size)
;			EndIf
;		EndIf
;		lInvItemLink = Object.TInventoryItemLink(TList_Next(pList))
;	Wend
;End Function
;
;Function TInventory_DeleteItemList(pList.TList)
;	If pList = Null Then RuntimeError "TInventory: List does not exist."
;	
;	Local lInvItemLink.TInventoryItemLink = Object.TInventoryItemLink(TList_First(pList))
;	While lInvItemLink <> Null
;		Delete lInvItemLink
;		lInvItemLink = Object.TInventoryItemLink(TList_Delete(pList))
;	Wend
;	
;	TList_Destroy(pList)
;End Function

;----------------------------------------------------------------
;-- Example
;----------------------------------------------------------------
SeedRnd(MilliSecs())

	; Test Item
Items(0) = New TItem
Items(0)\Name = "1dm³ Item"
Items(0)\Description = ""
Items(0)\Size = 1

	; Test Item
Items(1) = New TItem
Items(1)\Name = "10dm3 Item"
Items(1)\Description = ""
Items(1)\Size = 10

	; Test Item
Items(2) = New TItem
Items(2)\Name = "100dm3 Item"
Items(2)\Description = ""
Items(2)\Size = 100

	; Test Item
Items(3) = New TItem
Items(3)\Name = "1000dm3 Item"
Items(3)\Description = ""
Items(3)\Size = 1000

Local MyInv.TInventory = TInventory_Create(10000)
While Not KeyHit(1)
	Cls
	Locate 0,0
	PrintInv(MyInv)
	
	If Rand(0,1000) Mod 4 < 2
		TInventory_AddItem(MyInv, Rand(0,3), Rand(0,100))
		TInventory_RemoveItem(MyInv, Rand(0,3), Rand(0,100))
	Else
		TInventory_SetItemAmount(MyInv, Rand(0,3), Rand(0,100))
		TInventory_SetItemAmount(MyInv, Rand(0,3), Rand(0,100))
	EndIf
	
	WaitKey()
	Flip 0
Wend
TInventory_Destroy(MyInv)
End

Function PrintInv(Inv.TInventory)
	Print "Inventory " + Handle(Inv)
	Print "  Size:      " + (Inv\Size/1000) + "." + (Inv\Size Mod 1000) + " m³"
	Print "  Used:      " + (Inv\Used/1000) + "." + (Inv\Used Mod 1000) + " m³ / " + Int((Inv\Used/Float(Inv\Size))*100) + "%"
	Print "  Remaining: " + ((Inv\Size - Inv\Used)/1000) + "." + ((Inv\Size - Inv\Used) Mod 1000) + " m³ / " + Int(((Inv\Size - Inv\Used)/Float(Inv\Size))*100) + "%"
	Print "  Items:"
	For lIndex = 0 To (ITEM_MAXIMUMID - 1)
		If Items(lIndex) <> Null And TInventory_GetItemAmount(Inv, lIndex) > 0 Then
			Print "  - " + Items(lIndex)\Name + " x" + TInventory_GetItemAmount(Inv, lIndex)
		EndIf
	Next
	; Alternatively, using Extras
	;Local lList.TList = TInventory_GetItemList(Inv)
	;Local lEntry.TInventoryItemLink = Object.TInventoryItemLink(TList_First(lList))
	;While lEntry <> Null
	;	Print "  - " + Items(lEntry\ItemID)\Name + " x" + lEntry\Amount
	;	lEntry = Object.TInventoryItemLink(TList_Next(lList))
	;Wend
	;TInventory_DestroyItemList(lList)
End Function