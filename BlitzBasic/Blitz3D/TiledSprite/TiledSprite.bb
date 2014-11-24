;--------------------------------------------
; Constants
;--------------------------------------------
Const TSPRITE_BORDER_LFT%     = 1 ; Enable border on the left side.
Const TSPRITE_BORDER_RGT%     = 2 ; Enable border on the right side.
Const TSPRITE_BORDER_TOP%     = 4 ; Enable border on the top side.
Const TSPRITE_BORDER_BTM%     = 8 ; Enable border on the bottom side.
Const TSPRITE_BORDEROUT_LFT%  = 16 ; Push the border outside the boundaries on the left side.
Const TSPRITE_BORDEROUT_RGT%  = 32 ; Push the border outside the boundaries on the right side.
Const TSPRITE_BORDEROUT_TOP%  = 64 ; Push the border outside the boundaries on the top side.
Const TSPRITE_BORDEROUT_BTM%  = 128 ; Push the border outside the boundaries on the bottom side.
Const TSPRITE_ROUNDDOWN_HORZ% = 256 ; Round down instead of up horizontally (Disabled by TSPRITE_PARTIAL_HORZ).
Const TSPRITE_ROUNDDOWN_VERT% = 512 ; Round down instead of up vertically (Disabled by TSPRITE_PARTIAL_VERT).
Const TSPRITE_PARTIAL_HORZ% = 1024 ; Allow Partial scaling (last-element) instead of scaling all elements horizontally.
Const TSPRITE_PARTIAL_VERT% = 2048 ; Allow Partial scaling (last-element) instead of scaling all elements vertically.
Const TSPRITE_PARTIALCUT_HORZ% = 4096 ; Changes the mode of the Partial modifier to cutting, so that it doesn't scale horizontally.
Const TSPRITE_PARTIALCUT_VERT% = 8192 ; Changes the mode of the Partial modifier to cutting, so that it doesn't scale vertically.
Const TSPRITE_FORCESINGLE_HORZ% = 16384 ; Force to use only a single tile horizontally (for gradients and such).
Const TSPRITE_FORCESINGLE_VERT% = 32768 ; Force to use only a single tile vertically (for gradients and such).

Const HINT_LEFT% = 0
Const HINT_TOP% = 1
Const HINT_RIGHT% = 2
Const HINT_BOTTOM% = 3

;--------------------------------------------
; Types
;--------------------------------------------
Type TSprite
	; Texture (Use this for animations).
	Field mTexture% = 0
	Field mTextureBrush% = 0
	
	; Mesh & Surface
	Field mMesh% = 0
	Field mSurface% = 0
	Field mShareSurface% = False
	
	; Padding & Border
	Field mPadding%[4]
	Field mBorder%[4]
	
	; Outer & Inner Coordinates in Pixels
	Field mOuter%[4] ; Relative to mTexture in Pixels
	Field mInner%[4] ; Relative to mTexture in Pixels
	; Outer & Inner UV Coordinates in Texels
	Field mOuterUV#[4] ;Relative to mTexture in Texels
	Field mInnerUV#[4] ;Relative to mTexture in Texels
	
	; Scale
	Field mScale#[2]
	Field mBorderScale#[2]
End Type

Type TSpriteBuilder
	; Texture
	Field Width% = 0
	Field Height% = 0
	Field TextureBrush% = 0
	
	; Mesh & Surface
	Field Mesh% = 0
	Field MeshSurface% = 0
	Field SharedSurface% = False
	
	; Padding
	Field PaddingLeft% = 0
	Field PaddingTop% = 0
	Field PaddingRight% = 0
	Field PaddingBottom% = 0
	
	; Border
	Field BorderLeft% = 0
	Field BorderTop% = 0
	Field BorderRight% = 0
	Field BorderBottom% = 0
	
	; Scale
	Field ScaleX# = 1
	Field ScaleY# = 1
	Field BorderScaleX# = 1
	Field BorderScaleY# = 1
	
	; Internal Flags
	Field OwnBrush% = True
	Field OwnMesh% = True
	Field OwnMeshSurface% = True
End Type

