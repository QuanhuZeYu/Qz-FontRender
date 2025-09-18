package club.heiqi.qz_fontrender.fontSystem.impl;

import club.heiqi.qz_fontrender.fontSystem.CharacterGenFactory;
import club.heiqi.qz_fontrender.fontSystem.CharacterInfo;
import club.heiqi.qz_fontrender.fontSystem.CharacterTexturePage;
import club.heiqi.qz_fontrender.fontSystem.FontManager;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class ReplaceFontRender extends FontRenderer {
    public static final float DEFAULT_CHAR_WIDTH = 8f;
    public float curCharWidth = DEFAULT_CHAR_WIDTH;
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



    @Override
    public int drawStringWithShadow(String text, int x, int y, int color) {
        return drawString(text, x, y, color, true);
    }

    @Override
    public int drawString(String text, int x, int y, int color) {
        return drawString(text, x, y, color, false);
    }

    @Override
    public int drawString(String text, int x, int y, int color, boolean dropShadow) {
        enableAlpha();
        this.resetStyles();
        int l;

        if (dropShadow)
        {
            l = this.renderString(text, x + 1, y + 1, color, true);
            l = Math.max(l, this.renderString(text, x, y, color, false));
        }
        else
        {
            l = this.renderString(text, x, y, color, false);
        }

        return l;
    }

    @Override
    public int getStringWidth(String text) {
        float width = 0;
        String[] splits = text.split("(?=§)");
        for (String split : splits) {
            // 提取无操作符文字
            if (split.startsWith("§") && split.length() <= 2) continue;
            String s = split;
            if (split.startsWith("§")) s = split.substring(2);
            // 遍历分割单元内的字符
            for (int i = 0; i < s.length() - 1;) {
                int codepoint = text.codePointAt(i);
                int charCountInCodePoint = Character.charCount(codepoint);
                i += charCountInCodePoint;

                CharacterTexturePage page = factory.getPageOrGenChar(codepoint);
                // 如果没找到
                if (page == null) {
                    width += 4f;
                    continue;
                }
                CharacterInfo info = page.getInfo(codepoint);
                width += info.advanceX()/info.width()*this.curCharWidth;
            }
        }
        return (int) width;
    }

    @Override
    public int getCharWidth(char character) {
        String s = String.valueOf(character);
        int codepoint = s.codePointAt(0);
        if (s.equals("§")) return -1;
        if (s.equals(" ")) return (int) (this.curCharWidth/2);

        CharacterTexturePage page = factory.getPageOrGenChar(codepoint);
        if (page == null) {
            return (int) (this.curCharWidth/2);
        }
        CharacterInfo info = page.getInfo(codepoint);
        return (int) Math.ceil(info.advanceX()/info.width()*this.curCharWidth);
    }

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
            for (int i = 0; i < text.length();) {
                int codepoint = text.codePointAt(i);
                char[] chars = Character.toChars(codepoint);
                int charCountInCodePoint = Character.charCount(codepoint);
                i += charCountInCodePoint;
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
                    doDraw(4f);
                    continue;
                }
                int color = (((int)(alpha*255)) << 24) | red << 16 | green << 8 | blue;
                CharacterInfo info = page.renderChar(codepoint, color, posX, posY, this.curCharWidth, this.curCharWidth);
                float charWidth = info.advanceX() / info.width() * this.curCharWidth;
                if (trueCharacter.equals(" ")) charWidth = this.curCharWidth/2;

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

                    page.renderChar(codepoint, textColor, posX, posY, this.curCharWidth, this.curCharWidth);

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

    protected void doDraw(float f) {
        Tessellator tessellator;

        if (this.strikethroughStyle) {
            tessellator = Tessellator.instance;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            tessellator.startDrawingQuads();
            tessellator.addVertex((double)this.posX, (double)(this.posY + (float)(this.FONT_HEIGHT / 2)), 0.0D);
            tessellator.addVertex((double)(this.posX + f), (double)(this.posY + (float)(this.FONT_HEIGHT / 2)), 0.0D);
            tessellator.addVertex((double)(this.posX + f), (double)(this.posY + (float)(this.FONT_HEIGHT / 2) - 1.0F), 0.0D);
            tessellator.addVertex((double)this.posX, (double)(this.posY + (float)(this.FONT_HEIGHT / 2) - 1.0F), 0.0D);
            tessellator.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }

        if (this.underlineStyle) {
            tessellator = Tessellator.instance;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            tessellator.startDrawingQuads();
            int l = this.underlineStyle ? -1 : 0;
            tessellator.addVertex((double)(this.posX + (float)l), (double)(this.posY + (float)this.FONT_HEIGHT), 0.0D);
            tessellator.addVertex((double)(this.posX + f), (double)(this.posY + (float)this.FONT_HEIGHT), 0.0D);
            tessellator.addVertex((double)(this.posX + f), (double)(this.posY + (float)this.FONT_HEIGHT - 1.0F), 0.0D);
            tessellator.addVertex((double)(this.posX + (float)l), (double)(this.posY + (float)this.FONT_HEIGHT - 1.0F), 0.0D);
            tessellator.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }

        this.posX += (float)((int)f);
    }

    private void resetStyles() {
        this.randomStyle = false;
        this.boldStyle = false;
        this.italicStyle = false;
        this.underlineStyle = false;
        this.strikethroughStyle = false;
    }

    private int renderString(String text, int x, int y, int color, boolean shadow) {
        if (text == null) {
            return 0;
        }
        else {
            if (this.bidiFlag) {
                text = this.bidiReorder(text);
            }

            if ((color & 0b1111_1100_0000_0000_0000_0000_0000_0000) == 0) {
                color |= 0b1111_1111_0000_0000_0000_0000_0000_0000;
            }

            if (shadow) {
                color = (color & 0b1111_1100_1111_1100_1111_1100) >> 2 | color & 0b1111_1111_0000_0000_0000_0000_0000_0000;
            }

            this.red = (float)(color >> 16 & 255) / 255.0F;
            this.blue = (float)(color >> 8 & 255) / 255.0F;
            this.green = (float)(color & 255) / 255.0F;
            this.alpha = (float)(color >> 24 & 255) / 255.0F;
            setColor(this.red, this.blue, this.green, this.alpha);
            this.posX = (float)x;
            this.posY = (float)y;
            this.renderStringAtPos(text, shadow);
            return (int)this.posX;
        }
    }

    private String bidiReorder(String p_147647_1_) {
        try {
            Bidi bidi = new Bidi((new ArabicShaping(8)).shape(p_147647_1_), 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);
        }
        catch (ArabicShapingException arabicshapingexception) {
            return p_147647_1_;
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
