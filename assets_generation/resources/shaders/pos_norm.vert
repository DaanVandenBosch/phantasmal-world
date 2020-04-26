#version 450

layout(set = 0, binding = 0) uniform Uniforms {
    mat4 mvp_mat;
    mat3 normal_mat;
} uniforms;

layout(location = 0) in vec3 pos;
layout(location = 1) in vec3 normal;

layout(location = 0) out vec3 frag_normal;

void main() {
    gl_Position = uniforms.mvp_mat * vec4(pos, 1.0);
    frag_normal = normalize(uniforms.normal_mat * normal);
}