;--------------------------------------------
; Functions
;--------------------------------------------
; TSpriteBuilder
Function TSpriteBuilder_Create.TSpriteBuilder()
	Local SpriteBuilder.TSpriteBuilder = New TSpriteBuilder
	SpriteBuilder\TextureBrush = CreateBrush(1.0, 1.0, 1.0)
	SpriteBuilder\Mesh = CreateMesh()
	SpriteBuilder\MeshSurface = CreateSurface(SpriteBuilder\Mesh)
	Return SpriteBuilder
End Function

Function TSpriteBuilder_Padding.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, PaddingLeft%, PaddingTop%, PaddingRight%, PaddingBottom%)
	If SpriteBuilder <> Null Then
		SpriteBuilder\PaddingLeft = PaddingLeft
		SpriteBuilder\PaddingTop = PaddingTop
		SpriteBuilder\PaddingRight = PaddingRight
		SpriteBuilder\PaddingBottom = PaddingBottom
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_Scale.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, ScaleX#, ScaleY#)
	If SpriteBuilder <> Null Then
		SpriteBuilder\ScaleX = ScaleX
		SpriteBuilder\ScaleY = ScaleY
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_Border.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, BorderLeft%, BorderTop%, BorderRight%, BorderBottom%)
	If SpriteBuilder <> Null Then
		SpriteBuilder\BorderLeft = BorderLeft
		SpriteBuilder\BorderTop = BorderTop
		SpriteBuilder\BorderRight = BorderRight
		SpriteBuilder\BorderBottom = BorderBottom
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_BorderScale.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, BorderScaleX#, BorderScaleY#)
	If SpriteBuilder <> Null Then
		SpriteBuilder\BorderScaleX = BorderScaleX
		SpriteBuilder\BorderScaleY = BorderScaleY
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_Brush.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, Brush%)
	If SpriteBuilder <> Null And Brush <> 0 Then
		If SpriteBuilder\OwnBrush = True And SpriteBuilder\TextureBrush <> 0 Then FreeBrush SpriteBuilder\TextureBrush
		SpriteBuilder\TextureBrush = Brush
		If SpriteBuilder\MeshSurface <> 0 Then PaintSurface SpriteBuilder\MeshSurface, SpriteBuilder\TextureBrush
		
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_BrushSize.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, BrushW%, BrushH%)
	If SpriteBuilder <> Null And SpriteBuilder\TextureBrush <> 0 Then
		SpriteBuilder\Width = BrushW
		SpriteBuilder\Height = BrushH
		
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_BrushAlpha.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, Alpha#)
	If SpriteBuilder <> Null And SpriteBuilder\TextureBrush <> 0 Then
		BrushAlpha SpriteBuilder\TextureBrush, Alpha
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_BrushBlend.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, Blend%)
	If SpriteBuilder <> Null And SpriteBuilder\TextureBrush <> 0 Then
		BrushBlend SpriteBuilder\TextureBrush, Blend
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_BrushColor.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, Red#, Green#, Blue#)
	If SpriteBuilder <> Null And SpriteBuilder\TextureBrush <> 0 Then
		BrushColor SpriteBuilder\TextureBrush, Red, Green, Blue
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_BrushFX.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, FX%)
	If SpriteBuilder <> Null And SpriteBuilder\TextureBrush <> 0 Then
		BrushFX SpriteBuilder\TextureBrush, FX
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_BrushShininess.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, Shininess#)
	If SpriteBuilder <> Null And SpriteBuilder\TextureBrush <> 0 Then
		BrushShininess SpriteBuilder\TextureBrush, Shininess
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_BrushTexture.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, Texture%, Frame% = 0, Index% = 0)
	If SpriteBuilder <> Null And Texture <> 0 Then
		BrushTexture SpriteBuilder\TextureBrush, Texture, Frame, Index
		SpriteBuilder\Width = TextureWidth(Texture)
		SpriteBuilder\Height = TextureHeight(Texture)
		
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_Mesh.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, Mesh% = 0, MeshSurface% = 0, ShareSurface = False)
	If SpriteBuilder <> Null Then
		If SpriteBuilder\OwnMesh Then FreeEntity SpriteBuilder\Mesh
		If Mesh = 0 Then
			SpriteBuilder\Mesh = CreateMesh()
			SpriteBuilder\OwnMesh = True
		Else
			SpriteBuilder\Mesh = Mesh
			SpriteBuilder\OwnMesh = False
		EndIf
		TSpriteBuilder_MeshSurface(SpriteBuilder, MeshSurface, ShareSurface)
		
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_MeshSurface.TSpriteBuilder(SpriteBuilder.TSpriteBuilder, MeshSurface% = 0, ShareSurface% = False)
	If SpriteBuilder <> Null And SpriteBuilder\Mesh <> 0 Then
		If MeshSurface = 0 Then
			If SpriteBuilder\OwnMeshSurface = False Then SpriteBuilder\MeshSurface = CreateSurface(SpriteBuilder\Mesh, SpriteBuilder\TextureBrush)
			SpriteBuilder\OwnMeshSurface = True
		ElseIf MeshSurface <> 0 Then
			SpriteBuilder\MeshSurface = MeshSurface
			SpriteBuilder\OwnMeshSurface = False
		EndIf
		If SpriteBuilder\TextureBrush <> 0 Then PaintSurface SpriteBuilder\MeshSurface, SpriteBuilder\TextureBrush
		SpriteBuilder\SharedSurface = ShareSurface
		
		Return SpriteBuilder
	EndIf
	Return Null
