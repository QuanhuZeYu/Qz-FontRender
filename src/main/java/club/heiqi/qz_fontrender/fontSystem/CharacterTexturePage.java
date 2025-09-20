package club.heiqi.qz_fontrender.fontSystem;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

public class CharacterTexturePage {
    /**纹理页大小*/
    public final int width, height;
    /**每个字符单元大小*/
    public final int cWidth, cHeight;
    /**JavaAWT操作画布对象*/
    public final BufferedImage image;
    /**OpenGL纹理页ID*/
    public int textureID;
    /**存储的字符和它的信息*/
    public Map<Integer, CharacterInfo> storage = new HashMap<>();

    /**记录当前可添加位置的左上角坐标 左上角 0,0*/
    public int cX, cY;
    /**标记是否填满*/
    public boolean full = false;
    /**标记是否需要更新GL侧纹理*/
    public boolean needUpload = false;

    public CharacterTexturePage(int width, int height, int charWidth, int charHeight) {
        this.width = width;
        this.height = height;
        this.cWidth = charWidth;
        this.cHeight = charHeight;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public void addCharacterTexture(ImageAndInfo imageAndInfo) {
        if (full) return;
        Graphics2D pageGraphics = image.createGraphics();
        int cx = cX, cy = cY;
        pageGraphics.drawImage(imageAndInfo.image(), cX, cY, cWidth, cHeight, (ImageObserver) null);
        pageGraphics.dispose();

        // 递增坐标
        cX = cX + cWidth;
        if (cX >= width) {
            // 到达最右侧重置到开始并另起一行
            cX = 0;
            cY = cY + cHeight;
            // 如果另起一行的Y在最底部了标记填满
            if (cY >= height) {
                full = true;
            }
        }

        // 缓存记录信息
        CharacterInfo info = new CharacterInfo(imageAndInfo.info().codepoint(),
                cx, cy,
                imageAndInfo.info().width(), imageAndInfo.info().height(),
                imageAndInfo.info().advanceX(), imageAndInfo.info().advanceY(),
                imageAndInfo.info().ascent(), imageAndInfo.info().descent());
        storage.put(info.codepoint(), info);

        // 标记需要更新GL侧的纹理
        needUpload = true;
        // int width1 = imageAndInfo.image().getWidth();
        // int height1 = imageAndInfo.image().getHeight();
        // int[] pixels = new int[width1 * height1];
        // imageAndInfo.image().getRGB(0,0,width1,height1,pixels,0,width1);
        // ByteBuffer buffer = BufferUtils.createByteBuffer(width1 * height1 * 4);
        // for (int y = 0; y < width1; y++) {  // 修改这里：y从0递增到height-1
        //     for (int x = 0; x < height1; x++) {
        //         int pixel = pixels[y * width1 + x];  // 直接按原顺序访问
        //         buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
        //         buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
        //         buffer.put((byte) (pixel & 0xFF));         // B
        //         buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        //     }
        // }
        // buffer.flip();
        //
        // GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        // // 上传纹理数据
        // GL11.glTexSubImage2D(
        //         GL11.GL_TEXTURE_2D,
        //         0,
        //         cx, cy,
        //         width,height,
        //         GL11.GL_RGBA,
        //         GL11.GL_UNSIGNED_BYTE,
        //         buffer
        // );
    }

    public boolean isCharInPage(int codepoint) {
        CharacterInfo info = storage.get(codepoint);
        return info != null;
    }

    public CharacterInfo getInfo(int codepoint) {
        return storage.get(codepoint);
    }

    /**
     * 将BufferedImage上传到OpenGL中
     */
    public void loadTexture() {

        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

        // 移除垂直翻转：按原始顺序读取像素（从上到下）
        for (int y = 0; y < height; y++) {  // 修改这里：y从0递增到height-1
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];  // 直接按原顺序访问
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                buffer.put((byte) (pixel & 0xFF));         // B
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
            }
        }
        buffer.flip();

        if (textureID == 0) {
            textureID = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            // 上传纹理数据
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    width, height, 0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    buffer
            );
        }
        else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            // 上传纹理数据
            GL11.glTexSubImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    0, 0,
                    width,height,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    buffer
            );
        }

        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        // 设置纹理参数
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);

        needUpload = false;
    }

    public CharacterInfo renderChar(int codepoint, int color, float x, float y, float width, float height) {
        if (needUpload) loadTexture();

        CharacterInfo info = getInfo(codepoint);
        double u0 = info.getU0(this.width);
        double u1 = info.getU1(this.width);
        double v0 = info.getV0(this.height);
        double v1 = info.getV1(this.height);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(4);
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, floatBuffer);
        float alpha = ((color >> 24) & 255) / 255f;
        float red = ((color >> 16) & 255) / 255f;
        float green = ((color >> 8) & 255) / 255f;
        float blue = (color & 255) / 255f;
        GL11.glColor4f(red, green, blue, alpha);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2d(u0, v0);
        GL11.glVertex3f(x, y, 0);
        GL11.glTexCoord2d(u0, v1);
        GL11.glVertex3f(x, y+height, 0);
        GL11.glTexCoord2d(u1, v1);
        GL11.glVertex3f(x+width, y+height, 0);
        GL11.glTexCoord2d(u1, v0);
        GL11.glVertex3f(x+width, y, 0);
        GL11.glEnd();

        GL11.glColor4f(floatBuffer.get(0), floatBuffer.get(1), floatBuffer.get(2), floatBuffer.get(3));

        return info;
    }

    public void dispose() {
        GL11.glDeleteTextures(textureID);
    }
}
