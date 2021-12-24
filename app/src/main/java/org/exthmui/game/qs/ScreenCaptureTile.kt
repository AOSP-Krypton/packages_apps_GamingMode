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
import android.view.WindowManager.ScreenshotSource.SCREENSHOT_GLOBAL_ACTIONS
import android.view.WindowManager.TAKE_SCREENSHOT_FULLSCREEN

import com.android.internal.util.ScreenshotHelper

import org.exthmui.game.R
import org.exthmui.game.controller.FloatingViewController

class ScreenCaptureTile(context: Context) : TileBase(context) {

    private val screenshotHelper: ScreenshotHelper
    private val takeScreenshot: Runnable
    private var viewController: FloatingViewController? = null

    init {
        setText(R.string.qs_screen_capture)
        setIcon(R.drawable.ic_qs_screenshot)
        screenshotHelper = ScreenshotHelper(context)
        takeScreenshot = Runnable {
            screenshotHelper.takeScreenshot(
                TAKE_SCREENSHOT_FULLSCREEN, true, true,
                SCREENSHOT_GLOBAL_ACTIONS, handler, null
            )
        }
        setToggleable(false)
        isSelected = true
    }

    override fun handleClick(isSelected: Boolean) {
        viewController?.hideGamingMenu()
        handler.postDelayed(takeScreenshot, 300)
    }

    override fun onDestroy() {
        handler.removeCallbacks(takeScreenshot)
    }

    fun setViewController(controller: FloatingViewController) {
        viewController = controller
    }
}