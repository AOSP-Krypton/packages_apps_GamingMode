package top.littlefogcat.danmakulib.danmaku

import android.graphics.Color

data class Danmaku(
    var text: String? = null, // 文字
    var size: Int = DEFAULT_TEXT_SIZE, // 字号
    var mode: Mode = Mode.SCROLL, // 模式：滚动、顶部、底部
    var color: Int = Color.WHITE, // 默认白色
) {

    enum class Mode {
        SCROLL,
        TOP,
        BOTTOM
    }

    companion object {
        const val DEFAULT_TEXT_SIZE = 24
    }
}
