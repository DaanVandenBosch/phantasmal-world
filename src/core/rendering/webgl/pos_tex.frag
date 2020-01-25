#version 300 es

precision mediump float;

uniform sampler2D tex_sampler;

in vec2 f_tex;

out vec4 frag_color;

void main() {
    frag_color = texture(tex_sampler, f_tex);
}
