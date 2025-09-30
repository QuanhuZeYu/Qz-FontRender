package club.heiqi.qz_fontrender.mixins.early;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public class MixinNewFontRenderer01 {

    // @Inject(
    //         method = "startGame",
    //         at = @At(
    //                 value = "FIELD",
    //                 target = "Lnet/minecraft/client/Minecraft;standardGalacticFontRenderer:Lnet/minecraft/client/gui/FontRenderer;",
    //                 opcode = Opcodes.PUTFIELD,
    //                 shift = At.Shift.AFTER
    //         )
    // )
    // private void afterStandardGalacticFontRenderer(CallbackInfo ci) {
    //     Minecraft mc = ((Minecraft) (Object) this);
    //     FontRenderer fontRenderer = mc.fontRenderer;
    //
    //     FontRenderer replacer = new ReplaceFontRender(
    //             mc.gameSettings, fontRenderer.locationFontTexture, mc.renderEngine, false,
    //             2048,2048,64,64,5,64f*0.8f
    //     );
    //     mc.standardGalacticFontRenderer = replacer;
    //     mc.fontRenderer = replacer;
    // }
}
