Global sTestTxt$
sTestTxt$ = sTestTxt$ + "|fFF0000H|fFF7E00a|fFFFF00l|f7EFF00l|f00FF00o |f00FF7EW|f00FFFFe|f007EFFl|f0000FFt|f7E00FF!|fFF00FF!|fFF007E!"+Chr(10)
sTestTxt$ = sTestTxt$ + Chr(10)
sTestTxt$ = sTestTxt$ + "|fFFFFFFDies ist ein Text der |fFF0000Rot |f00FF00Grün |fFFFFFFund |f0000FFBlau |fFFFFFFist." + Chr(10)
sTestTxt$ = sTestTxt$ + "|f33FF33Man kann alle mögli|f0000FFchen Farben machen!" + Chr(10)
sTestTxt$ = sTestTxt$ + "|f000000|bFF0000Sogar|b-1-1-1 |bFF7E00der|b-1-1-1 |bFFFF00Hinter|b7EFF00grund|b-1-1-1 |b00FF00kann|b-1-1-1 |b00FF7Egesetzt|b-1-1-1 |b00FFFFwerden|b-1-1-1!" + Chr(10)
sTestTxt$ = sTestTxt$ + "|fAAAAFF|b-1-1-1Dies kann für mehr|f444400|bAAAAFFzeilige Selektion|fAAAAFF|b-1-1-1 verwendet werden!" + Chr(10)
sTestTxt$ = sTestTxt$ + "|fFFFFFFD|fFF7E00amit wäre der |b7E7E7E|fFFFFFFB|fFF7E00eispieltext|b-1-1-1 beendet|fFFFFFF!"

Graphics 400,300,32,2
SetBuffer BackBuffer()

Global timer = CreateTimer(30)

While Not KeyHit(1)
	Cls
	AdvText(200,150, sTestTxt, MouseX()/200.0, MouseY()/150.0, 1)
	
	Flip 0
	WaitTimer timer
Wend

Include "AdvText.bb"