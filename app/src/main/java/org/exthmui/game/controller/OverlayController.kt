/*
 * Copyright (C) 2020 The exTHmUI Open Source Project
 * Copyright (C) 2021 AOSP-Krypton Project
 *
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

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView

import androidx.core.content.edit
import androidx.core.math.MathUtils
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager

import dagger.hilt.android.qualifiers.ApplicationContext

import javax.inject.Inject
import javax.inject.Singleton

import kotlin.math.sqrt

import org.exthmui.game.R
import org.exthmui.game.misc.Constants
import org.exthmui.game.ui.GamingPerformanceView
import org.exthmui.game.ui.QuickSettingsView
import org.exthmui.game.ui.QuickStartAppView

import top.littlefogcat.danmakulib.danmaku.Danmaku
import top.littlefogcat.danmakulib.danmaku.DanmakuManager

// TODO Replace deprecated LocalBroadcastManager with a different impl
@Singleton
class OverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mDanmakuManager: DanmakuManager,
) {

    private lateinit var gamingFloatingLayout: View
    private lateinit var gamingFloatingButton: ImageView
    private lateinit var callControlButton: ImageView
    private lateinit var gamingOverlayView: LinearLayout
    private lateinit var gamingMenu: ScrollView
    private lateinit var danmakuContainer: FrameLayout
    private lateinit var qsView: QuickSettingsView
    private lateinit var qsAppView: QuickStartAppView
    private lateinit var gamingPerfView: GamingPerformanceView

    private lateinit var gamingFBLayoutParams: WindowManager.LayoutParams
    private lateinit var danmakuLayoutParams: WindowManager.LayoutParams

    private val configBundle = Bundle()
    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var bounds = windowManager.currentWindowMetrics.bounds

    private var callStatus: Int = TelephonyManager.CALL_STATE_IDLE

    private var floatingButtonSize = 0f

    private var perfProfilesSupported = false
    private var isPortrait: Boolean

    private enum class CoordinateType {
        X,
        Y
    }

    private val mOMReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.Broadcasts.BROADCAST_GAMING_MENU_CONTROL -> {
                    val cmd = intent.getStringExtra("cmd")
                    if (cmd == "hide") {
                        showHideGamingMenu(2)
                    } else if (cmd == "show") {
                        showHideGamingMenu(1)
                    }
                }
            }
        }
    }

    private val callStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            callStatus = intent.getIntExtra("state", TelephonyManager.CALL_STATE_IDLE)
            when (callStatus) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    callControlButton.setImageResource(R.drawable.ic_call_accept)
                    callControlButton.visibility = View.VISIBLE
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    callControlButton.setImageResource(R.drawable.ic_call_end)
                    callControlButton.visibility = View.VISIBLE
                }
                TelephonyManager.CALL_STATE_IDLE -> callControlButton.visibility = View.GONE
            }
        }
    }

    init {
        isPortrait =
            context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    fun initController(bundle: Bundle) {
        if (Settings.canDrawOverlays(context)) {
            initView()
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.Broadcasts.BROADCAST_GAMING_MENU_CONTROL)
        LocalBroadcastManager.getInstance(context).registerReceiver(mOMReceiver, intentFilter)
        LocalBroadcastManager.getInstance(context).registerReceiver(
            callStatusReceiver,
            IntentFilter(Constants.Broadcasts.BROADCAST_CALL_STATUS)
        )
        updateConfig(bundle)
    }

    fun updateConfiguration(newConfig: Configuration) {
        isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
        updateConfig()
    }

    fun updateConfig(bundle: Bundle) {
        configBundle.putAll(bundle)
        updateConfig()
    }

    fun showDanmaku(danmakuText: String) {
        val danmaku = Danmaku().apply {
            text = danmakuText
            mode = Danmaku.Mode.SCROLL
            size = autoSize(
                if (isPortrait)
                    configBundle.getInt(
                        Constants.ConfigKeys.DANMAKU_SIZE_VERTICAL,
                        Constants.ConfigDefaultValues.DANMAKU_SIZE_VERTICAL
                    )
                else
                    configBundle.getInt(
                        Constants.ConfigKeys.DANMAKU_SIZE_HORIZONTAL,
                        Constants.ConfigDefaultValues.DANMAKU_SIZE_HORIZONTAL
                    )
            )
        }
        mDanmakuManager.send(danmaku)
    }

    private fun initView() {
        with(context.resources) {
            perfProfilesSupported =
                getBoolean(com.android.internal.R.bool.config_gamingmode_performance)
            floatingButtonSize = getDimension(R.dimen.game_button_size)
        }

        initGamingMenu()
        initFloatingLayout()
        initDanmaku()
        restoreFloatingButtonOffset()
    }

    private fun updateConfig() {
        bounds = windowManager.currentWindowMetrics.bounds

        qsView.setConfig(configBundle)

        if (configBundle.containsKey(Constants.ConfigKeys.QUICK_START_APPS)) {
            qsAppView.setConfig(configBundle)
        }

        restoreFloatingButtonOffset()

        // 弹幕设置同屏最大弹幕数
        val config = mDanmakuManager.config // 弹幕相关设置
        config.scrollSpeed = if (isPortrait)
            configBundle.getInt(
                Constants.ConfigKeys.DANMAKU_SPEED_VERTICAL,
                Constants.ConfigDefaultValues.DANMAKU_SPEED_VERTICAL
            ) else
            configBundle.getInt(
                Constants.ConfigKeys.DANMAKU_SPEED_HORIZONTAL,
                Constants.ConfigDefaultValues.DANMAKU_SPEED_HORIZONTAL
            )
        config.lineHeight = (if (isPortrait)
            configBundle.getInt(
                Constants.ConfigKeys.DANMAKU_SIZE_VERTICAL,
                Constants.ConfigDefaultValues.DANMAKU_SIZE_VERTICAL
            ) else
            configBundle.getInt(
                Constants.ConfigKeys.DANMAKU_SIZE_HORIZONTAL,
                Constants.ConfigDefaultValues.DANMAKU_SIZE_HORIZONTAL
            )) + 4 // 设置行高
        config.maxScrollLine = bounds.height() / 2 / config.lineHeight

        // 性能配置
        gamingPerfView.setLevel(
            configBundle.getInt(
                Constants.ConfigKeys.PERFORMANCE_LEVEL,
                Constants.ConfigDefaultValues.PERFORMANCE_LEVEL
            )
        )

        // Danmaku Container visibility
        val showDanmaku = configBundle.getBoolean(
            Constants.ConfigKeys.SHOW_DANMAKU,
            Constants.ConfigDefaultValues.SHOW_DANMAKU
        )
        danmakuLayoutParams.width = if (showDanmaku) WindowManager.LayoutParams.MATCH_PARENT else 0
        danmakuLayoutParams.height = if (showDanmaku) WindowManager.LayoutParams.WRAP_CONTENT else 0
        windowManager.updateViewLayout(danmakuContainer, danmakuLayoutParams)

        val menuOpacity = configBundle.getInt(
            Constants.ConfigKeys.MENU_OPACITY,
            Constants.ConfigDefaultValues.MENU_OPACITY
        )
        gamingMenu.background.alpha = menuOpacity * 255 / 100
    }

    private fun restoreFloatingButtonOffset() {
        // 悬浮球位置调整
        val defaultX = (floatingButtonSize.toInt() - bounds.width()) / 2
        setButtonOffset(
            CoordinateType.X, gamingFBLayoutParams,
            sharedPrefs.getInt(
                if (isPortrait) Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_VERTICAL_X
                else Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_HORIZONTAL_X,
                defaultX
            )
        )
        setButtonOffset(
            CoordinateType.Y, gamingFBLayoutParams,
            sharedPrefs.getInt(
                if (isPortrait) Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_VERTICAL_Y
                else Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_HORIZONTAL_Y,
                10
            )
        )
        windowManager.updateViewLayout(gamingFloatingLayout, gamingFBLayoutParams)
    }

    private fun getBaseLayoutParams() =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

    @SuppressLint("InflateParams")
    private fun initGamingMenu() {
        gamingOverlayView = LayoutInflater.from(context).inflate(
            R.layout.gaming_overlay_layout,
            null
        ) as LinearLayout
        gamingOverlayView.visibility = View.GONE
        gamingOverlayView.setOnClickListener { showHideGamingMenu(0) }

        val gamingViewLayoutParams = getBaseLayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        windowManager.addView(gamingOverlayView, gamingViewLayoutParams)

        qsView = gamingOverlayView.findViewById(R.id.gaming_qs)
        qsAppView = gamingOverlayView.findViewById(R.id.gaming_qsapp)

        gamingPerfView = gamingOverlayView.findViewById(R.id.performance_controller)

        if (!perfProfilesSupported) {
            gamingPerfView.visibility = View.GONE
        }

        gamingMenu = gamingOverlayView.findViewById(R.id.gaming_menu)
        gamingMenu.background.alpha = Constants.ConfigDefaultValues.MENU_OPACITY * 255 / 100
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun initFloatingLayout() {
        gamingFloatingLayout =
            LayoutInflater.from(context).inflate(R.layout.gaming_button_layout, null)

        gamingFBLayoutParams = getBaseLayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        windowManager.addView(gamingFloatingLayout, gamingFBLayoutParams)

        gamingFloatingButton = gamingFloatingLayout.findViewById(R.id.floating_button)
        gamingFloatingButton.setOnClickListener { showHideGamingMenu(0) }
        gamingFloatingButton.setOnTouchListener(
            object : View.OnTouchListener {
                var origX = 0
                var origY = 0
                var touchX = 0
                var touchY = 0

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    val x = event.rawX.toInt()
                    val y = event.rawY.toInt()

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            origX = gamingFBLayoutParams.x
                            origY = gamingFBLayoutParams.y
                            touchX = x
                            touchY = y
                        }
                        MotionEvent.ACTION_MOVE -> {
                            hideFloatingButton(false)
                            setButtonOffset(
                                CoordinateType.X,
                                gamingFBLayoutParams,
                                origX + x - touchX
                            )
                            setButtonOffset(
                                CoordinateType.Y,
                                gamingFBLayoutParams,
                                origY + y - touchY
                            )
                            windowManager.updateViewLayout(
                                gamingFloatingLayout,
                                gamingFBLayoutParams
                            )
                        }
                        MotionEvent.ACTION_UP -> {
                            hideFloatingButton(true)
                            if (calcDistance(
                                    origX,
                                    origY,
                                    gamingFBLayoutParams.x,
                                    gamingFBLayoutParams.y
                                ) < 5
                            ) {
                                v.performClick()
                            } else {
                                if (isPortrait) {
                                    sharedPrefs.edit(commit = true) {
                                        putInt(
                                            Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_VERTICAL_X,
                                            gamingFBLayoutParams.x
                                        )
                                        putInt(
                                            Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_VERTICAL_Y,
                                            gamingFBLayoutParams.y
                                        )
                                    }
                                } else {
                                    sharedPrefs.edit(commit = true) {
                                        putInt(
                                            Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_HORIZONTAL_X,
                                            gamingFBLayoutParams.x
                                        )
                                        putInt(
                                            Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_HORIZONTAL_Y,
                                            gamingFBLayoutParams.y
                                        )
                                    }
                                }
                            }
                        }
                        else -> return false
                    }
                    return true
                }
            })
        hideFloatingButton(hide = true, init = true)

        callControlButton =
            gamingFloatingLayout.findViewById<ImageView>(R.id.call_control_button).apply {
                setOnClickListener { callControl() }
            }
        restoreFloatingButtonOffset()
    }

    private fun setButtonOffset(
        type: CoordinateType,
        param: WindowManager.LayoutParams,
        value: Int
    ) {
        val rButton = floatingButtonSize.toInt() / 2
        val rScreenX = (bounds.width() - rButton) / 2
        val rScreenY = (bounds.height() - rButton) / 2

        if (type == CoordinateType.X) {
            param.x = MathUtils.clamp(
                value,
                if (isPortrait) -rScreenX else -rScreenX, rScreenX
            )
        } else {
            param.y = MathUtils.clamp(
                value,
                if (isPortrait) -rScreenY else -rScreenY, rScreenY
            )
        }
    }

    private fun calcDistance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
        return sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)).toDouble())
    }

    /*
     * mode: 0=auto, 1=show, 2=hide
     */
    private fun showHideGamingMenu(mode: Int) {

        if (gamingOverlayView.visibility == View.VISIBLE && mode != 1) {
            // hide
            val menuLayoutParams = getBaseLayoutParams()
            menuLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            menuLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            windowManager.updateViewLayout(gamingOverlayView, menuLayoutParams)
            gamingOverlayView.visibility = View.GONE
            gamingFloatingLayout.visibility = View.VISIBLE
            hideFloatingButton(true)
        } else if (mode != 2) {
            // show
            var gravity = 0
            gravity = if (gamingFBLayoutParams.x > 0) {
                gravity or Gravity.END
            } else {
                gravity or Gravity.START
            }
            gravity = if (gamingFBLayoutParams.y > 0) {
                gravity or Gravity.BOTTOM
            } else {
                gravity or Gravity.TOP
            }

            val menuLayoutParams = getBaseLayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
            }
            windowManager.updateViewLayout(gamingOverlayView, menuLayoutParams)
            gamingOverlayView.gravity = gravity
            gamingOverlayView.visibility = View.VISIBLE

            val gamingMenuLayoutParams = gamingMenu.layoutParams
            gamingMenuLayoutParams.width = if (isPortrait)
                WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT
            gamingMenu.layoutParams = gamingMenuLayoutParams

            gamingFloatingLayout.visibility = View.GONE
        }
    }

    private fun initDanmaku() {
        danmakuContainer = FrameLayout(context)
        danmakuLayoutParams = getBaseLayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.START or Gravity.TOP
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        windowManager.addView(danmakuContainer, danmakuLayoutParams)
        mDanmakuManager.init(danmakuContainer)
    }

    private fun autoSize(origin: Int): Int {
        val autoSize = origin * bounds.width() / sDesignWidth
        if (origin != 0 && autoSize == 0) {
            return 1
        }
        return autoSize
    }

    fun destroy() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mOMReceiver)
        LocalBroadcastManager.getInstance(context).unregisterReceiver(callStatusReceiver)
        if (windowManager != null) {
            windowManager.removeViewImmediate(gamingFloatingLayout)
            windowManager.removeViewImmediate(danmakuContainer)
            windowManager.removeViewImmediate(gamingOverlayView)
        }
    }


    private fun hideFloatingButton(hide: Boolean, init: Boolean) {
        val current: Float = gamingFloatingButton.alpha
        val delayedHide: Long =
            if (init) FLOATING_BUTTON_HIDE_DELAY * 2 else FLOATING_BUTTON_HIDE_DELAY
        if (hide && current == 1f) {
            gamingFloatingButton.animate()
                .alpha(FLOATING_BUTTON_HIDE_ALPHA)
                .setStartDelay(delayedHide).duration = 250
        } else {
            gamingFloatingButton.alpha = 1f
        }
    }

    private fun hideFloatingButton(hide: Boolean) {
        hideFloatingButton(hide, false)
    }

    private fun callControl() {
        val intent = Intent(Constants.Broadcasts.BROADCAST_CALL_CONTROL)
        intent.putExtra("cmd", if (callStatus == TelephonyManager.CALL_STATE_OFFHOOK) 1 else 2)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    companion object {
        /** Default delay for the floating button  to be hidden (in ms) */
        private const val FLOATING_BUTTON_HIDE_DELAY = 1000L

        /** Default alpha value for hiding the floating button hidden */
        private const val FLOATING_BUTTON_HIDE_ALPHA = 0.1f

        private const val sDesignWidth = 1080
    }
}
