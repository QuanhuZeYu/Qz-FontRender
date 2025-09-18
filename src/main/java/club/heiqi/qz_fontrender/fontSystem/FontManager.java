package club.heiqi.qz_fontrender.fontSystem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FontManager {
    public static Logger LOG = LogManager.getLogger();
    public float fontSize = 32f;
    /**存储所有可用的awt字体对象*/
    public LinkedHashSet<Font> fonts = new LinkedHashSet<>();

    public FontManager(float fontSize) {
        this.fontSize = fontSize;
        initFontAssets();
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

    /**初始化字体资源*/
    public void initFontAssets() {
        File fontDir = new File(System.getProperty("user.dir"), "fonts");

        // 确保目标目录存在
        if (!fontDir.exists() && !fontDir.mkdirs()) {
            System.err.println("无法创建字体目录: " + fontDir);
            return;
        }

        // 检查目录是否为空
        File[] existingFiles = fontDir.listFiles(File::isFile);
        if (existingFiles != null && existingFiles.length > 0) {
            return; // 目录非空，无需操作
        }

        // 从 JAR 资源复制字体
        try {
            URL resourceUrl = getClass().getResource("/fonts");
            if (resourceUrl == null) {
                System.err.println("JAR 中未找到字体资源");
                return;
            }

            // 处理 JAR 内的资源文件
            if ("jar".equals(resourceUrl.getProtocol())) {
                String jarPath = resourceUrl.getPath().split("!")[0].replace("file:", "");
                try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();

                        if (name.startsWith("fonts/") && !entry.isDirectory()) {
                            String fileName = name.substring(name.lastIndexOf('/') + 1);
                            File destFile = new File(fontDir, fileName);

                            // 使用兼容 Java 8 的流复制方式
                            try (InputStream in = jar.getInputStream(entry);
                                 OutputStream out = new FileOutputStream(destFile)) {

                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, bytesRead);
                                }
                                System.out.println("复制字体: " + fileName);
                            }
                        }
                    }
                }
            }
            // 处理 IDE 环境中的资源
            else if ("file".equals(resourceUrl.getProtocol())) {
                File sourceDir = new File(resourceUrl.toURI());
                for (File srcFile : Objects.requireNonNull(sourceDir.listFiles())) {
                    if (srcFile.isFile()) {
                        File destFile = new File(fontDir, srcFile.getName());
                        Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("字体复制失败: " + e.getMessage());
            e.printStackTrace();
        }
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
