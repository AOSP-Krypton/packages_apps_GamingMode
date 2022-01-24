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
import android.content.SharedPreferences
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import android.view.View

import com.android.internal.statusbar.IStatusBarService

import dagger.hilt.android.scopes.ServiceScoped

import javax.inject.Inject

import org.exthmui.game.R

@ServiceScoped
class LockGestureTile @Inject constructor(
    sharedPreferences: SharedPreferences,
) : QSTile() {

    private val statusBarService = IStatusBarService.Stub.asInterface(
        ServiceManager.getService(Context.STATUS_BAR_SERVICE)
    )

    init {
        isSelected = sharedPreferences.getBoolean(DISABLE_GESTURE_KEY, false)
        try {
            statusBarService.setBlockedGesturalNavigation(isSelected)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to toggle gesture")
        }
    }

    override fun getTitleRes(): Int = R.string.qs_lock_gesture

    override fun getIconRes(): Int = R.drawable.ic_qs_disable_gesture

    override fun handleClick(v: View) {
        super.handleClick(v)
        try {
            statusBarService.setBlockedGesturalNavigation(isSelected)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to toggle gesture")
        }
    }

    override fun destroy() {
        statusBarService.setBlockedGesturalNavigation(false)
    }

    companion object {
        private const val TAG = "LockGestureTile"

        private const val DISABLE_GESTURE_KEY = "gaming_mode_disable_gesture"
    }
}