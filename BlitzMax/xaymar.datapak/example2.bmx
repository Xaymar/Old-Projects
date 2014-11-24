Framework brl.retro
Import xaymar.datapak

'Testing consistency of DataPaks
Local DPCnt:TDataPak = New TDataPak
DPCnt.SetName("My Container")

DPCnt.AddDataByte("My Byte", 127)
DPCnt.AddDataShort("My Short", 256)
DPCnt.AddDataInt("My Int", 65536)
DPCnt.AddDataLong("My Long", 618317896391:Long)
DPCnt.AddDataFloat("My Float", 1.23456789)
DPCnt.AddDataDouble("My Double", 1.2345678912345:Double)
DPCnt.AddDataString("My String", "Totally not yours!")
Local DPSCnt:TDataPak = DPCnt.AddDataContainer("My SubContainer")
	DPSCnt.AddDataByte("Sub Byte", 127)
	DPSCnt.AddDataShort("Sub Short", 256)
	DPSCnt.AddDataInt("Sub Int", 65536)
	DPSCnt.AddDataLong("Sub Long", 618317896391:Long)
	DPSCnt.AddDataFloat("Sub Float", 1.23456789)
	DPSCnt.AddDataDouble("Sub Double", 1.2345678912345:Double)
	DPSCnt.AddDataString("Sub String", "Totally not hub!")

DPCnt.ToFile("Consistency.sdp")
DPCnt.ToFile("Consistency.cdp", TDP_FLAG_COMPRESSED)
DPCnt.ToFile("Consistency.pdp", TDP_FLAG_PASSWORDED, "ThisIsATotallyUnsecurePasswordWithUnknownLengthToYouBecauseIDidn'tTellYouItsLength!")
DPCnt.ToFile("Consistency.mdp", TDP_FLAG_COMPRESSED | TDP_FLAG_PASSWORDED, "ThisIsATotallyUnsecurePasswordWithUnknownLengthToYouBecauseIDidn'tTellYouItsLength!")