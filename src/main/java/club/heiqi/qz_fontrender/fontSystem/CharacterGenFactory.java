package club.heiqi.qz_fontrender.fontSystem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class CharacterGenFactory {
    /**缓存字体管理器*/
    public final FontManager fontManager;
    /**所有page*/
    public final ArrayList<PageOperator> pageOperators = new ArrayList<>();
    /**正在生成的字符*/
    public final ConcurrentLinkedQueue<Integer> inGenerate = new ConcurrentLinkedQueue<>();
    /**页面大小*/
    public final int width, height;
    /**字符大小*/
    public final int charWidth, charHeight;
    /**维持可用池化数量*/
    public final int maintainPool;
    /**工厂线程锁 保证添加字符时的安全性*/
    public ReentrantLock addCharLock = new ReentrantLock();

    /**
     * @param width,height          纹理页大小
     * @param charWidth,charHeight  字符单元大小
     * @param maintainPool          维持可用池化大小
     */
    public CharacterGenFactory(FontManager manager,
                               int width, int height,
                               int charWidth, int charHeight,
                               int maintainPool) {
        this.fontManager = manager;
        this.width = width;
        this.height = height;
        this.charWidth = charWidth;
        this.charHeight = charHeight;
        if (maintainPool < 5) maintainPool = 5;
        this.maintainPool = maintainPool;
        checkPool();
    }

    /**检查所有page始终保持有5个未满page*/
    public void checkPool() {
        // 1. 检查数量是否有5个不足直接添加
        if (pageOperators.size() < maintainPool) {
            int addCount = maintainPool - pageOperators.size();
            for (int i = 0; i < addCount; i++) {
                pageOperators.add(new PageOperator(new CharacterTexturePage(width, height, charWidth, charHeight), this));
            }
            return;
        }

        // 2. 检查未满数量
        int under = 0;
        int operateCount = pageOperators.size();
        for (PageOperator pageOperator : pageOperators) {
            if (!pageOperator.isFull()) under++;
        }
        if (under < maintainPool) {
            int addCount = maintainPool - under;
            for (int i = 0; i < addCount; i++) {
                pageOperators.add(new PageOperator(new CharacterTexturePage(width, height, charWidth, charHeight), this));
            }
            return;
        }
    }

    @Nullable
    public CharacterTexturePage getPageOrGenChar(int codepoint) {
        if (inGenerate.contains(codepoint)) return null;  // 正在生成
        for (PageOperator operator : pageOperators) {
            if (operator.page.isCharinPage(codepoint)) {
                return operator.page;
            }
        }

        // 执行到这代表没有找到对应的Page
        addCharacter(codepoint);
        return null;
    }

    public boolean addCharacter(int codepoint) {
        addCharLock.lock();
        try {
            checkPool();
            for (PageOperator operator : pageOperators) {
                // 寻找可添加的纹理页进行添加操作
                if (operator.canAdd()) {
                    inGenerate.add(codepoint);
                    operator.addCharacter(codepoint, fontManager.findSuitable(codepoint));
                    return true;
                }
            }
            return false;
        } finally {
            addCharLock.unlock();
        }
    }

    public void generateDone(int codepoint) {
        inGenerate.remove(codepoint);
    }


    /**
     * 页面操作者
     */
    public static class PageOperator {
        public static Logger LOG = LogManager.getLogger();
        public final CharacterGenFactory factory;
        public final CharacterTexturePage page;
        public AtomicBoolean inAdd = new AtomicBoolean(false);

        public PageOperator(CharacterTexturePage page, CharacterGenFactory factory) {
            this.page = page;
            this.factory = factory;
        }

        public void addCharacter(int codepoint, Font font) {
            inAdd.set(true);
            char[] chars = Character.toChars(codepoint);
            String character = new String(chars);
            new Thread(() -> {
                try {
                    ImageAndInfo imageAndInfo = CharacterImageGenerator.renderCharacter(codepoint, font, page.cWidth, page.cHeight);
                    page.addCharacterTexture(imageAndInfo);
                } finally {
                    inAdd.set(false);
                    factory.generateDone(codepoint);
                }
            }, "添加字符:【"+character+"】").start();
        }

        public void addCharacter(String character, Font font) {
            int codepoint = character.codePointAt(0);
            addCharacter(codepoint, font);
        }

        public boolean isFull() {
            return page.full;
        }

        public boolean canAdd() {
            return (!page.full && !inAdd.get());
        }
    }
}
