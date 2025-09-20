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

    public static float characterSpacing = 0.1f;
    public static float spaceWidth = ReplaceFontRender.DEFAULT_CHAR_WIDTH/2f;
    public static float shadowOffsetX = -0.5f;
    public static float shadowOffsetY = -0.5f;

    public void init(File configFile) {
        if (config == null) {
            configPath = configFile.getAbsolutePath();
            config = new Configuration(configFile);
        }
        load();
    }

    public void load() {
        characterSpacing = config.getFloat("characterSpacing", Configuration.CATEGORY_GENERAL, 0.1f, Float.MIN_VALUE, Float.MAX_VALUE, "字间距");
        spaceWidth = config.getFloat("spaceWidth", Configuration.CATEGORY_GENERAL, ReplaceFontRender.DEFAULT_CHAR_WIDTH/2f, Float.MIN_VALUE, Float.MAX_VALUE, "空格宽度");
        shadowOffsetX = config.getFloat("shadowOffsetX", Configuration.CATEGORY_GENERAL, -0.5f, Float.MIN_VALUE, Float.MAX_VALUE, "投影位置偏移X");
        shadowOffsetY = config.getFloat("shadowOffsetY", Configuration.CATEGORY_GENERAL, -0.5f, Float.MIN_VALUE, Float.MAX_VALUE, "投影位置偏移Y");

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