End Function

Function TSpriteBuilder_Reset.TSpriteBuilder(SpriteBuilder.TSpriteBuilder)
	If SpriteBuilder <> Null Then
		If SpriteBuilder\OwnMesh Then FreeEntity SpriteBuilder\Mesh
		If SpriteBuilder\OwnBrush Then FreeBrush SpriteBuilder\TextureBrush
		
		Delete SpriteBuilder
		Return New TSpriteBuilder
	EndIf
End Function

; TSprite
Function TSprite_Create.TSprite(SpriteBuilder.TSpriteBuilder)
	If SpriteBuilder <> Null And SpriteBuilder\Mesh <> 0 And SpriteBuilder\MeshSurface <> 0 And SpriteBuilder\TextureBrush <> 0 Then
		Local Sprite.TSprite = New TSprite
		Local Width# = SpriteBuilder\Width
		Local Height# = SpriteBuilder\Height
		
		; Mesh & Surface.
		Sprite\mMesh = SpriteBuilder\Mesh
		Sprite\mSurface = SpriteBuilder\MeshSurface
		Sprite\mShareSurface = SpriteBuilder\SharedSurface
		
		; Texture.
		Sprite\mTextureBrush = SpriteBuilder\TextureBrush
		
		; Padding.
		Sprite\mPadding[0] = SpriteBuilder\PaddingLeft
		Sprite\mPadding[1] = SpriteBuilder\PaddingTop
		Sprite\mPadding[2] = SpriteBuilder\PaddingRight
		Sprite\mPadding[3] = SpriteBuilder\PaddingBottom
		
		; Border.
		Sprite\mBorder[0] = SpriteBuilder\BorderLeft
		Sprite\mBorder[1] = SpriteBuilder\BorderTop
		Sprite\mBorder[2] = SpriteBuilder\BorderRight
		Sprite\mBorder[3] = SpriteBuilder\BorderBottom
		
		; Scale.
		Sprite\mScale[0] = SpriteBuilder\ScaleX
		Sprite\mScale[1] = SpriteBuilder\ScaleY
		Sprite\mBorderScale[0] = SpriteBuilder\BorderScaleX
		Sprite\mBorderScale[1] = SpriteBuilder\BorderScaleY
		
		; Calculate Outer Limits.
		Sprite\mOuter[0] = Sprite\mPadding[0]
		Sprite\mOuter[1] = Sprite\mPadding[1]
		Sprite\mOuter[2] = Width - Sprite\mPadding[2]
		Sprite\mOuter[3] = Height - Sprite\mPadding[3]
		
		; Calculate Inner Limits.
		Sprite\mInner[0] = Sprite\mOuter[0] + Sprite\mBorder[0]
		Sprite\mInner[1] = Sprite\mOuter[1] + Sprite\mBorder[1]
		Sprite\mInner[2] = Sprite\mOuter[2] - Sprite\mBorder[2]
		Sprite\mInner[3] = Sprite\mOuter[3] - Sprite\mBorder[3]
		
		; Convert Outer Limits to valid UV coordinates.
		Sprite\mOuterUV[0] = Sprite\mOuter[0] / Width
		Sprite\mOuterUV[1] = Sprite\mOuter[1] / Height
		Sprite\mOuterUV[2] = Sprite\mOuter[2] / Width
		Sprite\mOuterUV[3] = Sprite\mOuter[3] / Height
		
		; Convert Inner Limits to valid UV coordinates.
		Sprite\mInnerUV[0] = Sprite\mInner[0] / Width
		Sprite\mInnerUV[1] = Sprite\mInner[1] / Height
		Sprite\mInnerUV[2] = Sprite\mInner[2] / Width
		Sprite\mInnerUV[3] = Sprite\mInner[3] / Height
		
		Return Sprite
	EndIf
	Return Null
