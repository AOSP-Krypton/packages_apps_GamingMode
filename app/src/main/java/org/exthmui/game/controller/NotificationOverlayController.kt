package org.exthmui.game.controller

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager.LayoutParams
import android.widget.TextView

import androidx.core.animation.addListener

import dagger.hilt.android.qualifiers.ApplicationContext

import java.util.LinkedList

import javax.inject.Inject
import javax.inject.Singleton

import org.exthmui.game.R

@Singleton
class NotificationOverlayController @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewController(context) {

    private val notificationOverlay = TextView(context).apply {
        gravity = Gravity.CENTER
        maxLines = 2
        setTextColor(Color.WHITE)
    }

    private var layoutParams: LayoutParams = LayoutParams().apply {
        width = LayoutParams.MATCH_PARENT
        height = LayoutParams.WRAP_CONTENT
        flags = flags or LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_NOT_TOUCHABLE or
                LayoutParams.FLAG_HARDWARE_ACCELERATED
        type = LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.TOP
    }

    var showNotificationOverlay = true

    private var verticalOffsetLandscape = 0
    private var verticalOffsetPortrait = 0

    private var sizePortrait = 0
    private var sizeLandscape = 0

    private var overlayAnimator: ObjectAnimator? = null

    private val handler = Handler(Looper.getMainLooper())

    private val notificationStack = LinkedList<String>()

    init {
        loadSettings()
        updateParams()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        overlayAnimator?.end()
        updateParams()
        if (notificationOverlay.parent != null) {
            windowManager.updateViewLayout(notificationOverlay, layoutParams)
        }
    }

    override fun onDestroy() {
        overlayAnimator?.cancel()
        removeViewSafely()
    }

    fun showNotificationAsOverlay(notification: String) {
        if (notificationOverlay.parent == null) {
            notificationOverlay.alpha = 0f
            notificationOverlay.text = notification
            windowManager.addView(notificationOverlay, layoutParams)
            pushNotification()
        } else {
            notificationStack.add(notification)
        }
    }

    private fun updateParams() {
        with(context.resources) {
            verticalOffsetLandscape =
                getDimensionPixelSize(R.dimen.notification_vertical_offset_landscape)
            verticalOffsetPortrait =
                getDimensionPixelSize(R.dimen.notification_vertical_offset_portrait)
        }
        layoutParams.y = getOffsetForPosition()
        notificationOverlay.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            (if (isPortrait) sizePortrait else sizeLandscape).toFloat()
        )
    }

    private fun getOffsetForPosition(): Int {
        return if (isPortrait) verticalOffsetPortrait else verticalOffsetLandscape
    }

    private fun loadSettings() {
        sizePortrait = Settings.System.getInt(
            context.contentResolver,
            Settings.System.GAMING_MODE_NOTIFICATION_SIZE_PORTRAIT,
            DEFAULT_NOTIFICATION_SIZE_PORTRAIT
        )
        sizeLandscape = Settings.System.getInt(
            context.contentResolver,
            Settings.System.GAMING_MODE_NOTIFICATION_SIZE_LANDSCAPE,
            DEFAULT_NOTIFICATION_SIZE_LANDSCAPE
        )
        showNotificationOverlay = Settings.System.getInt(
            context.contentResolver,
            Settings.System.GAMING_MODE_SHOW_NOTIFICATION_OVERLAY, 1
        ) == 1
    }

    private fun pushNotification() {
        val alphaPVH = PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
        val translationPVH = PropertyValuesHolder.ofFloat(
            "translationY",
            -getOffsetForPosition() * SLIDE_ANIMATION_DISTANCE_FACTOR,
            0f
        )
        overlayAnimator =
            ObjectAnimator.ofPropertyValuesHolder(notificationOverlay, alphaPVH, translationPVH)
                .apply {
                    duration = APPEAR_ANIMATION_DURATION
                    addListener(onEnd = {
                        handler.postDelayed({
                            popNotification()
                        }, DISPLAY_NOTIFICATION_DURATION)
                    })
                    start()
                }
    }

    private fun popNotification() {
        val alphaPVH = PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
        val translationPVH = PropertyValuesHolder.ofFloat(
            "translationY",
            0f,
            getOffsetForPosition() * SLIDE_ANIMATION_DISTANCE_FACTOR
        )
        overlayAnimator =
            ObjectAnimator.ofPropertyValuesHolder(notificationOverlay, alphaPVH, translationPVH)
                .apply {
                    duration = DISAPPEAR_ANIMATION_DURATION
                    addListener(onEnd = {
                        if (notificationStack.isEmpty()) {
                            removeViewSafely()
                        } else {
                            notificationOverlay.alpha = 0f
                            notificationOverlay.text = notificationStack.pop()
                            pushNotification()
                        }
                    })
                    start()
                }
    }

    private fun removeViewSafely() {
        if (notificationOverlay.parent != null)
            windowManager.removeViewImmediate(notificationOverlay)
    }

    companion object {
        private const val SLIDE_ANIMATION_DISTANCE_FACTOR = 0.5f

        private const val APPEAR_ANIMATION_DURATION = 500L
        private const val DISPLAY_NOTIFICATION_DURATION = 2000L
        private const val DISAPPEAR_ANIMATION_DURATION = 300L

        private const val DEFAULT_NOTIFICATION_SIZE_LANDSCAPE = 90
        private const val DEFAULT_NOTIFICATION_SIZE_PORTRAIT = 60
    }
}