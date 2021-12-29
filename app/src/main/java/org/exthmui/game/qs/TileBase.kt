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
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View

import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView

import org.exthmui.game.R

open class TileBase @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr), View.OnClickListener {

    private var isToggleable = true

    private val background = LayerDrawable(
        arrayOf(AppCompatResources.getDrawable(context, R.drawable.qs_background))
    )
    private var icon: Drawable? = null

    init {
        gravity = Gravity.CENTER
        maxEms = 6
        maxLines = 2
        with(context.resources) {
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                getDimensionPixelSize(R.dimen.gaming_qs_text_size).toFloat()
            )
            compoundDrawablePadding = getDimensionPixelSize(R.dimen.qs_icon_margin)
        }
    }

    final override fun setTextSize(value: Int, float: Float) {
        super.setTextSize(value, float)
    }

    fun setToggleable(isToggleable: Boolean) {
        this.isToggleable = isToggleable
    }

    fun setIcon(resId: Int) {
        if (resId != 0) {
            icon = AppCompatResources.getDrawable(context, resId)?.apply {
                setTint(if (isSelected) Color.BLACK else Color.WHITE)
            }
            if (background.numberOfLayers >= 2 && background.getDrawable(1) != null) {
                // Replace existing drawable at index instead of adding a layer on top it
                background.setDrawable(1, icon)
            } else {
                background.addLayer(icon)
            }
            val inset = context.resources.getDimensionPixelSize(R.dimen.gaming_qs_icon_padding)
            background.setLayerInset(1, inset, inset, inset, inset)
            val size = context.resources.getDimensionPixelSize(R.dimen.gaming_qs_icon_size)
            background.setBounds(0, 0, size, size)
            setCompoundDrawables(null, background, null, null)
        }
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        icon?.setTint(if (isSelected) Color.BLACK else Color.WHITE)
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