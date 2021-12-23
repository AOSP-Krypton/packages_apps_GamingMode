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

package org.exthmui.game.qs

import android.content.Context
import android.media.AudioManager

import org.exthmui.game.R

class DNDTile(context: Context) : TileBase(context, R.string.qs_dnd, R.drawable.ic_qs_dnd) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    override fun handleClick(isSelected: Boolean) {
        super.handleClick(isSelected)
        audioManager.ringerModeInternal =
            if (isSelected) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
    }

    init {
        isSelected = audioManager.ringerModeInternal == AudioManager.RINGER_MODE_SILENT
    }
}