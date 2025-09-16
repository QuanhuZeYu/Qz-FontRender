package club.heiqi.qz_fontrender.client;

import club.heiqi.qz_fontrender.ClientProxy;
import club.heiqi.qz_fontrender.MyMod;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjglr.BufferUtils;
import org.lwjglr.stb.STBTTFontinfo;
import org.lwjglr.stb.STBTruetype;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TTFFontManager {
    private static final Logger LOG = LogManager.getLogger();

    // 使用包装类管理字体资源
    private final List<FontResource> fontResources = new ArrayList<>();

    public static class FontResource {
        public final STBTTFontinfo fontInfo;
        public final ByteBuffer fontBuffer;
        public final String fontName;

        public FontResource(STBTTFontinfo fontInfo, ByteBuffer fontBuffer, String fontName) {
            this.fontInfo = fontInfo;
            this.fontBuffer = fontBuffer;
            this.fontName = fontName;
        }

        public void dispose() {
            if (fontInfo != null) {
                fontInfo.free();
            }
        }
    }

    public void loadAssetsFontsTTF() {
        File fontDir = new File(System.getProperty("user.dir"), "fonts");
        if (!fontDir.exists() || !fontDir.isDirectory()) fontDir.mkdirs();

        File[] fontFiles = fontDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".ttf"));

        if (fontFiles == null) {
            LOG.warn("文件夹中未找到字体文件: " + fontDir);
            return;
        }

        loadTTF(fontFiles);
    }

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

        // 遍历字体目录
        File[] fontFiles = fontDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".ttf"));

        if (fontFiles == null) {
            LOG.warn("文件夹中未找到字体文件: " + fontDirPath);
            return;
        }

        LOG.info("找到 {} 个字体文件,在 {}", fontFiles.length, fontDirPath);

        loadTTF(fontFiles);
    }

    public void loadTTF(File[] files) {
        int loadedCount = 0;
        for (File fontFile : files) {
            try {
                byte[] fontBytes = Files.readAllBytes(fontFile.toPath());
                ByteBuffer fontBuffer = BufferUtils.createByteBuffer(fontBytes.length);
                fontBuffer.put(fontBytes).flip();

                STBTTFontinfo fontInfo = STBTTFontinfo.create();
                if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) {
                    LOG.error("字体信息初始化失败: {}", fontFile.getName());
                    fontInfo.free(); // 释放失败创建的资源
                    continue; // 继续下一个字体
                }

                fontResources.add(new FontResource(fontInfo, fontBuffer, fontFile.getName()));
                loadedCount++;

            } catch (IOException e) {
                LOG.error("加载字体失败: {} - {}", fontFile.getName(), e.getMessage());
            } catch (Exception e) {
                LOG.error("加载字体时遇到未知错误: {}", fontFile.getName(), e);
            }
        }

        LOG.info("成功加载字体 {}/{}", loadedCount, files.length);
    }

    public void dispose() {
        LOG.info("Disposing {} font resources", fontResources.size());
        for (FontResource resource : fontResources) {
            resource.dispose();
        }
        fontResources.clear();
    }

    @SubscribeEvent
    public void onRenderTickEvent(TickEvent.RenderTickEvent event) {
        TTFFontManager manager = ((ClientProxy)MyMod.proxy).fontManager;
        manager.loadAssetsFontsTTF();
        manager.loadInstalledFontsTTF();
        unRegistry();
    }

    public void registry() {
        FMLCommonHandler.instance().bus().register(this);
    }

    public void unRegistry() {
        FMLCommonHandler.instance().bus().unregister(this);
    }
}
