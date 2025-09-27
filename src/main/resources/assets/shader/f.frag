#version 330

in vec2 texCoord;
out vec4 fragColor;

uniform vec4 color;
uniform sampler2D SamTex;
uniform vec4 uvBounds = vec4(0,0,1,1);
uniform vec2 textureSize = vec2(2048);
uniform vec2 smoothRange = vec2(0,1);
uniform float sigma = 3.14;
uniform float blurRadius = 1;
uniform int sampleCount = 1;

const float PI = 3.14159265359;

float gaussianWeight2D(vec2 offset) {
    float sigmaSquared = sigma * sigma;
    float normalization = 1.0 / (2.0 * PI * sigmaSquared);
    float exponent = -(offset.x * offset.x + offset.y * offset.y) / (2.0 * sigmaSquared);
    return normalization * exp(exponent);
}

vec4 safeSampler(sampler2D tex, vec2 uv) {
    if (uv.x < uvBounds.x || uv.x > uvBounds.z || uv.y < uvBounds.y || uv.y > uvBounds.w) {
        return vec4(0);
    }
    return texture(tex, uv);
}

// 修改后的高斯模糊函数
vec4 gaussianBlur(sampler2D tex, vec2 uv, vec2 texelSize) {
    float totalWeight = 0.0;
    vec4 accumulatedColor = vec4(0);

    for (int i = -sampleCount / 2; i <= sampleCount / 2; ++i) {
        for (int j = -sampleCount / 2; j <= sampleCount / 2; ++j) {
            vec2 offset = vec2(float(i), float(j)) * texelSize * blurRadius / float(sampleCount / 2);
            vec2 sampleUV = uv + offset;

            float weight = gaussianWeight2D(vec2(float(i), float(j)) / float(sampleCount / 2) * blurRadius);

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
    vec2 texelSize = 1 / textureSize;
    vec4 sampleColor = gaussianBlur(SamTex, texCoord, texelSize);
    sampleColor.a = smoothstep(smoothRange.x, smoothRange.y, sampleColor.a);
    fragColor = vec4(sampleColor.rgb * color.rgb, sampleColor.a);
}