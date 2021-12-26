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

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Switch

import androidx.preference.forEachIndexed

import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.OnMainSwitchChangeListener
import com.android.settingslib.widget.TopIntroPreference

abstract class MainSwitchSettingsFragment : SettingsFragment(), OnMainSwitchChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(getPreferenceScreenResId(), rootKey)
        findPreference<MainSwitchPreference>(getMainSwitchKey())?.also {
            updatePreferences(it.isChecked())
            it.addOnSwitchChangeListener(this)
        }
    }

    abstract fun getPreferenceScreenResId(): Int

    abstract fun getMainSwitchKey(): String

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onSwitchChanged(switchView: Switch, isChecked: Boolean) {
        updatePreferences(isChecked)
    }

    protected open fun updatePreferences(isChecked: Boolean) {
        preferenceScreen.forEachIndexed { _, preference ->
            if (preference !is MainSwitchPreference &&
                preference !is TopIntroPreference
            ) preference.isVisible = isChecked
        }
    }
}