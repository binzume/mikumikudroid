  precision highp float;
  varying vec2 vTexCoord;
  uniform sampler2D sTex;
  uniform sampler2D sSphere;
  const vec2 offset = vec2(0.0, 0.0);
  const float scaleR = 0.7;
  const float dx = 0.018;
  const float texAspect = 1.0;
  const float renderAspect = 16.0 / 9.0;
  
  vec2 calcTexCoord(vec2 texCoord) {
    vec2 scale_factor = vec2(scaleR,scaleR / renderAspect * 2.0);
	vec2 d = ( texCoord - vec2(0.25,0.5) ) * vec2(2.0, texAspect);
    float dd = d.x * d.x + d.y*d.y;
    vec2 t1 = d * (0.9 + 0.80 * dd + 1.00 * dd*dd);
    return t1 * scale_factor + vec2(0.5,0.5);
  }
  
  void main(){
    vec2 tc = calcTexCoord(vTexCoord - (vTexCoord.s < 0.5 ? vec2(-dx,0.0): vec2(0.5 + dx,0.0)));
    if (tc.x < 0.0 || tc.y < 0.0 || tc.x > 1.0 || tc.y > 1.0) {
      gl_FragColor = vec4(0.0,0.0,0.1,1.0);
    } else if (vTexCoord.s < 0.5) {
      gl_FragColor = texture2D(sTex,tc - offset);
    } else {
      gl_FragColor = texture2D(sSphere,tc + offset);
    }
  }
