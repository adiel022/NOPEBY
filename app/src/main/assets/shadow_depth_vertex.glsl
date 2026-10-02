// shadow_depth_vertex
attribute vec3 aPosition;

uniform mat4 uLightSpaceMatrix;
uniform mat4 uModel;

varying float vDepth;

void main() {
    // Transformamos al espacio de la luz
    vec4 lightPos = uLightSpaceMatrix * uModel * vec4(aPosition, 1.0);

    // Convertimos de clip space [-1,1] a [0,1]
    // vDepth = (lightPos.z / lightPos.w) * 0.5 + 0.5;
    float depth = lightPos.z / lightPos.w;
    depth = depth * 0.5 + 0.5;
    vDepth = clamp(depth, 0.0, 1.0);

    gl_Position = lightPos;
}
