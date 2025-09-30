#version 330

in vec2 texCoord;
in vec4 Color;
out vec4 fragColor;

uniform sampler2D SamTex;
uniform vec4 uvBounds = vec4(0,0,1,1);
uniform vec2 textureSize = vec2(2048);
uniform vec2 smoothRange = vec2(0,1);
uniform float sigma = 3.14;
uniform float blurRadius = 1;
uniform int sampleRadius = 1;

const float PI = 3.14159265359;

// 优化点：预计算常量
// 避免在每次循环迭代中重复计算这些值
const float INV_SQRT_2PI = 0.3989422804014327; // 1 / sqrt(2 * PI)
float sigmaSquared;
float normalizationFactor; // 用于代替 1.0 / (2.0 * PI * sigmaSquared)

// 安全采样函数不变，但仍是潜在的分支瓶颈
vec4 safeSampler(sampler2D tex, vec2 uv) {
    // 这是一个分支 (if) 操作，可能影响性能。
    // 如果可以，尽量用 GL_CLAMP_TO_EDGE 纹理环绕模式来处理边界，然后移除这个 if。
    if (uv.x < uvBounds.x || uv.x > uvBounds.z || uv.y < uvBounds.y || uv.y > uvBounds.w) {
        return vec4(0);
    }
    return texture(tex, uv);
}

// 优化点：简化权重计算并将其内联
float gaussianWeight2D_Optimized(vec2 offset) {
    // offset 传入的是相对于中心的“纹素”偏移量
    float exponent = -(offset.x * offset.x + offset.y * offset.y) / (2.0 * sigmaSquared);
    // 归一化因子在 main 函数中设置，这里只需计算 exp 部分
    return exp(exponent);
}

vec4 gaussianBlur(sampler2D tex, vec2 uv, vec2 texelSize) {
    float totalWeight = 0.0;
    vec4 accumulatedColor = vec4(0);

    // 缓存一些循环中不变的值
    float stepScale = blurRadius;
    vec2 baseStep = texelSize * stepScale; // 每次循环的 UV 步进量

    for (int i = -sampleRadius; i <= sampleRadius; ++i) {
        for (int j = -sampleRadius; j <= sampleRadius; ++j) {

            // 优化点：简化偏移量计算
            // offset 直接用于 UV 采样
            vec2 offsetUV = vec2(float(i), float(j)) * baseStep;
            vec2 sampleUV = uv + offsetUV;

            // offsetNormed 用于计算高斯权重，表示相对权重中心的距离
            vec2 offsetNormed = vec2(float(i), float(j)) * stepScale;

            // 优化点：使用简化的权重函数，只计算指数部分
            // 完整的权重 = normalizationFactor * exp(...)
            // 由于所有采样点的权重都乘以同一个 normalizationFactor，
            // 可以在循环结束后统一进行归一化，从而省略它。
            float weight = gaussianWeight2D_Optimized(offsetNormed);

            vec4 sampleColor = safeSampler(tex, sampleUV);

            accumulatedColor += sampleColor * weight;
            totalWeight += weight;
        }
    }

    // 完整的归一化在循环外完成
    if (totalWeight > 0.0) {
        accumulatedColor /= totalWeight;
    }

    return accumulatedColor;
}

void main() {
    // 优化点：在 main 函数开始处计算 uniform 相关的常量
    sigmaSquared = sigma * sigma;
    // normalizationFactor = 1.0 / (2.0 * PI * sigmaSquared); // 优化后不再需要

    vec2 texelSize = 1 / textureSize;
    vec4 sampleColor = gaussianBlur(SamTex, texCoord, texelSize);

    // smoothstep 用于平滑 Alpha 通道
    sampleColor.a = smoothstep(smoothRange.x, smoothRange.y, sampleColor.a);

    // 假设 Color.rgb 是额外的颜色/亮度调制
    fragColor = vec4(sampleColor.rgb * Color.rgb, sampleColor.a);
}