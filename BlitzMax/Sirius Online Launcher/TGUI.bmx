SuperStrict

Import BRL.LinkedList
Import BRL.Map
Import BRL.Graphics
Import BRL.GLGraphics
Import BRL.Max2D
Import BRL.GLMax2D
Import BRL.Pixmap
Import Pub.OpenGL
Import "Max2DExtended.bmx"

Import BRL.BMPLoader
Import BRL.JPGLoader
Import BRL.PNGLoader
Import BRL.TGALoader

'-------------------------------------------------------------------------------
Type TGUI
	' Variables ----------------------------------------------------------------
		' Skinning
	Field oSkinImageSet:TPixmap		 = Null
		' GUI Area
	Field oRootGadget:TGUIGadget	 = New TGUIGadget
	
	' Functions ----------------------------------------------------------------
	Function Create:TGUI(X:Float, Y:Float, W:Float, H:Float, Skin:Object)
		Local loGUI:TGUI = New TGUI
		
		Return loGUI
	EndFunction
	
	' Members ------------------------------------------------------------------
	Method New()
		Self.oRootGadget			 = (New TGUIGadget)
	EndMethod
	
	Method Destroy()
		Self.oRootGadget = Null
	EndMethod
	
	Method Update:Int(fDelta:Float)
		oRootGadget.Update(fDelta)
	EndMethod
	
	Method Render:Int(fDelta:Float)
		oRootGadget.Render(fDelta)
	EndMethod
	
	'' Changes the 
	Method SetRootGadget:TGUIGadget(oRootGadget:TGUIGadget)
		If oRootGadget = Null Or oRootGadget.Insane() Then Return Null
		
		Local loRootWindow:TGUIGadget = Self.oRootGadget
		Self.oRootGadget = oRootGadget
		Return loRootWindow
	EndMethod
	
	Method GetRootGadget:TGUIGadget()
		Return oRootGadget
	EndMethod
EndType

'-------------------------------------------------------------------------------
Type TGUIGadget
	' Constants ----------------------------------------------------------------
	Const COORD_ABSOLUTE:Byte		 = 0
	Const COORD_RELATIVE:Byte		 = 1
	
	' Variables ----------------------------------------------------------------
		' Gadget Coordinates
	Field bLocalCoordType:Byte[] = New Byte[4]
	Field dLocalCoord:Double[]	 = New Double[4]
	Field dCoord:Double[]		 = New Double[4]
		' Parent and Childs
	Field oParentGUI:TGUI			 = Null
	Field oParent:TGUIGadget		 = Null
	Field oChildList:TList			 = New TList
	
	' Members ------------------------------------------------------------------
	'' Deconstructor for TGUIGadget
	' Automatically removed itself from the parent and releases all pointers to chils
	Method Delete()
			' Unregister from parent gadget and release all pointers to childs.
		SetParent(Null)
		Self.oChildList.Clear()
		
			' Destroy objects in reverse order.
		Self.bCoordType = Null
		Self.dCoord = Null
		Self.dLocalCoord = Null
		Self.oChildList = Null
	EndMethod
	
	'' Checks if the Gadget went insane due to invalid usage or other influence.
	' @return <Bool> True if the TGUIGadget went insane, otherwise false.
	Method Insane:Byte()
		If Self.bCoordType = Null Then Return True
		If Self.dCoord = Null Then Return True
		If Self.dLocalCoord = Null Then Return True
		If Self.oChildList = Null Then Return True
	EndMethod
	
	'' Updates the GUIGadget and any other objects in it.
	' @return <Int>
	Method Update:Int(fDelta:Float)
	EndMethod
	
	Method Render:Int(fDelta:Float)
	EndMethod
	
		' Parent Management
	Method SetParent(oParent:TGUIGadget)
		If Self.oParent <> Null Then Self.oParent.RemoveChild(Self)
		If oParent <> Null Then oParent.AddChild(Self)
	EndMethod
	
	Method GetParent:TGUIGadget()
		Return Self.oParent
	EndMethod
	
		' Child Management
	Method IsChild:Int(oChild:TGUIGadget)
		If oChild = Null Then Return False
		Return Self.oChildList.Contains(oChild)
	EndMethod
	
	Method AddChild:Int(oChild:TGUIGadget)
		If oChild <> Null Then
			If IsChild(oChild) Then
				Return True
			Else
				Return (Self.oChildList.AddLast(oChild) <> Null)
			EndIf
		EndIf
	EndMethod
	
	Method RemoveChild:Int(oChild:TGUIGadget)
		If oChild <> Null Then
			If IsChild(oChild) Then
				Return True
			Else
				Return Self.oChildList.Remove(oChild)
			EndIf
		EndIf
	EndMethod
	
	Method GetChildAtIndex:TGUIGadget(iIndex:Int)
		If iIndex <> -1 Then
			Local liChildCount:Int = Self.oChildList.Count()
			If iIndex >= 0 And iIndex < liChildCount Then
				Return Self.oChildList.ValueAtIndex(iIndex)
			Else
				Return Null
			EndIf
		Else
			Return Self.oChildList.Last()
		EndIf
	EndMethod
	
	Method SetChildIndex:Int(oChild:TGUIGadget, iIndex:Int)
		If oChild <> Null Then
			If IsChild(oChild) Then
				Local liChildCount:Int = Self.oChildList.Count()
				
				If iIndex >= 0 And iIndex < liChildCount Then
					'Local loChild:TGUIGadget = Self.oChildList.ValueAtIndex(iIndex)
					'Local lChild:TGUIGadget = Self.oChildList.ValueAtIndex(iIndex)
					'Self.oChildList.Insert
				EndIf
			EndIf
		EndIf
	EndMethod
	
	' Signal Handlers ----------------------------------------------------------

EndType

'-------------------------------------------------------------------------------
Public
Type TGUIWindow Extends TGUIGadget
'-------------------------------------------------------------------------------
	
EndType