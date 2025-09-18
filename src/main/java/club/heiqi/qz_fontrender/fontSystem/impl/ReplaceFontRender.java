package club.heiqi.qz_fontrender.fontSystem.impl;

import club.heiqi.qz_fontrender.fontSystem.CharacterGenFactory;
import club.heiqi.qz_fontrender.fontSystem.CharacterInfo;
import club.heiqi.qz_fontrender.fontSystem.CharacterTexturePage;
import club.heiqi.qz_fontrender.fontSystem.FontManager;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;

public class ReplaceFontRender extends FontRenderer {
    public static final float DEFAULT_CHAR_WIDTH = 8f;
    public CharacterGenFactory factory;
    public FontManager fontManager;

    public ReplaceFontRender(GameSettings gameSettings, ResourceLocation location, TextureManager manager, boolean b,
                             int textureWidth, int textureHeight, int charWidth, int charHeight, int maintainPool,
                             float fontSize
    ) {
        super(gameSettings, location, manager, b);
        fontManager = new FontManager(fontSize);
        factory = new CharacterGenFactory(fontManager, textureWidth, textureHeight, charWidth, charHeight, maintainPool);
    }

    /**渲染指定颜色的对应字符*/
    // public float renderCharAtPos(int codepoint, int color, char style, float x, float y) {
    //     // 1. 找到对应字符的纹理页
    //     CharacterTexturePage page = factory.getPage(codepoint, fontManager.findSuitable(codepoint));
    //     if (page == null) return DEFAULT_CHAR_WIDTH;  // page=null 表示字符仍在生成中
    //
    //     // 2. 使用纹理页渲染字符
    //     CharacterInfo characterInfo = page.renderChar(codepoint, color, x, y, 8f, 8f);
    //     return characterInfo.advanceX();
    // }

    /**解析字符串以渲染，主要识别 § `0123456789abcdefklmnor` 17字符+5控制字符?
     * k = 随机化
     * l = 粗体
     * m = 删除线样式
     * n = 下划线
     * o = 斜体
     * r = 重置*/
    private void renderStringAtPos(String s, boolean shadow) {
        // 1. 以§做分割
        String[] splits = s.split("(?=§)");
        String randomSample = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000 " +
                "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000" +
                "ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀" +
                "αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u0000";

        // 2. 操作分割后的单元
        int red = (int) (this.red * 255), green = (int) (this.blue * 255), blue = (int) (this.green * 255);
        for (String split : splits) {
            // 2.1 先检查单元中是否有操作符
            if (split.startsWith("§") && split.length() >= 2) {
                char controlChar = split.toLowerCase().charAt(1);
                switch (controlChar) {
                    case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'  -> {
                        this.randomStyle = false;
                        this.boldStyle = false;
                        this.strikethroughStyle = false;
                        this.underlineStyle = false;
                        this.italicStyle = false;
                        int colorIndex = "0123456789abcdefklmnor".indexOf(controlChar);
                        if (shadow) colorIndex = colorIndex + 16;
                        int color = colorCode[colorIndex];
                        red = (color >> 16) & 255;
                        green = (color >> 8) & 255;
                        blue = color & 255;
                        setColor(red / 255f, (green & 255) / 255f, blue / 255f, alpha);
                    }
                    case 'k' -> {
                        randomStyle = true;
                    }
                    case 'l' -> {
                        boldStyle = true;
                    }
                    case 'm' -> {
                        strikethroughStyle = true;
                    }
                    case 'n' -> {
                        underlineStyle = true;
                    }
                    case 'o' -> {
                        italicStyle = true;
                    }
                    case 'r' -> {
                        this.randomStyle = false;
                        this.boldStyle = false;
                        this.strikethroughStyle = false;
                        this.underlineStyle = false;
                        this.italicStyle = false;
                        setColor(this.red, this.blue, this.green, this.alpha);
                    }
                }
            }


            // 2.2 提取剩余无操作符文字
            if (split.startsWith("§") && split.length() <= 2) continue;
            String text = split;
            if (split.startsWith("§")) text = split.substring(2);
            // 遍历字符
            for (int i = 0; i < text.length(); i++) {
                int codepoint = text.codePointAt(i);
                char[] chars = Character.toChars(codepoint);
                final char trueChar = chars[0];
                final String trueCharacter = new String(chars);
                int randomCharIndex = trueChar;

                // 处理随机化字符
                if (randomStyle) {
                    int indexOf = randomSample.indexOf(trueChar);
                    do {
                        randomCharIndex = fontRandom.nextInt(charWidth.length);
                    }
                    while (charWidth[indexOf] != charWidth[randomCharIndex]);

                    randomCharIndex = indexOf;
                }

                // 2.2.1 处理unicode和阴影的位置偏移
                float offset = unicodeFlag ? 0.5f : 1.0f;

                // ========== 渲染 ==========
                if (shadow) {
                    posX -= offset;
                    posY -= offset;
                }

                CharacterTexturePage page = factory.getPageOrGenChar(codepoint);
                // 如果没有找到则跳过 并还原坐标
                if (page == null) {
                    posX += offset;
                    posY += offset;
                    posX += 4.0f; // 多一个空格大小表示占位
                    continue;
                }
                int color = (((int)(alpha*255)) << 24) | red << 16 | green << 8 | blue;
                CharacterInfo info = page.renderChar(codepoint, color, posX, posY, 8f, 8f);
                float charWidth = (info.advanceX() / info.width()) * 8f;

                if (shadow) {
                    posX += offset;
                    posY += offset;
                }
                // ========== 渲染 ==========

                // 2.2.2 处理粗体
                if (boldStyle) {
                    posX += offset;

                    if (shadow) {
                        posX -= offset;
                        posY -= offset;
                    }

                    page.renderChar(codepoint, textColor, posX, posY, 8f, 8f);

                    if (shadow) {
                        posX += offset;
                        posY += offset;
                    }

                    posX -= offset;
                }

                // 2.2.3 处理下划线等情况
                doDraw(charWidth);
            }
        }
    }

    // region 私有方法做空
    // @Override
    // protected void bindTexture(ResourceLocation location) {
    //     throw new RuntimeException("DO NOT CALL THIS");
    // }
    //
    // @Override
    // protected void doDraw(float f) {
    //     throw new RuntimeException("DO NOT CALL THIS");
    // }
    //
    // @Override
    // protected void enableAlpha() {
    //     throw new RuntimeException("DO NOT CALL THIS");
    // }
    //
    // @Override
    // protected InputStream getResourceInputStream(ResourceLocation location) throws IOException {
    //     throw new RuntimeException("DO NOT CALL THIS");
    // }
    //
    // @Override
    // protected float renderDefaultChar(int p_78266_1_, boolean p_78266_2_) {
    //     throw new RuntimeException("DO NOT CALL THIS");
    // }
    //
    // @Override
    // protected float renderUnicodeChar(char p_78277_1_, boolean p_78277_2_) {
    //     throw new RuntimeException("DO NOT CALL THIS");
    // }
    //
    // @Override
    // protected void setColor(float r, float g, float b, float a) {
    //     throw new RuntimeException("DO NOT CALL THIS");
    // }
    // endregion
}
