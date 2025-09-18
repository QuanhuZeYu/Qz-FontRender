# 公共方法

## onResourceManagerReload

重载资源包时触发


## drawStringWithShadow

绘制带有阴影的指定字符串


## drawString

绘制指定字符串 (是否带阴影)


## getStringWidth

返回此字符串的宽度。相当于 FontMetrics.stringWidth（String s）。


## trimStringToWidth

修剪字符串以适合指定的宽度 (如果设置了 par3，则将其反转。)


## splitStringWidth

拆分并绘制带有自动换行的字符串（最大长度为参数 k）


## setUnicodeFlag

设置 unicodeFlag 控制字符串是否应使用 Unicode 字体而不是 default.png 字体呈现。


## getUnicodeFlag

获取 unicodeFlag，控制字符串是否应使用 Unicode 字体而不是default.png字体呈现。


## setBidiFlag

设置 bidiFlag 以控制在呈现任何字符串之前是否应运行 Unicode 双向算法。


## listFormattedStringToWidth

将字符串分解为适合指定宽度的片段列表。


## getBidiFlag

获取 bidiFlag，用于控制是否应在呈现任何字符串之前运行 Unicode 双向算法