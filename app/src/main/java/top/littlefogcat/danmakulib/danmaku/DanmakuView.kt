package top.littlefogcat.danmakulib.danmaku

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import android.widget.TextView

/**
 * DanmakuView的基类，继承自TextView，一个弹幕对应一个DanmakuView。
 * 这里实现了一些通用的功能。
 * <p>
 */
class DanmakuView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int,
) : TextView(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    var danmaku: Danmaku? = null
        set(value) {
            field = value
            text = danmaku?.text
            gravity = when (danmaku?.mode) {
                Danmaku.Mode.TOP, Danmaku.Mode.BOTTOM -> Gravity.CENTER
                else -> Gravity.START or Gravity.CENTER_VERTICAL
            }
        }

    private val onEnterListeners = mutableListOf<OnEnterListener>()

    private val onExitListeners = mutableListOf<OnExitListener>()

    /**
     * 弹幕进场时的监听
     */
    fun interface OnEnterListener {
        fun onEnter(view: DanmakuView)
    }

    /**
     * 弹幕离场后的监听
     */
    fun interface OnExitListener {
        fun onExit(view: DanmakuView)
    }

    /**
     * 显示时长 ms
     */
    private var duration = 0

    private val screenWidth: Int

    private val scroller = Scroller(context, LinearInterpolator())

    init {
        val wm = context.getSystemService(WindowManager::class.java)
        screenWidth = wm.currentWindowMetrics.bounds.width()
        setScroller(scroller)
    }

    /**
     * 显示弹幕
     */
    fun show(parent: ViewGroup, duration: Int) {
        this.duration = duration
        when (danmaku?.mode) {
            Danmaku.Mode.TOP, Danmaku.Mode.BOTTOM -> showFixedDanmaku(parent)
            else -> showScrollDanmaku(parent)
        }
        onEnterListeners.forEach { it.onEnter(this) }
        postDelayed({
            visibility = GONE
            onExitListeners.forEach { it.onExit(this) }
            parent.removeView(this)
        }, duration.toLong())
    }

    private fun showScrollDanmaku(parent: ViewGroup) {
        scrollTo(-screenWidth, 0)
        parent.addView(this)
        smoothScrollTo(getTextLength())
    }

    private fun showFixedDanmaku(parent: ViewGroup) {
        gravity = Gravity.CENTER
        parent.addView(this)
    }

    fun addOnEnterListener(l: OnEnterListener) {
        if (!onEnterListeners.contains(l)) {
            onEnterListeners.add(l)
        }
    }

    fun addOnExitListener(l: OnExitListener) {
        if (!onExitListeners.contains(l)) {
            onExitListeners.add(l)
        }
    }

    fun getTextLength() = paint.measureText(text.toString()).toInt()

    /**
     * 恢复初始状态
     */
    fun restore() {
        onEnterListeners.clear()
        onExitListeners.clear()
        visibility = VISIBLE
    }

    fun clearScroll() {
        scrollX = 0
        scrollY = 0
    }

    private fun smoothScrollTo(x: Int) {
        scroller.startScroll(scrollX, scrollY, x - scrollX, -scrollY, duration)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidate()
        }
    }
}
