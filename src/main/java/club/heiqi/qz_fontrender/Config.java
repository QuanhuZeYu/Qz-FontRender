package club.heiqi.qz_fontrender;

import java.io.File;

import club.heiqi.qz_fontrender.fontSystem.impl.ReplaceFontRender;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

public class Config {
    public static String configPath;
    public static Configuration config;

    public static boolean guiScaleFix;
    public static boolean backendGeneration;


    public static int sampleOffset, minSamplesPerAxis, maxSamplesPerAxis;


    public static float characterSpacing;
    public static float spaceWidth;
    public static float shadowOffsetX;
    public static float shadowOffsetY;
    public static float charSize;
    public static float lineSpacing;

    public static float guiScale;
    public static float smoothRangeMin, smoothRangeMax;
    public static float internalAlphaThreshold, sampleMultiplier, coverageEpsilon, aa_strength, strength, sigma, blackThreshold;

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


        sampleOffset = config.getInt("sampleOffset", Configuration.CATEGORY_GENERAL, 1, 1, 10, "采样计算中的偏移量");
        minSamplesPerAxis = config.getInt("minSamplesPerAxis", Configuration.CATEGORY_GENERAL, 5, 1, 10, "每个轴最小采样次数");
        maxSamplesPerAxis = config.getInt("maxSamplesPerAxis", Configuration.CATEGORY_GENERAL, 16, 1, 32, "每个轴最大采样次数");


        guiScale = config.getFloat("guiScale", Configuration.CATEGORY_GENERAL, 3.0f, Float.MIN_VALUE, Float.MAX_VALUE, "界面缩放(注意！此值会覆盖原版设定值！)");
        characterSpacing = config.getFloat("characterSpacing", Configuration.CATEGORY_GENERAL, 0.1f, Float.MIN_VALUE, Float.MAX_VALUE, "字间距");
        spaceWidth = config.getFloat("spaceWidth", Configuration.CATEGORY_GENERAL, ReplaceFontRender.DEFAULT_CHAR_WIDTH/2f, Float.MIN_VALUE, Float.MAX_VALUE, "空格宽度");
        shadowOffsetX = config.getFloat("shadowOffsetX", Configuration.CATEGORY_GENERAL, 0.2f, -Float.MAX_VALUE, Float.MAX_VALUE, "投影位置偏移X");
        shadowOffsetY = config.getFloat("shadowOffsetY", Configuration.CATEGORY_GENERAL, 0.2f, -Float.MAX_VALUE, Float.MAX_VALUE, "投影位置偏移Y");
        charSize = config.getFloat("charSize", Configuration.CATEGORY_GENERAL, 9f, -Float.MAX_VALUE, Float.MAX_VALUE, "字体大小");
        lineSpacing = config.getFloat("lineSpacing", Configuration.CATEGORY_GENERAL, 1.0f, -Float.MAX_VALUE, Float.MAX_VALUE, "行间距");
        smoothRangeMin = config.getFloat("smoothRangeMin", Configuration.CATEGORY_GENERAL, 0.2f, 0.0f, 1.0f, "平滑范围(控制alpha平滑区间)");
        smoothRangeMax = config.getFloat("smoothRangeMax", Configuration.CATEGORY_GENERAL, 0.8f, 0.0f, 1.0f, "平滑范围(控制alpha平滑区间)");
        internalAlphaThreshold = config.getFloat("internalAlphaThreshold", Configuration.CATEGORY_GENERAL, 0.99f, 0.0f, 1.0f, "字符内部实心却与的alpha判断阈值");
        sampleMultiplier = config.getFloat("sampleMultiplier", Configuration.CATEGORY_GENERAL, 1.5f, 0.0f, 1.0f, "采样次数计算中的乘法因子");
        coverageEpsilon = config.getFloat("coverageEpsilon", Configuration.CATEGORY_GENERAL, 0.0001f, 0.0f, 1.0f, "覆盖度除零避免的Epsilon值");
        aa_strength = config.getFloat("aa_strength", Configuration.CATEGORY_GENERAL, 1.0f, 0.0f, 10.0f, "0=无AA，1=标准");
        strength = config.getFloat("strength", Configuration.CATEGORY_GENERAL, 8.0f, 0.0f, 64.0f, "采样强度系数");
        sigma = config.getFloat("sigma", Configuration.CATEGORY_GENERAL, 1.0f, 0.0f, 10.0f, "高斯标准差");
        blackThreshold = config.getFloat("blackThreshold", Configuration.CATEGORY_GENERAL, 0.3f, 0.0f, 1.0f, "判定黑色阈值");

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
