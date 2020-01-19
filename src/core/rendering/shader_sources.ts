export const POS_VERTEX_SHADER_SOURCE = `#version 300 es

precision mediump float;

uniform mat4 transform;

in vec4 pos;

void main() {
    gl_Position = transform * pos;
}
`;

export const POS_FRAG_SHADER_SOURCE = `#version 300 es

precision mediump float;

out vec4 frag_color;

void main() {
    frag_color = vec4(0, 1, 1, 1);
}
`;

export const POS_TEX_VERTEX_SHADER_SOURCE = `#version 300 es

precision mediump float;

uniform mat4 transform;

in vec4 pos;
in vec2 tex;

out vec2 f_tex;

void main() {
    gl_Position = transform * pos;
    f_tex = tex;
}
`;

export const POS_TEX_FRAG_SHADER_SOURCE = `#version 300 es

precision mediump float;

uniform sampler2D tex_sampler;

in vec2 f_tex;

out vec4 frag_color;

void main() {
    frag_color = texture(tex_sampler, f_tex);
}
`;
