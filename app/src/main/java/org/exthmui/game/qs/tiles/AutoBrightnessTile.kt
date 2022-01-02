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

package org.exthmui.game.qs.tiles

import android.content.Context
import android.provider.Settings
import android.view.View

import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

import javax.inject.Inject

import org.exthmui.game.R

@ServiceScoped
class AutoBrightnessTile @Inject constructor(
    @ApplicationContext private val context: Context
) : QSTile() {

    private val initialMode = Settings.System.getInt(
        context.contentResolver,
        Settings.System.SCREEN_BRIGHTNESS_MODE,
        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
    )

    private var stateChanged = false

    init {
        val isAuto = initialMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        if (isAuto) {
            isSelected = Settings.System.getInt(
                context.contentResolver,
                Settings.System.GAMING_MODE_DISABLE_AUTO_BRIGHTNESS,
                1
            ) == 0
            if (!isSelected) {
                stateChanged = true
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
            }
        }
    }

    override fun getTitleRes(): Int = R.string.qs_auto_brightness

    override fun getIconRes(): Int = R.drawable.ic_qs_auto_brightness

    override fun handleClick(v: View) {
        super.handleClick(v)
        val newMode = if (isSelected) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        stateChanged = newMode != initialMode
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE, newMode
        )
    }

    override fun destroy() {
        if (!stateChanged) return
        stateChanged = false
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            initialMode
        )
    }
}
