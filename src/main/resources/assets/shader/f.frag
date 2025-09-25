#version 330

in vec2 texCoord;
out vec4 fragColor;

uniform vec4 color;
uniform sampler2D SamTex;
uniform vec2 texureSize;

// 计算当前像素覆盖的纹理区域内的字符边缘强度
float calculateEdgeStrength(vec2 uv, vec2 pixelSize) {
    // 使用Sobel算子检测边缘
    float topLeft = texture(SamTex, uv + vec2(-pixelSize.x, pixelSize.y)).a;
    float top = texture(SamTex, uv + vec2(0.0, pixelSize.y)).a;
    float topRight = texture(SamTex, uv + vec2(pixelSize.x, pixelSize.y)).a;
    float left = texture(SamTex, uv + vec2(-pixelSize.x, 0.0)).a;
    float right = texture(SamTex, uv + vec2(pixelSize.x, 0.0)).a;
    float bottomLeft = texture(SamTex, uv + vec2(-pixelSize.x, -pixelSize.y)).a;
    float bottom = texture(SamTex, uv + vec2(0.0, -pixelSize.y)).a;
    float bottomRight = texture(SamTex, uv + vec2(pixelSize.x, -pixelSize.y)).a;

    // Sobel算子
    float gx = -topLeft - 2.0 * left - bottomLeft + topRight + 2.0 * right + bottomRight;
    float gy = -topLeft - 2.0 * top - topRight + bottomLeft + 2.0 * bottom + bottomRight;

    return sqrt(gx * gx + gy * gy);
}

void main() {
    // 实际纹理一个像素大小对应的uv范围
    vec2 texturePixelSize = 1.0 / texureSize;

    // 计算UV坐标的变化率（导数）
    vec2 dx = dFdx(texCoord);
    vec2 dy = dFdy(texCoord);

    // 计算当前像素覆盖的UV范围
    float coverageU = length(dx);
    float coverageV = length(dy);

    // 计算当前像素覆盖的纹素数量
    float texelsCoveredU = coverageU / texturePixelSize.x;
    float texelsCoveredV = coverageV / texturePixelSize.y;
    float totalTexelsCovered = texelsCoveredU * texelsCoveredV;

    // 根据覆盖的纹素数量选择不同的渲染策略
    if (totalTexelsCovered <= 1.5) {
        // 覆盖一个或少量纹素，使用高质量抗锯齿
        vec4 texColor = texture(SamTex, texCoord);

        // 计算边缘强度并增强对比度
        float edgeStrength = calculateEdgeStrength(texCoord, texturePixelSize);
        float contrast = 1.0 + edgeStrength * 2.0;
        float adjustedAlpha = clamp((texColor.a - 0.5) * contrast + 0.5, 0.0, 1.0);

        // 应用smoothstep进行平滑
        float alphaSoftness = fwidth(texColor.a) * 0.5;
        float smoothedAlpha = smoothstep(
            0.5 - alphaSoftness,
            0.5 + alphaSoftness,
            adjustedAlpha
        );

        fragColor = vec4(color.rgb * texColor.rgb, smoothedAlpha);
    }
    else if (totalTexelsCovered <= 9.0) {
        // 覆盖多个纹素，使用自适应超级采样
        int gridSize = int(ceil(sqrt(totalTexelsCovered)));
        gridSize = min(gridSize, 8); // 限制最大采样数

        vec4 totalColor = vec4(0.0);
        float totalWeight = 0.0;

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                // 计算采样位置（使用Jitter采样减少规则图案）
                vec2 offset = vec2(
                (float(i) / float(gridSize) - 0.5 + (fract(sin(float(i*j)*12.9898)*43758.5453)-0.5)/float(gridSize)) * coverageU,
                (float(j) / float(gridSize) - 0.5 + (fract(sin(float(i+j)*78.233)*43758.5453)-0.5)/float(gridSize)) * coverageV
                );

                vec2 sampleCoord = texCoord + offset;
                vec4 sampleColor = texture(SamTex, sampleCoord);

                // 计算边缘区域的权重（边缘处权重更高以保持清晰度）
                float edgeStrength = calculateEdgeStrength(sampleCoord, texturePixelSize);
                float weight = 1.0 + edgeStrength * 3.0;

                totalColor += sampleColor * weight;
                totalWeight += weight;
            }
        }

        vec4 averageColor = totalColor / totalWeight;

        // 增强对比度以保持清晰度
        float contrast = 1.0 + totalTexelsCovered * 0.1;
        float adjustedAlpha = clamp((averageColor.a - 0.5) * contrast + 0.5, 0.0, 1.0);

        fragColor = vec4(color.rgb * averageColor.rgb, adjustedAlpha);
    }
    else {
        // 覆盖大量纹素，使用mipmap和锐化滤波
        // 计算适当的LOD级别
        float lod = log2(totalTexelsCovered) * 0.5;
        vec4 texColor = textureLod(SamTex, texCoord, lod);

        // 应用锐化滤波
        vec4 sharpened = texColor;

        // 获取周围像素进行锐化
        vec4 left = textureLod(SamTex, texCoord + vec2(-texturePixelSize.x, 0.0), lod);
        vec4 right = textureLod(SamTex, texCoord + vec2(texturePixelSize.x, 0.0), lod);
        vec4 top = textureLod(SamTex, texCoord + vec2(0.0, texturePixelSize.y), lod);
        vec4 bottom = textureLod(SamTex, texCoord + vec2(0.0, -texturePixelSize.y), lod);

        // 使用拉普拉斯算子进行锐化
        sharpened += (texColor * 5.0 - (left + right + top + bottom)) * 0.3;

        // 增强对比度
        float contrast = 1.0 + totalTexelsCovered * 0.05;
        float adjustedAlpha = clamp((sharpened.a - 0.5) * contrast + 0.5, 0.0, 1.0);

        fragColor = vec4(color.rgb * texColor.rgb, adjustedAlpha);
    }
}