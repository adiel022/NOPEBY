// anime_light_fragment.glsl
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

// Nuevos uniformes para sincronizar con RealShadow
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

// ------------------------------
// Decodifica profundidad RGBA
// ------------------------------
float decodeDepth(vec4 rgbaDepth) {
    const vec4 bitShift = vec4(1.0,
                               1.0 / 255.0,
                               1.0 / (255.0 * 255.0),
                               0.0);
    return dot(rgbaDepth, bitShift);
}

// ------------------------------
// Sombras DURAS estilo anime/Genshin
// ------------------------------
float ShadowCalculation(vec4 lightSpacePos, vec3 normal, vec3 lightDir) {
    vec3 projCoords = lightSpacePos.xyz / lightSpacePos.w;
    projCoords = projCoords * 0.5 + 0.5;

    if (projCoords.z > 1.0 ||
        projCoords.x < 0.0 || projCoords.x > 1.0 ||
        projCoords.y < 0.0 || projCoords.y > 1.0)
        return 0.0;

    // Bias dinámico para evitar shadow acne (manchas oscuras)
    float bias = max(0.01 * (1.0 - dot(normal, lightDir)), 0.001);

    float closestDepth = decodeDepth(texture2D(uShadowMap, projCoords.xy));
    float currentDepth = projCoords.z - bias;

    return (currentDepth > closestDepth) ? 1.0 : 0.0;
}

// ------------------------------
// PCF Suavizado con Poisson Disk para sombras proyectadas Anime
// ------------------------------
float ShadowCalculationSoft(vec4 lightSpacePos, vec3 normal, vec3 lightDir) {
    vec3 projCoords = lightSpacePos.xyz / lightSpacePos.w;
    projCoords = projCoords * 0.5 + 0.5;

    if (projCoords.z > 1.0 || projCoords.x < 0.0 || projCoords.x > 1.0 || projCoords.y < 0.0 || projCoords.y > 1.0)
        return 0.0;

    float bias = max(0.006 * (1.0 - dot(normal, lightDir)), 0.001);
    float currentDepth = projCoords.z - bias;

    float texelSize = 1.0 / 1024.0;
    float filterRadius = texelSize * 1.75;

    // Ángulo aleatorio continuo basado en la posición del fragmento para rotar la cuadrícula
    float angle = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) * 6.2831853;
    float cosA = cos(angle);
    float sinA = sin(angle);
    mat2 rotMat = mat2(cosA, -sinA, sinA, cosA);

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
        float pcfDepth = decodeDepth(texture2D(uShadowMap, projCoords.xy + offset));
        shadow += (currentDepth > pcfDepth) ? 1.0 : 0.0;
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

    float diff = max(dot(norm, lightDir), 0.0);

    // 1. Ramp de 3 tonos suave (Estilo Genshin/Blue Archive)
    // Nivel Sombra -> Medio -> Luz
    float ramp = smoothstep(0.25, 0.4, diff) * 0.5 + smoothstep(0.55, 0.75, diff) * 0.5;

    // 2. Sombra proyectada suave (PCF)
    float shadow = ShadowCalculationSoft(vLightSpacePos, norm, lightDir);

    // 3. Rim Light (Efecto de brillo en los bordes, vital para Blue Archive)
    float rim = 1.0 - max(dot(viewDir, norm), 0.0);
    rim = pow(rim, 5.0); // Borde fino y brillante
    vec3 rimEffect = uLightColor * rim * 0.4 * uLightIntensity;

    // 4. Colores de iluminación
    vec3 lightTone = uLightColor * uLightIntensity;
    vec3 shadeTone = uShadowColor;

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

    float effectiveShadowStrength = uShadowStrength * (1.0 - pointLightFactor);

    // El mapa de sombras (shadow) resta iluminación de forma suave
    float lightFactor = ramp * (1.0 - shadow * effectiveShadowStrength);

    // Mezcla final de colores
    vec3 lighting = mix(shadeTone, lightTone, lightFactor);

    vec3 baseColor = vColor;
    if (uHasTexture == 1) {
        baseColor = texture2D(uTexture, vTexCoord).rgb;
    }

    // ✨ Especular Blinn-Phong Anime
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(norm, halfwayDir), 0.0), max(uShininess, 1.0));
    float specStep = smoothstep(0.4, 0.6, spec);
    vec3 specEffect = uLightColor * specStep * uSpecularStrength * (1.0 - shadow * effectiveShadowStrength);

    // Aplicamos el color base del objeto
    vec3 finalColor = baseColor * (lighting + pointLightColor) + rimEffect + specEffect;

    // Corrección gamma suave para tonos pastel vibrantes
    finalColor = pow(finalColor, vec3(1.0 / 1.2));

    gl_FragColor = vec4(finalColor, uAlpha);
}
