Framework brl.retro
Import xaymar.datapak

'Advanced: Create a new Container
Local DPCnt:TDataPak = New TDataPak
DPCnt.SetName("MyContainer")

'Advanced: Add A new String (or any other Type: Byte, Short, Int, Long, Float, Double, Container)
Local DPStr:TDataPakString = New TDataPakString
DPStr.SetName("MyString")
DPStr.SetData("Hello :D")
DPCnt.AddData(DPStr)

'Easy: Add A new String (or any other Type: Byte, Short, Int, Long, Float, Double, Container)
DPCnt.AddDataString("MyString2", "Hallo :D")

'Easy: Safe to File
	'Uncompressed & Unpassworded [sdp = Standard Data Pak]
	DPCnt.ToFile("Test.sdp")
	'Compressed & Unpassworded   [cdp = Compressed Data Pak]
	DPCnt.ToFile("Test.cdp", TDP_FLAG_COMPRESSED)
	'Uncompressed & Passworded   [pdp = Passworded Data Pak]
	DPCnt.ToFile("Test.pdp", TDP_FLAG_PASSWORDED, "MyPassword")
	'Compressed & Passworded     [mdp = Merged Data Pak]
	DPCnt.ToFile("Test.mdp", TDP_FLAG_PASSWORDED | TDP_FLAG_COMPRESSED, "MyPassword")

'Easy: Loading from File
	'Uncompressed & Unpassworded [sdp = Standard Data Pak]
	Local DPCntS:TDataPak = TDataPak.FromFile("Test.sdp")
	'Compressed & Unpassworded   [cdp = Compressed Data Pak]
	Local DPCntC:TDataPak = TDataPak.FromFile("Test.cdp")
	'Uncompressed & Passworded   [pdp = Passworded Data Pak]
	Local DPCntP:TDataPak = TDataPak.FromFile("Test.pdp", "MyPassword")
	'Compressed & Passworded     [mdp = Merged Data Pak]
	Local DPCntM:TDataPak = TDataPak.FromFile("Test.mdp", "MyPassword")