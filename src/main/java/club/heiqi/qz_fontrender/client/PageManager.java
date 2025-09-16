package club.heiqi.qz_fontrender.client;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class PageManager {
    /**掌管的纹理页*/
    public int textureID;
    /**纹理页大小*/
    public int width, height;
    /**当前的字符缓存*/
    public Map<Integer, CharacterInfo> cache = new HashMap<>();

    public PageManager() {
        initPage();
    }

    /**
     * 创建 opengl 纹理对象
     */
    public void initPage() {
        textureID = GL11.glGenTextures();
        int currentTexID = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        // 绑定纹理
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        // 环绕模式（超出边界的处理）
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT); // S轴（水平）
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT); // T轴（垂直）

        // 过滤模式（缩放时的插值）
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR); // 缩小
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR); // 放大

        // 4. 分配纹理内存（不提供图像数据）
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,         // 纹理目标
                0,                          // Mipmap 级别
                GL11.GL_RGBA8,              // 内部格式 (如 GL_RGBA8, GL_RGB16F, GL_DEPTH_COMPONENT)
                width,                      // 纹理宽度
                height,                     // 纹理高度
                0,                          // 必须为0（历史遗留）
                GL11.GL_RGBA8,              // 像素数据格式
                GL11.GL_UNSIGNED_BYTE,      // 像素数据类型
                (ByteBuffer) null           // 图像数据 - 设为 null 表示不提供数据
        );

        // 生成 mipmap
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        // 恢复绑定
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexID);
    }


}
