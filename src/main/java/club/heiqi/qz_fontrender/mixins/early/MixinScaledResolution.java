package club.heiqi.qz_fontrender.mixins.early;

import club.heiqi.qz_fontrender.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScaledResolution.class)
public abstract class MixinScaledResolution {

    @Shadow private int scaledWidth;
    @Shadow private int scaledHeight;
    @Shadow private double scaledWidthD;
    @Shadow private double scaledHeightD;
    @Shadow private int scaleFactor;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(Minecraft minecraft, int width, int height, CallbackInfo ci) {
        // 初始化缩放后的宽度为物理宽度（尚未应用任何缩放）
        this.scaledWidth = width;

        // 初始化缩放后的高度为物理高度（尚未应用任何缩放）
        this.scaledHeight = height;

        // 初始化缩放因子为1（表示无额外缩放）
        this.scaleFactor = 1;

        // 获取当前字体渲染器是否使用Unicode字体（某些语言如中文需要特殊处理）
        boolean isUnicode = Minecraft.getMinecraft().fontRenderer != null && Minecraft.getMinecraft().fontRenderer.unicodeFlag;

        // 从游戏设置中获取GUI缩放级别
        // guiScale值含义: 0=自动, 1=最小, 2=正常, 3=大, 4=最大
        float guiScale = Minecraft.getMinecraft().gameSettings.guiScale;
        if (Config.guiScaleFix) {
            guiScale = (float) Config.guiScale;
        }

        // 处理自动缩放模式（guiScale = 0）
        // 将自动模式设置为一个非常大的值（1000），以便在后续循环中找到最大可能的缩放因子
        if (guiScale == 0) {
            guiScale = 1000;
        }

        // 计算最大合适的缩放因子
        // 循环条件：当前缩放因子小于目标缩放级别，且进一步缩放后宽度仍不低于320像素，高度仍不低于240像素
        // 320x240是Minecraft界面设计的基本最小尺寸，确保界面元素不会变得太小而无法使用
        while (this.scaleFactor < guiScale &&
                this.scaledWidth / (this.scaleFactor + 1) >= 320 &&
                this.scaledHeight / (this.scaleFactor + 1) >= 240) {
            // 增加缩放因子，尝试更大的缩放级别
            ++this.scaleFactor;
        }

        // 处理Unicode字体的特殊要求
        // 某些Unicode字体在奇数缩放因子下渲染效果不佳，因此需要调整为偶数
        // 但不会将缩放因子降低到1（最小有效值）
        // if (isUnicode && this.scaleFactor % 2 != 0 && this.scaleFactor != 1) {
        //     // 如果是Unicode字体且当前缩放因子为奇数且不为1，则减1使其变为偶数
        //     --this.scaleFactor;
        // }

        // 计算精确的缩放后尺寸（双精度浮点数）
        // 将物理尺寸除以缩放因子，得到理论上的界面尺寸
        this.scaledWidthD = (double)this.scaledWidth / (double)this.scaleFactor;
        this.scaledHeightD = (double)this.scaledHeight / (double)this.scaleFactor;

        // 将双精度尺寸向上取整为整数尺寸
        // 确保所有界面元素都能完整渲染，避免因舍入导致的部分像素缺失
        this.scaledWidth = (int) Math.ceil(this.scaledWidthD);
        this.scaledHeight = (int) Math.ceil(this.scaledHeightD);
    }
}
