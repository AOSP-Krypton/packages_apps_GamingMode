/*
 * Copyright (C) 2020 The exTHmUI Open Source Project
 * Copyright (C) 2021 AOSP-Krypton Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.exthmui.game.controller

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout

import dagger.hilt.android.qualifiers.ApplicationContext

import javax.inject.Inject
import javax.inject.Singleton

import org.exthmui.game.R

import top.littlefogcat.danmakulib.danmaku.Danmaku
import top.littlefogcat.danmakulib.danmaku.DanmakuView

@Singleton
class DanmakuController @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewController(context) {

    /**
     * 弹幕容器
     */
    private var danmakuContainer: FrameLayout? = null

    private val positionCalculator = DanmakuPositionCalculator()

    private var danmakuLayoutParams: WindowManager.LayoutParams? = null

    private var showDanmaku = false

    /**
     * 行高，单位px
     */
    private var lineHeight = 0

    /**
     * 滚动弹幕速度 (px/s)
     */
    private var scrollSpeed = 0

    /**
     * 顶部弹幕显示时长
     */
    private var durationTop = 5000

    /**
     * 底部弹幕的显示时长
     */
    private var durationBottom = 5000

    /**
     * 滚动弹幕的最大行数
     */
    private var maxScrollLine = 12

    private var sizeLandscape = DEFAULT_DANMAKU_SIZE_LANDSCAPE
    private var sizePortrait = DEFAULT_DANMAKU_SIZE_PORTRAIT

    private var speedLandscape = DEFAULT_DANMAKU_SPEED_LANDSCAPE
    private var speedPortrait = DEFAULT_DANMAKU_SPEED_PORTRAIT

    override fun initController() {
        loadSettings()
        danmakuContainer = FrameLayout(context)
        danmakuContainer?.visibility = if (showDanmaku) View.VISIBLE else View.GONE
        danmakuLayoutParams = getBaseLayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.START or Gravity.TOP
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        windowManager.addView(danmakuContainer, danmakuLayoutParams)
        updateViewConfig()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        updateViewConfig()
    }
    fun setShowDanmaku(show: Boolean) {
        showDanmaku = show
    }

    fun shouldShowDanmaku() = showDanmaku

    fun showDanmaku(danmakuText: String) {
        val danmaku = Danmaku().apply {
            text = danmakuText
            mode = Danmaku.Mode.SCROLL
            size = autoSize(
                if (isPortrait) sizePortrait
                else sizeLandscape
            )
        }
        show(danmaku)
    }

    private fun loadSettings() {
        sizePortrait = Settings.System.getInt(
            context.contentResolver,
            Settings.System.GAMING_MODE_DANMAKU_SIZE_VERTICAL, DEFAULT_DANMAKU_SIZE_PORTRAIT
        )
        sizeLandscape = Settings.System.getInt(
            context.contentResolver,
            Settings.System.GAMING_MODE_DANMAKU_SIZE_HORIZONTAL, DEFAULT_DANMAKU_SIZE_LANDSCAPE
        )
        speedLandscape = Settings.System.getInt(
            context.contentResolver,
            Settings.System.GAMING_MODE_DANMAKU_SPEED_HORIZONTAL, DEFAULT_DANMAKU_SPEED_LANDSCAPE
        )
        speedPortrait = Settings.System.getInt(
            context.contentResolver,
            Settings.System.GAMING_MODE_DANMAKU_SPEED_VERTICAL, DEFAULT_DANMAKU_SPEED_PORTRAIT
        )
        // Danmaku Container visibility
        showDanmaku = Settings.System.getInt(
            context.contentResolver,
            Settings.System.GAMING_MODE_SHOW_DANMAKU, 1
        ) == 1
    }

    private fun updateViewConfig() {
        scrollSpeed = if (isPortrait) speedPortrait else speedLandscape
        lineHeight = autoSize((if (isPortrait) sizePortrait else sizeLandscape) + 4) // 设置行高
        maxScrollLine = bounds.height() / 2 / lineHeight
    }

    /**
     * 发送一条弹幕
     */
    private fun show(danmaku: Danmaku): Int {
        if (danmakuContainer == null) {
            Log.w(TAG, "show: Container is null.")
            return RESULT_NULL_ROOT_VIEW
        }

        val view = (layoutInflater.inflate(
            R.layout.danmaku_view, danmakuContainer, false
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
        view.show(danmakuContainer!!, duration)
        return RESULT_OK
    }

    /**
     * @return 返回这个弹幕显示时长 (对于滚动弹幕返回速度)
     */
    private fun getDisplayDuration(danmaku: Danmaku?): Int =
        when (danmaku?.mode) {
            Danmaku.Mode.TOP -> durationTop
            Danmaku.Mode.BOTTOM -> durationBottom
            Danmaku.Mode.SCROLL -> scrollSpeed
            else -> scrollSpeed
        }

    override fun onDestroy() {
        windowManager.removeViewImmediate(danmakuContainer)
        danmakuContainer = null
    }

    private fun autoSize(origin: Int): Int {
        val autoSize = origin * bounds.width() / (if (isPortrait) DESIGN_WIDTH else DESIGN_HEIGHT)
        if (origin != 0 && autoSize == 0) {
            return 1
        }
        return autoSize
    }

    private inner class DanmakuPositionCalculator {

        private val lastDanmakus = mutableListOf<DanmakuView>() // 保存每一行最后一个弹幕消失的时间
        private val tops = Array(maxScrollLine) { false }
        private val bottoms = Array(maxScrollLine) { false }

        fun getLineHeightWithPadding() = (lineHeight * 1.35).toInt()

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

            val maxLine = maxScrollLine
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
            return danmakuContainer?.height ?: 1080
        }

        fun getParentWidth(): Int {
            return danmakuContainer?.width ?: 1920
        }
    }

    companion object {
        private const val TAG = "DanmakuManager"
        private const val DEBUG = false

        const val RESULT_OK = 0
        const val RESULT_NULL_ROOT_VIEW = 1
        const val TOO_MANY_DANMAKU = 2

        private const val DESIGN_WIDTH = 1080
        private const val DESIGN_HEIGHT = 1920

        // 弹幕速度
        private const val DEFAULT_DANMAKU_SPEED_LANDSCAPE = 300
        private const val DEFAULT_DANMAKU_SPEED_PORTRAIT = 300

        // 弹幕大小
        private const val DEFAULT_DANMAKU_SIZE_LANDSCAPE = 36
        private const val DEFAULT_DANMAKU_SIZE_PORTRAIT = 36
    }
}
