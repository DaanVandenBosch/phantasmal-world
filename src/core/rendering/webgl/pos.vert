#version 300 es

precision mediump float;

uniform mat4 transform;

in vec4 pos;

void main() {
    gl_Position = transform * pos;
}
