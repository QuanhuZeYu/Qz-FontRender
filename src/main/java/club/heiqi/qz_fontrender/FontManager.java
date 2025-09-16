package club.heiqi.qz_fontrender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
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
                name.toLowerCase().endsWith(".ttf"));

        if (fontFiles != null) {
            for (File fontFile : fontFiles) {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                    installedFonts.add(font);
                } catch (FontFormatException | IOException e) {
                    LOG.error(e);
                }
            }
        }
    }

    public void loadAssetsFontsTTF() {
        File fontDir = new File(System.getProperty("user.dir"), "fonts");
        if (!fontDir.exists() || !fontDir.isDirectory()) fontDir.mkdirs();

        File[] fontFiles = fontDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".ttf"));

        if (fontFiles != null) {
            for (File fontFile : fontFiles) {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                    installedFonts.add(font);
                } catch (FontFormatException | IOException e) {
                    LOG.error(e);
                }
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

    public static void main(String[] args) {
        FontManager manager = new FontManager();
        manager.printAllFontNames();

        // manager.printAllFontPath();
    }
}
