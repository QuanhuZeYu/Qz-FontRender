#version 330

in vec2 texCoord;
out vec4 fragColor;

uniform vec4 color;
uniform sampler2D SamTex;
uniform float boldAmount = 0.0;
uniform vec2 textureSize;

void main(void) {
    vec2 pixelSize = 1.0 / textureSize;

    vec4 texColor = texture(SamTex, texCoord);

    vec4 blendedColor = texColor * color;

    float adaptiveScale = max(1.0, 10.0 / min(textureSize.x, textureSize.y));
    float adjustedBoldAmount = boldAmount * adaptiveScale;

    vec4 maxColor = blendedColor;

    int sampleCount = int(clamp(8.0 * adaptiveScale, 8.0, 16.0));

    for (int i = 0; i < 16; i++) {
        if (i >= sampleCount) break;

        float angle = 6.28318530718 * float(i) / float(sampleCount);
        vec2 offset = vec2(cos(angle), sin(angle)) * adjustedBoldAmount;

        vec4 sampleColor = texture(SamTex, texCoord + offset * pixelSize) * color;
        maxColor = max(maxColor, sampleColor);
    }

    vec4 hSample = texture(SamTex, texCoord + vec2(adjustedBoldAmount * pixelSize.x, 0)) * color;
    vec4 vSample = texture(SamTex, texCoord + vec2(0, adjustedBoldAmount * pixelSize.y)) * color;
    maxColor = max(maxColor, hSample);
    maxColor = max(maxColor, vSample);

    float alpha = maxColor.a;
    float smoothRange = 0.05 + 0.02 * max(adjustedBoldAmount, 0.5);

    alpha = smoothstep(0.5 - smoothRange, 0.5 + smoothRange, alpha);

    fragColor = vec4(maxColor.rgb, alpha);
}