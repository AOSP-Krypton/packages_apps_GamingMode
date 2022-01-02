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
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager.ScreenshotSource.SCREENSHOT_GLOBAL_ACTIONS
import android.view.WindowManager.TAKE_SCREENSHOT_FULLSCREEN

import com.android.internal.util.ScreenshotHelper

import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

import javax.inject.Inject

import org.exthmui.game.R

@ServiceScoped
class ScreenCaptureTile @Inject constructor(
    @ApplicationContext context: Context,
) : QSTile() {

    private val handler = Handler(Looper.getMainLooper())
    private val screenshotHelper: ScreenshotHelper
    private val takeScreenshot: Runnable

    init {
        screenshotHelper = ScreenshotHelper(context)
        takeScreenshot = Runnable {
            screenshotHelper.takeScreenshot(
                TAKE_SCREENSHOT_FULLSCREEN, true, true,
                SCREENSHOT_GLOBAL_ACTIONS, handler, null
            )
        }
        isToggleable = false
        isSelected = true
    }

    override fun getTitleRes(): Int = R.string.qs_screen_capture

    override fun getIconRes(): Int = R.drawable.ic_qs_screenshot

    override fun handleClick(v: View) {
        host?.hideGamingMenu()
        handler.postDelayed(takeScreenshot, 300)
    }

    override fun destroy() {
        handler.removeCallbacks(takeScreenshot)
    }
}