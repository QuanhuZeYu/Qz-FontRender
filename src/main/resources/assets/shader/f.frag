#version 330

in vec2 texCoord;

out vec4 fragColor;

uniform vec4 color;
uniform sampler2D SamTex;
uniform float boldAmount = 0.0;
uniform vec2 textureSize; // 纹理尺寸 (width, height)

void main(void) {
    // 计算一个像素的大小
    vec2 pixelSize = 1.0 / textureSize;

    // 根据字符大小自适应调整采样范围
    float adaptiveScale = max(1.0, 10.0 / min(textureSize.x, textureSize.y));
    float adjustedBoldAmount = boldAmount * adaptiveScale;

    // 中心采样
    vec4 distance = texture(SamTex, texCoord);

    // 动态采样点数量 - 小字符使用更多采样点
    int sampleCount = int(clamp(8.0 * adaptiveScale, 8.0, 16.0));
    vec4 maxDistance = distance;

    // 使用循环进行多方向采样，确保覆盖所有可能的笔画
    for (int i = 0; i < 16; i++) {
        if (i >= sampleCount) break;

        // 计算采样角度 (均匀分布在圆上)
        float angle = 6.28318530718 * float(i) / float(sampleCount);
        vec2 offset = vec2(cos(angle), sin(angle)) * adjustedBoldAmount;

        // 采样并取最大值
        vec4 sampleDist = texture(SamTex, texCoord + offset * pixelSize);
        maxDistance = max(maxDistance, sampleDist);
    }

    // 额外确保水平和垂直方向的采样（重要笔画方向）
    vec4 hSample = texture(SamTex, texCoord + vec2(adjustedBoldAmount * pixelSize.x, 0));
    vec4 vSample = texture(SamTex, texCoord + vec2(0, adjustedBoldAmount * pixelSize.y));
    maxDistance = max(maxDistance, hSample);
    maxDistance = max(maxDistance, vSample);

    // 根据加粗量调整平滑范围
    float smoothRange = 0.05 + 0.02 * max(adjustedBoldAmount, 0.5);
    vec4 texcolor = smoothstep(0.5 - smoothRange, 0.5 + smoothRange, maxDistance);
    texcolor = clamp(texcolor * color, 0.0, 1.0);

    texcolor.a = smoothstep(0.0, 0.5, texcolor.a);

    fragColor = texcolor;
}