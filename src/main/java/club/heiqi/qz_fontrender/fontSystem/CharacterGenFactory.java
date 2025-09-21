package club.heiqi.qz_fontrender.fontSystem;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
    public final ArrayList<PageOperator> normalPage = new ArrayList<>(),
                                         boldPage = new ArrayList<>();
    /**高速缓存的字符*/
    public Cache<Integer, CharacterTexturePage> normalHighWay = CacheBuilder.newBuilder().maximumSize(10240).build(),
                                                boldHighWay = CacheBuilder.newBuilder().maximumSize(10240).build();
    /**正在生成的字符*/
    public final ConcurrentLinkedQueue<Integer> normalInGenerate = new ConcurrentLinkedQueue<>(),
                                                boldInGenerate = new ConcurrentLinkedQueue<>();
    /**页面大小*/
    public final int width, height;
    /**字符大小*/
    public final int charWidth, charHeight;
    /**维持可用池化数量*/
    public final int maintainPool;
    /**工厂线程锁 保证添加字符时的安全性*/
    public ReentrantLock normalLock = new ReentrantLock(),
                         boldLock = new ReentrantLock();

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
        if (normalPage.size() < maintainPool) {
            int addCount = maintainPool - normalPage.size();
            for (int i = 0; i < addCount; i++) {
                normalPage.add(new PageOperator(new CharacterTexturePage(width, height, charWidth, charHeight), this));
            }
            return;
        }
        if (boldPage.size() < maintainPool) {
            int addCount = maintainPool - boldPage.size();
            for (int i = 0; i < addCount; i++) {
                boldPage.add(new PageOperator(new CharacterTexturePage(width, height, charWidth, charHeight), this));
            }
            return;
        }

        // 2. 检查未满数量
        int under = 0;
        for (PageOperator pageOperator : normalPage) {
            if (!pageOperator.isFull()) under++;
        }
        if (under < maintainPool) {
            int addCount = maintainPool - under;
            for (int i = 0; i < addCount; i++) {
                normalPage.add(new PageOperator(new CharacterTexturePage(width, height, charWidth, charHeight), this));
            }
            return;
        }
        for (PageOperator pageOperator : boldPage) {
            if (!pageOperator.isFull()) under++;
        }
        if (under < maintainPool) {
            int addCount = maintainPool - under;
            for (int i = 0; i < addCount; i++) {
                boldPage.add(new PageOperator(new CharacterTexturePage(width, height, charWidth, charHeight), this));
            }
            return;
        }
    }

    /**
     * @param codepoint     码点
     * @param type          0-常规 1-粗体 2-斜体
     */
    @Nullable
    public CharacterTexturePage getPageOrGenChar(int codepoint, int type) {
        switch (type) {
            case EnumFontType.BOLD -> {
                if (boldInGenerate.contains(codepoint)) return null;  // 正在生成

                // 1. 先访问高速缓存
                CharacterTexturePage page;
                if ((page = boldHighWay.getIfPresent(codepoint)) != null) {
                    if (!page.isCharInPage(codepoint)) return null;
                    return page;
                }

                // 2. 高速缓存不存在则遍历所有纹理集
                for (PageOperator operator : boldPage) {
                    if (operator.page.isCharInPage(codepoint)) {
                        // 缓存一次
                        boldHighWay.put(codepoint, operator.page);
                        return operator.page;
                    }
                }

                // 执行到这代表没有找到对应的Page
                addCharacter(codepoint, type);
                return null;
            }
            default -> {
                if (normalInGenerate.contains(codepoint)) return null;  // 正在生成

                // 1. 先访问高速缓存
                CharacterTexturePage page;
                if ((page = normalHighWay.getIfPresent(codepoint)) != null) {
                    if (!page.isCharInPage(codepoint)) return null;
                    return page;
                }

                // 2. 高速缓存不存在则遍历所有纹理集
                for (PageOperator operator : normalPage) {
                    if (operator.page.isCharInPage(codepoint)) {
                        // 缓存一次
                        normalHighWay.put(codepoint, operator.page);
                        return operator.page;
                    }
                }

                // 执行到这代表没有找到对应的Page
                addCharacter(codepoint, type);
                return null;
            }
        }
    }

    public boolean addCharacter(int codepoint, int type) {
        switch (type) {
            case EnumFontType.BOLD -> {
                boldLock.lock();
                try {
                    checkPool();
                    for (PageOperator operator : boldPage) {
                        // 寻找可添加的纹理页进行添加操作
                        if (operator.canAdd()) {
                            boldInGenerate.add(codepoint);
                            operator.addCharacter(codepoint, fontManager.findSuitable(codepoint, type), type);
                            return true;
                        }
                    }
                    return false;
                } finally {
                    boldLock.unlock();
                }
            }
            default -> {
                normalLock.lock();
                try {
                    checkPool();
                    for (PageOperator operator : normalPage) {
                        // 寻找可添加的纹理页进行添加操作
                        if (operator.canAdd()) {
                            normalInGenerate.add(codepoint);
                            operator.addCharacter(codepoint, fontManager.findSuitable(codepoint, EnumFontType.NORMAL), EnumFontType.NORMAL);
                            return true;
                        }
                    }
                    return false;
                } finally {
                    normalLock.unlock();
                }
            }
        }
    }

    public void generateDone(int codepoint, int type) {
        switch (type) {
            case EnumFontType.BOLD -> {
                boldInGenerate.remove(codepoint);
            }
            default -> {
                normalInGenerate.remove(codepoint);
            }
        }
    }

    public void reset() {
        fontManager.reload();
        for (PageOperator operator : normalPage) {
            operator.page.dispose();
        }
        for (PageOperator operator : boldPage) {
            operator.page.dispose();
        }
        normalPage.clear();
        boldPage.clear();
        normalHighWay = CacheBuilder.newBuilder().maximumSize(10240).build();
        boldHighWay = CacheBuilder.newBuilder().maximumSize(10240).build();
        normalInGenerate.clear();
        boldInGenerate.clear();
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

        public void addCharacter(int codepoint, Font font, int type) {
            inAdd.set(true);
            char[] chars = Character.toChars(codepoint);
            String character = new String(chars);
            new Thread(() -> {
                try {
                    ImageAndInfo imageAndInfo = CharacterImageGenerator.renderCharacter(codepoint, font, page.cWidth, page.cHeight);
                    page.addCharacterTexture(imageAndInfo);
                } finally {
                    inAdd.set(false);
                    factory.generateDone(codepoint, type);
                }
            }, "添加字符:【"+character+"】").start();
        }

        public void addCharacter(String character, Font font, int type) {
            int codepoint = character.codePointAt(0);
            addCharacter(codepoint, font, type);
        }

        public boolean isFull() {
            return page.full;
        }

        public boolean canAdd() {
            return (!page.full && !inAdd.get());
        }
    }
}
