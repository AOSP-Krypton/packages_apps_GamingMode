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

package org.exthmui.game.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import org.exthmui.game.services.DeviceStateListenerService

import org.exthmui.game.services.GamingService

class GamingBroadcastReceiver: BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val deviceStateServiceIntent = Intent(context, DeviceStateListenerService::class.java)
        val serviceIntent = Intent(context, GamingService::class.java)
        if (intent.action == SYS_BROADCAST_GAMING_MODE_ON) {
            context.startServiceAsUser(deviceStateServiceIntent, UserHandle.CURRENT)
            context.startServiceAsUser(serviceIntent, UserHandle.CURRENT)
        } else if (intent.action == SYS_BROADCAST_GAMING_MODE_OFF) {
            context.stopServiceAsUser(deviceStateServiceIntent, UserHandle.CURRENT)
            context.stopServiceAsUser(serviceIntent, UserHandle.CURRENT)
        }
    }

    companion object {
        private const val SYS_BROADCAST_GAMING_MODE_ON = "exthmui.intent.action.GAMING_MODE_ON"
        private const val SYS_BROADCAST_GAMING_MODE_OFF = "exthmui.intent.action.GAMING_MODE_OFF"
    }
}
