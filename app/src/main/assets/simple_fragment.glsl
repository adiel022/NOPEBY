// simple_fragment.glsl
precision mediump float;

uniform sampler2D uTexture;
uniform int uHasTexture;

varying vec3 vColor;
varying vec2 vTexCoord;

void main() {
    if (uHasTexture == 1) {
        gl_FragColor = texture2D(uTexture, vTexCoord);
    } else {
        gl_FragColor = vec4(vColor, 1.0);
    }
}
