// shadow_light_vertex
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

    // transformar normales correctamente usando normal matrix
    vNormal = normalize((uNormalMatrix * vec4(aNormal, 0.0)).xyz);

    // Posición desde la perspectiva de la luz para el cálculo de sombras
    vLightSpacePos = uLightSpaceMatrix * worldPos;

    vColor = aColor;
    vTexCoord = aTexCoord;

    // Posición final del vértice proyectada en pantalla
    gl_Position = uMVP * vec4(aPosition, 1.0);
}
