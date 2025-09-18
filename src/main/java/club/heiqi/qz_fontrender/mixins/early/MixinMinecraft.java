package club.heiqi.qz_fontrender.mixins.early;

import club.heiqi.qz_fontrender.fontSystem.impl.ReplaceFontRender;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Unique
    public ReplaceFontRender qz_FontRender$replaceFontRender;

    @Inject(
            method = "runGameLoop",
            at = @At("HEAD")
    )
    public void perTick(CallbackInfo callbackInfo) {
        Minecraft mc = ((Minecraft)((Object)this));
        if (qz_FontRender$replaceFontRender == null) {
            qz_FontRender$replaceFontRender = new ReplaceFontRender(mc.gameSettings, mc.fontRenderer.locationFontTexture, mc.renderEngine, false,
                    2048, 2048, 32, 32, 5, 32*0.8f);
        }

        if (!(mc.fontRenderer instanceof ReplaceFontRender)) {
            mc.fontRenderer = qz_FontRender$replaceFontRender;
            System.out.println("Replace FontRenderer!");
        }
        if (!(mc.standardGalacticFontRenderer instanceof ReplaceFontRender)) {
            mc.fontRenderer = qz_FontRender$replaceFontRender;
            System.out.println("Replace FontRenderer!");
        }
    }
}
