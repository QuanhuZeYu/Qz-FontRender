package club.heiqi.qz_fontrender.fontSystem;

import club.heiqi.qz_fontrender.MyMod;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class CharacterTexturePage {
    /**纹理页大小*/
    public final int width, height;
    /**每个字符单元大小*/
    public final int cWidth, cHeight;
    /**JavaAWT操作画布对象*/
    public BufferedImage image;
    /**OpenGL纹理页ID*/
    public int textureID;
    /**存储的字符和它的信息*/
    public Map<Integer, CharacterInfo> storage = new HashMap<>();
    /**添加锁*/
    public ReentrantLock lock = new ReentrantLock();

    /**记录当前可添加位置的左上角坐标 左上角 0,0*/
    public int cX, cY;
    /**标记是否填满*/
    public boolean full = false;
    /**标记是否需要更新GL侧纹理*/
    public boolean needUpload = false;

    /**opengl渲染工具*/
    public static RenderTool renderTool;

    public CharacterTexturePage(int width, int height, int charWidth, int charHeight) {
        this.width = width;
        this.height = height;
        this.cWidth = charWidth;
        this.cHeight = charHeight;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public void addCharacterTexture(ImageAndInfo imageAndInfo) {
        if (full) return;
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

    public CharacterInfo getInfo(int codepoint) {
        return storage.get(codepoint);
    }

    public boolean isCharInPage(int codepoint) {
        CharacterInfo info = storage.get(codepoint);
        return info != null;
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
            // GL11.glTexImage2D(
            //         GL11.GL_TEXTURE_2D,
            //         0,
            //         GL11.GL_RGBA8,
            //         width, height, 0,
            //         GL11.GL_RGBA,
            //         GL11.GL_UNSIGNED_BYTE,
            //         buffer
            // );
        }

        // GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        // 设置纹理参数
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);

        needUpload = false;

        if (full) {
            image = null;
        }
    }

    public CharacterInfo renderChar(int codepoint, int color, float x, float y, float width, float height, boolean italic) {
        if (needUpload) loadTexture();

        CharacterInfo info = getInfo(codepoint);
        renderChar(info,color,x,y,width,height,italic);
        return info;
    }

    public void renderChar(CharacterInfo info, int color, float x, float y, float width, float height, boolean italic) {
        if (needUpload) loadTexture();

        float u0 = (float)info.getU0(this.width);
        float u1 = (float)info.getU1(this.width);
        float v0 = (float)info.getV0(this.height);
        float v1 = (float)info.getV1(this.height);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, floatBuffer);

        float[] vertex = {
                italic ? x+2 : x, y, 0,
                x, y+height, 0,
                x+width, y+height, 0,
                italic ? x+width+2 : x+width, y, 0
        };
        float[] uv = {
                u0, v0,
                u0, v1,
                u1, v1,
                u1, v0
        };
        int[] index = {
                0,1,2, 2,3,0
        };

        renderTool.render(vertex, uv, index, color, new Vector2f(this.width, this.height), new Vector4f(u0, v0, u1, v1));

        // GL11.glBegin(GL11.GL_QUADS);
        // GL11.glTexCoord2d(u0, v0);
        // GL11.glVertex3f(italic ? x+2 : x, y, 0);
        // GL11.glTexCoord2d(u0, v1);
        // GL11.glVertex3f(x, y+height, 0);
        // GL11.glTexCoord2d(u1, v1);
        // GL11.glVertex3f(x+width, y+height, 0);
        // GL11.glTexCoord2d(u1, v0);
        // GL11.glVertex3f(italic ? x+width+2 : x+width, y, 0);
        // GL11.glEnd();

        GL11.glColor4f(floatBuffer.get(0), floatBuffer.get(1), floatBuffer.get(2), floatBuffer.get(3));
    }

    /**
     * 将BufferedImage保存到指定路径文件中
     */
    public void saveImage(File savePath) {
        BufferedImage image = this.image;
        if (image == null) {
            // 从GPU中获取该纹理图像
            try {
                image = retrieveImageFromGPU();
            } catch (Exception e) {
                MyMod.LOG.error("Failed to retrieve texture from GPU: {}", savePath, e);
                return;
            }
        }

        try {
            // 确保目录存在
            File parentDir = savePath.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean mkdirs = parentDir.mkdirs();
            }

            // 获取文件扩展名（自动检测格式）
            String formatName = getFormatName(savePath);

            // 保存图像（使用PNG作为默认格式）
            if (formatName != null) {
                ImageIO.write(image, formatName, savePath);
            } else {
                // 如果无法从文件名确定格式，使用PNG格式并添加后缀
                File pngFile = new File(savePath.getAbsolutePath() + ".png");
                ImageIO.write(image, "PNG", pngFile);
            }
        } catch (IOException e) {
            MyMod.LOG.error("Failed to save image: {}", savePath, e);
        } catch (IllegalArgumentException e) {
            MyMod.LOG.error("Unsupported image format: {}", savePath, e);
        }
    }

    // 从GPU纹理中读取图像数据
    private BufferedImage retrieveImageFromGPU() {
        // 绑定纹理
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

        // 创建缓冲区来存储纹理数据
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // 创建BufferedImage并填充数据
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = (y * width + x) * 4;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                int a = buffer.get(i + 3) & 0xFF;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, argb);
            }
        }

        return image;
    }

    // 根据文件扩展名获取标准格式名称
    public String getFormatName(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return "PNG";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "JPEG";
        if (name.endsWith(".gif")) return "GIF";
        if (name.endsWith(".bmp")) return "BMP";
        if (name.endsWith(".wbmp")) return "WBMP";
        return null;
    }

    public void dispose() {
        GL11.glDeleteTextures(textureID);
        storage.clear();
    }
}
