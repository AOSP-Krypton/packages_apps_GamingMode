package org.exthmui.game.ui

import android.os.Bundle
import android.view.View

import androidx.fragment.app.FragmentActivity

import org.exthmui.game.R

class MainActivity: FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val view = findViewById<View>(R.id.homepage_container)
        // Prevent inner RecyclerView gets focus and invokes scrolling.
        view.isFocusableInTouchMode = true
        view.requestFocus()
    }
}