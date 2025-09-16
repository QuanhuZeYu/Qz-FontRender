package club.heiqi.qz_fontrender;

import club.heiqi.qz_fontrender.client.TTFFontManager;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    public TTFFontManager fontManager = new TTFFontManager();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        fontManager.registry();
    }
}
