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

class DanmakuSettingsFragment: MainSwitchSettingsFragment() {
    override fun getTitle(): Int = R.string.gaming_mode_notification_danmaku_title

    override fun getPreferenceScreenResId(): Int = R.xml.danmaku_settings_fragment

    override fun getMainSwitchKey(): String = MAIN_SWITCH_KEY

    companion object {
        private const val MAIN_SWITCH_KEY = "gaming_mode_show_danmaku"
    }
}
