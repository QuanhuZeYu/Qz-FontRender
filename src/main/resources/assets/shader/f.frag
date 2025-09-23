#version 330

in vec4 position;
in vec2 texCoord;

out vec4 fragColor;

uniform vec4 color;
uniform sampler2D SamTex;
uniform float boldAmount = 0.0;
uniform vec2 textureSize; // 纹理尺寸 (width, height)

void main(void) {
    // 计算一个像素的大小
    vec2 pixelSize = 1.0 / textureSize;

    // 中心采样
    vec4 distance = texture(SamTex, texCoord);

    // 周围采样
    vec4 distance2 = texture(SamTex, texCoord + vec2(pixelSize.x * boldAmount, 0));
    vec4 distance3 = texture(SamTex, texCoord + vec2(-pixelSize.x * boldAmount, 0));
    vec4 distance4 = texture(SamTex, texCoord + vec2(0, pixelSize.y * boldAmount));
    vec4 distance5 = texture(SamTex, texCoord + vec2(0, -pixelSize.y * boldAmount));

    // 取最大值实现加粗效果
    distance = max(distance, distance2);
    distance = max(distance, distance3);
    distance = max(distance, distance4);
    distance = max(distance, distance5);

    vec4 texcolor = smoothstep(0.5 - 0.05, 0.5 + 0.05, distance);

    vec3 blendColor = texcolor.rgb * color.rgb;

    float alpha = texcolor.a * color.a;

    if (alpha < 0.3) {
        alpha = 0;
    }

    fragColor = vec4(blendColor, texcolor.a * color.a);
}