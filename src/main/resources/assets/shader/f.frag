#version 330

in vec2 texCoord;
out vec4 fragColor;

uniform vec4 color;
uniform sampler2D SamTex;
uniform float boldAmount = 0.0;
uniform vec2 textureSize;

void main(void) {
    // 计算一个像素的大小
    vec2 pixelSize = 1.0 / textureSize;

    // 第一步：先进行颜色混合（模拟固定管线行为）
    vec4 texColor = texture(SamTex, texCoord);

    // 固定管线风格的MODULATE混合模式
    // 这是固定管线默认的纹理环境模式：texColor * color
    vec4 blendedColor = texColor * color;

    // 第二步：根据字符大小自适应调整采样范围
    float adaptiveScale = max(1.0, 10.0 / min(textureSize.x, textureSize.y));
    float adjustedBoldAmount = boldAmount * adaptiveScale;

    // 第三步：对混合后的颜色进行加粗处理
    // 使用距离场方法，但基于混合后的颜色值
    vec4 maxColor = blendedColor;

    // 动态采样点数量
    int sampleCount = int(clamp(8.0 * adaptiveScale, 8.0, 16.0));

    // 使用循环进行多方向采样
    for (int i = 0; i < 16; i++) {
        if (i >= sampleCount) break;

        // 计算采样角度
        float angle = 6.28318530718 * float(i) / float(sampleCount);
        vec2 offset = vec2(cos(angle), sin(angle)) * adjustedBoldAmount;

        // 采样并混合颜色，然后取最大值
        vec4 sampleColor = texture(SamTex, texCoord + offset * pixelSize) * color;
        maxColor = max(maxColor, sampleColor);
    }

    // 额外确保水平和垂直方向的采样
    vec4 hSample = texture(SamTex, texCoord + vec2(adjustedBoldAmount * pixelSize.x, 0)) * color;
    vec4 vSample = texture(SamTex, texCoord + vec2(0, adjustedBoldAmount * pixelSize.y)) * color;
    maxColor = max(maxColor, hSample);
    maxColor = max(maxColor, vSample);

    // 第四步：应用边缘平滑
    // 使用alpha通道作为边缘检测的依据
    float alpha = maxColor.a;
    float smoothRange = 0.05 + 0.02 * max(adjustedBoldAmount, 0.5);

    // 使用smoothstep创建平滑的边缘过渡
    alpha = smoothstep(0.5 - smoothRange, 0.5 + smoothRange, alpha);

    // 保持RGB颜色，只调整alpha通道
    fragColor = vec4(maxColor.rgb, alpha);
}