Strict

Import BRL.Stream
Import BRL.Retro

Function FileMD5$(filePath$, bufferSize=$400000)
  Assert (bufferSize & 63) = 0 Else "bufferSize must be a multiple of 64 bytes"
  
  Local h0 = $67452301, h1 = $EFCDAB89, h2 = $98BADCFE, h3 = $10325476
    
  Local r[] = [7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,..
                5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,..
                4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,..
                6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21]
                
  Local k[] = [$D76AA478, $E8C7B756, $242070DB, $C1BDCEEE, $F57C0FAF, $4787C62A,..
                $A8304613, $FD469501, $698098D8, $8B44F7AF, $FFFF5BB1, $895CD7BE,..
                $6B901122, $FD987193, $A679438E, $49B40821, $F61E2562, $C040B340,..
                $265E5A51, $E9B6C7AA, $D62F105D, $02441453, $D8A1E681, $E7D3FBC8,..
                $21E1CDE6, $C33707D6, $F4D50D87, $455A14ED, $A9E3E905, $FCEFA3F8,..
                $676F02D9, $8D2A4C8A, $FFFA3942, $8771F681, $6D9D6122, $FDE5380C,..
                $A4BEEA44, $4BDECFA9, $F6BB4B60, $BEBFBC70, $289B7EC6, $EAA127FA,..
                $D4EF3085, $04881D05, $D9D4D039, $E6DB99E5, $1FA27CF8, $C4AC5665,..
                $F4292244, $432AFF97, $AB9423A7, $FC93A039, $655B59C3, $8F0CCC92,..
                $FFEFF47D, $85845DD1, $6FA87E4F, $FE2CE6E0, $A3014314, $4E0811A1,..
                $F7537E82, $BD3AF235, $2AD7D2BB, $EB86D391]
                
  Local fileStream:TStream = OpenStream(filePath$, True, False)
  If fileStream = Null Then Return
  
  Local buffer:Byte Ptr = MemAlloc(bufferSize)
  Local bitCount:Long, dataTop = bufferSize
  
  Repeat
    Local bytesRead = fileStream.Read(buffer, bufferSize)
    
    If fileStream.EOF()
      dataTop = (((bytesRead + 8) Shr 6) + 1) Shl 6
      If dataTop > bufferSize
        buffer = MemExtend(buffer, bufferSize, dataTop)
      EndIf       
      
      bitCount :+ (bytesRead Shl 3)
      
      For Local b = (bytesRead + 1) Until (dataTop - 8)
        buffer[b] = 0
      Next
      
      buffer[bytesRead] = $80
      LEPokeLong(buffer, dataTop - 8, bitCount)
    Else
      bitCount :+ (bufferSize Shl 3)
    EndIf
    
    For Local chunkStart=0 Until (dataTop Shr 2) Step 16
      Local a = h0, b = h1, c = h2, d = h3
      
      For Local i=0 To 15
        Local f = d ~ (b & (c ~ d))
        Local t = d
        
        d = c ; c = b
        b = Rol((a + f + k[i] + LEPeekInt(buffer, (chunkStart + i) Shl 2)), r[i]) + b
        a = t
      Next
      
      For Local i=16 To 31
        Local f = c ~ (d & (b ~ c))
        Local t = d
  
        d = c ; c = b
        b = Rol((a + f + k[i] + LEPeekInt(buffer, (chunkStart + (((5 * i) + 1) & 15)) Shl 2)), r[i]) + b
        a = t
      Next
      
      For Local i=32 To 47
        Local f = b ~ c ~ d
        Local t = d
        
        d = c ; c = b
        b = Rol((a + f + k[i] + LEPeekInt(buffer, (chunkStart + (((3 * i) + 5) & 15)) Shl 2)), r[i]) + b
        a = t
      Next
      
      For Local i=48 To 63
        Local f = c ~ (b | ~d)
        Local t = d
        
        d = c ; c = b
        b = Rol((a + f + k[i] + LEPeekInt(buffer, (chunkStart + ((7 * i) & 15)) Shl 2)), r[i]) + b
        a = t
      Next

      h0 :+ a ; h1 :+ b
      h2 :+ c ; h3 :+ d
    Next
  Until fileStream.EOF()
  
  fileStream.Close()
  MemFree(buffer)
  
  Return (LEHex(h0) + LEHex(h1) + LEHex(h2) + LEHex(h3)).ToLower()  
End Function

Function Rol(val, shift)
  Return (val Shl shift) | (val Shr (32 - shift))
End Function

Function LEPeekInt(buffer:Byte Ptr, offset)
  Return (buffer[offset + 3] Shl 24) | (buffer[offset + 2] Shl 16) | ..
          (buffer[offset + 1] Shl 8) | buffer[offset] 
End Function

Function LEPokeLong(buffer:Byte Ptr, offset, value:Long)
  For Local b=7 To 0 Step -1
    buffer[offset + b] = (value Shr (b Shl 3)) & $ff
  Next
End Function

Function LEHex$(val)
  Local out$ = Hex(val)
  
  Return out$[6..8] + out$[4..6] + out$[2..4] + out$[0..2]
End Function