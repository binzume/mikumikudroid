#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTexCoord;
uniform samplerExternalOES sTex; // samplerExternalOES or sampler2D
void main() {
  vec4 color = texture2D(sTex,  vTexCoord);
  color.a = 0.5;
  gl_FragColor = color;
}
