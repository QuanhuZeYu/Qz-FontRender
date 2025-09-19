package club.heiqi.qz_fontrender.docs;

import club.heiqi.qz_fontrender.fontSystem.utils.StringUTF32;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

public class TextField extends GuiTextField {
    private final FontRenderer fontRenderer;
    /** 文本框的位置坐标 */
    public int xPosition, yPosition;
    /** 文本框的尺寸. */
    public int width, height;
    /** 当前文本框中的文本内容 */
    private String text = "";
    /**最大允许的字符数*/
    private int maxStringLength = 32;
    /**光标闪烁计数器*/
    private int cursorCounter;
    private boolean enableBackgroundDrawing = true;
    /**如果是true，文本框可能会通过单击屏幕上的其他位置来失去焦点*/
    private boolean canLoseFocus = true;
    /** 文本框是否获得焦点 */
    private boolean isFocused;
    /**如果此值与iSfocused一起是正确的，则Keytyped将处理键。 */
    private boolean isEnabled = true;
    /**应将当前字符索引用作渲染文本的开始。 */
    private int lineScrollOffset;
    /**当前光标位置 UTF32 索引位置*/
    private int cursorPosition;
    /**其他选择位置，也许与光标相同 | 文本选择结束位置 UTF32 索引位置*/
    private int selectionEnd;
    private int enabledColor = 14737632;
    private int disabledColor = 7368816;
    /**如果可见此文本框*/
    private boolean visible = true;


