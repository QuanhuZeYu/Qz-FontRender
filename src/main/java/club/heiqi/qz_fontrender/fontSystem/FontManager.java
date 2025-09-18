package club.heiqi.qz_fontrender.fontSystem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;

public class FontManager {
    public static Logger LOG = LogManager.getLogger();
    public float fontSize = 32f;
    /**存储所有可用的awt字体对象*/
    public LinkedHashSet<Font> fonts = new LinkedHashSet<>();

    public FontManager(float fontSize) {
        this.fontSize = fontSize;
        loadAssetsFontsTTF();
        loadInstalledFontsTTF();
    }

    public Font findSuitable(int codepoint) {
        for (Font font : fonts) {
            if (font.canDisplay(codepoint)) return font;
        }
        return get(0);
    }

    public Font get(int index) {
        return (Font) fonts.toArray()[index];
    }

    /**
     * 加载资源文件中的字体，放在链表最前面，优先级最高
     */
    public void loadAssetsFontsTTF() {
        File fontDir = new File(System.getProperty("user.dir"), "fonts");
        if (!fontDir.exists() || !fontDir.isDirectory()) {
            boolean mkdirs = fontDir.mkdirs();
        }

        File[] fontFiles = fontDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".ttf") ||
                name.toLowerCase().endsWith(".otf"));

        if (fontFiles != null) {
            loadTTF(fontFiles);
        }
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
        Collections.addAll(fonts, allFonts);
    }

    public void loadTTF(File[] files) {
        for (File fontFile : files) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                font = font.deriveFont(fontSize);
                fonts.add(font);
            } catch (FontFormatException | IOException e) {
                LOG.error(e);
            }
        }
    }
}
