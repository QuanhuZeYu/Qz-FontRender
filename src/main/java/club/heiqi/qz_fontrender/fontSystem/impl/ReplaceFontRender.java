package club.heiqi.qz_fontrender.fontSystem.impl;

import club.heiqi.qz_fontrender.Config;
import club.heiqi.qz_fontrender.fontSystem.CharacterGenFactory;
import club.heiqi.qz_fontrender.fontSystem.CharacterInfo;
import club.heiqi.qz_fontrender.fontSystem.CharacterTexturePage;
import club.heiqi.qz_fontrender.fontSystem.FontManager;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ReplaceFontRender extends FontRenderer {
    public static final float DEFAULT_CHAR_WIDTH = 8f;
    public float curCharWidth = DEFAULT_CHAR_WIDTH;
    public float saveR, saveG, saveB, saveA;
    public CharacterGenFactory factory;
    public FontManager fontManager;

    public ReplaceFontRender(GameSettings gameSettings, ResourceLocation location, TextureManager manager, boolean b,
                             int textureWidth, int textureHeight, int charWidth, int charHeight, int maintainPool,
                             float fontSize
    ) {
        super(gameSettings, location, manager, b);
        fontManager = new FontManager(fontSize);
        factory = new CharacterGenFactory(fontManager, textureWidth, textureHeight, charWidth, charHeight, maintainPool);
        setUnicodeFlag(true);
    }



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
        this.enableAlpha();
        this.resetStyles();
        int xPos;

        if (dropShadow) {
            xPos = this.renderString(text, x, y, color, true);
            xPos = Math.max(xPos, this.renderString(text, x, y, color, false));
        }
        else {
            xPos = this.renderString(text, x, y, color, false);
        }

        return xPos;
    }

    @Override
    public int getStringWidth(String text) {
        float width = 0;
        if (text == null) return 0;
        String[] splits = text.split("(?=§)");
        for (String split : splits) {
            // 提取无操作符文字
            if (split.startsWith("§") && split.length() <= 2) continue;
            String s = split;
            if (split.startsWith("§")) s = split.substring(2);
            // 遍历分割单元内的字符
            for (int i = 0; i < s.length();) {
                int codepoint = text.codePointAt(i);
                int charCountInCodePoint = Character.charCount(codepoint);
                i += charCountInCodePoint;

                CharacterTexturePage page = factory.getPageOrGenChar(codepoint);
                // 如果没找到
                if (page == null) {
                    width += Config.spaceWidth;
                    continue;
                }
                CharacterInfo info = page.getInfo(codepoint);
                width += info.advanceX()/info.width()*this.curCharWidth + Config.characterSpacing;
            }
        }
        return (int) width;
    }

    @Override
    public int getCharWidth(char character) {
        String s = String.valueOf(character);
        int codepoint = s.codePointAt(0);
        if (s.equals(" ")) return (int) Config.spaceWidth;

        CharacterTexturePage page = factory.getPageOrGenChar(codepoint);
        if (page == null) {
            return (int) Config.spaceWidth;
        }
        CharacterInfo info = page.getInfo(codepoint);
        return (int) Math.ceil(info.advanceX()/info.width()*this.curCharWidth + Config.characterSpacing);
    }

    @Override
    public boolean getUnicodeFlag() {
        return this.unicodeFlag;
    }

    @Override
    public void drawSplitString(String str, int x, int y, int wrapWidth, int textColor) {
        this.resetStyles();
        this.textColor = textColor;
        str = trimStringNewline(str);
        renderSplitString(str, x, y, wrapWidth, false);
    }

    @Override
    public boolean getBidiFlag() {
        return this.bidiFlag;
    }

    @Override
    public List<String> listFormattedStringToWidth(String str, int wrapWidth) {
        return Arrays.asList(wrapFormattedStringToWidth(str, wrapWidth).split("\n"));
    }

    @Override
    public void onResourceManagerReload(IResourceManager p_110549_1_) {
        factory.reset();
    }

    @Override
    public void setBidiFlag(boolean bidiFlag) {
        this.bidiFlag = bidiFlag;
    }

    @Override
    public void setUnicodeFlag(boolean unicodeFlag) {
        this.unicodeFlag = unicodeFlag;
    }

    @Override
    public int splitStringWidth(String text, int wrapWidth) {
        return this.FONT_HEIGHT * this.listFormattedStringToWidth(text, wrapWidth).size();
    }

    @Override
    public String trimStringToWidth(String text, int targetWidth, boolean b) {
        StringBuilder stringbuilder = new StringBuilder();

        float width = 0;
        String[] splits = text.split("(?=§)");
        for (String split : splits) {
            // 提取无操作符文字
            if (split.startsWith("§") && split.length() <= 2) continue;
            String s = split;
            if (split.startsWith("§")) s = split.substring(2);
            // 遍历分割单元内的字符
            for (int i = 0; i < s.length();) {
                int codepoint = text.codePointAt(i);
                char[] chars = Character.toChars(codepoint);
                String trueCharacter = new String(chars);

                int charCountInCodePoint = Character.charCount(codepoint);

                CharacterTexturePage page = factory.getPageOrGenChar(codepoint);
                // 如果没找到
                if (page == null) {
                    width += Config.spaceWidth;
                }
                else {
                    CharacterInfo info = page.getInfo(codepoint);
                    width += info.advanceX() / info.width() * this.curCharWidth + Config.characterSpacing;
                }

                if (width > targetWidth) return stringbuilder.toString();
                if (width == targetWidth) return stringbuilder.toString();
                if (width < targetWidth) stringbuilder.append(trueCharacter);

                i += charCountInCodePoint;
            }
        }

        return stringbuilder.toString();
    }

    @Override
    public String trimStringToWidth(String p_78269_1_, int p_78269_2_) {
        return this.trimStringToWidth(p_78269_1_, p_78269_2_, false);
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
                        setColor(red / 255f, green / 255f, blue / 255f, alpha);
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
                        this.resetStyles();
                        red = (int) (saveR * 255); green = (int) (saveG * 255); blue = (int) (saveB * 255);
                    }
                    // 任何没有见过的操作符都视作重置！
                    default -> {
                        this.resetStyles();
                        red = (int) (saveR * 255); green = (int) (saveG * 255); blue = (int) (saveB * 255);
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

                // ========== 渲染 ==========

                CharacterTexturePage page = factory.getPageOrGenChar(codepoint);
                // 如果没有找到则跳过 并还原坐标
                if (page == null) {
                    doDraw(Config.spaceWidth);
                    continue;
                }
                int color = (((int)(alpha*255)) << 24) | red << 16 | green << 8 | blue;
                CharacterInfo info = page.renderChar(codepoint, color, posX, posY, this.curCharWidth, this.curCharWidth);
                float charWidth = info.advanceX() / info.width() * this.curCharWidth + Config.characterSpacing;
                if (trueCharacter.equals(" ")) charWidth = Config.spaceWidth;
                // ========== 渲染 ==========

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

    /**
     * 返回当前X坐标位置 即光标位置
     */
    private int renderString(String text, int x, int y, int color, boolean shadow) {
        float fx = x;
        float fy = y;
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
                fx += Config.shadowOffsetX;
                fy += Config.shadowOffsetY;
            }

            this.alpha = (float)(color >> 24 & 255) / 255.0F;   saveA = this.alpha;
            this.red = (float)(color >> 16 & 255) / 255.0F;     saveR = this.red;
            this.blue = (float)(color >> 8 & 255) / 255.0F;     saveG = this.blue;
            this.green = (float)(color & 255) / 255.0F;         saveB = this.green;
            setColor(this.red, this.blue, this.green, this.alpha);
            this.posX = fx;
            this.posY = fy;
            this.renderStringAtPos(text, shadow);
            return (int)this.posX;
        }
    }










    @Override
    protected void bindTexture(ResourceLocation location) {

    }

    @Override
    protected void enableAlpha() {
        GL11.glEnable(GL11.GL_ALPHA_TEST);
    }

    @Override
    protected InputStream getResourceInputStream(ResourceLocation location) throws IOException {
        return Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();
    }


    @Override
    protected void setColor(float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
    }






    private String trimStringNewline(String p_78273_1_) {
        while (p_78273_1_ != null && p_78273_1_.endsWith("\n")) {
            p_78273_1_ = p_78273_1_.substring(0, p_78273_1_.length() - 1);
        }
        return p_78273_1_;
    }

    private void renderSplitString(String str, int x, int y, int wrapWidth, boolean addShadow) {
        List<String> list = this.listFormattedStringToWidth(str, wrapWidth);

        for (Iterator<String> iterator = list.iterator(); iterator.hasNext(); y += this.FONT_HEIGHT) {
            String s1 = iterator.next();
            renderStringAligned(s1, x, y, wrapWidth, this.textColor, addShadow);
        }
    }

    private void renderStringAligned(String s, int x, int y, int wrapWidth, int color, boolean shadow) {
        if (this.bidiFlag) {
            int i1 = this.getStringWidth(this.bidiReorder(s));
            x = x + wrapWidth - i1;
        }

        this.renderString(s, x, y, color, shadow);
    }

    private String wrapFormattedStringToWidth(String str, int wrapWidth) {
        str = getFormatFromString(str);
        StringBuilder builder = new StringBuilder();

        float width = 0;
        for (int i = 0; i < str.length();) {
            int codepoint = str.codePointAt(i);
            int count = Character.charCount(codepoint);
            char[] chars = Character.toChars(codepoint);
            String s = new String(chars);
            CharacterTexturePage page = factory.getPageOrGenChar(codepoint);
            if (page == null) {
                width += Config.spaceWidth;
            }
            else {
                CharacterInfo info = page.getInfo(codepoint);
                width += info.advanceX() / info.width() * DEFAULT_CHAR_WIDTH + Config.characterSpacing;
            }
            if (width >= wrapWidth) builder.append("\n");
            builder.append(s);
            i += count;
        }

        return builder.toString();
    }

    // private int sizeStringToWidth(String text, int wrapWidth) {
    //     int charCount = 0;
    //     float width = 0;
    //     String[] splits = text.split("(?=§)");
    //     for (String split : splits) {
    //         // 提取无操作符文字
    //         if (split.startsWith("§") && split.length() <= 2) continue;
    //         String s = split;
    //         if (split.startsWith("§")) s = split.substring(2);
    //         // 遍历分割单元内的字符
    //         for (int i = 0; i < s.length() - 1;) {
    //             int codepoint = text.codePointAt(i);
    //             int charCountInCodePoint = Character.charCount(codepoint);
    //             i += charCountInCodePoint;
    //
    //             CharacterTexturePage page = factory.getPageOrGenChar(codepoint);
    //             // 如果没找到
    //             if (page == null) {
    //                 width += 4f;
    //             }
    //             else {
    //                 CharacterInfo info = page.getInfo(codepoint);
    //                 width += info.advanceX() / info.width() * this.curCharWidth;
    //             }
    //             charCount++;
    //
    //             if (width > wrapWidth) return charCount - 1;
    //             if (width == wrapWidth) return charCount;
    //         }
    //     }
    //     return charCount;
    // }

    private String getFormatFromString(String text) {
        StringBuilder builder = new StringBuilder();
        String[] splits = text.split("(?=§)");
        for (String split : splits) {
            // 提取无操作符文字
            if (split.startsWith("§") && split.length() <= 2) continue;
            String s = split;
            if (split.startsWith("§")) s = split.substring(2);
            builder.append(s);
        }
        return builder.toString();
    }
}
