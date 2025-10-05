package club.heiqi.qz_fontrender;

import java.io.File;

import club.heiqi.qz_fontrender.fontsystem.impl.ReplaceFontRender;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

public class Config {
    public static String configPath;
    public static Configuration config;

    public static boolean guiScaleFix;


    public static int sampleRadius;


    public static double characterSpacing;
    public static double spaceWidth;
    public static double shadowOffsetX;
    public static double shadowOffsetY;
    public static double charSize;
    public static double lineSpacing;

    public static double guiScale;
    public static double sigma, blurRadius, smoothRangeMin, smoothRangeMax;
    public static double awtCharSize;
    public static double renderOffset;

    public static String[] fontSort = {};

    public void init(File configFile) {
        if (config == null) {
            configPath = configFile.getAbsolutePath();
            config = new Configuration(configFile);
        }
        load();
    }

    public void load() {
        guiScaleFix = config.getBoolean("guiScaleFix", Configuration.CATEGORY_GENERAL, false, "GUI缩放修复");


        sampleRadius = config.getInt("sampleRadius", Configuration.CATEGORY_GENERAL, 1, 0, Integer.MAX_VALUE, "高斯模糊采样半径");


        guiScale = config.get(Configuration.CATEGORY_GENERAL, "guiScale", 3.0, "界面缩放(注意！此值会覆盖原版设定值！)", Double.MIN_VALUE, Double.MAX_VALUE).getDouble();
        characterSpacing = config.get(Configuration.CATEGORY_GENERAL, "characterSpacing", 0.1, "字间距", Double.MIN_VALUE, Double.MAX_VALUE).getDouble();
        spaceWidth = config.get(Configuration.CATEGORY_GENERAL, "spaceWidth", ReplaceFontRender.DEFAULT_CHAR_WIDTH/2.0, "空格宽度", Double.MIN_VALUE, Double.MAX_VALUE).getDouble();
        shadowOffsetX = config.get(Configuration.CATEGORY_GENERAL, "shadowOffsetX", 0.3, "投影位置偏移X", -Double.MAX_VALUE, Double.MAX_VALUE).getDouble();
        shadowOffsetY = config.get(Configuration.CATEGORY_GENERAL, "shadowOffsetY", 0.3, "投影位置偏移Y", -Double.MAX_VALUE, Double.MAX_VALUE).getDouble();
        charSize = config.get(Configuration.CATEGORY_GENERAL, "charSize", 9.0, "字体大小", -Double.MAX_VALUE, Double.MAX_VALUE).getDouble();
        lineSpacing = config.get(Configuration.CATEGORY_GENERAL, "lineSpacing", 1.0, "行间距", -Double.MAX_VALUE, Double.MAX_VALUE).getDouble();
        sigma = config.get(Configuration.CATEGORY_GENERAL, "sigma", 2.0, "高斯核标准差", 0.0, 64.0).getDouble();
        blurRadius = config.get(Configuration.CATEGORY_GENERAL, "blurRadius", 1.0, "模糊程度", 0.0, 64.0).getDouble();
        smoothRangeMin = config.get(Configuration.CATEGORY_GENERAL, "smoothRangeMin", 0.0, "平滑最小参数", 0.0, 1.0).getDouble();
        smoothRangeMax = config.get(Configuration.CATEGORY_GENERAL, "smoothRangeMax", 1.0, "平滑最大参数", 0.0, 1.0).getDouble();
        awtCharSize = config.get(Configuration.CATEGORY_GENERAL, "awtCharSize", 64.0, "awt字体分辨率", 0.0, Double.MAX_VALUE).getDouble();
        renderOffset = config.get(Configuration.CATEGORY_GENERAL, "renderOffset", 0.1, "字符渲染偏移(前后偏移)", -Double.MAX_VALUE, Double.MAX_VALUE).getDouble();

        fontSort = config.get(Configuration.CATEGORY_GENERAL, "fontSort", fontSort).getStringList();

        // 设置字体大小
        ReplaceFontRender fontRender = ReplaceFontRender.getInstance();
        if (fontRender != null) {
            fontRender.setCharSize(charSize);
        }
        if (config.hasChanged()) {
            config.save();
        }
    }

    @SubscribeEvent
    public void onConfigChangeEvent(ConfigChangedEvent event) {
        if (!event.modID.equalsIgnoreCase(MyMod.MODID)) return;
        load();
    }

    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }
}
