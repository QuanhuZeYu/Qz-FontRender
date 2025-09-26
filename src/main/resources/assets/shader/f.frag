#version 330

in vec2 texCoord;
out vec4 fragColor;

uniform vec4 color;
uniform sampler2D SamTex;
uniform vec2 textureSize;
uniform vec4 uvBounds = vec4(0,0,1,1);
uniform float internalAlphaThreshold = 0.99;
uniform float blackThreshold = 0.5;
uniform float blurRadius = 4.0;
uniform vec2 smoothRange = vec2(0.1, 0.5);
uniform int smoothSwitcher = 0;

bool isBlack(vec3 color) {
    return (color.r < blackThreshold && color.g < blackThreshold && color.b < blackThreshold);
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

vec4 adaptiveGaussianBlur(sampler2D tex, vec2 uv, vec2 texelSize, float blurRadius, int maxSamples) {
    vec2 uvDerivative = fwidth(uv);
    float derivativeMag = length(uvDerivative);

    // 根据导数幅值自适应调整半径
    float adaptiveRadius = blurRadius * clamp(derivativeMag * 100.0, 0.5, 2.0);
    float sigma = adaptiveRadius * 0.333;

    int sampleCount = min(maxSamples, int(ceil(adaptiveRadius / derivativeMag)));
    sampleCount = max(3, sampleCount / 2 * 2 + 1);

    vec4 color = vec4(0.0);
    float totalWeight = 0.0;

    for (int i = -sampleCount / 2; i <= sampleCount / 2; ++i) {
        for (int j = -sampleCount / 2; j <= sampleCount / 2; ++j) {
            vec2 boldOffset = vec2(float(i), float(j)) * texelSize * adaptiveRadius / float(sampleCount / 2);
            vec2 sampleUV = uv + boldOffset;

            float weight = gaussianWeight2D(vec2(float(i), float(j)) / float(sampleCount / 2) * adaptiveRadius, sigma);

            vec4 sampleColor = safeSampler(tex, sampleUV);
            color += sampleColor * weight;
            totalWeight += weight;
        }
    }

    return color / totalWeight;
}

void main() {
    vec2 texelSize = 1.0 / textureSize;
    vec4 centerColor = texture(SamTex, texCoord);

    if (centerColor.a > internalAlphaThreshold) {
        fragColor = vec4(centerColor.rgb * color.rgb, centerColor.a);
        return;
    }

    vec4 gaussianSample = adaptiveGaussianBlur(SamTex, texCoord, texelSize, blurRadius, 4);

    if (gaussianSample.a > 0.0 && isBlack(gaussianSample.rgb)) {
        gaussianSample.rgb = vec3(1);
    }

    if (smoothSwitcher > 0) {
        gaussianSample = smoothstep(vec4(smoothRange.x), vec4(smoothRange.y), gaussianSample);
    }

    fragColor = vec4(gaussianSample.rgb * color.rgb, gaussianSample.a);
}