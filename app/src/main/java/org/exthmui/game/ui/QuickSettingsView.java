/*
 * Copyright (C) 2020 The exTHmUI Open Source Project
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

package org.exthmui.game.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import org.exthmui.game.R;
import org.exthmui.game.controller.DanmakuController;
import org.exthmui.game.controller.FloatingViewController;
import org.exthmui.game.qs.AutoBrightnessTile;
import org.exthmui.game.qs.DNDTile;
import org.exthmui.game.qs.DanmakuTile;
import org.exthmui.game.qs.LockGestureTile;
import org.exthmui.game.qs.ScreenCaptureTile;
import org.exthmui.game.qs.ScreenRecordTile;

public class QuickSettingsView extends LinearLayout {

    private DanmakuController mDanmakuController;
    private FloatingViewController mFloatingViewController;

    public QuickSettingsView(Context context) {
        this(context, null);
    }

    public QuickSettingsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickSettingsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public QuickSettingsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setDividerDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.qs_divider));
        setShowDividers(SHOW_DIVIDER_MIDDLE);
        setPadding(0, 0, 0, 8);
    }

    public void setDanmakuController(@NonNull DanmakuController danmakuController) {
        mDanmakuController = danmakuController;
    }

    public void setFloatingViewController(@NonNull FloatingViewController floatingViewController) {
        mFloatingViewController = floatingViewController;
    }

    public void addTiles() {
        final Context context = getContext();

        final ScreenCaptureTile screenCaptureTile = new ScreenCaptureTile(context);
        screenCaptureTile.setViewController(mFloatingViewController);
        addView(screenCaptureTile);

        addView(new ScreenRecordTile(context));

        final DanmakuTile danmakuTile = new DanmakuTile(context);
        danmakuTile.setDanmakuController(mDanmakuController);
        addView(danmakuTile);

        addView(new DNDTile(context));
        addView(new LockGestureTile(context));
        addView(new AutoBrightnessTile(context));
    }
}
