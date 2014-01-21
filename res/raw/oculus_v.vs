precision highp float;
attribute vec4 aPosition;
varying vec2 vTexCoord;
void main() {
  gl_Position = vec4(aPosition.x, -aPosition.y, 1, 1);
  vTexCoord = vec2(aPosition.zw);
}
