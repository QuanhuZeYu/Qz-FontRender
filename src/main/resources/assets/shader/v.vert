#version 330

layout(location=0) in vec3 pos;
layout(location=1) in vec2 tex;
layout(location=2) in vec4 color;

uniform mat4 modelview;
uniform mat4 projection;

out vec2 texCoord;
out vec4 Color;

void main(void) {
    gl_Position = projection * modelview * vec4(pos, 1);
    texCoord = tex;
    Color = color;
}