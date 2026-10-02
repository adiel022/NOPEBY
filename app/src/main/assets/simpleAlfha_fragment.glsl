precision mediump float;

varying vec3 vColor;
uniform float uAlpha;

void main() {
    gl_FragColor = vec4(vColor, uAlpha);
}
