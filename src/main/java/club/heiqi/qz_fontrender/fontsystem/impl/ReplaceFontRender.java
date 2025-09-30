package club.heiqi.qz_fontrender.fontsystem.impl;

import club.heiqi.qz_fontrender.Config;
import club.heiqi.qz_fontrender.fontsystem.*;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReplaceFontRender extends FontRenderer {
    public static final float DEFAULT_CHAR_WIDTH = 9f;
    public float curCharWidth;
    public float saveR, saveG, saveB, saveA;
    public BatchRenderFont batchRenderer = new BatchRenderFont();

    public ReplaceFontRender(GameSettings gameSettings, ResourceLocation location, TextureManager manager, boolean b,
                             int textureWidth, int textureHeight, int charWidth, int charHeight, int maintainPool,
                             float fontSize
    ) {
        super(gameSettings, location, manager, b);
        curCharWidth = Config.charSize;
    }

    @Override
    public void onResourceManagerReload(IResourceManager p_110549_1_) {

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
        if (text.isEmpty()) return 0;
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
    public int getStringWidth(final String text) {
        if (text == null) return 0;

        int textLength = text.length();
        double width = 0;
        int fontType = PageManager.NORMAL;

        for (int i = 0; i < textLength;) {
            int codepoint = text.codePointAt(i);

            // 判断该字符是否是操作符 且操作符下一个字符是否存在
            if (codepoint == '§' && i == textLength - 1) {
                break;
            }
            else if (codepoint == '§' && i < textLength - 1) {
                i++;  // 操作符步进
                codepoint = text.codePointAt(i);  // 操作指令
                i++;  // 操作指令步进

                // 执行操作指令
                switch (codepoint) {
                    case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'  -> {

                    }
                    case 'k' -> {
                        randomStyle = true;
                    }
                    case 'l' -> {
                        boldStyle = true;
                        fontType = PageManager.BOLD;
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
                        fontType = PageManager.NORMAL;
                    }
                    // 任何没有见过的操作符都视作重置！
                    default -> {
                        this.resetStyles();
                        fontType = PageManager.NORMAL;
                    }
                }

                // 提取下一个字符
                if (i < textLength) {
                    codepoint = text.codePointAt(i);
                }
                // 如果已经到达末尾结束
                else {
                    return (int) Math.ceil(width);
                }
            }

            else {
                CharPage page;
                // 获取字符页
                page = PageManager.getInstance().getPage(codepoint, fontType);
                // 字符页正在生成返回空格
                if (page == null) {
                    width += Config.spaceWidth;
                } else {
                    CharInfo info = page.getCharInfo(codepoint);  // 流程不错的情况下info不为null

                    // 处理随机化的情况
                    if (randomStyle && randomLoaded) {
                        float randomWidth = 0;
                        int randomCharCodepoint;
                        do {
                            int randomIndex = fontRandom.nextInt(randomSample.length());
                            randomCharCodepoint = randomSample.charAt(randomIndex);
                            randomWidth = randomSampleWidthList[randomIndex];
                        }
                        while (Math.abs(info.advance - randomWidth) > 0.05f);

                        CharPage replacePage = PageManager.getInstance().getPage(randomCharCodepoint, PageManager.NORMAL);
                        // 如果随机化的字符页为空回退到原始字符页
                        page = replacePage == null ? page : replacePage;
                        info = replacePage == null ? page.getCharInfo(codepoint) : replacePage.getCharInfo(randomCharCodepoint);
                    }
                    width += ((info.advance / info.width) * this.curCharWidth) + Config.characterSpacing;
                }

                i += Character.charCount(codepoint);
            }
        }
        return (int) Math.ceil(width);
    }

    @Override
    public int getCharWidth(char character) {
        String s = String.valueOf(character);
        int codepoint = s.codePointAt(0);
        if (s.equals(" ")) return (int) Config.spaceWidth;

        CharPage page = PageManager.getInstance().getPage(codepoint, 0);
        if (page == null) {
            return (int) Config.spaceWidth;
        }
        CharInfo info = page.getCharInfo(codepoint);
        return (int) Math.ceil(info.advance/info.width*this.curCharWidth + Config.characterSpacing);
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
    public void setBidiFlag(boolean bidiFlag) {
        this.bidiFlag = bidiFlag;
    }

    @Override
    public void setUnicodeFlag(boolean unicodeFlag) {
        this.unicodeFlag = unicodeFlag;
    }

    @Override
    public int splitStringWidth(String text, int wrapWidth) {
        return (int) Math.ceil(DEFAULT_CHAR_WIDTH * this.listFormattedStringToWidth(text, wrapWidth).size());
    }

    @Override
    public String trimStringToWidth(String text, int targetWidth, boolean b) {
        StringBuilder stringbuilder = new StringBuilder();

        int textLength = text.length();
        double width = 0;
        int fontType = PageManager.NORMAL;

        for (int i = 0; i < textLength;) {
            int codepoint = text.codePointAt(i);

            // 判断该字符是否是操作符 且操作符下一个字符是否存在
            if (codepoint == '§' && i < textLength - 1) {
                i++;  // 操作符步进
                codepoint = text.codePointAt(i);  // 操作指令
                i++;  // 操作指令步进

                // 执行操作指令
                switch (codepoint) {
                    case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'  -> {

                    }
                    case 'k' -> {
                        randomStyle = true;
                    }
                    case 'l' -> {
                        boldStyle = true;
                        fontType = PageManager.BOLD;
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
                        fontType = PageManager.NORMAL;
                    }
                    // 任何没有见过的操作符都视作重置！
                    default -> {
                        this.resetStyles();
                        fontType = PageManager.NORMAL;
                    }
                }

                // 提取下一个字符
                if (i < textLength) {
                    codepoint = text.codePointAt(i);
                }
                // 如果已经到达末尾结束
                else {
                    break;
                }
            }


            CharPage page;
            // 获取字符页
            page = PageManager.getInstance().getPage(codepoint, fontType);
            // 字符页正在生成返回空格
            if (page == null) {
                width += Config.spaceWidth;
            }
            else {
                CharInfo info = page.getCharInfo(codepoint);  // info不可为null

                // 处理随机化的情况
                if (randomStyle && randomLoaded) {
                    float randomWidth = 0;
                    int randomCharCodepoint;
                    do {
                        int randomIndex = fontRandom.nextInt(randomSample.length());
                        randomCharCodepoint = randomSample.charAt(randomIndex);
                        randomWidth = randomSampleWidthList[randomIndex];
                    }
                    while (Math.abs(info.advance - randomWidth) > 0.05f);

                    CharPage replacePage = PageManager.getInstance().getPage(randomCharCodepoint, PageManager.NORMAL);
                    // 如果随机化的字符页为空回退到原始字符页
                    page = replacePage == null ? page : replacePage;
                    info = replacePage == null ? page.getCharInfo(codepoint) : replacePage.getCharInfo(randomCharCodepoint);
                }
                width += ((info.advance / info.width) * this.curCharWidth) + Config.characterSpacing;
            }

            // 检查长度
            if (width > targetWidth) {
                break;
            }
            else {
                stringbuilder.append(new String(Character.toChars(codepoint)));
            }

            i += Character.charCount(codepoint);
        }

        return stringbuilder.toString();
    }

    @Override
    public String trimStringToWidth(String p_78269_1_, int p_78269_2_) {
        return this.trimStringToWidth(p_78269_1_, p_78269_2_, false);
    }


    public static final String randomSample = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ!\"#$%&'()*+,-./0123456789:;<=>?" +
            "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~" +
            "ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀" +
            "αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■";
    public static float[] randomSampleWidthList;
    /**解析字符串以渲染，主要识别 § `0123456789abcdefklmnor` 17字符+5控制字符?
     * k = 随机化
     * l = 粗体
     * m = 删除线样式
     * n = 下划线
     * o = 斜体
     * r = 重置*/
    private void renderStringAtPos(String s, boolean shadow) {
        loadRandomSampleWidth();

        this.FONT_HEIGHT = (int) Config.charSize;
        // 1. 以§做分割
        String[] splits = s.split("(?=§)");


        // 2. 操作分割后的单元
        int red = (int) (this.red * 255), green = (int) (this.blue * 255), blue = (int) (this.green * 255),
            fontType = PageManager.NORMAL, color = 0xff << 24 | red << 16 | green << 8 | blue & 0xff;
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
                        color = colorCode[colorIndex];
                    }
                    case 'k' -> {
                        randomStyle = true;
                    }
                    case 'l' -> {
                        boldStyle = true;
                        fontType = PageManager.BOLD;
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
                        fontType = PageManager.NORMAL;
                        red = (int) (saveR * 255); green = (int) (saveG * 255); blue = (int) (saveB * 255);
                        color = (int) (saveA * 255) << 24 | red << 16 | green << 8 | blue & 0xff;
                    }
                    // 任何没有见过的操作符都视作重置！
                    default -> {
                        this.resetStyles();
                        fontType = PageManager.NORMAL;
                        red = (int) (saveR * 255); green = (int) (saveG * 255); blue = (int) (saveB * 255);
                        color = (int) (saveA * 255) << 24 | red << 16 | green << 8 | blue & 0xff;
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
                int charCountInCodePoint = Character.charCount(codepoint);
                i += charCountInCodePoint;

                CharPage page = PageManager.getInstance().getPage(codepoint, fontType);
                LineInfo lineInfo = new LineInfo().setColor(color);
                // 如果没有找到则跳过
                if (page == null || Character.isSpaceChar(codepoint)) {
                    collectDraw(Config.spaceWidth, lineInfo);
                    continue;
                }
                CharInfo info = page.getCharInfo(codepoint);
                float charAdvance = info.advance;
                float charWidth = charAdvance / info.width * this.curCharWidth + Config.characterSpacing;

                // 处理随机化字符
                if (randomStyle && randomLoaded) {
                    float randomWidth = 0;
                    int randomCharCodepoint;
                    do {
                        int randomIndex = fontRandom.nextInt(randomSample.length());
                        randomCharCodepoint = randomSample.charAt(randomIndex);
                        randomWidth = randomSampleWidthList[randomIndex];
                    }
                    while (Math.abs(charAdvance - randomWidth) > 0.05f);

                    page = PageManager.getInstance().getPage(randomCharCodepoint, PageManager.NORMAL);
                    // 如果没有找到则跳过
                    if (page == null) {
                        collectDraw(Config.spaceWidth, lineInfo);
                        continue;
                    }
                }

                // ========== 渲染 ==========
                // batchRenderer.collect(posX,posY,curCharWidth,curCharWidth,page,info,color,italicStyle);
                // page.renderChar(info, color, posX, posY, this.curCharWidth, this.curCharWidth, this.italicStyle);
                // ========== 渲染 ==========

                // 2.2.3 处理下划线等情况
                collectDraw(charWidth, lineInfo);
            }
        }
    }

    private void renderStringAtPos_Version2(String text, boolean shadow) {
        int textLength = text.length();

        int fontType = PageManager.NORMAL;
        int color = (int) (saveA * 255) << 24
                | (int) (saveR * 255) << 16
                | (int) (saveG * 255) << 8
                | (int) (saveB * 255) & 255;

        for (int i = 0; i < textLength;) {
            int codepoint = text.codePointAt(i);

            // 判断该字符是否是操作符 且操作符下一个字符是否存在
            if (codepoint == '§' && i == textLength - 1) {
                return;
            }
            else if (codepoint == '§' && i < textLength - 1) {
                i++;  // 操作符步进
                codepoint = text.codePointAt(i);  // 操作指令
                i++;  // 操作指令步进

                // 执行操作指令
                switch (codepoint) {
                    case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'  -> {
                        this.randomStyle = false;
                        this.boldStyle = false;
                        this.strikethroughStyle = false;
                        this.underlineStyle = false;
                        this.italicStyle = false;
                        int colorIndex = "0123456789abcdefklmnor".indexOf(codepoint);
                        if (shadow) colorIndex = colorIndex + 16;
                        color = colorCode[colorIndex];
                    }
                    case 'k' -> {
                        randomStyle = true;
                    }
                    case 'l' -> {
                        boldStyle = true;
                        fontType = PageManager.BOLD;
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
                        fontType = PageManager.NORMAL;
                        color = (int) (saveA * 255) << 24
                                | (int) (saveR * 255) << 16
                                | (int) (saveG * 255) << 8
                                | (int) (saveB * 255) & 255;
                    }
                    // 任何没有见过的操作符都视作重置！
                    default -> {
                        this.resetStyles();
                        fontType = PageManager.NORMAL;
                        color = (int) (saveA * 255) << 24
                                | (int) (saveR * 255) << 16
                                | (int) (saveG * 255) << 8
                                | (int) (saveB * 255) & 255;
                    }
                }

                // 提取下一个字符
                if (i < textLength) {
                    codepoint = text.codePointAt(i);
                }
                // 如果已经到达末尾结束
                else {
                    return;
                }
            }
            else {
                float width = 0;
                CharPage page;
                LineInfo lineInfo = new LineInfo().setColor(color);
                // 获取字符页
                page = PageManager.getInstance().getPage(codepoint, fontType);
                // 字符页正在生成返回空格
                if (page == null) {
                    width = Config.spaceWidth;
                } else {
                    CharInfo info = page.getCharInfo(codepoint);  // info不可为null

                    // 处理随机化的情况
                    if (randomStyle && randomLoaded) {
                        float randomWidth = 0;
                        int randomCharCodepoint;
                        do {
                            int randomIndex = fontRandom.nextInt(randomSample.length());
                            randomCharCodepoint = randomSample.charAt(randomIndex);
                            randomWidth = randomSampleWidthList[randomIndex];
                        }
                        while (Math.abs(info.advance - randomWidth) > 0.05f);

                        CharPage replacePage = PageManager.getInstance().getPage(randomCharCodepoint, PageManager.NORMAL);
                        // 如果随机化的字符页为空回退到原始字符页
                        page = replacePage == null ? page : replacePage;
                        info = replacePage == null ? page.getCharInfo(codepoint) : replacePage.getCharInfo(randomCharCodepoint);
                    }
                    width = ((info.advance / info.width) * this.curCharWidth) + Config.characterSpacing;

                    // TODO 实际渲染环节
                    // batchRenderer.collect(posX, posY, curCharWidth, curCharWidth, page, info, color, italicStyle);
                    batchRenderer.collectRender(posX, posY, curCharWidth, page, info, color, italicStyle);
                }

                collectDraw(width, lineInfo);
                i += Character.charCount(codepoint);
            }
        }
    }

    private void renderAChar(float x, float y, float charSize, CharPage page, CharInfo info, int inColor, boolean italic) {
        float u0 = (float)info.getU0(page.textureSize);
        float u1 = (float)info.getU1(page.textureSize);
        float v0 = (float)info.getV0(page.textureSize);
        float v1 = (float)info.getV1(page.textureSize);
        float alpha = (float) (inColor >> 24) / 255;
        float red = (float) ((inColor >> 16) & 255) / 255;
        float green = (float) ((inColor >> 8) & 255) / 255;
        float blue = (float) (inColor & 255) / 255;

        float[] vertex = new float[] {
                // 左上
                italic ? x+2 : x, y, 0,
                // 左下
                x, y+charSize, 0,
                // 右下
                x+charSize, y+charSize, 0,
                // 右上
                italic ? x+charSize+2 : x+charSize, y, 0
        };
        float[] texCoord = new float[] {
                u0, v0,
                u0, v1,
                u1, v1,
                u1, v0,
        };
        float[] color = new float[] {
                red, green, blue, alpha,
                red, green, blue, alpha,
                red, green, blue, alpha,
                red, green, blue, alpha,
        };
        int[] index = new int[] {
                0,1,2, 2,3,0
        };

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, page.textureID);
        RenderTool.getInstance().render(vertex,texCoord,color,index);
    }

    public static class LineInfo {
        public ArrayList<Vector3d> lineVertex = new ArrayList<>();
        public Vector4f lineColor = new Vector4f();

        public LineInfo setColor(int color) {
            lineColor.x = (color >> 16) & 255;
            lineColor.y = (color >> 8) & 255;
            lineColor.z = (color) & 255;
            lineColor.w = (color >> 24) & 255;
            return this;
        }

        public void draw() {
            GL11.glColor4f(lineColor.x, lineColor.y, lineColor.z, lineColor.w);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBegin(GL11.GL_QUADS);

            for (Vector3d vertex : lineVertex) {
                GL11.glVertex3d(vertex.x, vertex.y, vertex.z);
            }

            GL11.glEnd();
        }

        public LineInfo addVertex(Vector3d v) {
            lineVertex.add(v);
            return this;
        }
    }
    public ArrayList<LineInfo> lineInfos = new ArrayList<>();
    private void collectDraw(float width, LineInfo lineInfo) {
        if (this.underlineStyle) {
            lineInfo.addVertex(new Vector3d((this.posX), (this.posY + this.curCharWidth), 0.0d))
                    .addVertex(new Vector3d((this.posX + width), (this.posY + this.curCharWidth), 0.0d))
                    .addVertex(new Vector3d((this.posX + width), (this.posY + this.curCharWidth - 1.0d), 0.0d))
                    .addVertex(new Vector3d((this.posX), (this.posY + this.curCharWidth - 1.0d), 0.0d));
            lineInfos.add(lineInfo);
        }
        if (this.strikethroughStyle) {
            lineInfo.addVertex(new Vector3d((this.posX), this.posY + (this.curCharWidth / 2) + 1, 0.0d))
                    .addVertex(new Vector3d((this.posX + width), this.posY + (this.curCharWidth / 2) + 1, 0.0d))
                    .addVertex(new Vector3d(new Vector3d((this.posX + width), (this.posY + this.curCharWidth - 1.0d), 0.0d)))
                    .addVertex(new Vector3d(new Vector3d((this.posX), (this.posY + this.curCharWidth - 1.0d), 0.0d)));
            lineInfos.add(lineInfo);
        }

        this.posX += width;
    }

    public void drawCollect() {
        for (LineInfo lineInfo : lineInfos) {
            lineInfo.draw();
        }
        lineInfos.clear();
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

            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            // 🐕 收集需要渲染的字符 🐱
            this.renderStringAtPos_Version2(text, shadow);
            batchRenderer.flush();

            drawCollect();

            GL11.glPopAttrib();

            return (int)Math.ceil(this.posX);
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






    private String trimStringNewline(String text) {
        while (text != null && text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private void renderSplitString(String str, int x, int y, int wrapWidth, boolean addShadow) {
        List<String> list = this.listFormattedStringToWidth(str, wrapWidth);

        for (String s1 : list) {
            renderStringAligned(s1, x, y, wrapWidth, this.textColor, addShadow);
            y += (int) Math.ceil(this.curCharWidth + Config.lineSpacing);
        }
    }

    private void renderStringAligned(String s, int x, int y, int wrapWidth, int color, boolean shadow) {
        if (s.isEmpty()) return;
        if (this.bidiFlag) {
            int i1 = this.getStringWidth(this.bidiReorder(s));
            x = x + wrapWidth - i1;
        }

        this.renderString(s, x, y, color, shadow);
    }

    private String wrapFormattedStringToWidth(String str, int wrapWidth) {
        StringBuilder builder = new StringBuilder();

        float width = 0;
        for (int i = 0; i < str.length();) {
            // 获取字符信息
            int codepoint = str.codePointAt(i);
            int count = Character.charCount(codepoint);
            char[] chars = Character.toChars(codepoint);
            String s = new String(chars);

            // 跳过操作符和对应的char
            if (s.equals("§")) {
                i ++;
                builder.append(s);
                if (i < str.length()) {
                    // 获取字符信息
                    codepoint = str.codePointAt(i);
                    chars = Character.toChars(codepoint);
                    s = new String(chars);
                    builder.append(s);
                    i++;
                }
                continue;
            }

            CharPage page = PageManager.getInstance().getPage(codepoint, PageManager.NORMAL);

            if (page == null) {
                width += Config.spaceWidth;
            }
            else {
                CharInfo info = page.getCharInfo(codepoint);
                width += info.advance / info.width * this.curCharWidth + Config.characterSpacing;
            }
            if (width > wrapWidth) {
                builder.append("\n");
                width = 0;
            }
            builder.append(s);
            i += count;
        }

        return builder.toString();
    }



    private boolean randomLoaded = false;
    private void loadRandomSampleWidth() {
        if (randomLoaded) return;

        if (randomSampleWidthList == null) {
            for (int i = 0; i < randomSample.length(); i++) {
                int codepoint = randomSample.codePointAt(i);
                PageManager.instance.getPage(codepoint, PageManager.NORMAL);
            }
            randomSampleWidthList = new float[randomSample.length()];
            return;
        }

        for (int i = 0; i < randomSample.length(); i++) {
            int codepoint = randomSample.codePointAt(i);
            CharPage page = PageManager.getInstance().getPage(codepoint, PageManager.NORMAL);
            if (page == null) return;
        }

        for (int i = 0; i < randomSample.length(); i++) {
            int codepoint = randomSample.codePointAt(i);
            CharPage page = PageManager.getInstance().getPage(codepoint, PageManager.NORMAL);
            if (page == null) return;
            CharInfo info = page.getCharInfo(codepoint);
            randomSampleWidthList[i] = info.advance;
        }

        randomLoaded = true;
    }

    public void setCharSize(float size) {
        this.curCharWidth = size;
        this.FONT_HEIGHT = (int) Math.ceil(curCharWidth);
    }

    public void resetCharSize() {
        this.curCharWidth = Config.charSize;
        this.FONT_HEIGHT = (int) Math.ceil(curCharWidth);
    }
}