End Function

Function TSprite_Fill(Sprite.TSprite, X#, Y#, Width#, Height#, Z#=0, Modes%=0)
	Local TileX%, TileXPos#, TileXPosE#, TileWidth#, TileHMult#, TileCountH#, TileCountH2, TempHMult#, TileUVLeft#, TileUVRight#
	Local TileY%, TileYPos#, TileYPosE#, TileHeight#, TileVMult#, TileCountV#, TileCountV2, TempVMult#, TileUVTop#, TileUVBottom#
	
	If Sprite <> Null And Sprite\mSurface <> 0 And Width > 0 And Height > 0 Then
		If Sprite\mShareSurface = False Then ClearSurface(Sprite\mSurface)
		
		; Translate Mode into easily used variables.
		Local BorderLft = ((Modes And TSPRITE_BORDER_LFT) > 0)
		Local BorderRgt = ((Modes And TSPRITE_BORDER_RGT) > 0)
		Local BorderTop = ((Modes And TSPRITE_BORDER_TOP) > 0)
		Local BorderBtm = ((Modes And TSPRITE_BORDER_BTM) > 0)
		Local BorderOutLft = (-1 + ((Modes And TSPRITE_BORDEROUT_LFT) > 0))
		Local BorderOutRgt = (-1 + ((Modes And TSPRITE_BORDEROUT_RGT) > 0))
		Local BorderOutTop = (-1 + ((Modes And TSPRITE_BORDEROUT_TOP) > 0))
		Local BorderOutBtm = (-1 + ((Modes And TSPRITE_BORDEROUT_BTM) > 0))
		Local RoundDownH = ((Modes And TSPRITE_ROUNDDOWN_HORZ) > 0)
		Local RoundDownV = ((Modes And TSPRITE_ROUNDDOWN_VERT) > 0)
		Local PartialH = ((Modes And TSPRITE_PARTIAL_HORZ) > 0)
		Local PartialV = ((Modes And TSPRITE_PARTIAL_VERT) > 0)
		Local PartialCutH = ((Modes And TSPRITE_PARTIALCUT_HORZ) > 0)
		Local PartialCutV = ((Modes And TSPRITE_PARTIALCUT_VERT) > 0)
		Local ForceSingleH = ((Modes And TSPRITE_FORCESINGLE_HORZ) > 0)
		Local ForceSingleV = ((Modes And TSPRITE_FORCESINGLE_VERT) > 0)
		
		; Calculate scaled sizes.
		Local SpriteBorder#[4]
		SpriteBorder[0] = (Sprite\mBorder[0] * Sprite\mBorderScale[0]) * BorderLft
		SpriteBorder[2] = (Sprite\mBorder[2] * Sprite\mBorderScale[0]) * BorderRgt
		SpriteBorder[1] = (Sprite\mBorder[1] * Sprite\mBorderScale[0]) * BorderTop
		SpriteBorder[3] = (Sprite\mBorder[3] * Sprite\mBorderScale[0]) * BorderBtm
		Local SpriteSize#[2]
		SpriteSize[0] = (Sprite\mInner[2]-Sprite\mInner[0])*Sprite\mScale[0]
		SpriteSize[1] = (Sprite\mInner[3]-Sprite\mInner[1])*Sprite\mScale[1]
		
		; Calculate real values.
		Local RealWidth#  = Width + (SpriteBorder[0] * BorderOutLft) + (SpriteBorder[2] * BorderOutRgt)
		Local RealHeight# = Height + (SpriteBorder[1] * BorderOutTop) + (SpriteBorder[3] * BorderOutBtm)
		
		; Recalculate scaled border if we are below minimum size.
		Local BorderScale#[4], BorderMaximum#[2], SizeScale#[2]
		BorderMaximum[0] = ((SpriteBorder[0] * -BorderOutLft) + (SpriteBorder[2] * -BorderOutRgt))
		BorderMaximum[1] = ((SpriteBorder[1] * -BorderOutTop) + (SpriteBorder[3] * -BorderOutBtm))
		If Width < BorderMaximum[0] Then 
			SizeScale[0] = (Width / BorderMaximum[0])
			SpriteBorder[0] = SpriteBorder[0] * SizeScale[0]
			SpriteBorder[2] = SpriteBorder[2] * SizeScale[0]
			RealWidth = 0
		EndIf
		If Height < BorderMaximum[1] Then 
			SizeScale[1] = (Height / BorderMaximum[1])
			SpriteBorder[1] = SpriteBorder[1] * SizeScale[1]
			SpriteBorder[3] = SpriteBorder[3] * SizeScale[1]
			RealHeight = 0
		EndIf
		
		; Calculate Position
		Local RealX# = X - (SpriteBorder[0] * BorderOutLft)
		Local RealY# = -(Y - (SpriteBorder[1] * BorderOutTop))
		
		; Calculate tiles.
		If ForceSingleH = 0 Then
			If RealWidth > 0 Then TileCountH = RealWidth / SpriteSize[0] Else TileCountH = 0
		Else
			TileCountH = 1
		EndIf
		If ForceSingleV = 0 Then
			If RealHeight > 0 Then TileCountV = RealHeight / SpriteSize[1] Else TileCountV = 0
		Else
			TileCountV = 1
		EndIf
		If PartialH = 1 Or RoundDownH = 0 Then TileCountH2 = Ceil(TileCountH) Else TileCountH2 = Floor(TileCountH)
		If PartialV = 1 Or RoundDownV = 0 Then TileCountV2 = Ceil(TileCountV) Else TileCountV2 = Floor(TileCountV)
		If PartialH = 1 Then TileWidth = (RealWidth / TileCountH) Else TileWidth = (RealWidth / TileCountH2)
		If PartialV = 1 Then TileHeight = (RealHeight / TileCountV) Else TileHeight = (RealHeight / TileCountV2)
		If (TileCountH2 < 0) Then TileCountH2 = 0 Else TileCountH2 = TileCountH2 - 1
		If (TileCountV2 < 0) Then TileCountV2 = 0 Else TileCountV2 = TileCountV2 - 1
		
		; Calculate Partial scale
		If PartialH = 1 Then TileHMult = TileCountH - TileCountH2
		If PartialV = 1 Then TileVMult = TileCountV - TileCountV2
		
		; Draw Corners
		If SpriteBorder[1] > 0 Then 
			If (BorderTop And BorderLft And SpriteBorder[0] > 0) Then TSprite_CreateQuad(Sprite\mSurface, RealX - SpriteBorder[0], RealX, RealY + SpriteBorder[1], RealY, 0, 0, Sprite\mOuterUV[0], Sprite\mInnerUV[0], Sprite\mOuterUV[1], Sprite\mInnerUV[1])
			If (BorderTop And BorderRgt And SpriteBorder[2] > 0) Then TSprite_CreateQuad(Sprite\mSurface, RealX + RealWidth, RealX + RealWidth + SpriteBorder[2], RealY + SpriteBorder[1], RealY, Z, Z, Sprite\mInnerUV[2], Sprite\mOuterUV[2], Sprite\mOuterUV[1], Sprite\mInnerUV[1])
		EndIf
		If SpriteBorder[3] > 0 Then
			If (BorderBtm And BorderLft And SpriteBorder[0] > 0) Then TSprite_CreateQuad(Sprite\mSurface, RealX - SpriteBorder[0], RealX, RealY - RealHeight, RealY - RealHeight - SpriteBorder[3], Z, Z, Sprite\mOuterUV[0], Sprite\mInnerUV[0], Sprite\mInnerUV[3], Sprite\mOuterUV[3])
			If (BorderBtm And BorderRgt And SpriteBorder[2] > 0) Then TSprite_CreateQuad(Sprite\mSurface, RealX + RealWidth, RealX + RealWidth + SpriteBorder[2], RealY - RealHeight, RealY - RealHeight - SpriteBorder[1], Z, Z, Sprite\mInnerUV[2], Sprite\mOuterUV[2], Sprite\mInnerUV[3], Sprite\mOuterUV[3])
		EndIf
		
		For TileX% = 0 To TileCountH2
			; Horizontal coordinates and UV.
			TileXPos = RealX + (TileX * TileWidth)
			TileXPosE = TileXPos + TileWidth
			TileUVLeft = Sprite\mInnerUV[0]
			TileUVRight = Sprite\mInnerUV[2]
			
			; Support for partial horizontal tiles.
			If PartialH And TileX = TileCountH2 Then
				TileXPosE = TileXPos + (TileWidth * TileHMult)
				If PartialCutH Then TileUVRight = (TileUVLeft * (1-TileHMult)) + (TileUVRight * TileHMult)
			EndIf
			
			For TileY% = 0 To TileCountV2
				; Vertical coordinates and UV.
				TileYPos = RealY - (TileY * TileHeight)
				TileYPosE = TileYPos - TileHeight
				TileUVTop = Sprite\mInnerUV[1]
				TileUVBottom = Sprite\mInnerUV[3]
				
				; Support for partial vertical tiles.
				If PartialV And TileY = TileCountV2 Then
					TileYPosE = TileYPos - (TileHeight * TileVMult)
					If PartialCutV Then TileUVBottom = (TileUVTop * (1-TileVMult)) + (TileUVBottom * TileVMult)
				EndIf
				
				; Tiles
				TSprite_CreateQuad(Sprite\mSurface, TileXPos, TileXPosE, TileYPos, TileYPosE, Z, Z, TileUVLeft, TileUVRight, TileUVTop, TileUVBottom)
				
			Next
			
			; Horizontal Border
			If (BorderTop) Then TSprite_CreateQuad(Sprite\mSurface, TileXPos, TileXPosE, RealY + SpriteBorder[1], RealY, Z, Z, TileUVLeft, TileUVRight, Sprite\mOuterUV[1], Sprite\mInnerUV[1])
			If (BorderBtm) Then TSprite_CreateQuad(Sprite\mSurface, TileXPos, TileXPosE, RealY - RealHeight, RealY - RealHeight - SpriteBorder[3], Z, Z, TileUVLeft, TileUVRight, Sprite\mInnerUV[3], Sprite\mOuterUV[3])
		Next
		
		For TileY% = 0 To TileCountV2
			; Vertical coordinates and UV.
			TileYPos = RealY - (TileY * TileHeight)
			TileYPosE = TileYPos - TileHeight
			TileUVTop = Sprite\mInnerUV[1]
			TileUVBottom = Sprite\mInnerUV[3]
			
			; Support for partial vertical tiles.
			If PartialV And TileY = TileCountV2 Then
				TileYPosE = TileYPos - (TileHeight * TileVMult)
				If PartialCutV Then TileUVBottom = (TileUVTop * (1-TileVMult)) + (TileUVBottom * TileVMult)
			EndIf
			
			; Vertical Border
			If (BorderLft) Then TSprite_CreateQuad(Sprite\mSurface, RealX - SpriteBorder[0], RealX, TileYPos, TileYPosE, Z, Z, Sprite\mOuterUV[0], Sprite\mInnerUV[0], TileUVTop, TileUVBottom)
			If (BorderRgt) Then TSprite_CreateQuad(Sprite\mSurface, RealX + RealWidth, RealX + RealWidth + SpriteBorder[2], TileYPos, TileYPosE, Z, Z, Sprite\mInnerUV[2], Sprite\mOuterUV[2], TileUVTop, TileUVBottom)
		Next
	EndIf
End Function

Function TSprite_CreateQuad(Surface%, X0#, X1#, Y0#, Y1#, Z0#, Z1#, U0# = 0, U1# = 0, V0# = 0, V1# = 0, W0# = 0, W1# = 0, TurnOrder% = 0, ShareVertices% = True)
	Local TrisOut% = 0
	
	Local vTL, vBL, vTR, vBR
	vTL = AddVertex(Surface, X0, Y0, Z0, U0, V0, W0)
	vTR = AddVertex(Surface, X1, Y0, (Z0+Z1)/2, U1, V0, (W0+W1)/2)
	vBL = AddVertex(Surface, X0, Y1, (Z0+Z1)/2, U0, V1, (W0+W1)/2)
	vBR = AddVertex(Surface, X1, Y1, Z1, U1, V1, W1)
	
	If ShareVertices Then
		If (TurnOrder) Then
			TrisOut = TrisOut + AddTriangle(Surface, vBL, vTL, vTR) Shl 16
			TrisOut = TrisOut + AddTriangle(Surface, vBL, vTR, vBR) 
		Else
			TrisOut = TrisOut + AddTriangle(Surface, vTL, vTR, vBR) Shl 16
			TrisOut = TrisOut + AddTriangle(Surface, vTL, vBR, vBL) 
		EndIf
	Else
		If TurnOrder Then
			Local vTR2, vBL2
			vTR2 = AddVertex(Surface, X1, Y0, (Z0+Z1)/2, U1, V0, (W0+W1)/2)
			vBL2 = AddVertex(Surface, X0, Y1, (Z0+Z1)/2, U0, V1, (W0+W1)/2)
			
			TrisOut = TrisOut + AddTriangle(Surface, vBL, vTL, vTR) Shl 16
			TrisOut = TrisOut + AddTriangle(Surface, vBL2, vTR2, vBR) 
		Else
			Local vTL2, vBR2
			vTL2 = AddVertex(Surface, X0, Y0, Z0, U0, V0, W0)
			vBR2 = AddVertex(Surface, X1, Y1, Z1, U1, V1, W1)
			
			TrisOut = TrisOut + AddTriangle(Surface, vTL, vTR, vBR) Shl 16
			TrisOut = TrisOut + AddTriangle(Surface, vTL2, vBR2, vBL) 
		EndIf
	EndIf
	Return TrisOut
End Function

Function TSprite_CreateQuadEx(Surface%, X0#, X1#, Y0#, Y1#, Z0#, Z1#, U0# = 0, U1# = 0, V0# = 0, V1# = 0, W0# = 0, W1# = 0, ShareVertices% = True)
	Local vTL, vTR, vCC, vBL, vBR
	vTL = AddVertex(Surface, X0, Y0, Z0, U0, V0, W0)
	vTR = AddVertex(Surface, X1, Y0, (Z0+Z1)/2, U1, V0, (W0+W1)/2)
	vCC = AddVertex(Surface, (X0+X1)/2, (Y0+Y1)/2, (Z0+Z1)/2, (U0+U1)/2, (V0+V1)/2, (W0+W1)/2)
	vBL = AddVertex(Surface, X0, Y1, (Z0+Z1)/2, U0, V1, (W0+W1)/2)
	vBR = AddVertex(Surface, X1, Y1, Z1, U1, V1, W1)
	
	If ShareVertices Then
		AddTriangle(Surface, vTL, vTR, vCC)
		AddTriangle(Surface, vBL, vTL, vCC)
		AddTriangle(Surface, vTR, vBR, vCC)
		AddTriangle(Surface, vBL, vBR, vCC)
	Else
		Local vTL2, vTR2, vBL2, vBR2, vCC2, vCC3, vCC4
		vTL2 = AddVertex(Surface, X0, Y0, Z0, U0, V0, W0)
		vTR2 = AddVertex(Surface, X1, Y0, (Z0+Z1)/2, U1, V0, (W0+W1)/2)
		vBL2 = AddVertex(Surface, X0, Y1, (Z0+Z1)/2, U0, V1, (W0+W1)/2)
		vBR2 = AddVertex(Surface, X1, Y1, Z1, U1, V1, W1)
		vCC2 = AddVertex(Surface, (X0+X1)/2, (Y0+Y1)/2, (Z0+Z1)/2, (U0+U1)/2, (V0+V1)/2, (W0+W1)/2)
		vCC3 = AddVertex(Surface, (X0+X1)/2, (Y0+Y1)/2, (Z0+Z1)/2, (U0+U1)/2, (V0+V1)/2, (W0+W1)/2)
		vCC4 = AddVertex(Surface, (X0+X1)/2, (Y0+Y1)/2, (Z0+Z1)/2, (U0+U1)/2, (V0+V1)/2, (W0+W1)/2)
		
		AddTriangle(Surface, vTL, vTR, vCC)
		AddTriangle(Surface, vBL, vTL2, vCC2)
		AddTriangle(Surface, vTR2, vBR, vCC3)
		AddTriangle(Surface, vBL2, vBR2, vCC4)
	EndIf
End Function
;~IDEal Editor Parameters:
;~F#1C#36#5D#65#70#79#84#8D#98#A2#AA#B2#BA#C2#CA#D5#E6#F7#102#1D3
;~F#1F8
;~C#Blitz3D