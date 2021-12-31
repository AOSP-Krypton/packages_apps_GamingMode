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

package org.exthmui.game.services

import android.app.KeyguardManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.UserHandle

class DeviceStateListenerService : Service() {
    private lateinit var keyguardManager: KeyguardManager

    private val deviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val serviceIntent = Intent(context, GamingService::class.java)
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> stopServiceAsUser(serviceIntent, UserHandle.CURRENT)
                Intent.ACTION_SCREEN_ON -> {
                    if (!keyguardManager.isDeviceLocked) {
                        startServiceAsUser(serviceIntent, UserHandle.CURRENT)
                    }
                }
                Intent.ACTION_USER_PRESENT -> startServiceAsUser(serviceIntent, UserHandle.CURRENT)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        keyguardManager = getSystemService(KeyguardManager::class.java)
        registerReceiver(deviceStateReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        })
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(deviceStateReceiver)
    }
}