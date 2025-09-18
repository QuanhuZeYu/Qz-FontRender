package club.heiqi.qz_fontrender.mixins.early;

import club.heiqi.qz_fontrender.fontSystem.impl.ReplaceFontRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinNewFontRenderer01 {

    @Inject(
            method = "startGame",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;standardGalacticFontRenderer:Lnet/minecraft/client/gui/FontRenderer;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void afterStandardGalacticFontRenderer(CallbackInfo ci) {
        Minecraft mc = ((Minecraft) (Object) this);
        FontRenderer fontRenderer = mc.fontRenderer;

        FontRenderer replacer = new ReplaceFontRender(
                mc.gameSettings, fontRenderer.locationFontTexture, mc.renderEngine, false,
                2048,2048,32,32,5,32f*0.8f
        );
        mc.standardGalacticFontRenderer = replacer;
        mc.fontRenderer = replacer;
    }
}
