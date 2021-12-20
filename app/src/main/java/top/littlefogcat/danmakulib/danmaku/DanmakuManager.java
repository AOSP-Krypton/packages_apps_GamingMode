package top.littlefogcat.danmakulib.danmaku;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.exthmui.game.R;

import java.lang.ref.WeakReference;

/**
 * 用法示例：
 * DanmakuManager dm = DanmakuManager.getInstance();
 * dm.init(getContext());
 * dm.show(new Danmaku("test"));
 * <p>
 * Created by LittleFogCat.
 */
@SuppressWarnings("unused")
public class DanmakuManager {
    private static final String TAG = DanmakuManager.class.getSimpleName();
    private static final int RESULT_OK = 0;
    private static final int RESULT_NULL_ROOT_VIEW = 1;
    private static final int RESULT_FULL_POOL = 2;
    private static final int TOO_MANY_DANMAKU = 2;

    private static DanmakuManager sInstance;

    /**
     * 弹幕容器
     */
    WeakReference<FrameLayout> mDanmakuContainer;

    private Config mConfig;

    private DanmakuPositionCalculator mPositionCal;

    private Context mContext;

    private DanmakuManager() {
    }

    public static DanmakuManager getInstance() {
        if (sInstance == null) {
            sInstance = new DanmakuManager();
        }
        return sInstance;
    }

    /**
     * 初始化。在使用之前必须调用该方法。
     */
    public void init(Context context, FrameLayout container) {
        mContext = context;
        setDanmakuContainer(container);

        mConfig = new Config();
        mPositionCal = new DanmakuPositionCalculator(this);
    }

    public Config getConfig() {
        if (mConfig == null) {
            mConfig = new Config();
        }
        return mConfig;
    }

    private DanmakuPositionCalculator getPositionCalculator() {
        if (mPositionCal == null) {
            mPositionCal = new DanmakuPositionCalculator(this);
        }
        return mPositionCal;
    }

    /**
     * 设置弹幕的容器，所有的弹幕都在这里面显示
     */
    public void setDanmakuContainer(final FrameLayout root) {
        if (root == null) {
            throw new NullPointerException("Danmaku container cannot be null!");
        }
        mDanmakuContainer = new WeakReference<>(root);
    }

    /**
     * 发送一条弹幕
     */
    public int send(Danmaku danmaku) {
        if (mDanmakuContainer == null || mDanmakuContainer.get() == null) {
            Log.w(TAG, "show: Root view is null.");
            return RESULT_NULL_ROOT_VIEW;
        }

        final DanmakuView view = (DanmakuView) LayoutInflater.from(mContext).inflate(
                        R.layout.danmaku_view, mDanmakuContainer.get(), false);
        view.addOnEnterListener(DanmakuView::clearScroll);
        view.addOnExitListener(v -> v.restore());
        view.setDanmaku(danmaku);

        // 字体大小
        int textSize = danmaku.size;
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        // 字体颜色
        try {
            view.setTextColor(danmaku.color);
        } catch (Exception e) {
            e.printStackTrace();
            view.setTextColor(Color.WHITE);
        }

        // 计算弹幕距离顶部的位置
        DanmakuPositionCalculator dpc = getPositionCalculator();
        int marginTop = dpc.getMarginTop(view);

        if (marginTop == -1) {
            Log.w(TAG, "send: screen is full, too many danmaku [" + danmaku + "]");
            return TOO_MANY_DANMAKU;
        }
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) view.getLayoutParams();
        if (p == null) {
            p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        p.topMargin = marginTop;
        view.setLayoutParams(p);
        view.setMinHeight((int) (getConfig().getLineHeight() * 1.35));
        int duration = getDisplayDuration(danmaku);
        if (danmaku.mode == Danmaku.Mode.scroll) {
            duration = (view.getTextLength() + dpc.getParentWidth()) / duration * 1000;
        }
        view.show(mDanmakuContainer.get(), duration);
        return RESULT_OK;
    }

    /**
     * @return 返回这个弹幕显示时长 (对于滚动弹幕返回速度)
     */
    int getDisplayDuration(Danmaku danmaku) {
        Config config = getConfig();
        int duration;
        switch (danmaku.mode) {
            case top:
                duration = config.getDurationTop();
                break;
            case bottom:
                duration = config.getDurationBottom();
                break;
            case scroll:
            default:
                duration = config.getScrollSpeed();
                break;
        }
        return duration;
    }

    /**
     * 一些配置
     */
    public static class Config {

        /**
         * 行高，单位px
         */
        private int lineHeight;

        /**
         * 滚动弹幕速度 (px/s)
         */
        private int scrollSpeed;
        /**
         * 顶部弹幕显示时长
         */
        private int durationTop;
        /**
         * 底部弹幕的显示时长
         */
        private int durationBottom;

        /**
         * 滚动弹幕的最大行数
         */
        private int maxScrollLine;

        public int getLineHeight() {
            return lineHeight;
        }

        public void setLineHeight(int lineHeight) {
            this.lineHeight = lineHeight;
        }

        public int getMaxScrollLine() {
            return maxScrollLine;
        }

        public int getScrollSpeed() {
            return scrollSpeed;
        }

        public void setScrollSpeed(int durationScroll) {
            this.scrollSpeed = durationScroll;
        }

        public int getDurationTop() {
            if (durationTop == 0) {
                durationTop = 5000;
            }
            return durationTop;
        }

        public void setDurationTop(int durationTop) {
            this.durationTop = durationTop;
        }

        public int getDurationBottom() {
            if (durationBottom == 0) {
                durationBottom = 5000;
            }
            return durationBottom;
        }

        public void setDurationBottom(int durationBottom) {
            this.durationBottom = durationBottom;
        }

        public int getMaxDanmakuLine() {
            if (maxScrollLine == 0) {
                maxScrollLine = 12;
            }
            return maxScrollLine;
        }

        public void setMaxScrollLine(int maxScrollLine) {
            this.maxScrollLine = maxScrollLine;
        }
    }

}
