package club.heiqi.qz_fontrender.fontSystem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FontManager {
    public static Logger LOG = LogManager.getLogger();
    public float fontSize = 32f;
    /**存储所有可用的awt字体对象*/
    public LinkedHashSet<Font> fonts = new LinkedHashSet<>();

    public FontManager(float fontSize) {
        this.fontSize = fontSize;
        initFontAssets();
        loadAssetsFontsTTF();
        // 不再载入系统字体保持可控性
        // loadInstalledFontsTTF();
    }

    public Font findSuitable(int codepoint, int type) {
        for (Font font : fonts) {
            if (type == EnumFontType.NORMAL && !font.getName().toLowerCase().contains("bold") && checkFontCanDisplay(font, codepoint)) {
                return font.deriveFont(Font.PLAIN);
            }
            if (type == EnumFontType.BOLD && font.getName().toLowerCase().contains("bold") && checkFontCanDisplay(font, codepoint)) {
                return font.deriveFont(Font.BOLD);
            }
        }

        // 兜底显示
        for (Font font : fonts) {
            if (font.canDisplay(codepoint)) {
                return font;
            }
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
                "fonts/10_NotoSerifCJKsc-VF.ttf",
                "fonts/11_seguiemj.ttf",
                "fonts/12_segmdl2.ttf",
                "fonts/12_SegoeIcons.ttf",
                "fonts/12_segoepr.ttf",
                "fonts/12_segoeprb.ttf",
                "fonts/12_segoesc.ttf",
                "fonts/12_segoescb.ttf",
                "fonts/12_segoeui.ttf",
                "fonts/12_segoeuib.ttf",
                "fonts/12_segoeuii.ttf",
                "fonts/12_segoeuil.ttf",
                "fonts/12_segoeuisl.ttf",
                "fonts/12_segoeuiz.ttf",
                "fonts/12_seguibl.ttf",
                "fonts/12_seguibli.ttf",
                "fonts/12_seguihis.ttf",
                "fonts/12_seguili.ttf",
                "fonts/12_seguisb.ttf",
                "fonts/12_seguisbi.ttf",
                "fonts/12_seguisli.ttf",
                "fonts/12_seguisym.ttf",
                "fonts/12_SegUIVar.ttf",
                "fonts/13_MaterialSymbolsSharp-VariableFont_FILL,GRAD,opsz,wght.ttf"
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

    public boolean checkFontCanDisplay(Font font, int codepoint) {
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
        // 字形信息
        GlyphVector glyphVector = font.createGlyphVector(frc, new String(Character.toChars(codepoint)));

        // 检查字形代码 - 如果为0，通常表示缺失字形
        int glyphCode = glyphVector.getGlyphCode(0);
        if (glyphCode == 0 || glyphCode == font.getMissingGlyphCode()) {
            return false;
        }

        // 检查字形轮廓
        if (glyphVector.getGlyphOutline(0) == null) {
            return false;
        }

        return true;
    }

    public void reload() {
        fonts.clear();
        loadAssetsFontsTTF();
        // loadInstalledFontsTTF();
    }
}
