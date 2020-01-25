#version 300 es

precision mediump float;

uniform mat4 transform;

in vec4 pos;
in vec2 tex;

out vec2 f_tex;

void main() {
    gl_Position = transform * pos;
    f_tex = tex;
}
