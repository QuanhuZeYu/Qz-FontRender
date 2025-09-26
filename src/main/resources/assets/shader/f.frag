#version 330

in vec2 texCoord;
out vec4 fragColor;

uniform vec4 color;
uniform sampler2D SamTex;
uniform vec4 uvBounds = vec4(0,0,1,1);
uniform float blackThreshold = 0.2;
uniform float blurRadius = 4.0;
uniform vec2 smoothRange = vec2(0.1, 0.5);
uniform int smoothSwitcher = 0;
uniform int sampleR = 1;

bool isBlack(vec3 inColor) {
    // TODO 首先判断COLOR自身是否小于 blackThreshold 阈值 不小于再判断输入颜色是否小于 blackThreshold 阈值 如果小于则判定为黑色
    if (color.r > blackThreshold && color.g > blackThreshold && color.b > blackThreshold) {
        if (inColor.r < blackThreshold && inColor.g < blackThreshold && inColor.b < blackThreshold) {
            return true;
        }
    }
    return false;
}

float gaussianWeight2D(vec2 offset, float sigma) {
    float x = offset.x;
    float y = offset.y;
    return exp(-(x * x + y * y) / (2.0 * sigma * sigma)) / (2.0 * 3.14159265359 * sigma * sigma);
}

vec4 safeSampler(sampler2D tex, vec2 uv) {
    if (uv.x < uvBounds.x || uv.x > uvBounds.z || uv.y < uvBounds.y || uv.y > uvBounds.w) {
        return vec4(0);
    }
    return texture(tex, uv);
}

// 修改后的高斯模糊函数
vec4 gaussianBlur(sampler2D tex, vec2 uv, vec2 texelSize, float blurRadius) {
    int sampleCount = sampleR;
    float sigma = blurRadius * 0.333;
    float totalWeight = 0.0;
    vec4 accumulatedColor = vec4(0);

    for (int i = -sampleCount / 2; i <= sampleCount / 2; ++i) {
        for (int j = -sampleCount / 2; j <= sampleCount / 2; ++j) {
            vec2 offset = vec2(float(i), float(j)) * texelSize * blurRadius / float(sampleCount / 2);
            vec2 sampleUV = uv + offset;

            float weight = gaussianWeight2D(vec2(float(i), float(j)) / float(sampleCount / 2) * blurRadius, sigma);

            vec4 sampleColor = safeSampler(tex, sampleUV);

            // 不再单独处理黑色像素，而是统一使用权重
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
    vec2 texelSize = vec2(dFdx(texCoord.x), dFdy(texCoord.y));

    // 传递颜色乘数到模糊函数
    vec4 gaussianSample = clamp(gaussianBlur(SamTex, texCoord, texelSize, blurRadius), vec4(0), vec4(1));

    // 如果alpha大于步进阈值 且 采样颜色被判定为错误黑色 重新设置颜色
    if (gaussianSample.a > smoothRange.y && isBlack(gaussianSample.rgb)) {
        gaussianSample.rgb = color.rgb;
    }
    // 在应用颜色乘数之前应用平滑
    if (smoothSwitcher > 0) {
        // 只对alpha通道应用平滑，避免影响颜色通道
        gaussianSample.a = smoothstep(smoothRange.x, smoothRange.y, gaussianSample.a);
    }

    // 输出最终颜色
    fragColor = vec4(gaussianSample.rgb * color.rgb, gaussianSample.a);
}