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

package org.exthmui.game.qs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.exthmui.game.R;

@SuppressLint("ViewConstructor")
public class TileBase extends LinearLayout implements View.OnClickListener {

    protected final ImageView qsIcon;
    protected final TextView qsText;
    private boolean isSelected;
    private boolean isToggleable = true;

    public TileBase(Context context, int textResId, int iconResId) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.gaming_qs_view, this, true);
        qsIcon = findViewById(R.id.qs_icon);
        qsIcon.setImageResource(iconResId);
        qsText = findViewById(R.id.qs_text);
        if (textResId != 0) qsText.setText(textResId);
        setOnClickListener(this);
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
        qsIcon.setSelected(selected);
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setToggleable(boolean isToggleable) {
        this.isToggleable = isToggleable;
    }

    @Override
    public void onClick(View v) {
        if (isToggleable) {
            isSelected = !isSelected;
        }
        handleClick(isSelected);
    }

    protected void handleClick(boolean isSelected) {
        if (isToggleable) setSelected(isSelected);
    }

    @Override
    protected void onDetachedFromWindow() {
        onDestroy();
        super.onDetachedFromWindow();
    }

    public void onDestroy() {}
}
