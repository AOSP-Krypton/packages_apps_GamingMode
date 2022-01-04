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
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.SystemProperties
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.edit
import androidx.core.math.MathUtils
import androidx.core.view.doOnLayout

import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

import javax.inject.Inject

import org.exthmui.game.R
import org.exthmui.game.qs.QuickSettingsView
import org.exthmui.game.qs.tiles.*

@ServiceScoped
class FloatingViewController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val callViewController: CallViewController,
    autoBrightnessTile: AutoBrightnessTile,
    lockGestureTile: LockGestureTile,
    notificationOverlayTile: NotificationOverlayTile,
    ringerModeTile: RingerModeTile,
    screenCaptureTile: ScreenCaptureTile
) : ViewController(context) {

    private val tiles = listOf(
        autoBrightnessTile,
        lockGestureTile,
        notificationOverlayTile,
        ringerModeTile,
        screenCaptureTile,
    )

    private var gamingFloatingLayout: View? = null
    private var perfLevelSeekBar: SeekBar? = null

    private lateinit var gamingFBLayoutParams: WindowManager.LayoutParams

    private var menuOpacity = DEFAULT_MENU_OPACITY
    private var floatingButtonSize = 0f

    private var perfProfilesSupported = false
    private var changePerfLevel = true
    private var performanceLevel = DEFAULT_PERFORMANCE_LEVEL

    private var dialog: AlertDialog? = null

    private lateinit var movableRect: Rect

    override fun initController() {
        with(context.resources) {
            perfProfilesSupported =
                getBoolean(com.android.internal.R.bool.config_gamingmode_performance)
            floatingButtonSize = getDimension(R.dimen.game_button_size)
        }
        calculateMovableRect()

        loadSettings()
        tiles.forEach { it.host = this }
        initGamingMenu()
        initFloatingLayout()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        floatingButtonSize = context.resources.getDimension(R.dimen.game_button_size)
        removeFloatingLayout()
        val wasShowing = dialog?.isShowing == true
        dialog?.dismiss()

        calculateMovableRect()
        initGamingMenu()
        initFloatingLayout()
        if (wasShowing) {
            showGamingMenu()
            hideFloatingLayout()
        } else {
            hideGamingMenu()
            showFloatingLayout()
        }
    }

    private fun calculateMovableRect() {
        val insets = windowManager.currentWindowMetrics.windowInsets.getInsets(
            WindowInsets.Type.statusBars() or
                    WindowInsets.Type.navigationBars() or
                    WindowInsets.Type.displayCutout()
        )
        val bounds = windowManager.currentWindowMetrics.bounds
        val halfWidth = bounds.width() / 2
        val halfHeight = bounds.height() / 2
        movableRect = Rect(
            -(halfWidth - insets.left),
            -(halfHeight - insets.top),
            (halfWidth - insets.right),
            (halfHeight - insets.bottom)
        )
    }

    override fun onDestroy() {
        tiles.forEach { it.destroy() }
        dialog?.dismiss()
        perfLevelSeekBar?.setOnSeekBarChangeListener(null)
        perfLevelSeekBar = null
        removeFloatingLayout()
        gamingFloatingLayout = null
    }

    private fun removeFloatingLayout() {
        if (gamingFloatingLayout?.parent != null) {
            windowManager.removeViewImmediate(gamingFloatingLayout)
        }
    }

    private fun showGamingMenu() {
        dialog?.let {
            it.window?.attributes?.gravity = getOverlayViewGravity()
            it.show()
        }
    }

    private fun getOverlayViewGravity(): Int {
        var overlayViewGravity = 0
        overlayViewGravity = if (gamingFBLayoutParams.x > 0) {
            overlayViewGravity or Gravity.END
        } else {
            overlayViewGravity or Gravity.START
        }
        overlayViewGravity = if (gamingFBLayoutParams.y > 0) {
            overlayViewGravity or Gravity.BOTTOM
        } else {
            overlayViewGravity or Gravity.TOP
        }
        return overlayViewGravity
    }

    fun showFloatingLayout() {
        gamingFloatingLayout?.visibility = View.VISIBLE
    }

    fun hideFloatingLayout() {
        gamingFloatingLayout?.visibility = View.GONE
    }

    fun hideGamingMenu() {
        dialog?.hide()
    }

    private fun loadSettings() {
        changePerfLevel = sharedPreferences.getBoolean(CHANGE_PERFORMANCE_LEVEL_KEY, true)
        performanceLevel =
            sharedPreferences.getInt(PERFORMANCE_LEVEL_KEY, DEFAULT_PERFORMANCE_LEVEL)
        menuOpacity = sharedPreferences.getInt(MENU_OPACITY_KEY, DEFAULT_MENU_OPACITY)
    }

    private fun restoreFloatingButtonOffset() {
        clampLayoutParams(
            x = sharedPreferences.getInt(
                if (isPortrait) FLOATING_BUTTON_COORDINATE_VERTICAL_X
                else FLOATING_BUTTON_COORDINATE_HORIZONTAL_X,
                movableRect.left
            ),
            y = sharedPreferences.getInt(
                if (isPortrait) FLOATING_BUTTON_COORDINATE_VERTICAL_Y
                else FLOATING_BUTTON_COORDINATE_HORIZONTAL_Y,
                0
            )
        )
        windowManager.updateViewLayout(gamingFloatingLayout, gamingFBLayoutParams)
    }

    @SuppressLint("InflateParams")
    private fun initGamingMenu() {
        val overlayView = layoutInflater.inflate(
            R.layout.gaming_overlay_layout,
            null
        ) as ConstraintLayout

        if (perfProfilesSupported && changePerfLevel) {
            perfLevelSeekBar = overlayView.findViewById<SeekBar>(R.id.perf_level_seekbar).also {
                it.progress = performanceLevel
                it.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        SystemProperties.set(PROP_GAMING_PERFORMANCE, progress.toString())
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }
        } else {
            overlayView.findViewById<Group>(R.id.performance_group).visibility = View.GONE
        }

        overlayView.background.alpha = menuOpacity * 255 / 100

        overlayView.findViewById<QuickSettingsView>(R.id.quick_settings_view).also {
            it.addTiles(tiles)
        }

        dialog = AlertDialog.Builder(context, R.style.Theme_AlertDialog).let {
            it.setView(overlayView)
            it.setCancelable(true)
            it.create()
        }.also {
            it.window?.apply {
                setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun initFloatingLayout() {
        gamingFBLayoutParams = getFloatingButtonLP()
        gamingFloatingLayout = layoutInflater.inflate(R.layout.gaming_button_layout, null).also {
            windowManager.addView(it, gamingFBLayoutParams)
            initFloatingButton(it.findViewById(R.id.floating_button))
            callViewController.initView(it.findViewById(R.id.call_control_button))
            it.doOnLayout {
                restoreFloatingButtonOffset()
            }
        }
    }

    private fun initFloatingButton(button: ImageView) {
        button.let {
            it.setOnClickListener { showGamingMenu() }
            it.setOnTouchListener(object : View.OnTouchListener {
                var diffX = 0
                var diffY = 0
                var moved = false

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    val x = event.rawX.toInt()
                    val y = event.rawY.toInt()

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            diffX = gamingFBLayoutParams.x - x
                            diffY = gamingFBLayoutParams.y - y
                        }
                        MotionEvent.ACTION_MOVE -> {
                            moved = true
                            clampLayoutParams(x = x + diffX, y = y + diffY)
                            windowManager.updateViewLayout(
                                gamingFloatingLayout,
                                gamingFBLayoutParams
                            )
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!moved) {
                                v.performClick()
                            } else {
                                moved = false
                                saveCoordinates()
                            }
                        }
                        else -> return false
                    }
                    return true
                }
            })
        }
    }

    private fun saveCoordinates() {
        sharedPreferences.edit(commit = true) {
            putInt(
                if (isPortrait) FLOATING_BUTTON_COORDINATE_VERTICAL_X else FLOATING_BUTTON_COORDINATE_HORIZONTAL_X,
                gamingFBLayoutParams.x
            )
            putInt(
                if (isPortrait) FLOATING_BUTTON_COORDINATE_VERTICAL_Y else FLOATING_BUTTON_COORDINATE_HORIZONTAL_Y,
                gamingFBLayoutParams.y
            )
        }
    }

    private fun clampLayoutParams(x: Int, y: Int) {
        val xOffset = gamingFloatingLayout!!.measuredWidth / 2
        val yOffset = gamingFloatingLayout!!.measuredHeight
        gamingFBLayoutParams.x = MathUtils.clamp(
            x,
            movableRect.left + xOffset,
            movableRect.right - xOffset
        )
        gamingFBLayoutParams.y = MathUtils.clamp(
            y,
            movableRect.top,
            movableRect.bottom - yOffset,
        )
    }

    private fun getFloatingButtonLP() =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

    companion object {
        private const val DEFAULT_PERFORMANCE_LEVEL = 5

        private const val DEFAULT_MENU_OPACITY = 75

        private const val PROP_GAMING_PERFORMANCE = "sys.performance.level"

        // 悬浮球位置
        private const val FLOATING_BUTTON_COORDINATE_HORIZONTAL_X = "floating_button_horizontal_x"
        private const val FLOATING_BUTTON_COORDINATE_HORIZONTAL_Y = "floating_button_horizontal_y"
        private const val FLOATING_BUTTON_COORDINATE_VERTICAL_X = "floating_button_vertical_x"
        private const val FLOATING_BUTTON_COORDINATE_VERTICAL_Y = "floating_button_vertical_y"

        private const val CHANGE_PERFORMANCE_LEVEL_KEY = "gaming_mode_change_performance_level"
        private const val PERFORMANCE_LEVEL_KEY = "gaming_mode_performance_level"
        private const val MENU_OPACITY_KEY = "gaming_mode_menu_opacity"
    }
}