#version 330

// 输入变量：来自顶点着色器的纹理坐标
in vec2 texCoord;
// 输出变量：最终的片元颜色
out vec4 fragColor;

// 统一变量：由应用程序传递的常量
uniform vec4 color;                     // 用于与纹理混合的基础颜色
uniform sampler2D SamTex;               // 2D纹理采样器
uniform vec2 textureSize;               // 纹理尺寸（宽度，高度）
uniform vec4 uvBounds = vec4(0,0,1,1);  // 用于安全采样的UV边界
uniform float internalAlphaThreshold = 0.99;  // 用于实心区域的Alpha阈值
uniform float blackThreshold = 0.5;      // 黑色检测阈值
uniform float blurRadius = 4.0;          // 模糊半径
uniform vec2 smoothRange = vec2(0.1, 0.5); // 平滑范围

// 检查颜色是否被视为黑色
bool isBlack(vec3 color) {
    return (color.r < blackThreshold && color.g < blackThreshold && color.b < blackThreshold);
}

// 2D高斯权重函数
float gaussianWeight2D(vec2 offset, float sigma) {
    float x = offset.x;
    float y = offset.y;
    return exp(-(x * x + y * y) / (2.0 * sigma * sigma)) / (2.0 * 3.14159265359 * sigma * sigma);
}

// 带UV边界检查的安全纹理采样
vec4 safeSampler(sampler2D tex, vec2 uv) {
    if (uv.x < uvBounds.x || uv.x > uvBounds.z || uv.y < uvBounds.y || uv.y > uvBounds.w) {
        return vec4(0);
    }
    return texture(tex, uv);
}

// 全2D高斯模糊，带自适应采样和加粗效果
vec4 adaptiveGaussianBlur(sampler2D tex, vec2 uv, vec2 texelSize, float blurRadius, int maxSamples) {
    // 计算自适应采样所需的UV导数
    vec2 uvDerivative = fwidth(uv);
    float derivativeMag = length(uvDerivative);

    // 根据导数幅值自适应调整半径
    float adaptiveRadius = blurRadius * clamp(derivativeMag * 100.0, 0.5, 2.0);
    float sigma = adaptiveRadius * 0.333; // 标准差为半径的1/3

    // 计算采样数量，确保为奇数以保证对称性
    int sampleCount = min(maxSamples, int(ceil(adaptiveRadius / derivativeMag)));
    sampleCount = max(3, sampleCount / 2 * 2 + 1); // 确保为奇数

    vec4 color = vec4(0.0);
    float totalWeight = 0.0;

    // 2D高斯核采样
    for (int i = -sampleCount / 2; i <= sampleCount / 2; ++i) {
        for (int j = -sampleCount / 2; j <= sampleCount / 2; ++j) {
            // 通过缩放偏移量应用加粗效果
            vec2 boldOffset = vec2(float(i), float(j)) * texelSize * adaptiveRadius / float(sampleCount / 2);
            vec2 sampleUV = uv + boldOffset;

            // 计算2D高斯权重
            float weight = gaussianWeight2D(vec2(float(i), float(j)) / float(sampleCount / 2) * adaptiveRadius, sigma);

            // 安全地采样纹理
            vec4 sampleColor = safeSampler(tex, sampleUV);
            color += sampleColor * weight;
            totalWeight += weight;
        }
    }

    // 根据总权重归一化颜色
    return color / totalWeight;
}

void main() {
    vec2 texelSize = 1.0 / textureSize;
    // 在中心位置采样纹理
    vec4 centerColor = texture(SamTex, texCoord);

    // 如果中心像素为实心（高Alpha值），直接输出混合后的颜色
    if (centerColor.a > internalAlphaThreshold) {
        fragColor = vec4(centerColor.rgb * color.rgb, centerColor.a);
        return;
    }

    // 应用带加粗效果的全2D高斯模糊
    vec4 gaussianSample = adaptiveGaussianBlur(SamTex, texCoord, texelSize, blurRadius, 4);

    // 增强黑色边缘以实现加粗效果
    if (gaussianSample.a > 0.0 && isBlack(gaussianSample.rgb)) {
        gaussianSample.rgb = vec3(blackThreshold * 1.5);
    }

    // 应用smoothstep以实现更平滑的过渡
    gaussianSample = smoothstep(vec4(smoothRange.x), vec4(smoothRange.y), gaussianSample);

    // 输出最终颜色
    fragColor = vec4(gaussianSample.rgb * color.rgb, gaussianSample.a);
}