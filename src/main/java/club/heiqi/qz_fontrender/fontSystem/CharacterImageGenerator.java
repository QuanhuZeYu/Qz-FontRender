package club.heiqi.qz_fontrender.fontSystem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CharacterImageGenerator {
    public static Logger LOG = LogManager.getLogger();

    /**
     * 传入码点返回该字符的BufferedImage
     */
    public static ImageAndInfo renderCharacter(int codepoint, Font font, int width, int height) {
        char[] chars = Character.toChars(codepoint);
        String character = new String(chars);
        float fontSize = font.getSize2D();

        // 创建临时图像获取字体渲染上下文
        BufferedImage tempImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tempGraphics = tempImage.createGraphics();
        FontRenderContext frc = tempGraphics.getFontRenderContext();

        double bounds2DWidth, bounds2DHeight;
        Rectangle2D visualBounds, logicalBounds;
        float advance, advanceX, advanceY, ascent, descent, leading;
        ImageAndInfo imageAndInfo;
        boolean retry = false;
        do {
            // 获取字符的精确边界
            GlyphVector glyphVector = font.createGlyphVector(frc, character);
            // 逻辑边界
            logicalBounds = glyphVector.getLogicalBounds();
            visualBounds = glyphVector.getVisualBounds();
            // 实际边界大小
            bounds2DWidth = visualBounds.getWidth();
            bounds2DHeight = visualBounds.getHeight();

            // 获取度量信息
            GlyphMetrics glyphMetrics = glyphVector.getGlyphMetrics(0);
            advance = glyphMetrics.getAdvance();
            advanceX = glyphMetrics.getAdvanceX();
            advanceY = glyphMetrics.getAdvanceY();
            float lsb = glyphMetrics.getLSB();
            float rsb = glyphMetrics.getRSB();
            boolean combining = glyphMetrics.isCombining();
            boolean standard = glyphMetrics.isStandard();
            boolean ligature = glyphMetrics.isLigature();
            boolean whitespace = glyphMetrics.isWhitespace();
            double bounds2DX = visualBounds.getX();
            double bounds2DY = visualBounds.getY();
            // 逻辑度量
            LineMetrics lineMetrics = font.getLineMetrics(character, frc);
            ascent = lineMetrics.getAscent();
            descent = lineMetrics.getDescent();
            leading = lineMetrics.getLeading();
            float lineHeight = lineMetrics.getHeight();

            double boundsWidth = logicalBounds.getWidth();
            double boundsHeight = logicalBounds.getHeight();
            double boundsX = logicalBounds.getX();
            double boundsY = logicalBounds.getY();

            if ((bounds2DWidth > width || bounds2DHeight > height) && font.getSize2D() >= fontSize/2) {
                font = font.deriveFont(font.getSize2D() - .5f);
                retry = true;
            }
            else {
                retry = false;
            }
        }
        while (retry);

        // 创建最终图像
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // 设置渲染质量
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 设置字体和颜色
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);

        // 准确的绘制在左上角
        float x = (float) 0;
        float y = (float) -logicalBounds.getY();  // 将图像拉到左上角
        g2d.drawString(character, x, y);

        // 释放资源并返回图像
        g2d.dispose();
        tempGraphics.dispose();
        CharacterInfo info = new CharacterInfo(codepoint,
                (int) Math.ceil(x), (int) Math.ceil(y),
                width, height,
                advanceX, advanceY,
                ascent, descent);
        return new ImageAndInfo(image, info);
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
            LOG.error("Failed to save image: {}", savePath, e);
        } catch (IllegalArgumentException e) {
            LOG.error("Unsupported image format: {}", savePath, e);
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
}
