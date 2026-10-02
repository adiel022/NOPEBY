// shadow_light_fragment.glsl
precision highp float;

varying vec3 vNormal;
varying vec3 vFragPos;
varying vec4 vLightSpacePos;
varying vec3 vColor;
varying vec2 vTexCoord;

uniform sampler2D uShadowMap;
uniform sampler2D uTexture;
uniform int uHasTexture;
uniform sampler2D uNormalMap;
uniform int uHasNormalMap;
uniform vec3 uLightPos;
uniform vec3 uViewPos;

uniform float uLightIntensity;
uniform float uShadowStrength;
uniform vec3 uLightColor;
uniform vec3 uShadowColor;
uniform float uSpecularStrength;
uniform float uShininess;
uniform float uAlpha;

uniform vec3 uPointLightPos;
uniform float uPointLightRadius;
uniform vec3 uPointLightColor;

// Decodifica la profundidad RGBA de 24 bits
float decodeDepth(vec4 rgbaDepth) {
    const vec4 bitShift = vec4(1.0,
                               1.0 / 255.0,
                               1.0 / (255.0 * 255.0),
                               0.0); // alfa ignorada
    return dot(rgbaDepth, bitShift);
}

   // 💡 PCF SUAVIZADO CON POISSON DISK ROTATIVO (Elimina artefactos de cuadrícula/escalera)
    float ShadowCalculation(vec4 lightSpacePos, vec3 normal, vec3 lightDir) {
    vec3 projCoords = lightSpacePos.xyz / lightSpacePos.w;
    projCoords = projCoords * 0.5 + 0.5;

    // Fuera del rango del shadow map
    if (projCoords.z > 1.0 || projCoords.x < 0.0 || projCoords.x > 1.0 ||
        projCoords.y < 0.0 || projCoords.y > 1.0) {
        return 0.0;
    }

    float bias = max(0.006 * (1.0 - dot(normal, lightDir)), 0.001);
    float currentDepth = projCoords.z - bias;
    float texelSize = 1.0 / 1024.0;
    float filterRadius = texelSize * 1.75;

    // Ángulo aleatorio continuo basado en la posición del fragmento para rotar la cuadrícula
    float angle = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) * 6.2831853;
    float cosA = cos(angle);
    float sinA = sin(angle);
    mat2 rotMat = mat2(cosA, -sinA, sinA, cosA);

    // Muestras Poisson Disk (12 muestras distribuidas en espiral)
    vec2 disk[12];
    disk[0]  = vec2(-0.326212, -0.405810);
    disk[1]  = vec2(-0.840144, -0.073580);
    disk[2]  = vec2(-0.695914,  0.457137);
    disk[3]  = vec2(-0.203345,  0.620716);
    disk[4]  = vec2( 0.962340, -0.194983);
    disk[5]  = vec2( 0.473434, -0.480026);
    disk[6]  = vec2( 0.519456,  0.767022);
    disk[7]  = vec2( 0.185461, -0.893124);
    disk[8]  = vec2( 0.507431,  0.064425);
    disk[9]  = vec2( 0.896420,  0.412458);
    disk[10] = vec2(-0.321940, -0.932615);
    disk[11] = vec2(-0.791559, -0.597710);

    float shadow = 0.0;
    for (int i = 0; i < 12; i++) {
        vec2 offset = rotMat * disk[i] * filterRadius;
        float closestDepth = decodeDepth(texture2D(uShadowMap, projCoords.xy + offset));
        shadow += (currentDepth > closestDepth ? 1.0 : 0.0);
    }

    return shadow / 12.0;
}

void main() {
    vec3 norm = normalize(vNormal);
    if (uHasNormalMap == 1) {
        vec3 nMap = texture2D(uNormalMap, vTexCoord).rgb * 2.0 - 1.0;
        norm = normalize(norm + nMap * 0.7);
    }
    vec3 lightDir = normalize(uLightPos - vFragPos);
    vec3 viewDir = normalize(uViewPos - vFragPos);

    // ☀️ Configuración de Luces desde Uniforms
    float lightIntensity = uLightIntensity;
    float ambientIntensity = 0.45;
    float shadowStrength = uShadowStrength;

    // 💡 Luz de Punto Emisora Retro (PS1) y Cancelación Gradual de Sombra
    float pointLightFactor = 0.0;
    vec3 pointLightColor = vec3(0.0);
    if (uPointLightRadius > 0.01) {
        float distToPoint = length(vFragPos - uPointLightPos);
        if (distToPoint < uPointLightRadius) {
            float normDist = distToPoint / uPointLightRadius;
            float falloff = clamp(1.0 - normDist, 0.0, 1.0);
            pointLightFactor = falloff * falloff * (3.0 - 2.0 * falloff);
            pointLightColor = uPointLightColor * pointLightFactor;
        }
    }

    float effectiveShadowStrength = shadowStrength * (1.0 - pointLightFactor);

    // 🌓 Difusa con Sombras
    float shadow = ShadowCalculation(vLightSpacePos, norm, lightDir);
    float diff = max(dot(norm, lightDir), 0.0);

    vec3 baseColor = vColor;
    if (uHasTexture == 1) {
        baseColor = texture2D(uTexture, vTexCoord).rgb;
    }

    // Aplicamos el color de la luz a la parte difusa
    vec3 diffuseColor = baseColor * uLightColor * diff * lightIntensity * (1.0 - shadow * effectiveShadowStrength);
    vec3 pointLightDiffuse = baseColor * pointLightColor;

    // 🌌 Ambiental
    // Aplicamos el color de la sombra a la parte ambiental para dar atmósfera
    vec3 ambientColor = baseColor * uShadowColor * ambientIntensity;

    // ✨ Especular (Brillo del Sol/Luz) - Blinn-Phong con uLightColor y Especularidad por Mesh
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(norm, halfwayDir), 0.0), max(uShininess, 1.0));
    vec3 specular = uLightColor * spec * uSpecularStrength * (1.0 - shadow * effectiveShadowStrength);

    // 🌟 Rim Light / Fresnel (Resplandor en los bordes)
    float rim = 1.0 - max(dot(viewDir, norm), 0.0);
    rim = pow(rim, 3.0);
    vec3 rimLight = vec3(1.0) * rim * 0.25;

    // 🌍 Fake Reflection (MatCap style / Environment)
    // Simula el reflejo del "cielo" y el "suelo" para dar profundidad
    vec3 reflectDir = reflect(-viewDir, norm);
    float envReflection = smoothstep(-0.2, 1.0, reflectDir.y);
    vec3 envColor = vec3(0.1, 0.15, 0.2) * envReflection; // Reflejo azulado superior

    // 🎨 Combinación Final
    vec3 finalColor = ambientColor + diffuseColor + pointLightDiffuse + specular + rimLight + envColor;

    // Gamma correction simple (opcional pero ayuda a que no se vea tan saturado/oscuro)
    finalColor = pow(finalColor, vec3(1.0 / 1.1));

    gl_FragColor = vec4(finalColor, uAlpha);
}
