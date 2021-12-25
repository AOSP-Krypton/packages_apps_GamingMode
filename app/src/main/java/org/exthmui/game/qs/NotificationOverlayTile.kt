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
import android.provider.Settings

import org.exthmui.game.R
import org.exthmui.game.controller.NotificationOverlayController

class NotificationOverlayTile(context: Context): TileBase(context) {

    private var notificationOverlayController: NotificationOverlayController? = null

    init {
        setText(R.string.qs_danmaku)
        setIcon(R.drawable.ic_qs_danmaku)
        isSelected = Settings.System.getInt(context.contentResolver,
            Settings.System.GAMING_MODE_SHOW_DANMAKU, 0) == 1
    }

    override fun handleClick(isSelected: Boolean) {
        super.handleClick(isSelected)
        notificationOverlayController?.showNotificationOverlay = isSelected
    }

    fun setController(controller: NotificationOverlayController) {
        notificationOverlayController = controller
    }
}
