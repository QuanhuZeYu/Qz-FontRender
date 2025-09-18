package club.heiqi.qz_fontrender.mixins.early;

import club.heiqi.qz_fontrender.fontSystem.impl.ReplaceFontRender;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FontRenderer.class, priority = 1001)
public abstract class MixinFontRenderer {

    // @Unique
    // public ReplaceFontRender qz_FontRender$replaceFontRender;
    //
    // @Inject(
    //         method = "<init>",
    //         at = @At("TAIL")
    // )
    // public void onConstructorTail(
    //         GameSettings settings,
    //         ResourceLocation location,
    //         TextureManager textureManager,
    //         boolean isUnicode,
    //         CallbackInfo ci
    // ) {
    //     qz_FontRender$replaceFontRender = new ReplaceFontRender(settings, location, textureManager, isUnicode);
    // }
}
