#version 120

// GLSL 120 兼容模式下，可以直接使用内置属性
// 但由于您的 Java 代码 (RenderTool) 仍然使用了 glVertexAttribPointer(0), (1), (2)
// 为了兼容 RenderTool，我们继续使用 attribute 声明并手动关联。
// 即使使用内置变量，也需要 RenderTool 中的 GL11.gl*Pointer() 才能生效，
// 但我们已将其替换为 GL20.glVertexAttribPointer()，所以这里必须使用 attribute。

attribute vec3 pos;     // 对应 RenderTool 中的 Location 0
attribute vec2 tex;     // 对应 RenderTool 中的 Location 1
attribute vec4 color;   // 对应 RenderTool 中的 Location 2

// 矩阵可以直接使用内置的，如果 RenderTool 使用 GL11.glMatrixMode() 设置矩阵的话。
// 但因为 RenderTool 想要使用 Uniform 上传，我们保留 Uniform 声明。
uniform mat4 modelview;
uniform mat4 projection;

varying vec2 texCoord; // GLSL 120 使用 varying
varying vec4 Color;    // GLSL 120 使用 varying

void main(void) {
    // 逻辑保持不变
    gl_Position = projection * modelview * vec4(pos, 1.0);
    texCoord = tex;
    Color = color;
}