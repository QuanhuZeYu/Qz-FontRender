#version 330

// 输入变量：从顶点着色器传递过来的纹理坐标
in vec2 texCoord;
// 输出变量：最终片元的颜色
out vec4 fragColor;

// 统一变量（Uniforms）：由应用程序传入的常量
uniform vec4 color;                     // 基础颜色，用于与纹理颜色混合
uniform sampler2D SamTex;               // 2D纹理采样器
uniform vec2 textureSize;               // 纹理的尺寸（宽度和高度），修正拼写
uniform vec2 smoothRange = vec2(0.05, 0.5);  // 平滑范围，用于smoothstep

// 新增的可配置uniforms
uniform float internalAlphaThreshold = 0.9;  // 内部实心区域的alpha阈值
uniform float sampleMultiplier = 0.5;        // 采样次数计算中的乘法因子
uniform int sampleOffset = 1;                // 采样次数计算中的偏移量
uniform int minSamplesPerAxis = 3;           // 每个轴的最小采样次数
uniform int maxSamplesPerAxis = 8;           // 每个轴的最大采样次数
uniform float coverageEpsilon = 0.0001;      // 覆盖度除零避免的epsilon值

void main () {
    // 计算当前片元纹理坐标在屏幕空间中的变化率（导数）
    vec2 texCoordDerivative = fwidth(texCoord);

    // 在纹理的中心位置进行采样，获取初始颜色
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
    int num_x = clamp(int(ceil(texels_x * sampleMultiplier)) + sampleOffset, minSamplesPerAxis, maxSamplesPerAxis);
    int num_y = clamp(int(ceil(texels_y * sampleMultiplier)) + sampleOffset, minSamplesPerAxis, maxSamplesPerAxis);

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

    // 双重循环进行超采样
    for (int ix = 0; ix < num_x; ix++) {
        for (int iy = 0; iy < num_y; iy++) {
            // 计算当前采样的偏移
            vec2 offset = vec2(float(ix) * step_x, float(iy) * step_y);

            // 计算采样UV坐标
            vec2 sample_uv = start_uv + offset;

            // 采样纹理
            vec4 samp = texture(SamTex, sample_uv);

            // 对每个采样的alpha应用smoothstep，计算覆盖度（这比平均后smoothstep质量更高）
            float coverage = smoothstep(smoothRange.x, smoothRange.y, samp.a);

            // 累加预乘的RGB和覆盖度
            total_rgb += samp.rgb * coverage;
            total_coverage += coverage;
        }
    }

    // 计算平均覆盖度（最终alpha）
    float final_a = total_coverage / float(total_samples);

    // 计算平均RGB（如果total_coverage小于epsilon，避免除零）
    vec3 avg_rgb = (total_coverage > coverageEpsilon) ? total_rgb / total_coverage : vec3(0.0);

    // 输出最终颜色：平均RGB与uniform颜色混合，alpha为平均覆盖度
    fragColor = vec4(avg_rgb * color.rgb, final_a);
}