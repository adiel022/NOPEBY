// anime_depth_vertex.glsl
attribute vec3 aPosition;

uniform mat4 uLightSpaceMatrix;
uniform mat4 uModel;

varying float vDepth;

void main() {
    vec4 lightPos = uLightSpaceMatrix * uModel * vec4(aPosition, 1.0);
    vDepth = (lightPos.z / lightPos.w) * 0.5 + 0.5;
    gl_Position = lightPos;
}
