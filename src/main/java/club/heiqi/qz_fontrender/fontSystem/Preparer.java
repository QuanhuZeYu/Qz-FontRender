package club.heiqi.qz_fontrender.fontSystem;

import club.heiqi.qz_fontrender.Config;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Preparer {
    public final CharacterGenFactory factory;
    /**直接生成所有字符！*/
    public ConcurrentHashMap<Integer, ImageAndInfo> normalImages = new ConcurrentHashMap<>(),
                                                    boldImages = new ConcurrentHashMap<>();
    /**生成线程*/
    public ArrayList<Thread> prepareThreads = new ArrayList<>();

    public Preparer(CharacterGenFactory factory) {
        this.factory = factory;

        prepareAllChar();
        register();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow("正在后台准备字体，可能存在卡顿，当前可正常游玩", 0, (int) Config.charSize * 2, 0xf05556);
        ArrayList<Integer> collect = new ArrayList<>();
        // 处理常规字体
        normalImages.forEach((codepoint, info) -> {
            if (factory.addCharacter(info, EnumFontType.NORMAL))
                collect.add(codepoint);
        });
        collect.forEach((codepoint) -> {
            normalImages.remove(codepoint);
        });
        collect.clear();

        // 处理粗体
        boldImages.forEach((codepoint, info) -> {
            if (factory.addCharacter(info, EnumFontType.BOLD))
                collect.add(codepoint);
        });
        collect.forEach((codepoint) -> {
            boldImages.remove(codepoint);
        });

        for (Thread thread : prepareThreads) {
            if (thread.isAlive()) return;
        }
        // 所有线程结束
        if (normalImages.isEmpty() && boldImages.isEmpty()) {
            unregister();
        }
    }

    public void prepareAllChar() {
        int allCount = 65536;
        int start = 0;
        int end = 0;
        while (allCount > 0) {
            int take = 1000;
            if (end + take <= 65536)
                end += take;
            else
                end = 65536;

            int finalStart = start;
            int finalEnd = end;
            Thread prepareThread = new Thread(() -> {
                for (int i = finalStart; i < finalEnd; i++) {
                    ImageAndInfo imageAndInfo = CharacterImageGenerator.renderCharacter(
                            i,
                            factory.fontManager.findSuitable(i, EnumFontType.NORMAL),
                            factory.charWidth, factory.charHeight
                    );
                    normalImages.put(i, imageAndInfo);

                    ImageAndInfo imageAndInfo1 = CharacterImageGenerator.renderCharacter(
                            i,
                            factory.fontManager.findSuitable(i, EnumFontType.BOLD),
                            factory.charWidth, factory.charHeight
                    );
                    boldImages.put(i, imageAndInfo1);
                }
            }, "字符准备器");
            prepareThreads.add(prepareThread);
            prepareThread.start();

            allCount -= take;
            start += take;
        }

    }


    public void register() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void unregister() {
        FMLCommonHandler.instance().bus().unregister(this);
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
