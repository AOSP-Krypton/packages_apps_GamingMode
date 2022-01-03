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
import android.content.SharedPreferences
import android.media.AudioManager
import android.provider.Settings
import android.view.View

import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

import javax.inject.Inject

import org.exthmui.game.R

@ServiceScoped
class RingerModeTile @Inject constructor(
    @ApplicationContext context: Context,
    sharedPreferences: SharedPreferences,
) : QSTile() {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val initialMode = audioManager.ringerModeInternal

    private var stateChanged = false

    init {
        if (initialMode != AudioManager.RINGER_MODE_SILENT) {
            val silentModeEnabled = sharedPreferences.getBoolean(DISABLE_RINGTONE_KEY, false)
            if (silentModeEnabled) {
                audioManager.ringerModeInternal = AudioManager.RINGER_MODE_SILENT
                stateChanged = true
            }
        }
        isSelected = audioManager.ringerModeInternal == AudioManager.RINGER_MODE_SILENT
    }

    override fun getTitleRes(): Int = R.string.qs_ringer_mode

    override fun getIconRes(): Int = R.drawable.ic_qs_ringer

    override fun handleClick(v: View) {
        super.handleClick(v)
        audioManager.ringerModeInternal =
            if (isSelected) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
        stateChanged = audioManager.ringerModeInternal != initialMode
    }

    override fun destroy() {
        if (!stateChanged) return
        stateChanged = false
        audioManager.ringerModeInternal = initialMode
    }

    companion object {
        private const val DISABLE_RINGTONE_KEY = "gaming_mode_disable_ringtone"
    }
}