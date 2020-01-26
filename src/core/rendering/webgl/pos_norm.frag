#version 300 es

precision mediump float;

const vec3 light_pos = normalize(vec3(-1, 1, 1));
const vec4 sky_color = vec4(1, 1, 1, 1);
const vec4 ground_color = vec4(0.1, 0.1, 0.1, 1);

in vec3 frag_normal;

out vec4 frag_color;

void main() {
    float cos0 = dot(frag_normal, light_pos);
    float a = 0.5 + 0.5 * cos0;
    float a_back = 1.0 - a;

    if (gl_FrontFacing) {
        frag_color = mix(ground_color, sky_color, a);
    } else {
        frag_color = mix(ground_color, sky_color, a_back);
    }
}
