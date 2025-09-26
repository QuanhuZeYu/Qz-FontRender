#version 330

// 输入变量：从顶点着色器传递过来的纹理坐标
in vec2 texCoord;
// 输出变量：最终片元的颜色
out vec4 fragColor;

// 统一变量（Uniforms）：由应用程序传入的常量
uniform vec4 color;                     // 基础颜色，用于与纹理颜色混合
uniform sampler2D SamTex;               // 2D纹理采样器
uniform vec2 textureSize;               // 纹理的尺寸（宽度和高度）
uniform vec2 smoothRange = vec2(0.2, 0.8);  // 平滑范围，用于smoothstep
uniform vec4 uvBounds = vec4(0, 1, 0, 1);

// 新增的可配置uniforms
uniform float internalAlphaThreshold = 0.99;  // 内部实心区域的alpha阈值
uniform float sampleMultiplier = 1.5;        // 采样次数计算中的乘法因子
uniform int sampleOffset = 1;                // 采样次数计算中的偏移量
uniform int minSamplesPerAxis = 5;           // 每个轴的最小采样次数
uniform int maxSamplesPerAxis = 16;           // 每个轴的最大采样次数
uniform float coverageEpsilon = 0.0001;      // 覆盖度除零避免的epsilon值
uniform float aa_strength = 1.0;

// 新增: 使用提供的公式进行自适应采样
uniform float strength = 8;              // 采样强度系数
uniform float sigma = 1.0;  // 高斯标准差，调大=更平滑

// 新增: 背景颜色替换阈值和替换颜色
uniform float blackThreshold = 0.5;      // 黑色判断阈值
uniform vec3 replacementColor = vec3(1, 1, 1);  // 替换黑色为灰色（根据原始代码）

bool isBlack(vec3 rgb) {
    return (rgb.r < blackThreshold && rgb.g < blackThreshold && rgb.b < blackThreshold);
}

vec4 safeSampler(vec2 uv) {
    if (uv.x < uvBounds.x || uv.x > uvBounds.y || uv.y < uvBounds.z || uv.y > uvBounds.w) {
        return vec4(0);  // 超出边界，返回透明黑色
    }
    return texture(SamTex, uv);
}

void main () {
    // 计算当前片元纹理坐标在屏幕空间中的变化率（导数）
    vec2 texCoordDerivative = fwidth(texCoord);

    // 使用提供的公式计算自适应采样因子
    float adaptiveFactor = strength * fwidth(dot(texCoord, textureSize)) /
    sqrt(textureSize.x * textureSize.x + textureSize.y * textureSize.y);

    // 增强边缘检测：将自适应因子应用到导数计算中
    texCoordDerivative *= max(1.0, adaptiveFactor);

    // 在纹理的中心位置进行采样，获取初始颜色（使用原始texture，不应用替换，以保持内部颜色不变）
    vec4 centerColor = texture(SamTex, texCoord);

    // 条件判断：如果中心采样点的透明度高于阈值，则认为这是实心内部区域
    if (centerColor.a > internalAlphaThreshold) {
        // 直接输出纹理颜色与uniform颜色的混合结果，并保持原有的透明度
        fragColor = vec4(centerColor.rgb * color.rgb, centerColor.a);
        // 提前返回，跳过后面所有的计算，优化性能
        return;
    }

    // 计算像素在纹理空间中覆盖的纹素数量（各向异性考虑）
    float texels_x = texCoordDerivative.x * textureSize.x;
    float texels_y = texCoordDerivative.y * textureSize.y;

    // 根据覆盖的纹素数自适应计算每个方向的采样次数
    // 使用自适应因子增强采样次数计算
    int num_x = clamp(int(ceil(texels_x * sampleMultiplier * adaptiveFactor)) + sampleOffset,
                      minSamplesPerAxis, maxSamplesPerAxis);
    int num_y = clamp(int(ceil(texels_y * sampleMultiplier * adaptiveFactor)) + sampleOffset,
                      minSamplesPerAxis, maxSamplesPerAxis);

    // 计算每个方向的采样步长，以覆盖整个像素足迹
    float step_x = texCoordDerivative.x / float(num_x - 1);
    float step_y = texCoordDerivative.y / float(num_y - 1);

    // 像素足迹的半宽（用于起始偏移）
    vec2 half_deriv = texCoordDerivative * 0.5;

    // 起始采样UV坐标（像素足迹的左下角）
    vec2 start_uv = texCoord - half_deriv;

    // 初始化累加器：总覆盖度（alpha）和预乘颜色
    float total_coverage = 0.0;
    vec3 total_rgb = vec3(0.0);

    // 计算总采样次数
    int total_samples = num_x * num_y;
    float total_weight = 0.0;  // 新增

    float aa_width = max(texCoordDerivative.x, texCoordDerivative.y) * aa_strength;  // 基于UV derivative的像素宽度

    // 双重循环进行超采样（不跳过任何采样，保持原始采样行为）
    for (int ix = 0; ix < num_x; ix++) {
        for (int iy = 0; iy < num_y; iy++) {
            // 计算当前采样的偏移
            vec2 offset = vec2(float(ix) * step_x, float(iy) * step_y);

            // 计算采样UV坐标
            vec2 sample_uv = start_uv + offset;

            // 采样纹理，使用safeSampler仅替换颜色（不影响alpha或采样 inclusion）
            vec4 samp = safeSampler(sample_uv);

            float raw_a = samp.a;
            float coverage = clamp(raw_a / aa_width, 0.0, 1.0);  // 或 smoothstep(0.0, aa_width, raw_a) 如果需要非线性

            // 计算高斯权重
            vec2 center_offset = vec2(float(ix) - float(num_x-1)/2.0, float(iy) - float(num_y-1)/2.0);  // 相对中心
            float weight = exp(-(center_offset.x*center_offset.x + center_offset.y*center_offset.y) / (2.0 * sigma * sigma));

            // 累加预乘的RGB和覆盖度
            vec3 currentRGB = samp.rgb * coverage * weight;
            total_rgb += currentRGB;
            total_coverage += coverage * weight;
            total_weight += weight;
        }
    }

    // 计算平均覆盖度（最终alpha）
    float final_a = (total_weight > coverageEpsilon) ? total_coverage / total_weight : 0.0;

    // 计算平均RGB（如果total_coverage小于epsilon，避免除零，使用vec3(0.0)作为fallback）
    vec3 avg_rgb = (total_coverage > coverageEpsilon) ? total_rgb / total_coverage : replacementColor * 0.5;
    if (isBlack(avg_rgb)) {
        avg_rgb = replacementColor * 0.5;
    }

    // 输出最终颜色：平均RGB与uniform颜色混合，alpha为平均覆盖度
    fragColor = vec4(avg_rgb * color.rgb, final_a);
}