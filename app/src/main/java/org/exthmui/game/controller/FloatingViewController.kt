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

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.SystemProperties
import android.provider.Settings
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView

import androidx.core.content.edit
import androidx.core.math.MathUtils
import androidx.preference.PreferenceManager

import dagger.hilt.android.qualifiers.ApplicationContext

import javax.inject.Inject
import javax.inject.Singleton

import kotlin.math.sqrt

import org.exthmui.game.R
import org.exthmui.game.ui.GamingPerformanceView
import org.exthmui.game.ui.QuickSettingsView
import org.exthmui.game.ui.QuickStartAppView

@Singleton
class FloatingViewController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callViewController: CallViewController,
    private val notificationOverlayController: NotificationOverlayController,
) : ViewController(context) {

    private var gamingFloatingLayout: View? = null
    private var gamingFloatingButton: ImageView? = null
    private var gamingOverlayView: LinearLayout? = null
    private var gamingMenu: ScrollView? = null
    private var qsView: QuickSettingsView? = null
    private var qsAppView: QuickStartAppView? = null
    private var gamingPerfView: GamingPerformanceView? = null

    private lateinit var gamingFBLayoutParams: WindowManager.LayoutParams

    private var qsApps: List<String>? = null
    private var menuOpacity = DEFAULT_MENU_OPACITY
    private var floatingButtonSize = 0f

    private var perfProfilesSupported = false
    private var changePerfLevel = true
    private var performanceLevel = DEFAULT_PERFORMANCE_LEVEL

    private lateinit var sharedPrefs: SharedPreferences

    override fun initController() {
        with(context.resources) {
            perfProfilesSupported =
                getBoolean(com.android.internal.R.bool.config_gamingmode_performance)
            floatingButtonSize = getDimension(R.dimen.game_button_size)
        }
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        loadSettings()
        initGamingMenu()
        initFloatingLayout()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        floatingButtonSize = context.resources.getDimension(R.dimen.game_button_size)
        restoreFloatingButtonOffset()
    }

    override fun onDestroy() {
        windowManager.removeViewImmediate(gamingFloatingLayout)
        gamingFloatingLayout = null
        gamingFloatingButton = null

        windowManager.removeViewImmediate(gamingOverlayView)
        gamingOverlayView = null
        gamingMenu = null
        qsView = null
        qsAppView = null
        gamingPerfView?.setOnUpdateListener(null)
        gamingPerfView = null
    }

    fun hideGamingMenu() {
        showHideGamingMenu(2)
    }

    private fun loadSettings() {
        val qsAppsFlattened: String? = Settings.System.getString(
            context.contentResolver,
            Settings.System.GAMING_MODE_QS_APP_LIST
        )
        qsApps = qsAppsFlattened?.takeIf { it.isNotEmpty() }?.split(";")?.filter { it.isNotBlank() }
        changePerfLevel = Settings.System.getInt(
            context.contentResolver,
            Settings.System.GAMING_MODE_CHANGE_PERFORMANCE_LEVEL,
            1
        ) == 1
        performanceLevel = Settings.System.getInt(
            context.contentResolver,
            Settings.System.GAMING_MODE_PERFORMANCE_LEVEL,
            DEFAULT_PERFORMANCE_LEVEL
        )
        menuOpacity = Settings.System.getInt(
            context.contentResolver,
            Settings.System.GAMING_MODE_MENU_OPACITY, DEFAULT_MENU_OPACITY
        )
    }

    private fun restoreFloatingButtonOffset() {
        // 悬浮球位置调整
        val defaultX = (floatingButtonSize.toInt() - bounds.width()) / 2
        setButtonOffset(
            CoordinateType.X, gamingFBLayoutParams,
            sharedPrefs.getInt(
                if (isPortrait) FLOATING_BUTTON_COORDINATE_VERTICAL_X
                else FLOATING_BUTTON_COORDINATE_HORIZONTAL_X,
                defaultX
            )
        )
        setButtonOffset(
            CoordinateType.Y, gamingFBLayoutParams,
            sharedPrefs.getInt(
                if (isPortrait) FLOATING_BUTTON_COORDINATE_VERTICAL_Y
                else FLOATING_BUTTON_COORDINATE_HORIZONTAL_Y,
                10
            )
        )
        windowManager.updateViewLayout(gamingFloatingLayout, gamingFBLayoutParams)
    }

    @SuppressLint("InflateParams")
    private fun initGamingMenu() {
        gamingOverlayView = (layoutInflater.inflate(
            R.layout.gaming_overlay_layout,
            null
        ) as LinearLayout).also {
            it.visibility = View.GONE
            it.setOnClickListener { showHideGamingMenu(0) }
        }

        val gamingViewLayoutParams = getBaseLayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        windowManager.addView(gamingOverlayView, gamingViewLayoutParams)

        qsView = gamingOverlayView!!.findViewById<QuickSettingsView>(R.id.gaming_qs).also {
            it.setNotificationOverlayController(notificationOverlayController)
            it.setFloatingViewController(this)
            it.addTiles()
        }
        qsAppView = gamingOverlayView!!.findViewById<QuickStartAppView>(R.id.gaming_qsapp).also {
            it.setOnClickListener {
                showHideGamingMenu(2)
            }
            it.setQSApps(qsApps)
        }

        gamingPerfView =
            gamingOverlayView!!.findViewById<GamingPerformanceView>(R.id.performance_controller)
                .also {
                    if (perfProfilesSupported && changePerfLevel) {
                        it.setLevel(performanceLevel)
                    } else {
                        it.visibility = View.GONE
                    }
                    it.setOnUpdateListener { level ->
                        SystemProperties.set(PROP_GAMING_PERFORMANCE, level.toString())
                    }
                }

        gamingMenu = gamingOverlayView!!.findViewById<ScrollView>(R.id.gaming_menu).also {
            it.background.alpha = menuOpacity * 255 / 100
        }
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun initFloatingLayout() {
        gamingFloatingLayout =
            layoutInflater.inflate(R.layout.gaming_button_layout, null)

        gamingFBLayoutParams = getBaseLayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        windowManager.addView(gamingFloatingLayout, gamingFBLayoutParams)

        gamingFloatingButton = gamingFloatingLayout!!.findViewById(R.id.floating_button)
        gamingFloatingButton!!.setOnClickListener { showHideGamingMenu(0) }
        gamingFloatingButton!!.setOnTouchListener(
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
                                            FLOATING_BUTTON_COORDINATE_VERTICAL_X,
                                            gamingFBLayoutParams.x
                                        )
                                        putInt(
                                            FLOATING_BUTTON_COORDINATE_VERTICAL_Y,
                                            gamingFBLayoutParams.y
                                        )
                                    }
                                } else {
                                    sharedPrefs.edit(commit = true) {
                                        putInt(
                                            FLOATING_BUTTON_COORDINATE_HORIZONTAL_X,
                                            gamingFBLayoutParams.x
                                        )
                                        putInt(
                                            FLOATING_BUTTON_COORDINATE_HORIZONTAL_Y,
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

        callViewController.initView(gamingFloatingLayout?.findViewById(R.id.call_control_button))
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

    private fun hideFloatingButton(hide: Boolean, init: Boolean) {
        gamingFloatingButton?.let {
            val delayedHide: Long =
                if (init) FLOATING_BUTTON_HIDE_DELAY * 2 else FLOATING_BUTTON_HIDE_DELAY
            if (hide && it.alpha == 1f) {
                it.animate().alpha(FLOATING_BUTTON_HIDE_ALPHA)
                    .setStartDelay(delayedHide).duration = 250
            } else {
                it.alpha = 1f
            }
        }
    }

    private fun hideFloatingButton(hide: Boolean) {
        hideFloatingButton(hide, false)
    }

    /*
     * mode: 0=auto, 1=show, 2=hide
     */
    private fun showHideGamingMenu(mode: Int) {
        if (gamingOverlayView?.visibility == View.VISIBLE && mode != 1) {
            // hide
            val menuLayoutParams = getBaseLayoutParams()
            menuLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            menuLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            windowManager.updateViewLayout(gamingOverlayView, menuLayoutParams)
            gamingOverlayView?.visibility = View.GONE
            gamingFloatingLayout?.visibility = View.VISIBLE
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

            gamingOverlayView?.let {
                it.gravity = gravity
                it.visibility = View.VISIBLE
            }
            windowManager.updateViewLayout(gamingOverlayView, getBaseLayoutParams())

            gamingFloatingLayout?.visibility = View.GONE
        }
    }

    private enum class CoordinateType {
        X,
        Y
    }

    companion object {
        /** Default delay for the floating button  to be hidden (in ms) */
        private const val FLOATING_BUTTON_HIDE_DELAY = 1000L

        /** Default alpha value for hiding the floating button hidden */
        private const val FLOATING_BUTTON_HIDE_ALPHA = 0.1f

        private const val DEFAULT_PERFORMANCE_LEVEL = 5

        private const val DEFAULT_MENU_OPACITY = 75

        private const val PROP_GAMING_PERFORMANCE = "sys.performance.level"

        // 悬浮球位置
        private const val FLOATING_BUTTON_COORDINATE_HORIZONTAL_X = "floating_button_horizontal_x"
        private const val FLOATING_BUTTON_COORDINATE_HORIZONTAL_Y = "floating_button_horizontal_y"
        private const val FLOATING_BUTTON_COORDINATE_VERTICAL_X = "floating_button_vertical_x"
        private const val FLOATING_BUTTON_COORDINATE_VERTICAL_Y = "floating_button_vertical_y"
    }
}