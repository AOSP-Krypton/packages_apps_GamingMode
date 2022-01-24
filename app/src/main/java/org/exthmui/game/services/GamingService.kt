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

package org.exthmui.game.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import android.os.RemoteException
import android.os.UserHandle
import android.provider.Settings
import android.util.Log

import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject

import org.exthmui.game.R
import org.exthmui.game.controller.CallViewController
import org.exthmui.game.controller.FloatingViewController
import org.exthmui.game.controller.NotificationOverlayController
import org.exthmui.game.ui.MainActivity

@AndroidEntryPoint(Service::class)
class GamingService : Hilt_GamingService() {

    @Inject
    lateinit var callViewController: CallViewController

    @Inject
    lateinit var floatingViewController: FloatingViewController

    @Inject
    lateinit var notificationOverlayController: NotificationOverlayController

    private val notificationService = NotificationService()

    override fun onCreate() {
        super.onCreate()
        floatingViewController.init()
        callViewController.init()
        notificationOverlayController.init()
        notificationService.init(this, notificationOverlayController)
        registerNotificationListener()

        createNotificationChannel(getString(R.string.channel_gaming_mode_status))
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == STOP_SERVICE_ACTION) {
            stopServiceAsUser(
                Intent(this, DeviceStateListenerService::class.java),
                UserHandle.CURRENT
            )
            stopSelf()
            return START_NOT_STICKY
        }
        Settings.System.putInt(contentResolver, Settings.System.GAMING_MODE_ACTIVE, 1)
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val stopGamingIntent = PendingIntent.getService(
            this,
            STOP_SERVICE_REQUEST_CODE,
            Intent(this, GamingService::class.java).apply {
                action = STOP_SERVICE_ACTION
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        val openActivityIntent = PendingIntent.getActivity(
            this,
            OPEN_ACTIVITY_REQUEST_CODE,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_GAMING_MODE_STATUS).let {
            it.addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.action_stop_gaming_mode),
                    stopGamingIntent
                ).build()
            )
            it.setContentIntent(openActivityIntent)
            it.setContentText(getString(R.string.gaming_mode_active))
            it.setSmallIcon(R.drawable.ic_game)
            it.build()
        }
    }

    private fun registerNotificationListener() {
        val componentName = ComponentName(this, GamingService::class.java)
        try {
            notificationService.registerAsSystemService(
                this,
                componentName,
                UserHandle.USER_CURRENT
            )
        } catch (e: RemoteException) {
            Log.e(TAG, "RemoteException while registering danmaku service")
        }
    }

    private fun unregisterNotificationListener() {
        try {
            notificationService.unregisterAsSystemService()
        } catch (e: RemoteException) {
            Log.e(TAG, "RemoteException while unregistering danmaku service")
        }
    }

    override fun onDestroy() {
        unregisterNotificationListener()
        notificationOverlayController.destroy()
        callViewController.destroy()
        floatingViewController.destroy()
        Settings.System.putInt(contentResolver, Settings.System.GAMING_MODE_ACTIVE, 0)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        floatingViewController.updateConfiguration(newConfig)
        callViewController.updateConfiguration(newConfig)
        notificationOverlayController.updateConfiguration(newConfig)
    }

    private fun createNotificationChannel(channelName: String) {
        val channel = NotificationChannel(
            CHANNEL_GAMING_MODE_STATUS,
            channelName, NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "GamingService"

        private const val CHANNEL_GAMING_MODE_STATUS = "gaming_mode_status"
        private const val NOTIFICATION_ID = 1

        private const val STOP_SERVICE_REQUEST_CODE = 1001
        private const val OPEN_ACTIVITY_REQUEST_CODE = 1002

        private const val STOP_SERVICE_ACTION = "org.exthmui.game.GamingService.ACTION_STOP_SELF"
    }
}