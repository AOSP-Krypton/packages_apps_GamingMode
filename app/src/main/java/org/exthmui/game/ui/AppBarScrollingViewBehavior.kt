/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.exthmui.game.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View

import androidx.coordinatorlayout.widget.CoordinatorLayout

import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior

/**
 * This scrolling view behavior will set the background of the [AppBarLayout] as
 * transparent and without the elevation.
 */
class AppBarScrollingViewBehavior(context: Context?, attrs: AttributeSet?) :
    ScrollingViewBehavior(context, attrs) {
    
    private var initialized = false

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        val changed = super.onDependentViewChanged(parent, child, dependency)
        if (!initialized && dependency is AppBarLayout) {
            initialized = true
            dependency.setBackgroundColor(Color.TRANSPARENT)
        }
        return changed
    }
}