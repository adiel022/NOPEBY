// anime_light_vertex.glsl (versión corregida)
attribute vec3 aPosition;
attribute vec3 aNormal;
attribute vec3 aColor;
attribute vec2 aTexCoord;

uniform mat4 uMVP;
uniform mat4 uModel;
uniform mat4 uNormalMatrix;
uniform mat4 uLightSpaceMatrix;

varying vec3 vNormal;
varying vec3 vFragPos;
varying vec4 vLightSpacePos;
varying vec3 vColor;
varying vec2 vTexCoord;

void main() {
    vec4 worldPos = uModel * vec4(aPosition, 1.0);
    vFragPos = worldPos.xyz;

    // normales correctamente transformadas
    vNormal = normalize((uNormalMatrix * vec4(aNormal, 0.0)).xyz);

    vLightSpacePos = uLightSpaceMatrix * worldPos;
    vColor = aColor;
    vTexCoord = aTexCoord;

    // gl_Position = uMVP * worldPos;
    gl_Position = uMVP * vec4(aPosition, 1.0);
}
