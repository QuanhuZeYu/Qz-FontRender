package club.heiqi.qz_fontrender;

import club.heiqi.qz_fontrender.fontsystem.CharImageGenerator;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        CharImageGenerator.getInstance().register();
    }
}
