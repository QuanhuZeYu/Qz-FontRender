package club.heiqi.qz_fontrender.fontSystem;

import com.github.bsideup.jabel.Desugar;

/**
 * @param codepoint         字符对应的Unicode码点
 * @param x,y               字符坐标 左上0,0
 * @param width,height      字符的大小
 * @param advanceX,advanceY 字体度量信息
 * @param ascent,descent    字体上升下降高度
 */
@Desugar
public record CharacterInfo(int codepoint,
                            int x, int y,
                            float width, float height,
                            float advanceX, float advanceY,
                            float ascent, float descent) {

    // ========== UV坐标信息 ==========
    /**
     * 左侧
     */
    public double getU0(int width) {
        return (double) x / width;
    }
    /**
     * 右侧
     */
    public double getU1(int width) {
        return (double) (x + this.width) / width;
    }
    /**
     * 上侧
     */
    public double getV0(int height) {
        return (double) y / height;
    }
    /**
     * 下侧
     */
    public double getV1(int height) {
        return (double) (y + this.height) / height;
    }
}
