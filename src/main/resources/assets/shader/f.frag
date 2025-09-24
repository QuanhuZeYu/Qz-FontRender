#version 330

in vec2 texCoord;
out vec4 fragColor;

uniform vec4 color;
uniform sampler2D SamTex;
uniform float edgeWidth = 0.2;
uniform vec2 texturesize;
uniform vec4 uvBounds;
uniform float gain = 1.5;
uniform int sampleRadius = 1; // 采样半径

// 安全采样函数
vec4 safeSample(vec2 offset) {
    vec2 newCoord = vec2(texCoord + offset);
    if (newCoord.x <= uvBounds.x || newCoord.x >= uvBounds.z ||
    newCoord.y <= uvBounds.y || newCoord.y >= uvBounds.w) {
        return vec4(0);
    }
    return texture(SamTex, newCoord);
}

// 高斯模糊采样函数
vec4 gaussianBlurSample(int radius) {
    vec2 texelSize = vec2(1.0) / texturesize;
    vec4 result = vec4(0.0);
    float totalWeight = 0.0;

    // 限制半径范围
    radius = clamp(radius, 1, 10);

    // 计算高斯核
    int kernelSize = radius * 2 + 1;
    float sigma = float(radius) / 2.0;
    float twoSigma2 = 2.0 * sigma * sigma;

    // 遍历采样点
    for (int y = -radius; y <= radius; y++) {
        for (int x = -radius; x <= radius; x++) {
            // 计算高斯权重
            float weight = exp(-(float(x*x + y*y)) / twoSigma2);
            totalWeight += weight;

            // 采样并累加
            vec2 offset = vec2(x, y) * texelSize;
            result += safeSample(offset) * weight;
        }
    }

    // 归一化
    if (totalWeight > 0.0) {
        result /= totalWeight;
    }

    return result;
}

void main() {
    // 获取中心采样点颜色
    vec4 centerSample = safeSample(vec2(0));

    // 检查中心颜色是否大于 vec3(0.2)
    if (centerSample.r > 0.2 || centerSample.g > 0.2 || centerSample.b > 0.2) {
        // 不应用模糊，使用中心采样点的颜色并应用增益
        vec3 enhancedColor = centerSample.rgb * gain;
        fragColor = vec4(color.rgb * clamp(enhancedColor, 0.0, 1.0), 1.0);
        return;
    }

    // 使用高斯模糊采样
    vec4 blurredSample = gaussianBlurSample(sampleRadius);

    // 应用增益并限制在有效范围内
    vec3 texColor = clamp(blurredSample.rgb * gain, 0.0, 1.0);

    // 应用文本颜色
    vec3 finalColor = texColor * color.rgb;

    // 使用平滑步进函数实现抗锯齿
    float smoothedAlpha = smoothstep(0.5 - edgeWidth, 0.5 + edgeWidth, blurredSample.a);

    // 输出最终颜色
    fragColor = vec4(finalColor, smoothedAlpha);
}