    public String filterAllowedCharacters(String input) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length();) {
            int codepoint = input.codePointAt(i);
            int charCount = Character.charCount(codepoint);
            i += charCount;
            String s = new String(Character.toChars(codepoint));
            if (!s.equals("§") && codepoint >= 32 && codepoint != 127) {
                builder.append(s);
            }
        }
        return builder.toString();
    }
    
    public boolean isAllowedCharacter(int codepoint) {
        String s = new String(Character.toChars(codepoint));
        return (!s.equals("§") && codepoint >= 32 && codepoint != 127);
    }

    public TextField(FontRenderer fontRenderer, int x, int y, int width, int height) {
        super(fontRenderer,x,y,width,height);
        this.fontRenderer = fontRenderer;
        this.xPosition = x;
        this.yPosition = y;
        this.width = width;
        this.height = height;
    }

    /**增加光标计数器*/
    public void updateCursorCounter() {
        ++this.cursorCounter;
    }

    /**设置文本框的文本*/
    public void setText(String text) {
        if (text.codePointCount(0, text.length()) > this.maxStringLength) {
            int count = 0;
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < text.length();) {
                int codepoint = text.codePointAt(i);
                int charCount = Character.charCount(codepoint);
                i += charCount;
                count++;
                builder.append(new String(Character.toChars(codepoint)));
                if (count >= this.maxStringLength) {
                    this.text = builder.toString();
                    break;
                }
            }
        }
        else {
            this.text = text;
        }

        this.setCursorPositionEnd();
    }

    /**返回文本框的内容*/
    public String getText() {
        return this.text;
    }

    /**返回光标和SelectionEnd之间的文本*/
    public String getSelectedText() {
        int left = Math.min(this.cursorPosition, this.selectionEnd);
        int right = Math.max(this.cursorPosition, this.selectionEnd);
        StringBuilder builder = new StringBuilder();
        int charIndex = 0;
        // 取出 left - right 的文本 包含 l r
        for (int i = 0; i < StringUTF32.length(this.text);) {
            int codepoint = this.text.codePointAt(i);
            char[] chars = Character.toChars(codepoint);
            String s = new String(chars);
            int charCount = Character.charCount(codepoint);
            if (charIndex >= left) {
                builder.append(s);
            }
            if (charIndex >= right) {
                break;
            }
            i += charCount;
            charIndex++;
        }
        return builder.toString();
    }

    /**替换选定的文本，或在光标上的位置插入文本*/
    public void writeText(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }

        String filteredInput = filterAllowedCharacters(input);
        if (filteredInput.isEmpty()) {
            return;
        }

        // 确定选择区域的左右边界
        int left = Math.min(cursorPosition, selectionEnd);
        int right = Math.max(cursorPosition, selectionEnd);

        // 计算当前文本的字符数（考虑Unicode字符）
        int currentLength = StringUTF32.length(this.text);
        int selectionLength = right - left;

        // 计算可以写入的最大字符数
        int availableSpace = maxStringLength - currentLength + selectionLength;
        if (availableSpace <= 0) {
            // 没有可用空间
            return;
        }

        // 截取输入文本以适应可用空间
        String textToInsert;
        // 如果输入文本长度大于可写入长度
        if (StringUTF32.length(filteredInput) > availableSpace) {
            textToInsert = StringUTF32.get(filteredInput, 0, availableSpace - 1);
        } else {
            textToInsert = filteredInput;
        }

        // 构建新文本：左侧文本 + 插入文本 + 右侧文本
        StringBuilder newText = new StringBuilder();
        newText.append(StringUTF32.get(this.text, 0, left - 1));
        newText.append(textToInsert);
        newText.append(StringUTF32.get(this.text, left, StringUTF32.length(this.text) - 1));

        text = newText.toString();

        // 移动光标到插入结束的位置
        int insertedLength = StringUTF32.length(textToInsert);
        cursorPosition = left + insertedLength;
        selectionEnd = cursorPosition;
    }

    /**删除从光标位置开始的指定数量的单词。负数将删除剩下的单词
     *光标。*/
    public void deleteWords(int count) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                this.writeText("");
            }
            else {
                this.deleteFromCursor(this.getNthWordFromCursor(count) - this.cursorPosition);
            }
        }
    }

    /**删除选定的文本，sherwsie从光标的两侧删除字符。参数：删除num*/
    public void deleteFromCursor(int count) {
        if (count == 0) return;
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                this.writeText("");
            }
            else {
                if (count < 0) {
                    this.text = StringUTF32.delete(this.text, this.cursorPosition-1, this.cursorPosition-1);
                    this.cursorPosition--;
                }
                else {
                    this.text = StringUTF32.delete(this.text, this.cursorPosition, this.cursorPosition);
                }
            }
        }
    }

    /**获取N个偏移位置单词的光标位置*/
    public int getNthWordFromCursor(int count) {
        return this.getNthWordFromPos(count, this.getCursorPosition());
    }

    /**
     *获得n个单词的位置。 n可能是反向的，然后向后看。参数：n，位置
     */
    public int getNthWordFromPos(int count, int cursorPosition) {
        return this.func_146197_a(count, cursorPosition, true);
    }

    @Override
    public int func_146197_a(int count, int cursorPos, boolean b) {
        int cursorPosCopy = cursorPos;
        boolean isCountNeg = count < 0;
        int countAbs = Math.abs(count);

        int pointCount = this.text.codePointCount(0, StringUTF32.length(this.text));
        boolean[] mask = new boolean[pointCount];

        int maskIndex = 0;
        for (int i = 0; i < StringUTF32.length(this.text);) {
            int codepoint = this.text.codePointAt(i);
            int charCount = Character.charCount(codepoint);
            String s = new String(Character.toChars(codepoint));

            mask[maskIndex] = s.equals(" ");

            i += charCount;
            maskIndex++;
        }

        for (int i = 0; i < countAbs; ++i) {
            if (isCountNeg) {
                // 将指针向前移动到非空格位置
                // 如果 b 并且 光标位置大于0 并且 光标位置-1的字符为空格
                while (b && cursorPosCopy > 0 && mask[cursorPosCopy - 1]) {
                    // 复制光标的位置递减
                    --cursorPosCopy;
                }

                // 继续将指针移动到非空格位置
                while (cursorPosCopy > 0 && !mask[cursorPosCopy - 1]) {
                    --cursorPosCopy;
                }
            }
            else {
                // |<-前 后->|
                // 将指针向后移动到空格处
                while (b && cursorPosCopy < pointCount && !mask[cursorPosCopy - 1]) {
                    ++cursorPosCopy;
                }
                // 如果找到了，将光标递增到下一个非空格位置
                while (b && cursorPosCopy < pointCount && mask[cursorPosCopy - 1]) {
                    ++cursorPosCopy;
                }
            }
        }

        //返回光标位置
        return cursorPosCopy;
    }

    /**通过指定数量的字符移动文本光标，并清除选择*/
    public void moveCursorBy(int count) {
        this.setCursorPosition(this.selectionEnd + count);
    }

    /**将光标的位置设置为提供的索引*/
    public void setCursorPosition(int index) {
        this.cursorPosition = index;
        int length = StringUTF32.length(this.text);

        if (this.cursorPosition < 0) {
            this.cursorPosition = 0;
        }

        if (this.cursorPosition > length) {
            this.cursorPosition = length;
        }

        this.setSelectionPos(this.cursorPosition);
    }

    /**将光标位置设置为开始*/
    public void setCursorPositionZero() {
        this.setCursorPosition(0);
    }

    /**将光标位置设置为文本之后*/
    public void setCursorPositionEnd() {
        this.setCursorPosition(StringUTF32.length(this.text));
    }

    /**从您的guiscreen调用此方法将键处理到文本框中*/
    public boolean textboxKeyTyped(char charIn, int codepoint) {
        if (!this.isFocused) {
            return false;
        }

        switch (charIn) {
            // 光标回到开始
            case 1:
                this.setCursorPositionEnd();
                this.setSelectionPos(0);
                return true;
            // 复制选择的文字
            case 3:
                GuiScreen.setClipboardString(this.getSelectedText());
                return true;
            // 粘贴选择文字
            case 22:
                if (this.isEnabled) {
                    this.writeText(GuiScreen.getClipboardString());
                }
                return true;
            // 剪切
            case 24:
                GuiScreen.setClipboardString(this.getSelectedText());

                if (this.isEnabled) {
                    this.writeText("");
                }

                return true;
            // 操作符之外的任意输入
            default:
                switch (codepoint) {
                    // 退格键
                    case 14:
                        if (GuiScreen.isCtrlKeyDown()) {
                            if (this.isEnabled) {
                                this.deleteWords(-1);
                            }
                        }
                        else if (this.isEnabled) {
                            this.deleteFromCursor(-1);
                        }

                        return true;
                    // home?
                    case 199:
                        if (GuiScreen.isShiftKeyDown()) {
                            this.setSelectionPos(0);
                        }
                        else {
                            this.setCursorPositionZero();
                        }

                        return true;
                    // 左方向键
                    case 203:
                        if (GuiScreen.isShiftKeyDown()) {
                            if (GuiScreen.isCtrlKeyDown()) {
                                this.setSelectionPos(this.getNthWordFromPos(-1, this.getSelectionEnd()));
                            }
                            else {
                                this.setSelectionPos(this.getSelectionEnd() - 1);
                            }
                        }
                        else if (GuiScreen.isCtrlKeyDown()) {
                            this.setCursorPosition(this.getNthWordFromCursor(-1));
                        }
                        else {
                            this.moveCursorBy(-1);
                        }

                        return true;
                    // 右方向键
                    case 205:
                        if (GuiScreen.isShiftKeyDown()) {
                            if (GuiScreen.isCtrlKeyDown()) {
                                this.setSelectionPos(this.getNthWordFromPos(1, this.getSelectionEnd()));
                            }
                            else {
                                this.setSelectionPos(this.getSelectionEnd() + 1);
                            }
                        }
                        else if (GuiScreen.isCtrlKeyDown()) {
                            this.setCursorPosition(this.getNthWordFromCursor(1));
                        }
                        else {
                            this.moveCursorBy(1);
                        }

                        return true;
                    // END键
                    case 207:
                        if (GuiScreen.isShiftKeyDown()) {
                            this.setSelectionPos(StringUTF32.length(this.text));
                        }
                        else {
                            this.setCursorPositionEnd();
                        }

                        return true;
                    // 后退格？
                    case 211:
                        if (GuiScreen.isCtrlKeyDown()) {
                            if (this.isEnabled) {
                                this.deleteWords(1);
                            }
                        }
                        else if (this.isEnabled) {
                            this.deleteFromCursor(1);
                        }

                        return true;
                    // 其他codepoint
                    default:
                        if (isAllowedCharacter(codepoint)) {
                            if (this.isEnabled) {
                                this.writeText(new String(Character.toChars(codepoint)));
                            }

                            return true;
                        }
                        else {
                            return false;
                        }
                }
        }
    }

    /**
     * Args: x, y, buttonClicked
     */
    public void mouseClicked(int x, int y, int buttonClicked) {
        boolean flag = x >= this.xPosition && x < this.xPosition + this.width && y >= this.yPosition && y < this.yPosition + this.height;

        if (this.canLoseFocus) {
            this.setFocused(flag);
        }

        if (this.isFocused && buttonClicked == 0) {
            int distanceX = x - this.xPosition;

            if (this.enableBackgroundDrawing) {
                distanceX -= 4;
            }

            String s = this.fontRenderer.trimStringToWidth(StringUTF32.get(this.text,0,this.lineScrollOffset), this.getWidth());
            this.setCursorPosition(this.fontRenderer.trimStringToWidth(s, distanceX).length() + this.lineScrollOffset);
        }
    }

    /**
     * Draws the textbox
     */
    public void drawTextBox() {
        if (this.getVisible()) {
            if (this.getEnableBackgroundDrawing()) {
                drawRect(this.xPosition - 1, this.yPosition - 1, this.xPosition + this.width + 1, this.yPosition + this.height + 1, -6250336);
                drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, -16777216);
            }

            int i = this.isEnabled ? this.enabledColor : this.disabledColor;
            int count1 = this.cursorPosition - this.lineScrollOffset;
            int count2 = this.selectionEnd - this.lineScrollOffset;
            String s = this.fontRenderer.trimStringToWidth(StringUTF32.get(this.text,0,this.lineScrollOffset), this.getWidth());
            boolean flag = count1 >= 0 && count1 <= StringUTF32.length(s);
            boolean flag1 = this.isFocused && this.cursorCounter / 6 % 2 == 0 && flag;
            int l = this.enableBackgroundDrawing ? this.xPosition + 4 : this.xPosition;
            int i1 = this.enableBackgroundDrawing ? this.yPosition + (this.height - 8) / 2 : this.yPosition;
            int j1 = l;

            if (count2 > StringUTF32.length(s)) {
                count2 = StringUTF32.length(s);
            }

            if (!s.isEmpty()) {
                String s1 = flag ? /* s.substring(0, count1) */StringUTF32.get(s,0,count1-1) : s;
                j1 = this.fontRenderer.drawStringWithShadow(s1, l, i1, i);
            }

            boolean flag2 = this.cursorPosition < StringUTF32.length(this.text) || StringUTF32.length(this.text) >= this.getMaxStringLength();
            int k1 = j1;

            if (!flag)
            {
                k1 = count1 > 0 ? l + this.width : l;
            }
            else if (flag2)
            {
                k1 = j1 - 1;
                --j1;
            }

            if (StringUTF32.length(s) > 0 && flag && count1 < StringUTF32.length(s))
            {
                this.fontRenderer.drawStringWithShadow(StringUTF32.get(s,count1,s.length()), j1, i1, i);
            }

            if (flag1)
            {
                if (flag2)
                {
                    Gui.drawRect(k1, i1 - 1, k1 + 1, i1 + 1 + this.fontRenderer.FONT_HEIGHT, -3092272);
                }
                else
                {
                    this.fontRenderer.drawStringWithShadow("_", k1, i1, i);
                }
            }

            if (count2 != count1)
            {
                int l1 = l + this.fontRenderer.getStringWidth(/* s.substring(0, count2) */StringUTF32.get(s,0,count2-1));
                this.drawCursorVertical(k1, i1 - 1, l1 - 1, i1 + 1 + this.fontRenderer.FONT_HEIGHT);
            }
        }
    }

    /**
     * draws the vertical line cursor in the textbox
     */
    private void drawCursorVertical(int p_146188_1_, int p_146188_2_, int p_146188_3_, int p_146188_4_) {
        int i1;

        if (p_146188_1_ < p_146188_3_) {
            i1 = p_146188_1_;
            p_146188_1_ = p_146188_3_;
            p_146188_3_ = i1;
        }

        if (p_146188_2_ < p_146188_4_) {
            i1 = p_146188_2_;
            p_146188_2_ = p_146188_4_;
            p_146188_4_ = i1;
        }

        if (p_146188_3_ > this.xPosition + this.width) {
            p_146188_3_ = this.xPosition + this.width;
        }

        if (p_146188_1_ > this.xPosition + this.width) {
            p_146188_1_ = this.xPosition + this.width;
        }

        Tessellator tessellator = Tessellator.instance;
        GL11.glColor4f(0.0F, 0.0F, 255.0F, 255.0F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
        GL11.glLogicOp(GL11.GL_OR_REVERSE);
        tessellator.startDrawingQuads();
        tessellator.addVertex((double)p_146188_1_, (double)p_146188_4_, 0.0D);
        tessellator.addVertex((double)p_146188_3_, (double)p_146188_4_, 0.0D);
        tessellator.addVertex((double)p_146188_3_, (double)p_146188_2_, 0.0D);
        tessellator.addVertex((double)p_146188_1_, (double)p_146188_2_, 0.0D);
        tessellator.draw();
        GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public void setMaxStringLength(int length) {
        this.maxStringLength = length;

        if (StringUTF32.length(this.text) > length) {
            this.text = this.text.substring(0, length);
        }
    }

    /**
     * returns the maximum number of character that can be contained in this textbox
     */
    public int getMaxStringLength() {
        return this.maxStringLength;
    }

    /**
     * returns the current position of the cursor
     */
    public int getCursorPosition() {
        return this.cursorPosition;
    }

    /**
     * get enable drawing background and outline
     */
    public boolean getEnableBackgroundDrawing() {
        return this.enableBackgroundDrawing;
    }

    /**
     * enable drawing background and outline
     */
    public void setEnableBackgroundDrawing(boolean p_146185_1_) {
        this.enableBackgroundDrawing = p_146185_1_;
    }

    /**
     * Sets the text colour for this textbox (disabled text will not use this colour)
     */
    public void setTextColor(int p_146193_1_) {
        this.enabledColor = p_146193_1_;
    }

    public void setDisabledTextColour(int p_146204_1_) {
        this.disabledColor = p_146204_1_;
    }

    /**
     * Sets focus to this gui element
     */
    public void setFocused(boolean p_146195_1_) {
        if (p_146195_1_ && !this.isFocused) {
            this.cursorCounter = 0;
        }

        this.isFocused = p_146195_1_;
    }

    /**
     * Getter for the focused field
     */
    public boolean isFocused() {
        return this.isFocused;
    }

    public void setEnabled(boolean p_146184_1_) {
        this.isEnabled = p_146184_1_;
    }

    /**
     * the side of the selection that is not the cursor, may be the same as the cursor
     */
    public int getSelectionEnd() {
        return this.selectionEnd;
    }

    /**
     * returns the width of the textbox depending on if background drawing is enabled
     */
    public int getWidth() {
        return this.getEnableBackgroundDrawing() ? this.width - 8 : this.width;
    }

    /**
     * Sets the position of the selection anchor (i.e. position the selection was started at)
     */
    public void setSelectionPos(int pos) {
        int length = StringUTF32.length(this.text);

        if (pos > length) {
            pos = length;
        }

        if (pos < 0) {
            pos = 0;
        }

        this.selectionEnd = pos;

        if (this.fontRenderer != null) {
            if (this.lineScrollOffset > length) {
                this.lineScrollOffset = length;
            }

            int width1 = this.getWidth();
            String s = this.fontRenderer.trimStringToWidth(StringUTF32.get(this.text,0,this.lineScrollOffset), width1);
            int l = StringUTF32.length(s) + this.lineScrollOffset;

            if (pos == this.lineScrollOffset) {
                this.lineScrollOffset -= this.fontRenderer.trimStringToWidth(this.text, width1, true).length();
            }

            if (pos > l) {
                this.lineScrollOffset += pos - l;
            }
            else if (pos <= this.lineScrollOffset) {
                this.lineScrollOffset -= this.lineScrollOffset - pos;
            }

            if (this.lineScrollOffset < 0) {
                this.lineScrollOffset = 0;
            }

            if (this.lineScrollOffset > length) {
                this.lineScrollOffset = length;
            }
        }
    }

    /**
     * if true the textbox can lose focus by clicking elsewhere on the screen
     */
    public void setCanLoseFocus(boolean p_146205_1_) {
        this.canLoseFocus = p_146205_1_;
    }

    /**
     * returns true if this textbox is visible
     */
    public boolean getVisible() {
        return this.visible;
    }

    /**
     * Sets whether or not this textbox is visible
     */
    public void setVisible(boolean p_146189_1_) {
        this.visible = p_146189_1_;
    }
}
