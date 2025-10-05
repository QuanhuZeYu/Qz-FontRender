package club.heiqi.qz_fontrender.mixins.early;

import club.heiqi.qz_fontrender.fontsystem.impl.ReplaceFontRender;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(
            method = "runGameLoop",
            at = @At("HEAD")
    )
    public void perTick(CallbackInfo callbackInfo) {
        Minecraft mc = ((Minecraft)((Object)this));
        if (!(mc.fontRenderer instanceof ReplaceFontRender)) {
            mc.fontRenderer = ReplaceFontRender.getInstance();
            System.out.println("Replace FontRenderer!");
        }
        if (!(mc.standardGalacticFontRenderer instanceof ReplaceFontRender)) {
            mc.standardGalacticFontRenderer = ReplaceFontRender.getInstance();
            System.out.println("Replace FontRenderer!");
        }
    }
}
