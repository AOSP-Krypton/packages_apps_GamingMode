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

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import org.exthmui.game.R;
import org.exthmui.game.qs.AppTile;

import java.util.List;

public class QuickStartAppView extends LinearLayout {

    public QuickStartAppView(Context context) {
        this(context, null);
    }

    public QuickStartAppView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickStartAppView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public QuickStartAppView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setDividerDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.qs_divider));
        setShowDividers(SHOW_DIVIDER_MIDDLE);
        setPadding(0, 0, 0, 8);
        setVisibility(GONE);
    }

    public void setQSApps(List<String> apps) {
        removeAllViewsInLayout();
        setVisibility(GONE);
        
        if (apps == null || apps.isEmpty()) return;
        setVisibility(VISIBLE);
        final Context context = getContext();
        apps.forEach(app -> addView(new AppTile(context, app)));
    }
}
