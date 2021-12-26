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

import android.os.Bundle

import org.exthmui.game.R

class GamingDashboardFragment : MainSwitchSettingsFragment() {
    override fun getTitle(): Int = R.string.game_space

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        val isPerfModeSupported = requireContext().resources.getBoolean(
            com.android.internal.R.bool.config_gamingmode_performance
        )
        if (!isPerfModeSupported) {
            preferenceScreen.removePreferenceRecursively(PERF_CATEGORY_KEY)
        }
    }

    override fun getPreferenceScreenResId(): Int = R.xml.gaming_dashboard_fragment

    override fun getMainSwitchKey(): String = MAIN_SWITCH_KEY

    companion object {
        private const val PERF_CATEGORY_KEY = "gaming_mode_performance"

        private const val MAIN_SWITCH_KEY = "gaming_mode_enabled"
    }
}