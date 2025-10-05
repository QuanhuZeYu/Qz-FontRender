package club.heiqi.qz_fontrender.configGUI;

import club.heiqi.qz_fontrender.Config;
import club.heiqi.qz_fontrender.MyMod;
import club.heiqi.qz_uilib.client.ConfigGuiTemplate;
import club.heiqi.qz_uilib.widget.ButtonWithTextWidget;
import club.heiqi.qz_uilib.widget.layout.VerticalLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;

import java.util.ArrayList;
import java.util.List;

public class QzConfigGUI extends ConfigGuiTemplate {

    public QzConfigGUI(GuiScreen parent) {
        super(parent);

        // 该部分将添加到GUI模板类中此处不再重复
        // ButtonWithTextWidget quitButton = new ButtonWithTextWidget().setText("取消&退出");
        // quitButton.perfectWidth = -1;
        // quitButton.setCallBack(() -> {
        //     saveOperators.clear();
        //     if (parent != null)
        //         Minecraft.getMinecraft().displayGuiScreen(parent);
        // });
        // root.addChild(quitButton);
    }

    @Override
    public void initGui() {
        super.initGui();
        root.setLayout(new VerticalLayout());

        ButtonWithTextWidget exSettingButton = new ButtonWithTextWidget().setText("字体排序设置");
        exSettingButton.perfectWidth = -1;
        exSettingButton.setCallBack(() -> {
            Minecraft.getMinecraft().displayGuiScreen(new QzExFontConfigGUI(this));
        });
        root.addChild(root.children.size()-1, exSettingButton);
    }

    @Override
    public List<ConfigCategory> getCategory() {
        List<ConfigCategory> result = new ArrayList<>();

        result.add(Config.config.getCategory(Configuration.CATEGORY_GENERAL));

        return result;
    }

    @Override
    public void saveConfigCallback() {
        MyMod.proxy.config.load();
        Config.config.save();
    }
}
