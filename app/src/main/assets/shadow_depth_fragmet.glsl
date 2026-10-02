// shadow_depth_fragmet
// precision mediump float;
precision highp float;
varying float vDepth;

// Codifica la profundidad en 4 canales RGBA (24 bits efectivos)
vec4 encodeDepth(float depth) {
    // 255/256 = 0.99609375, evita el redondeo hacia 1.0
    const vec4 bitShift = vec4(1.0,
                               255.0,
                               255.0 * 255.0,
                               255.0 * 255.0 * 255.0);
    const vec4 bitMask = vec4(1.0 / 255.0,
                              1.0 / 255.0,
                              1.0 / 255.0,
                              0.0);

    vec4 encoded = fract(depth * bitShift);
    encoded -= encoded.yzww * bitMask;
    return encoded;
}

void main() {
    gl_FragColor = encodeDepth(vDepth);
}
