package club.heiqi.qz_fontrender.fontsystem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;

public class CharPage {
    public static Logger LOG = LogManager.getLogger();
    public final int textureID;
    public final int textureSize, charSize;
    public final HashMap<Integer, CharInfo> chars = new HashMap<>();

    public CharPage(int textureSize, int charSize) {
        this.textureSize = textureSize;
        this.charSize = charSize;
        textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                textureSize,
                textureSize,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void addChar(ByteBuffer image, CharInfo charInfo) {
        int curCount = getCurCharCount();  // 纹理页中的字符数量
        int x, y;
        if (curCount == 0) {
            x = 0;
            y = 0;
        }
        else {
            int totalWidth = charSize * curCount;
            x = totalWidth % textureSize;
            y = (totalWidth / textureSize) * charSize;
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                x,
                y,
                charSize,
                charSize,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                image
        );
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        charInfo.x = x;
        charInfo.y = y;

        chars.put(charInfo.codepoint, charInfo);
    }


    public boolean canAddChar() {
        return getCurCharCount() < getMaxCount();
    }

    public int getCurCharCount() {
        return chars.size();
    }

    public int getMaxCount() {
        return (textureSize * textureSize) / (charSize * charSize);
    }

    public void dispose() {
        GL11.glDeleteTextures(textureID);
    }


    public boolean isCharInPage(int codepoint) {
        CharInfo charInfo = chars.get(codepoint);
        return charInfo != null;
    }

    public CharInfo getCharInfo(int codepoint) {
        CharInfo charInfo = chars.get(codepoint);
        if (charInfo != null) return charInfo;
        LOG.error("字符 【{}】 不在本纹理页 ({}) 中", new String(Character.toChars(codepoint)), textureID);
        throw new RuntimeException("字符 【"+new String(Character.toChars(codepoint))+"】 不在本纹理页 ("+textureID+") 中");
    }

    @Override
    public int hashCode() {
        return textureID;
    }
}
