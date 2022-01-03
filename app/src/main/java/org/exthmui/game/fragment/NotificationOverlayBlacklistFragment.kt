/*
 * Copyright (C) 2021 AOSP-Krypton Project
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

package org.exthmui.game.fragment

import org.exthmui.game.R

class NotificationOverlayBlacklistFragment: AppListFragment() {
    override fun getTitle() = R.string.gaming_mode_notification_overlay_blacklist_title

    override fun getKey() = NOTIFICATION_APP_BLACKLIST_KEY

    companion object {
        private const val NOTIFICATION_APP_BLACKLIST_KEY = "gaming_mode_notification_app_blacklist"
    }
}