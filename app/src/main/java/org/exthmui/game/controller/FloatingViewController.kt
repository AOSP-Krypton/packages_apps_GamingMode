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
import android.graphics.drawable.ColorDrawable
import android.os.SystemProperties
import android.provider.Settings
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.edit
import androidx.core.math.MathUtils
import androidx.preference.PreferenceManager

import dagger.hilt.android.qualifiers.ApplicationContext

import javax.inject.Inject
import javax.inject.Singleton

import org.exthmui.game.R
import org.exthmui.game.ui.QuickSettingsView

@Singleton
class FloatingViewController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callViewController: CallViewController,
    private val notificationOverlayController: NotificationOverlayController,
) : ViewController(context) {

    private var gamingFloatingLayout: View? = null
    private var perfLevelSeekBar: SeekBar? = null

    private lateinit var gamingFBLayoutParams: WindowManager.LayoutParams

    private var menuOpacity = DEFAULT_MENU_OPACITY
    private var floatingButtonSize = 0f

    private var perfProfilesSupported = false
    private var changePerfLevel = true
    private var performanceLevel = DEFAULT_PERFORMANCE_LEVEL

    private lateinit var sharedPrefs: SharedPreferences

    private var dialog: AlertDialog? = null

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
        dialog?.dismiss()
        perfLevelSeekBar?.setOnSeekBarChangeListener(null)
        perfLevelSeekBar = null
        windowManager.removeView(gamingFloatingLayout)
        gamingFloatingLayout = null
    }

    private fun showGamingMenu() {
        dialog?.let {
            it.window?.attributes?.gravity = getOverlayViewGravity()
            it.show()
        }
        gamingFloatingLayout?.visibility = View.GONE
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

    fun hideGamingMenu() {
        dialog?.hide()
        gamingFloatingLayout?.visibility = View.VISIBLE
    }

    private fun loadSettings() {
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
        val defaultX = ((floatingButtonSize - bounds.width()) / 2f).toInt()
        val defaultY = ((floatingButtonSize - bounds.height()) / 2f).toInt()
        gamingFBLayoutParams.x = sharedPrefs.getInt(
            if (isPortrait) FLOATING_BUTTON_COORDINATE_VERTICAL_X
            else FLOATING_BUTTON_COORDINATE_HORIZONTAL_X,
            defaultX
        )
        gamingFBLayoutParams.y = sharedPrefs.getInt(
            if (isPortrait) FLOATING_BUTTON_COORDINATE_VERTICAL_Y
            else FLOATING_BUTTON_COORDINATE_HORIZONTAL_Y,
            defaultY
        )
        windowManager.updateViewLayout(gamingFloatingLayout, gamingFBLayoutParams)
    }

    @SuppressLint("InflateParams")
    private fun initGamingMenu() {
        val overlayView = layoutInflater.inflate(
            R.layout.gaming_overlay_layout,
            null
        ) as ConstraintLayout

        overlayView.findViewById<QuickSettingsView>(R.id.quick_settings_view).also {
            it.setNotificationOverlayController(notificationOverlayController)
            it.setFloatingViewController(this)
            it.addTiles()
        }

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

        dialog = AlertDialog.Builder(context, R.style.Theme_AlertDialog).let {
            it.setView(overlayView)
            it.setCancelable(true)
            it.setOnCancelListener { gamingFloatingLayout?.visibility = View.VISIBLE }
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
        gamingFloatingLayout =
            layoutInflater.inflate(R.layout.gaming_button_layout, null)

        gamingFBLayoutParams = getFloatingButtonLP()
        windowManager.addView(gamingFloatingLayout, gamingFBLayoutParams)
        initFloatingButton()

        callViewController.initView(gamingFloatingLayout?.findViewById(R.id.call_control_button))
        restoreFloatingButtonOffset()
    }

    private fun initFloatingButton() {
        gamingFloatingLayout!!.findViewById<ImageView>(R.id.floating_button).also {
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
                            gamingFBLayoutParams.x =
                                getClampedValue(x + diffX, bounds.width())
                            gamingFBLayoutParams.y =
                                getClampedValue(y + diffY, bounds.height())
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
        sharedPrefs.edit(commit = true) {
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

    private fun getClampedValue(value: Int, max: Int): Int {
        val allowedMax = ((max - floatingButtonSize) / 2f).toInt()
        return MathUtils.clamp(value, -allowedMax, allowedMax)
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
    }
}