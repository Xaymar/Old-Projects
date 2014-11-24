varying vec2 Texcoord;

float clip(float val, float low, float high) {
	float valout,diff;
	diff = high-low+1; valout = val-low;
	if (valout > diff) {valout -= float(floor(valout/diff))*diff;}
	if (valout < 0) {valout -= float(floor(valout/diff))*diff;}
	return (valout+low);
}

void main(void)
{
   gl_Position = ftransform();
   Texcoord = vec2(clip(gl_MultiTexCoord0.x,0,1),clip(gl_MultiTexCoord0.y,0,1));
}