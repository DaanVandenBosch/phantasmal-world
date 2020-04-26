#version 450

precision mediump float;
precision mediump sampler;

layout(set = 0, binding = 1) uniform sampler tex_sampler;
layout(set = 0, binding = 2) uniform texture2D tex;

layout(location = 0) in vec2 frag_tex_coords;

layout(location = 0) out vec4 out_color;

void main() {
    out_color = texture(sampler2D(tex, tex_sampler), frag_tex_coords);
}
