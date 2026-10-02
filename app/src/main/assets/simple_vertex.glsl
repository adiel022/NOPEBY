// simple_vertex.glsl
attribute vec3 aPosition;
attribute vec3 aColor;
attribute vec2 aTexCoord;

uniform mat4 uMVP;

varying vec3 vColor;
varying vec2 vTexCoord;

void main() {
    gl_Position = uMVP * vec4(aPosition, 1.0);
    vColor = aColor;
    vTexCoord = aTexCoord;
}
