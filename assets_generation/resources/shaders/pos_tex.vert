#version 450

layout(set = 0, binding = 0) uniform Uniforms {
    mat4 mvp_mat;
} uniforms;

layout(location = 0) in vec3 pos;
layout(location = 2) in vec2 tex_coords;

layout(location = 0) out vec2 frag_tex_coords;

void main() {
    gl_Position = uniforms.mvp_mat * vec4(pos, 1.0);
    frag_tex_coords = tex_coords;
}
