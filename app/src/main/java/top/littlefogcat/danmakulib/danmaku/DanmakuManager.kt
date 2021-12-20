package top.littlefogcat.danmakulib.danmaku

import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout

import dagger.hilt.android.qualifiers.ApplicationContext

import java.lang.ref.WeakReference

import javax.inject.Inject

import org.exthmui.game.R

class DanmakuManager @Inject constructor(
    @ApplicationContext val context: Context,
) {

    private val layoutInflater = LayoutInflater.from(context)

    /**
     * 弹幕容器
     */
    private lateinit var danmakuContainer: WeakReference<FrameLayout>

    val config = Config()

    private val positionCalculator = DanmakuPositionCalculator()

    /**
     * 初始化。在使用之前必须调用该方法。
     */
    fun init(container: FrameLayout) {
        danmakuContainer = WeakReference(container)
    }

    /**
     * 发送一条弹幕
     */
    fun send(danmaku: Danmaku): Int {
        if (danmakuContainer.get() == null) {
            Log.w(TAG, "show: Root view is null.")
            return RESULT_NULL_ROOT_VIEW
        }

        val view = (layoutInflater.inflate(
            R.layout.danmaku_view, danmakuContainer.get(), false
        ) as DanmakuView).also {
            it.addOnEnterListener { view -> view.clearScroll() }
            it.addOnExitListener { view -> view.restore() }
            it.danmaku = danmaku
            // 字体大小
            it.setTextSize(TypedValue.COMPLEX_UNIT_PX, danmaku.size.toFloat())
            // 字体颜色
            it.setTextColor(danmaku.color)
        }

        // 计算弹幕距离顶部的位置
        val marginTop: Int = positionCalculator.getMarginTop(view)

        if (marginTop == -1) {
            Log.w(TAG, "send(): screen is full, too many danmaku $danmaku")
            return TOO_MANY_DANMAKU
        }

        var lp = view.layoutParams as? FrameLayout.LayoutParams
        if (lp == null) {
            lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        lp.topMargin = marginTop
        view.layoutParams = lp

        view.minHeight = positionCalculator.getLineHeightWithPadding()

        var duration = getDisplayDuration(danmaku)
        if (danmaku.mode == Danmaku.Mode.SCROLL) {
            duration =
                (view.getTextLength() + positionCalculator.getParentWidth()) / duration * 1000
        }
        view.show(danmakuContainer.get()!!, duration)
        return RESULT_OK
    }

    /**
     * @return 返回这个弹幕显示时长 (对于滚动弹幕返回速度)
     */
    fun getDisplayDuration(danmaku: Danmaku?): Int =
        when (danmaku?.mode) {
            Danmaku.Mode.TOP -> config.durationTop
            Danmaku.Mode.BOTTOM -> config.durationBottom
            Danmaku.Mode.SCROLL -> config.scrollSpeed
            else -> config.scrollSpeed
        }

    /**
     * 一些配置
     */
    class Config {
        /**
         * 行高，单位px
         */
        var lineHeight = 0

        /**
         * 滚动弹幕速度 (px/s)
         */
        var scrollSpeed = 0

        /**
         * 顶部弹幕显示时长
         */
        var durationTop = 5000

        /**
         * 底部弹幕的显示时长
         */
        var durationBottom = 5000

        /**
         * 滚动弹幕的最大行数
         */
        var maxScrollLine = 12
    }

    private inner class DanmakuPositionCalculator {

        private val lastDanmakus = mutableListOf<DanmakuView>() // 保存每一行最后一个弹幕消失的时间
        private val tops = Array(config.maxScrollLine) { false }
        private val bottoms = Array(config.maxScrollLine) { false }

        fun getLineHeightWithPadding() = (config.lineHeight * 1.35).toInt()

        fun getMarginTop(view: DanmakuView) =
            when (view.danmaku?.mode) {
                Danmaku.Mode.SCROLL -> getScrollY(view)
                Danmaku.Mode.TOP -> getTopY(view)
                Danmaku.Mode.BOTTOM -> getBottomY(view)
                else -> -1
            }

        private fun getScrollY(view: DanmakuView): Int {
            if (lastDanmakus.isEmpty()) {
                lastDanmakus.add(view)
                return 0
            }

            var i = 0
            while (i < lastDanmakus.size) {
                val last = lastDanmakus[i]
                val timeDisappear = calTimeDisappear(last) // 最后一条弹幕还需多久消失
                val timeArrive = calTimeArrive(view) // 这条弹幕需要多久到达屏幕边缘
                val isFullyShown = isFullyShown(last)
                if (DEBUG) Log.d(
                    TAG,
                    "getScrollY: i: $i, timeDisappear: $timeDisappear, timeArrive: $timeArrive, isFullyShown: $isFullyShown"
                )
                if (timeDisappear <= timeArrive && isFullyShown) {
                    // 如果最后一个弹幕在这个弹幕到达之前消失，并且最后一个字已经显示完毕，
                    // 那么新的弹幕就可以在这一行显示
                    lastDanmakus[i] = view
                    return i * getLineHeightWithPadding()
                }
                i++
            }

            val maxLine = config.maxScrollLine
            if (maxLine == 0 || i < maxLine) {
                lastDanmakus.add(view)
                return i * getLineHeightWithPadding()
            }

            return -1
        }

        private fun getTopY(view: DanmakuView): Int {
            for (i in 0..tops.size) {
                if (!tops[i]) {
                    tops[i] = true
                    view.addOnExitListener { tops[i] = false }
                    return i * getLineHeightWithPadding()
                }
            }
            return -1
        }

        private fun getBottomY(view: DanmakuView): Int {
            for (i in 0..bottoms.size) {
                if (!bottoms[i]) {
                    bottoms[i] = true
                    view.addOnExitListener { bottoms[i] = false }
                    return getParentHeight() - (i + 1) * getLineHeightWithPadding()
                }
            }
            return -1
        }

        /**
         * 这条弹幕是否已经全部出来了。如果没有的话，
         * 后面的弹幕不能出来，否则就重叠了。
         */
        private fun isFullyShown(view: DanmakuView): Boolean {
            val scrollX = view.scrollX
            val textLength = view.getTextLength()
            if (DEBUG) Log.d(
                TAG,
                "isFullyShown? scrollX=$scrollX, textLength=$textLength, parentWidth=${getParentWidth()}"
            )
            return textLength - scrollX < getParentWidth()
        }

        /**
         * 这条弹幕还有多少毫秒彻底消失。
         */
        private fun calTimeDisappear(view: DanmakuView): Int {
            return ((view.getTextLength() - view.scrollX) / calSpeed(view)).toInt()
        }

        /**
         * 这条弹幕还要多少毫秒抵达屏幕边缘。
         */
        private fun calTimeArrive(view: DanmakuView): Int {
            return (getParentWidth() / calSpeed(view)).toInt()
        }

        /**
         * 这条弹幕的速度。单位：px/ms
         */
        private fun calSpeed(view: DanmakuView): Float {
            val s: Int = view.getTextLength() + getParentWidth()
            val t: Int = getDisplayDuration(view.danmaku)
            if (view.danmaku?.mode == Danmaku.Mode.SCROLL) {
                return t / 1000f
            }
            return s.toFloat() / t
        }

        private fun getParentHeight(): Int {
            val parent = danmakuContainer.get()
            if (parent == null || parent.height == 0) {
                return 1080
            }
            return parent.height
        }

        fun getParentWidth(): Int {
            val parent = danmakuContainer.get()
            if (parent == null || parent.width == 0) {
                return 1920
            }
            return parent.width
        }
    }


    companion object {
        private const val TAG = "DanmakuManager"
        private const val DEBUG = false

        const val RESULT_OK = 0
        const val RESULT_NULL_ROOT_VIEW = 1
        const val TOO_MANY_DANMAKU = 2
    }
}
