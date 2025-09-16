package club.heiqi.qz_fontrender.client;

public class CharacterInfo {
    /**
     * 所在的纹理ID
     */
    public int textureID;
    /**
     * 纹理坐标系坐标
     */
    public int x, y;
    /**
     * 字符大小
     */
    public int width, height;

    public CharacterInfo(int textureID, int x, int y, int width, int height) {
        this.textureID = textureID;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // ========== 获取UV信息 ==========
    public double getU0(int width, int height) {
        return (double) x / width;
    }

    public double getU1(int width, int height) {
        return (double) (x + this.width) / width;
    }

    public double getV0(int width, int height) {
        return (double) y / height;
    }

    public double getV1(int width, int height) {
        return (double) (y + this.height) / height;
    }
}
