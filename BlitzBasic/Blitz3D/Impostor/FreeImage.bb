;FreeImage Module for B3D/B+
;Author: markcw, edited 29 Nov 08
;Site: freeimage.sourceforge.net/

;Load and save image functions

Function FiLoadImage(filename$)
 ;Loads an image from filename$
 ;Returns an image or False if fails
 ;Uses FiLoad and FiRead
	
	Local dib, image
	dib = FiLoad(filename$)
	image = FiRead(dib)
	FreeImage_Unload(dib)
	Return image
	
End Function

Function FiSaveImage(image, filename$)
 ;Saves an image as filename$
 ;Returns True if succeeds or False if fails
 ;Uses FiWrite and FiSave
	
	Local dib, bool
	dib = FiWrite(image)
	bool = FiSave(dib, filename$)
	FreeImage_Unload(dib)
	Return bool
	
End Function

Function FiLoad(filename$)
 ;Loads a FreeImage bitmap from filename$
 ;Returns a FreeImage bitmap or False if fails
	
	Local fif, dib
	fif = FreeImage_GetFIFFromFilename(filename$) ;Format from extension
	If fif >= 0 ;Format is valid
		dib = FreeImage_Load(fif, filename$, 0)
	EndIf
	Return dib
	
End Function

Function FiRead(dib)
 ;Reads a FreeImage bitmap and writes an image
 ;Returns an image or False if fails
	
	Local dib24, pixel, width, height, image, buffer, ix, iy
	
	dib24 = FreeImage_ConvertTo24Bits(dib) ;Copy to 24 bit, no palette
	If dib24 = 0 Then Return 0 ;Unknown format
	pixel = CreateBank(4) ;Temp bank
	width = FreeImage_GetWidth(dib)
	height = FreeImage_GetHeight(dib)
	image = CreateImage(width, height)
	buffer = GraphicsBuffer()
	
	LockBuffer(ImageBuffer(image))
	For iy = 0 To height - 1
		For ix = 0 To width - 1
			FreeImage_GetPixelColor(dib24, ix, height - 1 - iy, pixel) ;Invert
			WritePixelFast ix, iy, PeekInt(pixel, 0), ImageBuffer(image)
		Next
	Next
	UnlockBuffer(ImageBuffer(image))
	
	SetBuffer buffer
	FreeImage_Unload(dib24)
	FreeBank pixel
	Return image
	
End Function

Function FiSave(dib, filename$)
 ;Saves a FreeImage bitmap as filename$
 ;Returns True if succeeds or False if fails
	
	Local fif, bpp, dib8, dib24, bool
	
	If dib = 0 Then Return 0 ;No image
	fif = FreeImage_GetFIFFromFilename(filename$) ;Format from extension
	bpp = FreeImage_GetBPP(dib) ;bpp will be one of 1/4/8/16/24/32
	dib8 = dib ;8 bit or other, variable passed to FreeImage_Save
	dib24 = dib ;24 bit
	
	If fif >= 0 ;Format is valid
		If fif = 11 ;Check pgm format, pgm=11/pgmraw=12
			dib8 = FreeImage_ConvertToGreyscale(dib) ;8 bit greyscale
		ElseIf fif = 25 ;Gif format, gif=25
			If bpp <> 24 ;Dib not 24 bits
				dib24 = FreeImage_ConvertTo24Bits(dib)
			EndIf
			dib8 = FreeImage_ColorQuantize(dib24, 0) ;8 bit paletted
			If dib24 <> dib ;24 bit conversion was required
				FreeImage_Unload(dib24)
			EndIf
		EndIf
		bool = FreeImage_Save(fif, dib8, filename$, 0)
		If dib8 <> dib ;8 bit conversion was required
			FreeImage_Unload(dib8)
		EndIf
	EndIf
	
	Return bool
	
End Function

Function FiWrite(image)
 ;Reads an image and writes a FreeImage bitmap
 ;Returns a FreeImage bitmap or False if fails
	
	Local pixel, width, height, dib, buffer, ix, iy
	
	If image = 0 Then Return 0 ;No image
	pixel = CreateBank(4) ;Temp bank
	width = ImageWidth(image)
	height = ImageHeight(image)
	dib = FreeImage_Allocate(width, height, 24, $ff0000, $00ff00, $0000ff)
	buffer = GraphicsBuffer()
	
	LockBuffer(ImageBuffer(image))
	For iy = 0 To height - 1
		For ix = 0 To width - 1
			PokeInt pixel, 0, ReadPixelFast(ix, iy, ImageBuffer(image))
			FreeImage_SetPixelColor(dib, ix, height - 1 - iy, pixel) ;Invert
		Next
	Next
	UnlockBuffer(ImageBuffer(image))
	
	SetBuffer buffer
	FreeBank pixel
	Return dib
	
End Function

Function FiUnload(dib)
 ;Frees a FreeImage bitmap, a wrapper function
 ;Returns nothing
	
	FreeImage_Unload(dib)
	
End Function

;Load and save anim image functions

Function FiLoadAnimImage(filename$, index = 0, frames = 0)
 ;Loads an anim image from a FreeImage multipage bitmap
 ;index -> first frame, 0=first, frames -> number of frames, 0=all
 ;Returns an image or False if fails
 ;Uses FiOpenAnim
	
	Local dib, count, pixel, page, width, height
	Local image, graphic, buffer, dib24, ix, iy, dst
	
	dib = FiOpenAnim(filename$, 1) ;Load read-only
	If dib = 0 Then Return 0 ;No dib
	count = FreeImage_GetPageCount(dib) ;Number of frames
	If index < 0 Or index > count - 1 Then index = count - 1
	If frames < 1 Or frames > count Then frames = count
	If frames + index > count Then frames = count - index
	
	pixel = CreateBank(4)
	page = FreeImage_LockPage(dib, 0) ;Lock first page
	width = FreeImage_GetWidth(page) ;Get the width/height
	height = FreeImage_GetHeight(page)
	FreeImage_UnlockPage(dib, page, 0) ;Unlock
	image = CreateImage(width, height, frames)
	graphic = CreateImage(width, height)
	buffer = GraphicsBuffer()
	
	For count = 0 To index + frames - 1
		page = FreeImage_LockPage(dib, count) ;Lock next page
		dib24 = FreeImage_ConvertTo24Bits(page) ;Copy to 24 bits, no palette
		FreeImage_UnlockPage(dib, page, 0) ;Unlock
		
		LockBuffer(ImageBuffer(graphic))
		For iy = 0 To height - 1
			For ix = 0 To width - 1
				FreeImage_GetPixelColor(dib24, ix, height - 1 - iy, pixel) ;Invert
				WritePixelFast ix, iy, PeekInt(pixel, 0), ImageBuffer(graphic)
			Next
		Next
		UnlockBuffer(ImageBuffer(graphic))
		
		If count - index >= 0 ;Frame is valid
			dst = ImageBuffer(image, count - index) ;Copy graphic to frame
			CopyRect 0, 0, width, height, 0, 0, ImageBuffer(graphic), dst
		EndIf
		FreeImage_Unload(dib24) ;Free dib
	Next
	
	SetBuffer buffer
	FreeImage graphic
	FreeBank pixel
	FreeImage_CloseMultiBitmap(dib, 0)
	Return image
	
