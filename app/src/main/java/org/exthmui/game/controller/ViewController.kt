package org.exthmui.game.controller

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.LayoutInflater
import android.view.WindowManager

open class ViewController(private val context: Context) {
    protected var isPortrait: Boolean =
        context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    protected val windowManager: WindowManager = context.getSystemService(WindowManager::class.java)
    protected var bounds = windowManager.currentWindowMetrics.bounds
    protected var layoutInflater: LayoutInflater = LayoutInflater.from(context)

    protected var initDone = false

    fun init() {
        if (Settings.canDrawOverlays(context)) {
            initController()
        }
        initDone = true
    }

    protected open fun initController() {}

    fun updateConfiguration(newConfig: Configuration) {
        isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
        bounds = windowManager.currentWindowMetrics.bounds
        onConfigurationChanged(newConfig)
    }

    protected open fun getBaseLayoutParams() =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

    protected open fun onConfigurationChanged(newConfig: Configuration) {}

    fun destroy() {
        onDestroy()
        initDone = false
    }

    protected open fun onDestroy() {}
}