/*
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
import org.exthmui.game.controller.FloatingViewController

abstract class QSTile {

    var isToggleable = true
    var isSelected = false
        set(value) {
            field = value
            callback?.onStateChanged(value)
        }

    var host: FloatingViewController? = null

    private var callback: Callback? = null

    abstract fun getTitleRes(): Int

    abstract fun getIconRes(): Int

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    open fun handleClick(v: View) {
        if (isToggleable) {
            isSelected = !isSelected
        }
    }

    open fun destroy() {}

    interface Callback {
        fun onStateChanged(selected: Boolean)
    }
}