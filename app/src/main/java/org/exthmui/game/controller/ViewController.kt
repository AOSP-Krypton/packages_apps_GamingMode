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

package org.exthmui.game.controller

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.view.LayoutInflater
import android.view.WindowManager

open class ViewController(private val context: Context) {
    protected var isPortrait: Boolean =
        context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    protected val windowManager: WindowManager = context.getSystemService(WindowManager::class.java)
    protected var bounds = windowManager.currentWindowMetrics.bounds
    protected var layoutInflater: LayoutInflater = LayoutInflater.from(context)

    fun init() {
        if (Settings.canDrawOverlays(context)) {
            initController()
        }
    }

    protected open fun initController() {}

    fun updateConfiguration(newConfig: Configuration) {
        isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
        bounds = windowManager.currentWindowMetrics.bounds
        onConfigurationChanged(newConfig)
    }

    protected open fun onConfigurationChanged(newConfig: Configuration) {}

    fun destroy() {
        onDestroy()
    }

    protected open fun onDestroy() {}
}