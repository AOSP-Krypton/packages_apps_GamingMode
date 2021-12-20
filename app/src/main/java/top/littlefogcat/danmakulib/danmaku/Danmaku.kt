package top.littlefogcat.danmakulib.danmaku

import android.graphics.Color

// TODO Remove JvmField annotation once everything is in kt
data class Danmaku(
    @JvmField var text: String? = null, // 文字
    @JvmField var size: Int = DEFAULT_TEXT_SIZE, // 字号
    @JvmField var mode: Mode = Mode.SCROLL, // 模式：滚动、顶部、底部
    @JvmField var color: Int = Color.WHITE, // 默认白色
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
