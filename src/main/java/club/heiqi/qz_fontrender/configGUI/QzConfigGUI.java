package club.heiqi.qz_fontrender.configGUI;

import club.heiqi.qz_fontrender.Config;
import club.heiqi.qz_fontrender.MyMod;
import club.heiqi.qz_uilib.gui.ConfigGuiTemplate;
import club.heiqi.qz_uilib.widget.*;
import club.heiqi.qz_uilib.widget.layout.HorizontalLayout;
import club.heiqi.qz_uilib.widget.layout.VerticalLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
    public ListWidget createConfigList() {
        List<ConfigCategory> categories = this.getCategory();
        ListWidget configList = new ListWidget();

        for(ConfigCategory category : categories) {
            String categoryName = category.getName();
            LabelWidget categoryLabel = (new LabelWidget()).setText(categoryName);
            categoryLabel.setPerfectSize(-1.0F, 64.0F);

            for(Map.Entry<String, Property> entry : category.entrySet()) {
                String title = (String)entry.getKey();
                Property property = (Property)entry.getValue();
                LabelWidget titleLabel = (new LabelWidget()).setText(title);
                titleLabel.setTooltip(property.comment);
                Property.Type type = property.getType();
                Widget valueWidget = new Widget();
                switch (type) {
                    case INTEGER:
                        if (!property.isList()) {
                            int initValue = property.getInt();

                            IntegerEditWidget edit = new IntegerEditWidget();
                            edit.content = String.valueOf(initValue);
                            edit.setPerfectSize(-1.0F, 32.0F + edit.insideMargins * 2.0F);
                            Consumer<String> onTextChange = (value) -> {
                                int intValue = edit.getIntValue();
                                if (intValue >= Integer.parseInt(property.getMinValue()) && intValue <= Integer.parseInt(property.getMaxValue())) {
                                    this.saveOperators.put(title, (Runnable)() -> property.set(intValue));
                                }

                            };
                            edit.setTextChangeCallBack(onTextChange);

                            IntegerSliderWidget sliderWidget = new IntegerSliderWidget().setRange(Integer.parseInt(property.getMinValue()), Integer.parseInt(property.getMaxValue()));
                            sliderWidget.setSliderChangeCallBack((integer) -> {
                                edit.setContent(integer.toString());
                                this.saveOperators.put(title, (Runnable)() -> property.set(integer));
                            });
                            edit.perfectWidth = sliderWidget.perfectWidth = -1;

                            valueWidget.setLayout(new HorizontalLayout());
                            valueWidget.addChild(edit);
                            valueWidget.addChild(sliderWidget);
                            valueWidget.setPerfectHeight(Arrays.asList(edit, sliderWidget));
                        }
                        break;
                    case BOOLEAN:
                        if (!property.isList()) {
                            boolean initValue = property.getBoolean();
                            ButtonWithTextWidget edit = new ButtonWithTextWidget();
                            edit.setText(String.valueOf(initValue)).setTextColor((Integer)this.boolColorMap.get(initValue));
                            edit.setPerfectSize(-1.0F, 32.0F + edit.insideMargins * 2.0F);
                            valueWidget = edit;
                            edit.setCallBack(() -> {
                                boolean setValue = false;
                                if (edit.text.equalsIgnoreCase("false")) {
                                    setValue = true;
                                } else {
                                    setValue = false;
                                }

                                edit.setText(String.valueOf(setValue)).setTextColor(this.boolColorMap.get(setValue));
                                edit.perfectWidth = -1.0F;
                                boolean finalSetValue = setValue;
                                this.saveOperators.put(title, () -> property.set(finalSetValue));
                            });
                        }
                        break;
                    case STRING:
                        if (!property.isList()) {
                            String initValue = property.getString();
                            TextEditWidget edit = new TextEditWidget();
                            edit.setContent(initValue);
                            edit.setPerfectSize(-1.0F, 32.0F + edit.insideMargins * 2.0F);
                            valueWidget = edit;
                            Consumer<String> onTextChange = (value) -> this.saveOperators.put(title, () -> property.set(value));
                            edit.setTextChangeCallBack(onTextChange);
                        }
                        break;
                    case DOUBLE:
                        double initValue = property.getDouble();
                        DoubleEditWidget edit = new DoubleEditWidget();
                        edit.setContent(String.valueOf(initValue));
                        edit.setPerfectSize(-1.0F, 32.0F + edit.insideMargins * 2.0F);
                        valueWidget = edit;
                        Consumer<String> onTextChange = (value) -> {
                            double doubleValue = edit.getDoubleValue();
                            if (doubleValue >= Double.parseDouble(property.getMinValue()) && doubleValue <= Double.parseDouble(property.getMaxValue())) {
                                this.saveOperators.put(title, () -> property.set(doubleValue));
                            }

                        };
                        edit.setTextChangeCallBack(onTextChange);
                }

                titleLabel.perfectWidth = valueWidget.perfectWidth = -1.0F;
                Widget hW = (new Widget()).setLayout(new HorizontalLayout());
                hW.addChild(titleLabel);
                hW.addChild(valueWidget);
                hW.setPerfectHeight(Arrays.asList(titleLabel, valueWidget));
                configList.addChild(hW);
            }
        }

        return configList;
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