End Function

Function FiSaveAnimImage(image, filename$, frames, index = 0)
 ;Saves an anim image as filename$
 ;frames -> number of frames, index -> first frame, 0=first
 ;Returns True if succeeds or False if fails
 ;Uses FiOpenAnim
	
	Local dib, fif, pixel, width, height, dib24
	Local buffer, count, ix, iy, page
	
	If image = 0 Then Return 0 ;No image
	dib = FiOpenAnim(filename$, 2, 0) ;2=create new
	fif = FreeImage_GetFIFFromFilename(filename$) ;format from extension
	frames = frames + index
	If frames < 1 Then frames = 1 ;Limit
	If index < 0 Or index > frames - 1 Then index = frames - 1
	
	pixel = CreateBank(4) ;Temp bank
	width = ImageWidth(image)
	height = ImageHeight(image)
	dib24 = FreeImage_Allocate(width, height, 24, $ff0000, $00ff00, $0000ff)
	buffer = GraphicsBuffer()
	
	For count = index To frames - 1
		LockBuffer(ImageBuffer(image, count))
		For iy = 0 To height - 1
			For ix = 0 To width - 1
				PokeInt pixel, 0, ReadPixelFast(ix, iy, ImageBuffer(image, count))
				FreeImage_SetPixelColor(dib24, ix, height - 1 - iy, pixel) ;Invert
			Next
		Next
		UnlockBuffer(ImageBuffer(image, count))
		
		If fif = 25 ;Gif format
			page = FreeImage_ColorQuantize(dib24, 0) ;8 bit palette
		Else ;Other format, ico=1/tif=18
			page = FreeImage_ConvertTo24Bits(dib24) ;24 bits
		EndIf
		FreeImage_AppendPage(dib, page) ;Add next page
		FreeImage_Unload(page)
	Next
	
	SetBuffer buffer
	FreeBank pixel
	FreeImage_Unload(dib24)
	FreeImage_CloseMultiBitmap(dib, 0)
	Return 1
	
End Function

Function FiAnimFrames(filename$, index = 0, frames = 0)
 ;Returns the number of frames in a FreeImage multipage bitmap
 ;index -> first frame, 0=first, frames -> number of frames, 0=all
 ;Uses FiOpenAnim
	
	Local dib, count
	dib = FiOpenAnim(filename$, 1) ;Load as read-only
	count = FreeImage_GetPageCount(dib)
	If index < 0 Or index > count - 1 Then index = count - 1
	If frames < 1 Or frames > count Then frames = count
	If frames + index > count Then frames = count - index
	FreeImage_CloseMultiBitmap(dib, 0)
	Return frames
	
End Function

Function FiOpenAnim(filename$, opentype = 0, flags = 0)
 ;Opens a FreeImage multipage bitmap from filename$
 ;opentype -> 0=open read/write, 1=open read-only, 2=create new
 ;Returns a FreeImage multipage bitmap or False if fails
	
	Local fif, dib, isnew, isread
	If opentype = 1 Then isread = 1
	If opentype = 2 Then isnew = 1
	fif = FreeImage_GetFIFFromFilename(filename$) ;Format from extension
	If fif >= 0 ;Format is valid
		dib = FreeImage_OpenMultiBitmap(fif, filename$, isnew, isread, 0, flags)
	EndIf
	Return dib
	
End Function

Function FiCloseAnim(dib, flags = 0)
 ;Closes a FreeImage multipage bitmap, a wrapper function
 ;Returns nothing
	
	FreeImage_CloseMultiBitmap(dib, flags)
	
End Function

;Image manipulation functions

Function FiRescale(dib, scale, filter = 0)
 ;Rescales a FreeImage bitmap
 ;scale -> scale as percentage, filter -> scale algorithm 0..5
 ;Returns a rescaled FreeImage bitmap or False if fails
	
	Local width, height
	width = (scale * FreeImage_GetWidth(dib)) / 100
	height = (scale * FreeImage_GetHeight(dib)) / 100
	Return FreeImage_Rescale(dib, width, height, filter)
	
End Function

