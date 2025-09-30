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

    public static boolean guiScaleFix, backendGeneration, prepareTipRender;


    public static int sampleRadius;


    public static float characterSpacing;
    public static float spaceWidth;
    public static float shadowOffsetX;
    public static float shadowOffsetY;
    public static float charSize;
    public static float lineSpacing;

    public static float guiScale;
    public static float sigma, blurRadius, smoothRangeMin, smoothRangeMax;
    public static float awtCharSize;

    public void init(File configFile) {
        if (config == null) {
            configPath = configFile.getAbsolutePath();
            config = new Configuration(configFile);
        }
        load();
    }

    public void load() {
        backendGeneration = config.getBoolean("backendGeneration", Configuration.CATEGORY_GENERAL, true, "后台持续生成字符");
        guiScaleFix = config.getBoolean("guiScaleFix", Configuration.CATEGORY_GENERAL, false, "GUI缩放修复");
        prepareTipRender = config.getBoolean("prepareTipRender", Configuration.CATEGORY_GENERAL, true, "准备字体提示信息渲染");


        sampleRadius = config.getInt("sampleRadius", Configuration.CATEGORY_GENERAL, 1, 0, Integer.MAX_VALUE, "高斯模糊采样半径");


        guiScale = config.getFloat("guiScale", Configuration.CATEGORY_GENERAL, 3.0f, Float.MIN_VALUE, Float.MAX_VALUE, "界面缩放(注意！此值会覆盖原版设定值！)");
        characterSpacing = config.getFloat("characterSpacing", Configuration.CATEGORY_GENERAL, 0.1f, Float.MIN_VALUE, Float.MAX_VALUE, "字间距");
        spaceWidth = config.getFloat("spaceWidth", Configuration.CATEGORY_GENERAL, ReplaceFontRender.DEFAULT_CHAR_WIDTH/2f, Float.MIN_VALUE, Float.MAX_VALUE, "空格宽度");
        shadowOffsetX = config.getFloat("shadowOffsetX", Configuration.CATEGORY_GENERAL, 0.3f, -Float.MAX_VALUE, Float.MAX_VALUE, "投影位置偏移X");
        shadowOffsetY = config.getFloat("shadowOffsetY", Configuration.CATEGORY_GENERAL, 0.3f, -Float.MAX_VALUE, Float.MAX_VALUE, "投影位置偏移Y");
        charSize = config.getFloat("charSize", Configuration.CATEGORY_GENERAL, 9f, -Float.MAX_VALUE, Float.MAX_VALUE, "字体大小");
        lineSpacing = config.getFloat("lineSpacing", Configuration.CATEGORY_GENERAL, 1.0f, -Float.MAX_VALUE, Float.MAX_VALUE, "行间距");
        sigma = config.getFloat("sigma", Configuration.CATEGORY_GENERAL, 2.0f, 0.0f, 64.0f, "高斯核标准差");
        blurRadius = config.getFloat("blurRadius", Configuration.CATEGORY_GENERAL, 1f, 0.0f, 64.0f, "模糊程度");
        smoothRangeMin = config.getFloat("smoothRangeMin", Configuration.CATEGORY_GENERAL, 0f, 0.0f, 1.0f, "平滑最小参数");
        smoothRangeMax = config.getFloat("smoothRangeMax", Configuration.CATEGORY_GENERAL, 1f, 0.0f, 1.0f, "平滑最大参数");
        awtCharSize = config.getFloat("awtCharSize", Configuration.CATEGORY_GENERAL, 64f, 0.0f, Float.MAX_VALUE, "awt字体分辨率");

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
