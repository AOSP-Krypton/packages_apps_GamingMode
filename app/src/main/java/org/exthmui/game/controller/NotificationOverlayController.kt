package org.exthmui.game.controller

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager.LayoutParams
import android.widget.TextView

import androidx.core.animation.addListener

import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

import java.util.LinkedList

import javax.inject.Inject

import org.exthmui.game.R

@ServiceScoped
class NotificationOverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: SharedPreferences,
) : ViewController(context) {

    private val notificationOverlay = TextView(context).apply {
        gravity = Gravity.CENTER
        maxLines = 2
        setTextColor(Color.WHITE)
        isFocusable = false
        isClickable = false
    }

    private var layoutParams: LayoutParams = LayoutParams().apply {
        height = LayoutParams.WRAP_CONTENT
        flags = flags or LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_NOT_TOUCHABLE or
                LayoutParams.FLAG_HARDWARE_ACCELERATED
        type = LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.TOP
    }

    var showNotificationOverlay = true

    private var verticalOffsetLandscape = 0
    private var verticalOffsetPortrait = 0

    private var sizePortrait = 0
    private var sizeLandscape = 0

    private var overlayAlphaAnimator: ValueAnimator? = null
    private var overlayPositionAnimator: ValueAnimator? = null

    private val handler = Handler(Looper.getMainLooper())

    private val notificationStack = LinkedList<String>()

    init {
        loadSettings()
        updateParams()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        overlayAlphaAnimator?.end()
        overlayPositionAnimator?.end()
        updateParams()
        updateViewLayoutSafely(layoutParams)
    }

    override fun onDestroy() {
        overlayAlphaAnimator?.cancel()
        overlayPositionAnimator?.cancel()
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
        layoutParams.width = (context.resources.getInteger(
            R.integer.notification_overlay_max_width
        ) * windowManager.currentWindowMetrics.bounds.width()) / 100
        notificationOverlay.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            (if (isPortrait) sizePortrait else sizeLandscape).toFloat()
        )
    }

    private fun getOffsetForPosition(): Int {
        return if (isPortrait) verticalOffsetPortrait else verticalOffsetLandscape
    }

    private fun loadSettings() {
        sizePortrait = sharedPreferences.getInt(
            NOTIFICATION_SIZE_PORTRAIT_KEY,
            DEFAULT_NOTIFICATION_SIZE_PORTRAIT
        )
        sizeLandscape = sharedPreferences.getInt(
            NOTIFICATION_SIZE_LANDSCAPE_KEY,
            DEFAULT_NOTIFICATION_SIZE_LANDSCAPE
        )
        showNotificationOverlay = sharedPreferences.getBoolean(SHOW_NOTIFICATION_OVERLAY_KEY, true)
    }

    private fun pushNotification() {
        val end = getOffsetForPosition().toFloat()
        val start = end * (1 - SLIDE_ANIMATION_DISTANCE_FACTOR)
        overlayPositionAnimator = getPositionAnimator(APPEAR_ANIMATION_DURATION, start, end).also {
            it.addListener(onEnd = {
                handler.postDelayed({
                    popNotification()
                }, DISPLAY_NOTIFICATION_DURATION)
            })
            it.start()
        }
        startAlphaAnimation(APPEAR_ANIMATION_DURATION, 0f, 1f)
    }

    private fun popNotification() {
        val start = getOffsetForPosition().toFloat()
        val end = start * (1 + SLIDE_ANIMATION_DISTANCE_FACTOR)
        overlayPositionAnimator =
            getPositionAnimator(DISAPPEAR_ANIMATION_DURATION, start, end).also {
                it.addListener(onEnd = {
                    if (notificationStack.isEmpty()) {
                        removeViewSafely()
                    } else {
                        notificationOverlay.alpha = 0f
                        notificationOverlay.text = notificationStack.pop()
                        pushNotification()
                    }
                })
                it.start()
            }
        startAlphaAnimation(DISAPPEAR_ANIMATION_DURATION, 1f, 0f)
    }

    private fun getPositionAnimator(duration: Long, vararg values: Float): ValueAnimator {
        val lpCopy = LayoutParams().also { it.copyFrom(layoutParams) }
        return ValueAnimator.ofFloat(*values).apply {
            this.duration = duration
            addUpdateListener {
                lpCopy.y = (it.animatedValue as Float).toInt()
                updateViewLayoutSafely(lpCopy)
            }
        }
    }

    private fun startAlphaAnimation(duration: Long, vararg values: Float) {
        overlayAlphaAnimator = ValueAnimator.ofFloat(*values).apply {
            this.duration = duration
            addUpdateListener {
                notificationOverlay.alpha = it.animatedValue as Float
            }
        }.also { it.start() }
    }

    private fun updateViewLayoutSafely(layoutParams: LayoutParams) {
        if (notificationOverlay.parent != null)
            windowManager.updateViewLayout(notificationOverlay, layoutParams)
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

        private const val SHOW_NOTIFICATION_OVERLAY_KEY = "gaming_mode_show_notification_overlay"

        private const val NOTIFICATION_SIZE_LANDSCAPE_KEY =
            "gaming_mode_notification_size_landscape"
        private const val DEFAULT_NOTIFICATION_SIZE_LANDSCAPE = 60

        private const val NOTIFICATION_SIZE_PORTRAIT_KEY = "gaming_mode_notification_size_portrait"
        private const val DEFAULT_NOTIFICATION_SIZE_PORTRAIT = 60
    }
}