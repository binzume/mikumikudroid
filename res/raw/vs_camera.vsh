precision highp float;
attribute vec4 aPosition;
varying vec2 vTexCoord;
void main() {
  gl_Position = vec4(aPosition.xy, 0.2, 1);
  vTexCoord = vec2(aPosition.zw);
}
