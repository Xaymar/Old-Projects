#version 150
uniform sampler2D diffuseMap;
uniform sampler2D stateMap;
varying vec2 Texcoord;

uniform bool enabled;
uniform float mixStep;
int[14] stateLevels;

bool _sameSide(vec3 p1, vec3 p2, vec3 a, vec3 b) {
	vec3 cp1 = cross(b-a, p1-a);
	vec3 cp2 = cross(b-a, p2-a);
	if (dot(cp1,cp2) >= 0) {return true;}
	return false;
}
bool inTriangle(vec3 p, vec3 a, vec3 b, vec3 c) {
	if (_sameSide(p,a, b,c) && _sameSide(p,b, a,c) && _sameSide(p,c, a,b)) {return true;}
	return false;
}
bool inTriangle(vec2 p, vec2 a, vec2 b, vec2 c) {
	return inTriangle(vec3(p,0),vec3(a,0),vec3(b,0),vec3(c,0));
}
vec2 lineClosestPoint(vec2 a, vec2 b, vec2 p, bool clmp) {
	vec2 ap = p - a;
	vec2 ab = b - a;
	
	float ab2 = ab.x*ab.x + ab.y*ab.y;
	float ap_ab = ap.x*ab.x + ap.y*ab.y;
	float t = ap_ab / ab2;
	
	if (clmp == true) {
		if (t < 0.0) {
			t = 0.0;
		} else if (t > 1.0) {
			t = 1.0;
		}
	}
	vec2 closest = a + ab*t;
	return closest;
}

int clip(int val, int low, int high) {
	int valout,diff;
	diff = high-low+1; valout = val-low;
	if (valout >= diff) {valout -= int(floor(valout/diff))*diff;}
	if (valout < 0) {valout -= int(floor(valout/diff))*diff;}
	return (valout+low);
}

float clip(float val, float low, float high) {
	float valout,diff;
	diff = high-low+1; valout = val-low;
	if (valout >= diff) {valout -= (floor(valout/diff))*diff;}
	if (valout < 0) {valout -= (floor(valout/diff))*diff;}
	return (valout+low);
}

vec4 getTexel(sampler2D map, ivec2 pos, int lod) {
	ivec2 mapSize = textureSize(map, lod);
	pos.x = clip(pos.x, 0, mapSize.x-1);
	pos.y = clip(pos.y, 0, mapSize.y-1);
	return texelFetch(map, pos, lod);
}

