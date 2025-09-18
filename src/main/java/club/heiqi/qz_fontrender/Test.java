package club.heiqi.qz_fontrender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FontManager {
    public static Logger LOG = LogManager.getLogger();
    public List<Font> installedFonts;

    public FontManager() {
        installedFonts = new ArrayList<>();
        loadAssetsFontsTTF();
        loadInstalledFontsTTF();
    }

    /**
     * 加载系统中所有已安装的字体
     */
    public void loadInstalledFontsTTF() {
        // 获取系统图形环境
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        // 获取所有已安装字体（包括TTF和其他格式）
        Font[] allFonts = ge.getAllFonts();

        // 筛选TTF字体并存入列表
        for (Font font : allFonts) {
            // 检查字体是否为TrueType（TTF）
            if (font.getFontName().contains("TrueType")
                    || font.getFontName().contains("TTF")) {
                installedFonts.add(font);
            }
        }
    }

    public void loadAssetsFontsTTF() {
        File fontDir = new File(System.getProperty("user.dir"), "fonts");
        if (!fontDir.exists() || !fontDir.isDirectory()) fontDir.mkdirs();

        File[] fontFiles = fontDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".ttf"));

        if (fontFiles != null) {
            loadTTF(fontFiles);
        }
    }

    public void loadTTF(File[] files) {
        for (File fontFile : files) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                font = font.deriveFont(64f);
                installedFonts.add(font);
            } catch (FontFormatException | IOException e) {
                LOG.error(e);
            }
        }
    }

    /**
     * 打印所有已安装字体的名称
     */
    public void printAllFontNames() {
        for (Font font : installedFonts) {
            LOG.info(font.getFontName());
        }
    }

    /**
     * 传入码点返回该字符的BufferedImage
     */
    public static BufferedImage renderCharacter(int codepoint, Font font, int width, int height) {
        char[] chars = Character.toChars(codepoint);
        String character = new String(chars);

        // 创建临时图像获取字体渲染上下文
        BufferedImage tempImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tempGraphics = tempImage.createGraphics();
        FontRenderContext frc = tempGraphics.getFontRenderContext();

        double bounds2DWidth, bounds2DHeight;
        Rectangle2D bounds2D;
        do {
            // 获取字符的精确边界
            Rectangle2D bounds = font.getStringBounds(character, frc);
            GlyphVector glyphVector = font.createGlyphVector(frc, character);
            // 获取度量信息
            GlyphMetrics glyphMetrics = glyphVector.getGlyphMetrics(0);
            float advance = glyphMetrics.getAdvance();
            float advanceX = glyphMetrics.getAdvanceX();
            float advanceY = glyphMetrics.getAdvanceY();
            float lsb = glyphMetrics.getLSB();
            float rsb = glyphMetrics.getRSB();
            boolean combining = glyphMetrics.isCombining();
            boolean standard = glyphMetrics.isStandard();
            boolean ligature = glyphMetrics.isLigature();
            boolean whitespace = glyphMetrics.isWhitespace();
            bounds2D = glyphMetrics.getBounds2D();
            bounds2DWidth = bounds2D.getWidth();
            bounds2DHeight = bounds2D.getHeight();
            double boundsWidth = bounds.getWidth();
            double boundsHeight = bounds.getHeight();

            if (bounds2DWidth > width || bounds2DHeight > height) font = font.deriveFont(font.getSize2D() - .5f);
        }
        while (bounds2DWidth > width || bounds2DHeight > height);

        // 创建最终图像
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // 设置渲染质量
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 设置字体和颜色
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);

        // 调整绘制位置（考虑字符基线）
        float x = (float) -bounds2D.getX();
        float y = (float) -bounds2D.getY();
        g2d.drawString(character, x, y);

        // 释放资源并返回图像
        g2d.dispose();
        tempGraphics.dispose();
        return image;
    }

    /**
     * 将BufferedImage保存到指定路径文件中
     */
    public static void saveImage(BufferedImage image, File savePath) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }

        try {
            // 确保目录存在
            File parentDir = savePath.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
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
            throw new RuntimeException("Failed to save image: " + savePath, e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unsupported image format: " + savePath, e);
        }
    }

    // 根据文件扩展名获取标准格式名称
    public static String getFormatName(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return "PNG";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "JPEG";
        if (name.endsWith(".gif")) return "GIF";
        if (name.endsWith(".bmp")) return "BMP";
        if (name.endsWith(".wbmp")) return "WBMP";
        return null;
    }

    public void printAllFontPath() {
        // 获取系统字体目录路径
        String osName = System.getProperty("os.name").toLowerCase();
        String fontDirPath = "";

        if (osName.contains("win")) {
            fontDirPath = System.getenv("SystemRoot") + "\\Fonts\\";
        } else if (osName.contains("mac")) {
            fontDirPath = "/Library/Fonts/";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            fontDirPath = "/usr/share/fonts/";
        } else {
            LOG.warn("Unsupported operating system: " + osName);
            return;
        }

        File fontDir = new File(fontDirPath);
        if (!fontDir.exists() || !fontDir.isDirectory()) {
            LOG.error("Font directory not found: " + fontDirPath);
            return;
        }

        // 遍历字体目录并打印字体文件路径
        File[] fontFiles = fontDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".ttf") ||
                        name.toLowerCase().endsWith(".otf") ||
                        name.toLowerCase().endsWith(".ttc"));

        if (fontFiles != null) {
            for (File fontFile : fontFiles) {
                LOG.info(fontFile.getAbsolutePath());
            }
        }
    }

    /**
     * 将BufferedImage上传到OpenGL中
     * @param image 只接受ARGB格式类型
     */
    public static int loadTexture(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

        // 垂直翻转 + ARGB转RGBA
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                buffer.put((byte) (pixel & 0xFF));         // B
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
            }
        }
        buffer.flip();

        int textureID = GL11.glGenTextures();
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
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        // 设置纹理参数
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);

        return textureID;
    }

    public static void main(String[] args) {
        FontManager manager = new FontManager();
        manager.printAllFontNames();

        File saveDir = new File(System.getProperty("user.dir"), "images");
        for (Font font : manager.installedFonts) {
            File saveFile = new File(saveDir, font.getName()+".png");
            // if (!font.getName().equalsIgnoreCase("霞鹜文楷 Medium")) continue;
            BufferedImage image = manager.renderCharacter("`".codePointAt(0), font, 64, 64);
            manager.saveImage(image, saveFile);
        }
    }
}
