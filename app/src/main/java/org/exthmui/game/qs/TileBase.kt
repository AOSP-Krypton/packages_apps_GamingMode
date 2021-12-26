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
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import android.widget.ImageView

import org.exthmui.game.R

open class TileBase @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes), View.OnClickListener {

    private var isSelected = false
    private var isToggleable = true

    private val qsIcon: ImageView
    private val qsText: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.gaming_qs_view, this, true)
        qsIcon = findViewById(R.id.qs_icon)
        qsText = findViewById(R.id.qs_text)
    }

    fun setToggleable(isToggleable: Boolean) {
        this.isToggleable = isToggleable
    }

    fun setText(resId: Int) {
        if (resId == 0) {
            qsText.visibility = View.GONE
        } else {
            qsText.setText(resId)
            qsText.visibility = View.VISIBLE
        }
    }

    fun setIcon(resId: Int) {
        if (resId == 0) {
            qsIcon.visibility = View.GONE
        } else {
            qsIcon.setImageResource(resId)
            qsIcon.setColorFilter(if (isSelected) Color.BLACK else Color.WHITE)
            qsIcon.visibility = View.VISIBLE
        }
    }

    override fun setSelected(selected: Boolean) {
        isSelected = selected
        qsIcon.isSelected = selected
        qsIcon.setColorFilter(if (isSelected) Color.BLACK else Color.WHITE)
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
        setOnClickListener(this)
    }

    override fun onDetachedFromWindow() {
        setOnClickListener(null)
        onDestroy()
        super.onDetachedFromWindow()
    }

    open fun onDestroy() {}
}