#version 120

// GLSL 120 使用 varying 接收
varying vec2 texCoord;
varying vec4 Color;

// 输出颜色通过内置变量 gl_FragColor 实现，不需要 out 声明

uniform sampler2D SamTex;
uniform vec4 uvBounds = vec4(0.0, 0.0, 1.0, 1.0);
uniform vec2 textureSize = vec2(2048.0, 2048.0);
uniform vec2 smoothRange = vec2(0.0, 1.0);
uniform float sigma = 3.14;
uniform float blurRadius = 1.0;
uniform int sampleRadius = 1;

const float PI = 3.14159265359;

const float INV_SQRT_2PI = 0.3989422804014327;
float sigmaSquared;
// float normalizationFactor; // 未使用，保持不变

vec4 safeSampler(sampler2D tex, vec2 uv) {
    if (uv.x < uvBounds.x || uv.x > uvBounds.z || uv.y < uvBounds.y || uv.y > uvBounds.w) {
        return vec4(0.0);
    }
    // 纹理采样函数必须使用 texture2D
    return texture2D(tex, uv);
}


float gaussianWeight2D_Optimized(vec2 offset) {

    float exponent = -(offset.x * offset.x + offset.y * offset.y) / (2.0 * sigmaSquared);

    return exp(exponent);
}

vec4 gaussianBlur(sampler2D tex, vec2 uv, vec2 texelSize) {
    float totalWeight = 0.0;
    vec4 accumulatedColor = vec4(0.0);

    float stepScale = blurRadius;
    vec2 baseStep = texelSize * stepScale;

    // GLSL 120/130 对循环变量的限制较多，但此处的循环结构是安全的。
    // 注意：循环中的 float(i) 转换是必要的，因为循环变量 i 是 int。
    for (int i = -sampleRadius; i <= sampleRadius; ++i) {
        for (int j = -sampleRadius; j <= sampleRadius; ++j) {

            vec2 offsetUV = vec2(float(i), float(j)) * baseStep;
            vec2 sampleUV = uv + offsetUV;

            vec2 offsetNormed = vec2(float(i), float(j)) * stepScale;

            float weight = gaussianWeight2D_Optimized(offsetNormed);

            vec4 sampleColor = safeSampler(tex, sampleUV);

            accumulatedColor += sampleColor * weight;
            totalWeight += weight;
        }
    }

    if (totalWeight > 0.0) {
        accumulatedColor /= totalWeight;
    }

    return accumulatedColor;
}

void main() {
    sigmaSquared = sigma * sigma;

    vec2 texelSize = 1.0 / textureSize; // 将整数 1 更改为 1.0，确保浮点除法
    vec4 sampleColor = gaussianBlur(SamTex, texCoord, texelSize);

    sampleColor.a = smoothstep(smoothRange.x, smoothRange.y, sampleColor.a);

    // 将结果写入内置变量
    gl_FragColor = vec4(sampleColor.rgb * Color.rgb, sampleColor.a);
}