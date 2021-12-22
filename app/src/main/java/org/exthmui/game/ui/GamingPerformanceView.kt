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

package org.exthmui.game.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.SeekBar

import org.exthmui.game.R

class GamingPerformanceView(
    context: Context,
    attrs: AttributeSet?
): LinearLayout(context, attrs), SeekBar.OnSeekBarChangeListener {
    
    constructor(context: Context): this(context, null)
    
    private val seekBar: SeekBar
    private var listener: ((level: Int) -> Unit)? = null
    
    init {
        LayoutInflater.from(context).inflate(R.layout.gaming_perofrmance_layout, this, true)
        seekBar = findViewById<SeekBar>(R.id.performance_seek).also {
            it.setOnSeekBarChangeListener(this)
        }
    }

    fun setLevel(level: Int) {
        seekBar.progress = level
    }

    fun setOnUpdateListener(listener: ((Int) -> Unit)?) {
        this.listener = listener
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        // No-op
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        // No-op
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        listener?.invoke(this.seekBar.progress)
    }
}