void main(void)
{
	vec4 clrFlt = vec4(0,0,0,0);
	vec4 clrTex = texture2D(diffuseMap, Texcoord);
	
	if (enabled) {
		ivec2 stateMapSize = textureSize(stateMap, 0);
		ivec2 diffuseMapSize = textureSize(diffuseMap, 0);
		
		//Early-Out if texture sizes differ
		if ((stateMapSize.x != diffuseMapSize.x) || (stateMapSize.y != diffuseMapSize.y)) {
			if (mod(Texcoord.x*diffuseMapSize.x,2) <= 1) {
				if (mod(Texcoord.y*diffuseMapSize.y,2) <= 1) {
					gl_FragColor = vec4(1,0,1,1);
				} else {
					gl_FragColor = vec4(0,0,0,1);
				}
			} else {
				if (mod(Texcoord.y*diffuseMapSize.y,2) <= 1) {
					gl_FragColor = vec4(0,0,0,1);
				} else {
					gl_FragColor = vec4(1,0,1,1);
				}
			}
			return;
		}
		
		//Map intarrays into bigger intarray
		stateLevels[0] = 0;
		stateLevels[1] = 19;
		stateLevels[2] = 38;
		stateLevels[3] = 57;
		stateLevels[4] = 76;
		stateLevels[5] = 95;
		stateLevels[6] = 114;
		stateLevels[7] = 133;
		stateLevels[8] = 152;
		stateLevels[9] = 171;
		stateLevels[10] = 190;
		stateLevels[11] = 209;
		stateLevels[12] = 228;
		stateLevels[13] = 247;
		
		vec2 texPos = vec2(clip(Texcoord.x*stateMapSize.x-0.5, 0, stateMapSize.x-1),
			clip(Texcoord.y*stateMapSize.y-0.5,0,stateMapSize.y-1));
		vec2 dist = fract(texPos);
		ivec2 texel = ivec2(int(floor(texPos.x)),int(floor(texPos.y)));
		int state = int(getTexel(stateMap, texel, 0).r*255.0);
		
		//2x2 Array of Texels for interpolation
		vec4 texelTL = getTexel(diffuseMap, texel+ivec2(0,0), 0);
		vec4 texelTR = getTexel(diffuseMap, texel+ivec2(1,0), 0);
		vec4 texelBL = getTexel(diffuseMap, texel+ivec2(0,1), 0);
		vec4 texelBR = getTexel(diffuseMap, texel+ivec2(1,1), 0);
		
		vec4 top = mix(texelTL,texelTR,dist.x);
		vec4 bot = mix(texelBL,texelBR,dist.x);
		vec4 lft = mix(texelTL,texelBL,dist.y);
		vec4 rgt = mix(texelTR,texelBR,dist.y);
		
		if (state == stateLevels[0]) {
			clrFlt = mix(top,bot,dist.y);
		}
		if (state == stateLevels[1]) {
			if (dist.y < 0.5) {
				clrFlt = top;
			} else {
				clrFlt = bot;
			}
		}
		if (state == stateLevels[2]) {
			if (dist.x < 0.5) {
				clrFlt = lft;
			} else {
				clrFlt = rgt;
			}
		}
		if (state == stateLevels[3]) {
			if (inTriangle(dist, vec2(.5,1), vec2(1,.5), vec2(1,1))) {
				clrFlt = texelBR;
			} else {
				clrFlt = mix(top,lft,length(dist)); //Replace this with triangle calculation
			}
		}
		if (state == stateLevels[4]) {
			if (inTriangle(dist, vec2(.5,1), vec2(0,.5), vec2(0,1))) {
				clrFlt = texelBL;
			} else {
				clrFlt = mix(top,rgt,length(dist)); //Replace this with triangle calculation
			}
		}
		if (state == stateLevels[5]) {
			if (inTriangle(dist, vec2(0,.5), vec2(.5,0), vec2(0,0))) {
				clrFlt = texelTL;
			} else {
				clrFlt = mix(bot,rgt,length(dist)); //Replace this with triangle calculation
			}
		}
		if (state == stateLevels[6]) {
			if (inTriangle(dist, vec2(.5,0), vec2(1,.5), vec2(1,0))) {
				clrFlt = texelTR;
			} else {
				clrFlt = mix(bot,lft,length(dist)); //Replace this with triangle calculation
			}
		}
		if (state == stateLevels[7]) {
			if (dist.y < 0.5) {
				if (dist.x < 0.5) {
					clrFlt = texelTL;
				 } else {
				 	clrFlt = texelTR;
				 }
			} else {
				clrFlt = bot;
			}
		}
		if (state == stateLevels[8]) {
			if (dist.y > 0.5) {
				if (dist.x < 0.5) {
					clrFlt = texelBL;
				 } else {
				 	clrFlt = texelBR;
				 }
			} else {
				clrFlt = top;
			}
		}
		if (state == stateLevels[9]) {
			if (dist.x < 0.5) {
				if (dist.y < 0.5) {
					clrFlt = texelTL;
				 } else {
				 	clrFlt = texelBL;
				 }
			} else {
				clrFlt = rgt;
			}
		}
		if (state == stateLevels[10]) {
			if (dist.x > 0.5) {
				if (dist.y < 0.5) {
					clrFlt = texelTR;
				 } else {
				 	clrFlt = texelBR;
				 }
			} else {
				clrFlt = lft;
			}
		}
		if (state == stateLevels[11]) {
			if (inTriangle(dist, vec2(.5,1), vec2(0,.5), vec2(0,1))) {
				clrFlt = texelBL;
			} else {
				if (inTriangle(dist, vec2(.5,0), vec2(1,.5), vec2(1,0))) {
					clrFlt = texelTR;
				} else {
					clrFlt = mix(texelTL,texelBR,length(dist));
				}
			}
		}
		if (state == stateLevels[12]) {
			if (inTriangle(dist, vec2(.5,1), vec2(1,.5), vec2(1,1))) {
				clrFlt = texelBR;
			} else {
				if (inTriangle(dist, vec2(0,.5), vec2(.5,0), vec2(0,0))) {
					clrFlt = texelTL;
				} else {
					clrFlt = mix(texelTR,texelBL,length(dist));
				}
			}
		}
		if (state == stateLevels[13]) {
			if (dist.x < 0.5) {
				if (dist.y < 0.5) {
					clrFlt = texelTL;
				} else {
					clrFlt = texelBL;
				}
			} else {
				if (dist.y < 0.5) {
					clrFlt = texelTR;
				} else {
					clrFlt = texelBR;
				}
			}
		}
		
		//Debug codes:
		//clrFlt = vec4(vec2(texel)/stateMapSize,0,1); //Texel position
		//clrFlt = vec4(state/255.0,state/255.0,state/255.0,1); //State Level
	} else {
		clrFlt = clrTex;
	}
	
	gl_FragColor = mix(clrTex,clrFlt,mixStep);
}