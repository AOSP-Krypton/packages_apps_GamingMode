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

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import android.widget.ImageView

import org.exthmui.game.R

@SuppressLint("ViewConstructor", "InflateParams")
open class TileBase(
    context: Context?,
    textResId: Int,
    iconResId: Int,
) : LinearLayout(context), View.OnClickListener {

    private var isSelected = false
    private var isToggleable = true

    private val root = LayoutInflater.from(context).inflate(R.layout.gaming_qs_view, null)
    protected val qsIcon: ImageView = root.findViewById<ImageView>(R.id.qs_icon).apply {
        setImageResource(iconResId)
    }
    protected val qsText: TextView = root.findViewById(R.id.qs_text)

    fun setToggleable(isToggleable: Boolean) {
        this.isToggleable = isToggleable
    }

    override fun setSelected(selected: Boolean) {
        isSelected = selected
        qsIcon.isSelected = selected
    }

    override fun isSelected(): Boolean {
        return isSelected
    }

    override fun onClick(v: View) {
        if (isToggleable) {
            isSelected = !isSelected
        }
        handleClick(isSelected)
    }

    protected open fun handleClick(isSelected: Boolean) {
        if (isToggleable) setSelected(isSelected)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addView(root)
        setOnClickListener(this)
    }

    override fun onDetachedFromWindow() {
        onDestroy()
        super.onDetachedFromWindow()
    }

    open fun onDestroy() {}

    init {
        if (textResId != 0) qsText.setText(textResId)
    }
}