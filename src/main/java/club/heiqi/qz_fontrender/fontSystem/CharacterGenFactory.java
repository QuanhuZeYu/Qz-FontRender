package club.heiqi.qz_fontrender.fontSystem;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class CharacterGenerationFactory {
    /**可操作的所有page*/
    public final ArrayList<PageOperator> pages = new ArrayList<>();
    /**字符大小*/
    public final int charWidth, charHeight;

    public CharacterGenerationFactory(int charWidth, int charHeight) {
        this.charWidth = charWidth;
        this.charHeight = charHeight;
    }


    public static class PageOperator {
        public final CharacterTexturePage page;
        public AtomicBoolean inAdd = new AtomicBoolean(false);
        public ReentrantLock lock = new ReentrantLock();

        public PageOperator(CharacterTexturePage page) {
            this.page = page;
        }

        public void addCharacter(int codepoint, Font font) {
            lock.lock();
            inAdd.set(true);
            try {
                ImageAndInfo imageAndInfo = CharacterImageGenerator.renderCharacter(codepoint, font, page.cWidth, page.cHeight);
                page.addCharacterTexture(imageAndInfo);
            } finally {
                lock.unlock();
                inAdd.set(false);
            }
        }

        public void addCharacter(String character, Font font) {
            int codepoint = character.codePointAt(0);
            addCharacter(codepoint, font);
        }
    }
}
