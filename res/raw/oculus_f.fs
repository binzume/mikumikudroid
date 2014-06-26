  precision highp float;
  varying vec2 vTexCoord;
  uniform sampler2D sTex;
  uniform sampler2D sSphere;
  
  const vec2 offset = vec2(0.0, 0.0);
  const float texAspect = 1.0;

// Nexus7
  const float scaleR = 0.90;
  const float dx = -0.032;
  const float renderAspect = 9.0 / 7.0; //nexus7=9/7
// Nexus5
//  const float scaleR = 0.85; // nexus7=0.90
//  const float dx = 0.018; // nexus7= -0.032
//  const float renderAspect = 9.0 / 8.0; //nexus7=9/7
  
  
  vec2 calcTexCoord(vec2 texCoord) {
    vec2 scale_factor = vec2(scaleR * 2.0,scaleR * renderAspect);
	vec2 d = ( texCoord - vec2(0.25 , 0.5) ) * scale_factor;
    float dd = d.x * d.x + d.y*d.y;
    vec2 t1 = d * (0.8 + 1.20 * dd + 2.00 * dd*dd);
    return t1 * vec2(1.0 , 1.0) + vec2(0.5,0.5);
  }
  
  void main(){
    vec2 tc = calcTexCoord(vTexCoord - (vTexCoord.s < 0.5 ? vec2(0.0 - dx,0.0): vec2(0.5 + dx,0.0)));
    if (tc.x < 0.0 || tc.y < 0.0 || tc.x > 1.0 || tc.y > 1.0) {
      gl_FragColor = vec4(0.0,0.0,0.1,1.0);
    } else if (vTexCoord.s < 0.5) {
      gl_FragColor = texture2D(sTex,tc - offset);
    } else {
      gl_FragColor = texture2D(sSphere,tc + offset);
    }
  }
