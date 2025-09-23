package club.heiqi.qz_fontrender.fontSystem;

import club.heiqi.qz_fontrender.Config;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class CharacterGenFactory {
    /**缓存字体管理器*/
    public final FontManager fontManager;
    /**所有page*/
    public final ArrayList<PageOperator> normalPage = new ArrayList<>(),
                                         boldPage = new ArrayList<>();
    /**高速缓存的字符*/
    public Cache<Integer, CharacterTexturePage> normalHighWay = CacheBuilder.newBuilder().maximumSize(65536).build(),
                                                boldHighWay = CacheBuilder.newBuilder().maximumSize(65536).build();
    /**记录已经生成的字符*/
    public final BitSet normalGenerated = new BitSet(65536),
                        boldGenerated = new BitSet(65536);
    /**正在生成的字符*/
    public final ArrayList<Integer> normalInGenerate = new ArrayList<>(),
                                    boldInGenerate = new ArrayList<>();
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

        if (Config.backendGeneration) new Preparer(this);
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
        under = 0;
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

                // 先确认是否生成过
                if (boldGenerated.get(codepoint)) {
                    // 2. 高速缓存不存在则遍历所有纹理集
                    for (PageOperator operator : boldPage) {
                        if (operator.page.isCharInPage(codepoint)) {
                            // 缓存一次
                            boldHighWay.put(codepoint, operator.page);
                            return operator.page;
                        }
                    }
                }

                // 没有生成过 或者没有找到page
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

                if (normalGenerated.get(codepoint)) {
                    // 2. 高速缓存不存在则遍历所有纹理集
                    for (PageOperator operator : normalPage) {
                        if (operator.page.isCharInPage(codepoint)) {
                            // 缓存一次
                            normalHighWay.put(codepoint, operator.page);
                            return operator.page;
                        }
                    }
                }

                // 没有生成过 或者没有找到page
                addCharacter(codepoint, type);
                return null;

            }
        }
    }

    public boolean addCharacter(int codepoint, int type) {
        // 已经生成好了就不需要Add
        if (checkInGeneration(codepoint, type)) return true;

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

    public boolean addCharacter(ImageAndInfo info, int type) {
        int codepoint = info.info().codepoint();
        if (checkInGeneration(codepoint, type)) return true;

        switch (type) {
            case EnumFontType.BOLD -> {
                boldLock.lock();
                // 没有生成添加
                try {
                    checkPool();
                    for (PageOperator operator : boldPage) {
                        if (operator.canAdd()) {
                            boldInGenerate.add(codepoint);
                            operator.inAdd.set(true);
                            operator.page.addCharacterTexture(info);
                            operator.inAdd.set(false);
                            generateDone(codepoint, EnumFontType.BOLD, operator.page);
                            return true;
                        }
                    }
                } finally {
                    boldLock.unlock();
                }
            }
            default -> {
                normalLock.lock();
                // 没有生成添加
                try {
                    checkPool();
                    for (PageOperator operator : normalPage) {
                        if (operator.canAdd()) {
                            normalInGenerate.add(codepoint);
                            operator.inAdd.set(true);
                            operator.page.addCharacterTexture(info);
                            operator.inAdd.set(false);
                            generateDone(codepoint, EnumFontType.NORMAL, operator.page);
                            return true;
                        }
                    }
                } finally {
                    normalLock.unlock();
                }
            }
        }
        // 添加失败
        return false;
    }

    /**
     * 返回是否正在生成 或者已经生成
     */
    public boolean checkInGeneration(int codepoint, int type) {
        switch (type) {
            case EnumFontType.BOLD -> {
                if (boldGenerated.get(codepoint)) {
                    boldInGenerate.remove(Integer.valueOf(codepoint));
                    return true;  // 已经生成
                }
                else {
                    return boldInGenerate.contains(codepoint);
                }
            }
            default -> {
                if (normalGenerated.get(codepoint)) {
                    normalInGenerate.remove(Integer.valueOf(codepoint));
                    return true;  // 已经生成
                }
                else {
                    return normalInGenerate.contains(codepoint);
                }
            }
        }
    }

    public void generateDone(int codepoint, int type, CharacterTexturePage page) {
        switch (type) {
            case EnumFontType.BOLD -> {
                boldInGenerate.remove(Integer.valueOf(codepoint));
                boldHighWay.put(codepoint, page);
                boldGenerated.set(codepoint);
            }
            default -> {
                normalInGenerate.remove(Integer.valueOf(codepoint));
                normalHighWay.put(codepoint, page);
                normalGenerated.set(codepoint);
            }
        }
    }

    public void reset() {
        normalLock.lock();
        boldLock.lock();
        fontManager.reload();
        for (PageOperator operator : normalPage) {
            operator.page.dispose();
        }
        for (PageOperator operator : boldPage) {
            operator.page.dispose();
        }
        normalGenerated.clear();
        boldGenerated.clear();
        normalPage.clear();
        boldPage.clear();
        normalHighWay = CacheBuilder.newBuilder().maximumSize(65536).build();
        boldHighWay = CacheBuilder.newBuilder().maximumSize(65536).build();
        normalInGenerate.clear();
        boldInGenerate.clear();
        normalLock.unlock();
        boldLock.unlock();
    }

    private void debugSaveAllPage() {
        File saveDir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < normalPage.size(); i++) {
            PageOperator operator = normalPage.get(i);
            operator.page.saveImage(new File(saveDir, "images/normal_"+i+".png"));
        }
        for (int i = 0; i < boldPage.size(); i++) {
            PageOperator operator = boldPage.get(i);
            operator.page.saveImage(new File(saveDir, "images/bold_"+i+".png"));
        }
    }


    /**
     * 页面操作者
     */
    public static class PageOperator {
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
                    factory.generateDone(codepoint, type, page);
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
