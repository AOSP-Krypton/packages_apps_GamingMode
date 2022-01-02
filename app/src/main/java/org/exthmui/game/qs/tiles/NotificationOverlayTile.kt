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

import android.view.View

import dagger.hilt.android.scopes.ServiceScoped

import javax.inject.Inject

import org.exthmui.game.R
import org.exthmui.game.controller.NotificationOverlayController

@ServiceScoped
class NotificationOverlayTile @Inject constructor(
    private val notificationOverlayController: NotificationOverlayController,
) : QSTile() {

    init {
        isSelected = notificationOverlayController.showNotificationOverlay
    }

    override fun getTitleRes(): Int = R.string.qs_notification_overlay

    override fun getIconRes(): Int = R.drawable.ic_qs_notification_overlay

    override fun handleClick(v: View) {
        super.handleClick(v)
        notificationOverlayController.showNotificationOverlay = isSelected
    }
}