Function FiRotateClassic(dib, angle#)
 ;Rotates a FreeImage bitmap by a degree, 0..360
 ;Returns a rotated FreeImage bitmap
	
	Local dlo, dhi
	angle# = -angle# ;Invert angle
	dlo = FiFloatToDouble(angle#, 0)
	dhi = FiFloatToDouble(angle#, 1)
	Return FreeImage_RotateClassic(dib, dlo, dhi)
	
End Function

Function FiRotateClassicEx(dib, angle#, bgcolor = 0)
 ;Rotates a FreeImage bitmap by a degree, 0..360
 ;bgcolor -> RGB background color
 ;Returns a rotated FreeImage bitmap
 ;From FreeImage source by Hervé Drolon
 ;NB: sub-functions need 16/24/32 bits per pixel
	
	Local bpp, hsrc, hdst
	bpp = FreeImage_GetBPP(dib)
	hsrc = dib ;Init source
	If bpp < 16 ;Image is 1/4/8 bits per pixel
		hsrc = FreeImage_ConvertTo24Bits(dib) ;Copy to 24 bits
	EndIf
	hdst = FiRotateAny(hsrc, angle#, bgcolor)
	If hsrc <> dib ;Conversion required
		FreeImage_Unload(hsrc) ;Free from memory
	EndIf
	Return hdst
	
End Function

Function FiAdjustGamma(dib, gamma#)
 ;Adjust gamma correction on a 8/24/32-bit FreeImage bitmap, 0.1..10.0
 ;A value of 1.0 leaves the image alone
 ;less than one darkens it, and greater than one lightens it
 ;Returns True if succeeds or False if fails
	
	Local dlo, dhi
	dlo = FiFloatToDouble(gamma#, 0)
	dhi = FiFloatToDouble(gamma#, 1)
	Return FreeImage_AdjustGamma(dib, dlo, dhi)
	
End Function

Function FiAdjustBrightness(dib, brightness#)
 ;Adjusts the brightness of a 8/24/32-bit FreeImage bitmap, -100..100
 ;A value 0 means no change, less than 0 will make the image darker
 ;and greater than 0 will make the image brighter
 ;Note: FreeImage_AdjustBrightness is actually an intensity algorithm
 ;Adapted from FreeImage source by Hervé Drolon
	
	Local plut, i, value#, bool
	plut = CreateBank(256) ;Lookup table
	If brightness# > 100 Then brightness# = 100 ;-100..100
	If brightness# < -100 Then brightness# = -100
	For i = 0 To 255 ;Build the lut
		value# = i + (255 * brightness# / 100) ;Calc brightness
		If value# > 255 Then value# = 255 ;value 0..255
		If value# < 0 Then value# = 0
		PokeByte plut, i, Floor(value# + 0.5)
	Next
	bool = FreeImage_AdjustCurve(dib, plut, 0) ;Apply lut
	FreeBank plut ;Free from memory
	Return bool
	
End Function

Function FiAdjustIntensity(dib, intensity#)
 ;Adjusts the intensity of a 8/24/32-bit FreeImage bitmap, -100..100
 ;A value 0 means no change, less than 0 will decrease the contrast
 ;and greater than 0 will increase the contrast
 ;Note: FreeImage_AdjustBrightness is actually an intensity algorithm
 ;From FreeImage source by Hervé Drolon
	
	Local plut, i, value#, bool
	plut = CreateBank(256) ;Lookup table
	If intensity# > 100 Then intensity# = 100 ;-100..100
	If intensity# < -100 Then intensity# = -100
	For i = 0 To 255 ;Build the lut
		value# = i * (100 + intensity#) / 100 ;Calc intensity
		If value# > 255 Then value# = 255 ;value 0..255
		If value# < 0 Then value# = 0
		PokeByte plut, i, Floor(value# + 0.5)
	Next
	bool = FreeImage_AdjustCurve(dib, plut, 0) ;Apply lut
	FreeBank plut ;Free from memory
	Return bool
	
End Function

Function FiAdjustContrast(dib, contrast#)
 ;Adjusts the contrast of a 8/24/32-bit FreeImage bitmap, -100..100
 ;A value 0 means no change, less than 0 will decrease the contrast
 ;and greater than 0 will increase the contrast
 ;Returns True if succeeds or False if fails
	
	Local dlo, dhi
	dlo = FiFloatToDouble(contrast#, 0)
	
	dhi = FiFloatToDouble(contrast#, 1)
	Return FreeImage_AdjustContrast(dib, dlo, dhi)
	
End Function

;Information functions

Function FiGetWidth(dib)
 ;Returns the width of a FreeImage bitmap, a wrapper function
	
	Return FreeImage_GetWidth(dib) ;Get bitmap info
	
End Function

Function FiGetHeight(dib)
 ;Returns the height of a FreeImage bitmap, a wrapper function
	
	Return FreeImage_GetHeight(dib)
	
End Function

Function FiGetBPP(dib)
 ;Returns the bits per pixel of a FreeImage bitmap, a wrapper function
	
	Return FreeImage_GetBPP(dib)
	
End Function

;Clipboard functions

Function FiCopyToClipboard(dib)
 ;Copies a FreeImage bitmap to the clipboard
 ;Returns True is succeeds or False if fails
 ;Uses User32.dll And Kernel32.dll
 ;From: Copying a DIB to the clipboard, by John Simmons
 ;Site: www.codeproject.com
	
	Local phdr, ppal, pbits, sdib, sbits, bank, shdr, spal, hmem
	
	phdr = FreeImage_GetInfoHeader(dib) ;Pointer to info header
	ppal = FreeImage_GetPalette(dib) ;Pointer to palette
	pbits = FreeImage_GetBits(dib) ;Pointer to bits
	
 ;Calc bits, DWORD-aligned scanline (Pitch) * Height
	sbits = FreeImage_GetPitch(dib) * FreeImage_GetHeight(dib)
	sdib = FreeImage_GetDIBSize(dib) ;total size
	bank = CreateBank(sdib) ;Bank to store dib
	FiApiMemoryToBank(bank, phdr, 40) ;Move info header to bank
	shdr = PeekInt(bank, 0) ;biSize
	
 ;Calc palette, FreeImage bitmaps use the BITMAPINFO struct
	spal = PeekInt(bank, 32) ;biClrUsed
	If Not spal
		If PeekShort(bank, 14) <> 24 ;No color table for 24-bit
			spal = 1 Shl PeekShort(bank, 14) ;biBitCount, colors=2/16/256
		EndIf
	EndIf
	spal = spal * 4 ;sizeof(RGBQUAD)
	
 ;Move bits, palette and header separately for proper alignment
	FiApiMemoryToBank(bank, pbits, sbits) ;Move bits to bank
	CopyBank bank, 0, bank, shdr + spal, sbits ;Move bits up
	FiApiMemoryToBank(bank, ppal, spal) ;Move palette to bank
	CopyBank bank, 0, bank, shdr, spal ;Move palette up
	FiApiMemoryToBank(bank, phdr, shdr) ;Move info header to bank
	
	
 ;Alloc memory block to store our dib
	hmem = FiApiGlobalAlloc(66, sdib) ;GHND=66, MOVEABLE=2|ZEROINIT=64
	If Not hmem ;Major bummer if we couldn't get memory block
		FreeBank bank
		Return False ;Fail
	EndIf
	
	phdr = FiApiGlobalLock(hmem) ;Lock memory and get pointer to it
	FiApiBankToMemory(phdr, bank, sdib) ;Move dib to memory
	FiApiGlobalUnlock(hmem) ;Unlock the dib
	
 ;Send the dib to the clipboard
	If FiApiOpenClipboard(0) ;hwnd
		FiApiEmptyClipboard() ;Free last data
		FiApiSetClipboardData(8, hmem) ;CF_DIB=8, hdata[bitmap]
		FiApiCloseClipboard()
	EndIf
	
	FreeBank bank
	Return True ;Success
	
End Function

Function FiPasteFromClipboard()
 ;Pastes the clipboard to a new FreeImage bitmap
 ;Returns a FreeImage bitmap or dummy bitmap if fails
 ;Uses User32.dll And Kernel32.dll
 ;From: Copying a DIB to the clipboard, by John Simmons
 ;Site: www.codeproject.com
	
	Local hmem, dib, phdr, bank, shdr, spal, bw, bh
	Local bpp, sbits, sdib, ppal, pbits
	
 ;Receive the bitmap from the clipboard as a dib
	If FiApiOpenClipboard(0) ;hwnd
		hmem = FiApiGetClipboardData(8) ;CF_DIB=8
		FiApiCloseClipboard()
	EndIf
	If Not hmem ;If we didn't get a dib, return a dummy bitmap
		dib = FreeImage_Allocate(1, 1, 24, $ff0000, $00ff00, $0000ff)
		Return dib ;Fail
	EndIf
	
	phdr = FiApiGlobalLock(hmem) ;Lock memory and get pointer to it
	bank = CreateBank(40) ;Init bank, we will resize it later
	FiApiMemoryToBank(bank, phdr, 40) ;Move info header to bank
	shdr = PeekInt(bank, 0) ;biSize
	
 ;Calc palette, Clipboard dibs use the BITMAPINFO struct
	spal = PeekInt(bank, 32) ;biClrUsed
	If Not spal
		If PeekShort(bank, 14) <> 24 ;No color table for 24-bit
			spal=1 Shl PeekShort(bank, 14) ;biBitCount, colors=2/16/256
		EndIf
	EndIf
	spal = spal * 4 ;sizeof(RGBQUAD)
	bw = PeekInt(bank, 4) ;biWidth
	bh = PeekInt(bank, 8) ;biHeight
	bpp = PeekShort(bank, 14) ;biBitCount
	
 ;Calc bits, DWORD-aligned scanline (Width * BitCount) * Height
	sbits = ((bw * bpp + 31) / 32 * 4) * bh
	sdib = shdr + spal + sbits ;Total size
	ResizeBank bank, sdib ;Resize bank to store dib
	FiApiMemoryToBank(bank, phdr, sdib) ;Move dib to bank
	FiApiGlobalUnlock(hmem) ;Unlock the dib
	
 ;Alloc FreeImage bitmap, this has its own info header
	dib = FreeImage_Allocate(bw, bh, bpp, $ff0000, $00ff00, $0000ff)
	ppal = FreeImage_GetPalette(dib) ;Pointer to palette
	pbits = FreeImage_GetBits(dib) ;Pointer to bits
	
 ;Move palette and bits (not header) separately for proper alignment
	CopyBank bank, shdr, bank, 0, spal ;Move palette down
	FiApiBankToMemory(ppal, bank, spal) ;Move palette to memory
	CopyBank bank, shdr + spal, bank, 0, sbits ;Move bits down
	FiApiBankToMemory(pbits, bank, sbits) ;Move bits to memory
	
	FreeBank bank
	Return dib ;Success
	
End Function

;Bank functions

Function FiBankFromFile(filename$, size = 0, pos = 0)
 ;Creates and reads a bank from filename$
 ;size -> file size, pos -> file position
 ;Returns a bank
	
	Local bank, file
	If size = 0 Then size = FileSize(filename$)
	bank = CreateBank(size)
	file = ReadFile(filename$)
	If file
		SeekFile(file, pos)
		ReadBytes(bank, file, 0, size)
		CloseFile(file)
	EndIf
	Return bank
	
End Function

Function FiBankToFile(bank, filename$, size = 0, pos = 0)
 ;Appends a bank to filename$
 ;size -> file size, pos -> file position
 ;Returns True if succeeds or False if fails
	
	Local file
	If bank = 0 Then Return 0 ;No bank
	If size = 0 Then size = FileSize(filename$)
	file = OpenFile(filename$) ;Existing file
	If file = 0 Then file = WriteFile(filename$) ;New file
	If file
		SeekFile(file, pos)
		WriteBytes(bank, file, 0, size)
		CloseFile(file)
		file = 1
	EndIf
	Return file
	
End Function

Function FiLoadFromBank(bank)
 ;Loads a FreeImage bitmap from a bank
 ;Returns a FreeImage bitmap or False if fails
	
	Local stream, fif, dib
	If bank = 0 Then Return 0 ;No bank
	stream = FreeImage_OpenMemory(bank, BankSize(bank)) ;Attach to memory
	fif = FreeImage_GetFileTypeFromMemory(stream, 0) ;Format from filetype
	If fif >= 0 ;Format is valid
		dib = FreeImage_LoadFromMemory(fif, stream, 0) ;Load from memory
	EndIf
	FreeImage_CloseMemory(stream) ;Close memory
	Return dib
	
End Function

;Zlib functions

Function FiZlibLoadImage(filename$, datafile$)
 ;Loads an image from a zlib compressed file stored in datafile$
 ;Returns an image
 ;Uses FiZlibUnpack, FiLoadFromBank and FiRead
	
	Local bank, dib, image
	bank = FiZlibUnpack(filename$, datafile$)
	dib = FiLoadFromBank(bank)
	image = FiRead(dib)
	FreeBank bank
	FreeImage_Unload(dib)
	Return image
	
End Function

Function FiZlibPack(filename$, datafile$)
 ;Compresses a file with zlib and appends it to datafile$
 ;Returns True if succeeds or False if fails
 ;Uses FiBankFromFile and FiBankToFile
	
	Local file, hdrsize, pos, dstsize, srcsize
	Local dataname$, count, srcbank, dstbank
	
	file = ReadFile(datafile$)
	If file ;Check if file exists
		hdrsize = ReadInt(file) ;"zlib" file type header
		While Not Eof(file)
			pos = FilePos(file)
			dstsize = ReadInt(file)
			srcsize = ReadInt(file)
			dataname$ = ""
			For count = 1 To Len(filename$)
				dataname$ = dataname$ + Chr(ReadByte(file))
			Next
			If filename$ = dataname$ Then count = -1 : Exit ;Found file
			SeekFile(file, pos + dstsize) ;Next file block
		Wend
		CloseFile(file)
		If count < 0 Then Return 1 ;File exists, don't save
	EndIf
	
	srcsize = FileSize(filename$) ;Load uncompressed file
	hdrsize = Len(filename$) + 9
	dstsize = srcsize + Int(srcsize * 0.1) + 12
	srcbank = FiBankFromFile(filename$, dstsize + hdrsize)
	
	dstbank = CreateBank(dstsize + hdrsize) ;Compress file
	dstsize = FreeImage_ZLibCompress(dstbank, dstsize, srcbank, srcsize)
	CopyBank dstbank, 0, srcbank, hdrsize, dstsize ;Room for header
	PokeInt srcbank, 0, dstsize + hdrsize ;Compressed + header size
	PokeInt srcbank, 4, srcsize ;Uncompressed size
	For count = 1 To Len(filename$) ;Filename
		PokeByte srcbank, count + 7, Asc(Mid(filename$, count, 1))
	Next
	PokeByte srcbank, hdrsize - 1, 0 ;Null terminator
	
	If FileType(datafile$) = 0 ;File doesn't exist
		file = WriteFile(datafile$)
		WriteInt file, $62696c7a ;"zlib" file type header
		CloseFile(file)
	EndIf
	
	srcsize = FileSize(datafile$) ;Save compressed file
	FiBankToFile(srcbank, datafile$, dstsize + hdrsize, srcsize)
	FreeBank dstbank
	FreeBank srcbank
	Return file
	
End Function

Function FiZlibUnpack(filename$, datafile$)
 ;Uncompresses a zlib compressed file stored in datafile$
 ;Returns a bank or False if fails
 ;Uses FiBankFromFile
	
	Local file, hdrsize, pos, dstsize, srcsize
	Local dataname$, count, srcbank, dstbank
	
	file = ReadFile(datafile$)
	If file ;Check if file exists
		hdrsize = ReadInt(file) ;"zlib" file type header
		While Not Eof(file)
			pos = FilePos(file)
			dstsize = ReadInt(file)
			srcsize = ReadInt(file)
			dataname$ = ""
			For count = 1 To Len(filename$)
				dataname$ = dataname$ + Chr(ReadByte(file))
			Next
			If filename$ = dataname$ Then count = -1 : Exit ;Found file
			SeekFile(file, pos + dstsize) ;Next file block
		Wend
		CloseFile(file)
		If count >= 0 Then Return 0 ;File doesn't exist, don't load
	EndIf
	
	hdrsize = Len(filename$) + 9 ;Load compressed file
	dstsize = dstsize - hdrsize
	srcbank = FiBankFromFile(datafile$, srcsize, pos + hdrsize)
	
	dstbank = CreateBank(srcsize) ;Uncompress file
	dstsize = FreeImage_ZLibUncompress(dstbank, srcsize, srcbank, dstsize)
	FreeBank srcbank
	Return dstbank
	
End Function

Function FiGZipLoadImage(filename$, datafile$)
 ;Loads an image from a gzip compressed file stored in datafile$
 ;Returns an image
 ;Uses FiGUnzip, FiLoadFromBank and FiRead
	
	Local bank, dib, image
	bank = FiGUnzip(filename$, datafile$)
	dib = FiLoadFromBank(bank)
	image = FiRead(dib)
	FreeBank bank
	FreeImage_Unload(dib)
	Return image
	
End Function

Function FiGZip(filename$, datafile$)
 ;Compresses a file with gzip and appends it to datafile$
 ;Returns True if succeeds or False if fails
 ;Uses FiBankFromFile and FiBankToFile
	
	Local file, hdrsize, pos, dstsize, srcsize
	Local dataname$, count, srcbank, dstbank
	
	file = ReadFile(datafile$)
	If file ;Check if file exists
		hdrsize = ReadInt(file) ;"gzip" file type header
		While Not Eof(file)
			pos = FilePos(file)
			dstsize = ReadInt(file)
			srcsize = ReadInt(file)
			dataname$ = ""
			For count = 1 To Len(filename$)
				dataname$ = dataname$ + Chr(ReadByte(file))
			Next
			If filename$ = dataname$ Then count = -1 : Exit ;Found file
			SeekFile(file, pos + dstsize) ;Next file block
		Wend
		CloseFile(file)
		If count < 0 Then Return 1 ;File exists, don't save
	EndIf
	
	srcsize = FileSize(filename$) ;Load uncompressed file
	hdrsize = Len(filename$) + 9
	dstsize = srcsize + Int(srcsize * 0.1) + 24
	srcbank = FiBankFromFile(filename$, dstsize + hdrsize)
	
	dstbank = CreateBank(dstsize + hdrsize) ;Compress file
	dstsize = FreeImage_ZLibGZip(dstbank, dstsize, srcbank, srcsize)
	CopyBank dstbank, 0, srcbank, hdrsize, dstsize ;Room for header
	PokeInt srcbank, 0, dstsize + hdrsize ;Compressed + header size
	PokeInt srcbank, 4, srcsize ;Uncompressed size
	For count = 1 To Len(filename$) ;Filename
		PokeByte srcbank, count + 7, Asc(Mid(filename$, count, 1))
	Next
	PokeByte srcbank, hdrsize - 1, 0 ;Null terminator
	
	If FileType(datafile$) = 0 ;File doesn't exist
		file = WriteFile(datafile$)
		WriteInt file, $70697a67 ;"gzip" file type header
		CloseFile(file)
	EndIf
	
	srcsize = FileSize(datafile$) ;Save compressed file
	FiBankToFile(srcbank, datafile$, dstsize + hdrsize, srcsize)
	FreeBank dstbank
	FreeBank srcbank
	Return file
	
End Function

Function FiGUnzip(filename$, datafile$)
 ;Uncompresses a gzip compressed file stored in datafile$
 ;Returns a bank or False if fails
 ;Uses FiBankFromFile
	
	Local file, hdrsize, pos, dstsize, srcsize
	Local dataname$, count, srcbank, dstbank
	
	file = ReadFile(datafile$)
	If file ;Check if file exists
		hdrsize = ReadInt(file) ;"gzip" file type header
		While Not Eof(file)
			pos = FilePos(file)
			
			dstsize = ReadInt(file)
			srcsize = ReadInt(file)
			dataname$ = ""
			For count = 1 To Len(filename$)
				dataname$ = dataname$ + Chr(ReadByte(file))
			Next
			If filename$ = dataname$ Then count = -1 : Exit ;Found file
			SeekFile(file, pos + dstsize) ;Next file block
		Wend
		CloseFile(file)
		If count >= 0 Then Return 0 ;File doesn't exist, don't load
	EndIf
	
	hdrsize = Len(filename$) + 9 ;Load compressed file
	dstsize = dstsize - hdrsize
	srcbank = FiBankFromFile(datafile$, srcsize, pos + hdrsize)
	
	dstbank = CreateBank(srcsize) ;Uncompress file
	dstsize = FreeImage_ZLibGUnzip(dstbank, srcsize, srcbank, dstsize)
	FreeBank srcbank
	Return dstbank
	
End Function

;Memory stream functions

Function FiSaveToMemory(filename$)
 ;Loads a FreeImage bitmap and saves it to a memory stream
 ;Returns a FreeImage memory stream or False if fails
	
	Local fif, dib, stream
	fif = FreeImage_GetFileType(filename$, 0) ;Format from filetype
	If fif >= 0 ;Format is valid
		dib = FreeImage_Load(fif, filename$, 0)
		stream = FreeImage_OpenMemory(dib, 0) ;Attach to memory
		FreeImage_SaveToMemory(fif, dib, stream, 0) ;Save to memory
		FreeImage_Unload(dib)
	EndIf
	Return stream
	
End Function

Function FiLoadFromMemory(stream)
 ;Loads a FreeImage bitmap from a memory stream
 ;Returns a FreeImage bitmap or False if fails
	
	Local fif, dib
	FreeImage_SeekMemory(stream, 0, 0) ;Seek to memory start
	fif = FreeImage_GetFileTypeFromMemory(stream, 0) ;Format from memory
	If fif >= 0 ;Format is valid
		dib = FreeImage_LoadFromMemory(fif, stream, 0)
	EndIf
	Return dib
	
End Function

Function FiMemoryToFile(stream, filename$)
 ;Saves a memory stream as filename$
 ;Assumes the file extension is known
 ;Returns True if succeeds or False if fails
 ;Uses Kernel32.dll
	
	Local buf, size, bank, file
	buf = CreateBank(4) ;Memory buffer
	size = CreateBank(4) ;Size of buffer
	FreeImage_AcquireMemory(stream, buf, size) ;Get buffer from stream
	bank = CreateBank(PeekInt(size, 0)) ;Bank for bitmap
	FiApiMemoryToBank(bank, PeekInt(buf, 0), PeekInt(size, 0)) ;Move to bank
	file = WriteFile(filename$)
	If file ;Check if file opened
		WriteBytes bank, file, 0, PeekInt(size, 0)
		CloseFile(file)
	EndIf
	FreeBank buf ;Free banks
	FreeBank size
	FreeBank bank
	If file Then file = 1
	Return file
	
End Function

Function FiMemorySize(stream)
 ;Returns the size of a FreeImage bitmap in a memory stream
	
	Local size
	FreeImage_SeekMemory(stream, 0, 2) ;Seek to memory end
	size = FreeImage_TellMemory(stream) ;Memory position is file size
	Return size
	
End Function

Function FiCloseMemory(stream)
 ;Frees a FreeImage bitmap in a memory stream, a wrapper function
 ;Returns nothing
	
	FreeImage_CloseMemory(stream)
	
End Function

;FiRotateClassicEx functions

Function FiRotateAny(hsrc, angle#, bgcolor)
 ;Rotates an image by a degree. Angle is unlimited
 ;Used by FiRotateClassicEx
 ;From FreeImage source by Hervé Drolon
	
	Local hmid, hdst
	
	hmid = hsrc ;Init middle
	While angle# < 0 : angle# = angle# + 360 : Wend ;Wrap angle to 0..360
	While angle# >= 360 : angle# = angle# - 360 : Wend
	
	If angle# > 45 And angle# <= 135 ;Angle in range 45..135
		hmid = FiRotate90(hsrc)
		angle# = angle# - 90
	ElseIf angle# > 135 And angle# <= 225 ;Angle in range 135..225
		hmid = FiRotate180(hsrc)
		angle# = angle# - 180
	ElseIf angle# > 225 And angle# <= 315 ;Angle in range 225..315
		hmid = FiRotate270(hsrc)
		angle# = angle# - 270
	EndIf
	
	If angle# = 0 ;Angle is 0
		If hmid = hsrc ;Nothing to do
			Return FreeImage_Clone(hsrc) ;Clone handle
		Else ;No more rotation needed
			Return hmid ;Rotated handle, multiple of 90
		EndIf
	Else ;Last rotation, angle in range -45..45
		hdst = FiRotate45(hmid, angle#, bgcolor)
		If hmid <> hsrc ;Middle conversion required
			FreeImage_Unload(hmid) ;Free from memory
		EndIf
		Return hdst ;Rotated handle, sheared
	EndIf
	
End Function

Function FiRotate45(hsrc, angle#, bgcolor)
 ;Rotates an image by a degree in range -45..45 (counter clockwise)
 ;Using the 3-shear technique
 ;Used by FiRotateClassicEx
 ;From FreeImage source by Hervé Drolon
	
	Local bpp, sinv#, tanv#, srcw, srch, i, shear#, offset, weight
	Local w1, h1, hdst1, w2, h2, hdst2, w3, h3, hdst3
	
	bpp = FreeImage_GetBPP(hsrc) ;Init values
	sinv# = Sin(angle#)
	tanv# = Tan(angle# / 2)
	srcw = FreeImage_GetWidth(hsrc)
	srch = FreeImage_GetHeight(hsrc)
	
	h1 = srch ;Calc 1st shear destination image dimensions
	w1 = srcw + Int(Float(srch) * Abs(tanv#) + 0.5)
	hdst1 = FreeImage_Allocate(w1, h1, bpp, 0, 0, 0)
	For i = 0 To h1 - 1 ;Perform 1st shear (horizontal)
		If tanv# >= 0 ;Positive angle
			shear# = (Float(i) + 0.5) * tanv#
		Else ;Negative angle
			shear# = (Float(Int(i) - h1) + 0.5) * tanv#
		EndIf
		offset = Int(Floor(shear#))
		weight = 255 * (shear# - Float(offset)) ;Was weight+1
		While weight < 0 : weight = weight + 256 : Wend ;Wrap byte to 0..255
		While weight > 255 : weight = weight - 256 : Wend
		FiHorizontalSkew(hsrc, hdst1, i, offset, weight, bgcolor)
	Next
	
	w2 = w1 ;Calc 2nd shear destination image dimensions
	h2 = Float(srcw) * Abs(sinv#) + Float(srch) * Cos(angle#) + 0.5
	hdst2 = FreeImage_Allocate(w2, h2, bpp, 0, 0, 0)
	If sinv# >= 0 ;Positive angle
		shear# = Float(srcw - 1) * sinv#
	Else ;Negative angle
		shear# = -sinv# * Float(srcw - w2)
	EndIf
	For i = 0 To w2 - 1 ;Perform 2nd shear (vertical)
		shear# = shear# - sinv#
		offset = Int(Floor(shear#))
		weight = 255 * (shear# - Float(offset)) ;Was weight+1
		While weight < 0 : weight = weight + 256 : Wend ;Wrap byte to 0..255
		While weight > 255 : weight = weight - 256 : Wend
		offset = offset + 1 ;Was offset
		FiVerticalSkew(hdst1, hdst2, i, offset, weight, bgcolor)
	Next
	FreeImage_Unload(hdst1) ;Free 1st shear
	
	h3 = h2 ;Calc 3rd shear destination image dimensions
	w3 = Float(srch) * Abs(sinv#) + Float(srcw) * Cos(angle#) + 0.5
	hdst3 = FreeImage_Allocate(w3, h3, bpp, 0, 0, 0)
	If sinv# >= 0 ;Positive angle
		shear# = Float(srcw - 1) * sinv#* - tanv#
	Else ;Negative angle
		shear# = tanv#*(Float(srcw - 1) * -sinv# + Float(1 - h3))
	EndIf
	For i = 0 To h3 - 1 ;Perform 3rd shear (horizontal)
		shear# = shear# + tanv#
		offset = Int(Floor(shear#))
		weight = 255 * (shear# - Float(offset)) ;Was weight+1
		While weight < 0 : weight = weight + 256 : Wend ;Wrap byte to 0..255
		While weight > 255 : weight = weight - 256 : Wend
		FiHorizontalSkew(hdst2, hdst3, i, offset, weight, bgcolor)
	Next
	FreeImage_Unload(hdst2) ;Free 2nd shear
	
	Return hdst3 ;3rd shear handle
	
End Function

Function FiHorizontalSkew(hsrc, hdst, row, offset, weight, bgcolor)
 ;Skews a row horizontally (with filtered weights)
 ;Limited to 45 degree skewing only. Filters two adjacent pixels
 ;Used by FiRotateClassicEx
 ;From FreeImage source by Hervé Drolon
	
	Local pxlsrc, pxlleft, pxloldleft, srcw, dstw, bytespp, i, j, ix, byte
	
	pxlsrc = CreateBank(12) ;4 byte arrays, for 32bit max
	pxlleft = 4 ;2nd array offset
	pxloldleft = 8 ;3rd array offset
	srcw = FreeImage_GetWidth(hsrc) + 2 ;Was srcw
	dstw = FreeImage_GetWidth(hdst)
	bytespp = FreeImage_GetLine(hsrc) / FreeImage_GetWidth(hsrc)
	
	For i = 0 To srcw - 1 ;Loop through row pixels
		PokeInt pxlsrc, 0, bgcolor ;Get background color
		If i > 0 And i < srcw - 1 ;Source in bounds
			FreeImage_GetPixelColor(hsrc, i - 1, row, pxlsrc) ;Was i
		EndIf
		For j = 0 To bytespp - 1 ;Calc weights
			PokeByte pxlsrc, j + pxlleft, PeekByte(pxlsrc, j) * weight / 256
		Next
		ix = i + offset - 1 ;Was offset
		If ix >= 0 And ix < dstw ;Check boundaries
			For j = 0 To bytespp - 1 ;Update left over on source
				byte=PeekByte(pxlsrc, j + pxlleft) - PeekByte(pxlsrc, j + pxloldleft)
				PokeByte pxlsrc, j, PeekByte(pxlsrc, j) - byte
			Next
			FreeImage_SetPixelColor(hdst, ix, row, pxlsrc)
		EndIf
		For j = 0 To bytespp - 1 ;Save leftover for next pixel in scan
			PokeByte pxlsrc, j + pxloldleft, PeekByte(pxlsrc, j + pxlleft)
		Next
	Next
	
	PokeInt pxlsrc, 0, bgcolor ;Get background color
	
	ix = srcw + offset - 1 ;Go to rightmost point of skew, nb: offset
	While ix < dstw ;Fill gap right of skew with background
		FreeImage_SetPixelColor(hdst, ix, row, pxlsrc)
		ix = ix + 1 ;Was ix
	Wend
	
	If offset > 0
		For j = 0 To offset - 1 ;Fill gap left of skew with background
			FreeImage_SetPixelColor(hdst, j, row, pxlsrc)
		Next
	EndIf
	
	FreeBank pxlsrc ;Free from memory
	
End Function

Function FiVerticalSkew(hsrc, hdst, col, offset, weight, bgcolor)
 ;Skews a column vertically (with filtered weights)
 ;Limited to 45 degree skewing only. Filters two adjacent pixels
 ;Used by FiRotateClassicEx
 ;From FreeImage source by Hervé Drolon
	
	Local pxlsrc, pxlleft, pxloldleft, srch, dsth, bytespp, i, j, iy, byte
	
	pxlsrc = CreateBank(12) ;4 byte arrays, for 32bit max
	pxlleft = 4 ;2nd array offset
	pxloldleft = 8 ;3rd array offset
	srch = FreeImage_GetHeight(hsrc) + 2 ;Was srch
	dsth = FreeImage_GetHeight(hdst)
	bytespp = FreeImage_GetLine(hsrc) / FreeImage_GetWidth(hsrc)
	
	For i = 0 To srch - 1 ;Loop through column pixels
		PokeInt pxlsrc, 0, bgcolor ;Get background color
		If i > 0 And i < srch - 1 ;Source in bounds
			FreeImage_GetPixelColor(hsrc, col, i - 1, pxlsrc) ;Was i
		EndIf
		For j = 0 To bytespp - 1 ;Calc weights
			PokeByte pxlsrc, j + pxlleft, PeekByte(pxlsrc, j) * weight / 256
		Next
		iy = i + offset - 1 ;Was offset
		If iy >= 0 And iy < dsth ;Check boundaries
			For j = 0 To bytespp - 1 ;Update left over on source
				byte = PeekByte(pxlsrc, j + pxlleft) - PeekByte(pxlsrc, j + pxloldleft)
				PokeByte pxlsrc, j, PeekByte(pxlsrc, j) - byte
			Next
			FreeImage_SetPixelColor(hdst, col, iy, pxlsrc)
		EndIf
		For j = 0 To bytespp - 1 ;Save leftover for next pixel in scan
			PokeByte pxlsrc, j + pxloldleft, PeekByte(pxlsrc, j + pxlleft)
		Next
	Next
	
	PokeInt pxlsrc, 0, bgcolor ;Get background color
	
	iy = srch + offset - 1 ;Go to bottom point of skew, nb: offset
	While iy < dsth ;Fill gap below skew with background
		FreeImage_SetPixelColor(hdst, col, iy, pxlsrc)
		iy = iy + 1 ;Was iy
	Wend
	
	If offset > 0
		For i = 0 To offset - 1 ;Fill gap above skew with background
			FreeImage_SetPixelColor(hdst, col, i, pxlsrc)
		Next
	EndIf
	
	FreeBank pxlsrc ;Free from memory
	
End Function

Function FiRotate90(hsrc)
 ;Rotates an image by 90 degrees (counter clockwise)
 ;Used by FiRotateClassicEx
 ;From FreeImage source by Hervé Drolon
 ;Code adapted from CxImage: www.xdp.it/cximage.htm
	
	Local pcolor, bpp, width, height, hdst, xs, ys, minw, minh, x, y, y2
	
	pcolor = CreateBank(4) ;Bank structure, for 32 bit max
	bpp = FreeImage_GetBPP(hsrc)
	width = FreeImage_GetHeight(hsrc)
	height = FreeImage_GetWidth(hsrc)
	hdst = FreeImage_Allocate(width, height, bpp, 0, 0, 0)
	
	For ys = 0 To height - 1 Step 64 ;Loop for x-segment and y-segment
		For xs = 0 To width - 1 Step 64 ;64=rblock, size of image blocks
			minh = height : If ys + 64 < height Then minh = ys + 64
			For y = ys To minh - 1 ;Do rotation
				y2 = height - y - 1
				minw = width : If xs + 64 < width Then minw = xs + 64
				For x = xs To minw - 1
					FreeImage_GetPixelColor(hsrc, y2, x, pcolor)
					FreeImage_SetPixelColor(hdst, x, y, pcolor)
				Next
			Next
		Next
	Next
	
	FreeBank pcolor ;Free from memory
	
	Return hdst ;Rotated handle
	
End Function

Function FiRotate180(hsrc)
 ;Rotates an image by 180 degrees (counter clockwise)
 ;Used by FiRotateClassicEx
 ;From FreeImage source by Hervé Drolon
	
	Local pcolor, bpp, width, height, hdst, x, y
	
	pcolor = CreateBank(4) ;Bank structure, for 32 bit max
	bpp = FreeImage_GetBPP(hsrc)
	width = FreeImage_GetWidth(hsrc)
	height = FreeImage_GetHeight(hsrc)
	hdst = FreeImage_Allocate(width, height, bpp, 0, 0, 0)
	
	For y = 0 To height - 1
		For x = 0 To width - 1
			FreeImage_GetPixelColor(hsrc, x, y, pcolor)
			FreeImage_SetPixelColor(hdst, width - x - 1, height - y - 1, pcolor)
		Next
	Next
	
	FreeBank pcolor ;Free from memory
	
	Return hdst ;Rotated handle
	
End Function

Function FiRotate270(hsrc)
 ;Rotates an image by 270 degrees (counter clockwise)
 ;Used by FiRotateClassicEx
 ;From FreeImage source by Hervé Drolon
 ;Code adapted from CxImage: www.xdp.it/cximage.htm
	
	Local pcolor, bpp, width, height, hdst, xs, ys, minw, minh, x, y, x2
	
	pcolor = CreateBank(4) ;Bank structure, for 32 bit max
	bpp = FreeImage_GetBPP(hsrc)
	width = FreeImage_GetHeight(hsrc)
	height = FreeImage_GetWidth(hsrc)
	hdst = FreeImage_Allocate(width, height, bpp, 0, 0, 0)
	
	For ys = 0 To height - 1 Step 64 ;Loop for x-segment and y-segment
		For xs = 0 To width - 1 Step 64 ;rblock=64, size of image blocks
			minw = width : If xs + 64 < width Then minw = xs + 64
			For x = xs To minw - 1 ;Do rotation
				x2 = width - x - 1
				minh = height : If ys + 64 < height Then minh = ys + 64
				For y = ys To minh - 1
					FreeImage_GetPixelColor(hsrc, y, x2, pcolor)
					FreeImage_SetPixelColor(hdst, x, y, pcolor)
				Next
			Next
		Next
	Next
	
	FreeBank pcolor ;Free from memory
	
	Return hdst ;Rotated handle
	
End Function

;FloatToDouble functions

Function FiFloatToDouble(value#, dpart = 0)
 ;Converts a float into a double as 2 integers
 ;dpart -> Double flag indicating which part to return, 0=dlo, 1=dhi
 ;Returns a low or high double integer - decimal equivalent of the float
 ;Site: techsupt.winbatch.com/TS/T000001034F21.html
	
	Local integer, sign, exponent, fraction, dexp, dlo, dhi
	integer = FiFloatToInt(value#)
	sign = integer And $80000000 ;Sign bit
	exponent = integer And $7F800000 ;8-bit exponent
	fraction = integer And $007FFFFF ;23-bit mantissa
	dexp = ((exponent Shr 23) - 127 + 1023) Shl 20 ;Double exponent
	dlo = (fraction And 7) Shl 29
	dhi = sign Or dexp Or (fraction Shr 3)
	If dpart = 0 Then Return dlo
	Return dhi
	
End Function

Function FiDoubleToFloat#(dlo, dhi)
 ;Converts a double as 2 integers into a float
 ;dlo -> Low double integer, dhi -> High double integer
 ;Returns a float - decimal equivalent of the double as 2 integers
 ;Site: techsupt.winbatch.com/TS/T000001034F21.html
	
	
	Local dsgn, sign, dexp, exponent, fraction
	dsgn = Abs(dhi Shr 31) ;Double sign
	sign = dsgn Shl 31 ;Sign bit
	dexp = Abs((dhi Shr 20) - (dsgn Shl 11)) ;Double exponent
	exponent = (dexp + 127 - 1023) Shl 23 ;8-bit exponent
	fraction = ((dhi And $000FFFFF) Shl 3) + (dlo Shr 29) ;23-bit mantissa
	Return FiIntToFloat(sign Or exponent Or fraction)
	
End Function

Function FiFloatToInt(value#)
 ;Converts a float into a float as an integer
 ;Returns an integer that is the binary equivalent of the float
 ;Site: wiki.tcl.tk/756
	
	Local sign, exponent, fraction#
	Local f1f#, f2f#, f3f#, se1, e2f1, f1, f2, f3
	If value# > 0 Then sign = 0 Else sign = 1
	value# = Abs(value#)
	exponent = Int(Floor(Log(value#) / 0.69314718055994529)) + 127
	fraction# = (value# / (2 ^ (exponent - 127))) - 1
	If exponent < 0 Then exponent = 0 : fraction# = 0.0 ;Round off to zero
	If exponent > 255 Then exponent = 255 ;Outside legal range for a float
	fraction# = fraction# * 128.0
	f1f# = Floor(fraction#)
	fraction# = (fraction# - f1f#) * 256.0
	f2f# = Floor(fraction#)
	fraction# = (fraction# - f2f#) * 256.0
	f3f# = Floor(fraction#)
	f1 = Int(f1f#) : f2 = Int(f2f#) : f3 = Int(f3f#)
	se1 = (sign Shl 7) Or (exponent Shr 1) ;Sign and Exponent1
	e2f1 = ((exponent And 1) Shl 7) Or f1 ;Exponent2 and Fraction1
	Return (se1 Shl 24) Or (e2f1 Shl 16) Or (f2 Shl 8) Or f3
	
End Function

Function FiIntToFloat#(value)
 ;Converts a float as an integer into a float
 ;Returns a float that is the binary equivalent of the integer
 ;Site: www.cs.princeton.edu/introcs/91float/
	
	Local sign, exponent, fraction
	sign = (value And $80000000) Shr 31
	exponent = (value And $7F800000) Shr 23
	fraction = value And $007FFFFF
	Return (-1 ^ sign) * (2 ^ (exponent - 127)) * (1 + (fraction / (2 ^ 23)))
	
End Function

;End of FreeImage Module
;~IDEal Editor Parameters:
;~C#Blitz3D