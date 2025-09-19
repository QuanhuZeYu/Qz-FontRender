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
import java.nio.file.*;
import java.util.*;
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

        List<String> jarList = Arrays.asList(
                "fonts/LXGWWenKai-Light.ttf",
                "fonts/LXGWWenKai-Medium.ttf",
                "fonts/LXGWWenKai-Regular.ttf",
                "fonts/LXGWWenKaiMono-Light.ttf",
                "fonts/LXGWWenKaiMono-Medium.ttf",
                "fonts/LXGWWenKaiMono-Regular.ttf",
                "fonts/segoeui.ttf",
                "fonts/segoeuib.ttf",
                "fonts/segoeuii.ttf",
                "fonts/segoeuil.ttf",
                "fonts/segoeuisl.ttf",
                "fonts/segoeuiz.ttf",
                "fonts/seguibl.ttf",
                "fonts/seguibli.ttf",
                "fonts/seguiemj.ttf",
                "fonts/seguihis.ttf",
                "fonts/seguili.ttf",
                "fonts/seguisb.ttf",
                "fonts/seguisbi.ttf",
                "fonts/seguisli.ttf",
                "fonts/seguisym.ttf",
                "fonts/SegUIVar.ttf"
        );
        for (String jarFile : jarList) {
            File saveFile = new File(fontDir, jarFile.split("/")[1]);

            try {
                moveFileFromJar(jarFile, saveFile.getAbsolutePath());
            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    public void moveFileFromJar(String jarInternalPath, String targetPath) throws IOException {
        // 确保路径格式正确
        String internalPath = jarInternalPath.startsWith("/") ? jarInternalPath : "/" + jarInternalPath;

        try (InputStream inputStream = this.getClass().getResourceAsStream(internalPath)) {
            if (inputStream == null) {
                throw new IOException("文件未找到于Jar内: " + internalPath);
            }

            Path target = Paths.get(targetPath);

            // 创建目标目录（如果不存在）
            Files.createDirectories(target.getParent());

            // 复制文件内容
            try (OutputStream outputStream = Files.newOutputStream(target,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }

        // 注意：无法从运行的Jar中删除源文件，这实际上是一个复制操作
        // 如果需要真正移动（删除原文件），需要特殊处理Jar文件本身
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
                name.toLowerCase().endsWith(".otf") ||
                name.toLowerCase().endsWith(".ttc"));

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

    public void reload() {
        fonts.clear();
        loadAssetsFontsTTF();
        loadInstalledFontsTTF();
    }
}
