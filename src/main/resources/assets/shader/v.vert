#version 330

layout(location=0) in vec3 pos;
layout(location=1) in vec2 tex;

uniform mat4 modelview;
uniform mat4 projection;

out vec2 texCoord;

void main(void) {
    gl_Position = projection * modelview * vec4(pos, 1);
    texCoord = tex;
}