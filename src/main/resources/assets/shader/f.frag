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

const float INV_SQRT_2PI = 0.3989422804014327;
float sigmaSquared;
float normalizationFactor;

vec4 safeSampler(sampler2D tex, vec2 uv) {
    if (uv.x < uvBounds.x || uv.x > uvBounds.z || uv.y < uvBounds.y || uv.y > uvBounds.w) {
        return vec4(0);
    }
    return texture(tex, uv);
}


float gaussianWeight2D_Optimized(vec2 offset) {

    float exponent = -(offset.x * offset.x + offset.y * offset.y) / (2.0 * sigmaSquared);

    return exp(exponent);
}

vec4 gaussianBlur(sampler2D tex, vec2 uv, vec2 texelSize) {
    float totalWeight = 0.0;
    vec4 accumulatedColor = vec4(0);

    float stepScale = blurRadius;
    vec2 baseStep = texelSize * stepScale;

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

    vec2 texelSize = 1 / textureSize;
    vec4 sampleColor = gaussianBlur(SamTex, texCoord, texelSize);

    sampleColor.a = smoothstep(smoothRange.x, smoothRange.y, sampleColor.a);

    fragColor = vec4(sampleColor.rgb * Color.rgb, sampleColor.a);
}