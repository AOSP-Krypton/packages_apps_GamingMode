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

package org.exthmui.game.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.internal.statusbar.IStatusBarService;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

import org.exthmui.game.R;
import org.exthmui.game.controller.CallViewController;
import org.exthmui.game.controller.DanmakuController;
import org.exthmui.game.controller.FloatingViewController;

@AndroidEntryPoint(Service.class)
public class GamingService extends Hilt_GamingService {

    private static final String TAG = "GamingService";

    private static final String SYS_BROADCAST_GAMING_MODE_OFF = "exthmui.intent.action.GAMING_MODE_OFF";
    private static final String CHANNEL_GAMING_MODE_STATUS = "gaming_mode_status";

    private static final int NOTIFICATION_ID = 1;

    private boolean mMenuOverlay;

    private final BroadcastReceiver mGamingModeOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equals(SYS_BROADCAST_GAMING_MODE_OFF)) {
                stopSelf();
            }
        }
    };

    @Inject
    CallViewController mCallViewController;

    @Inject
    DanmakuController mDanmakuController;

    @Inject
    FloatingViewController mFloatingViewController;

    private final DanmakuService mDanmakuService = new DanmakuService();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(getString(R.string.channel_gaming_mode_status));

        registerNotificationListener();

        registerReceiver(mGamingModeOffReceiver, new IntentFilter(SYS_BROADCAST_GAMING_MODE_OFF));

        PendingIntent stopGamingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(SYS_BROADCAST_GAMING_MODE_OFF), PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_GAMING_MODE_STATUS);
        Notification.Action.Builder actionBuilder = new Notification.Action.Builder(null, getString(R.string.action_stop_gaming_mode), stopGamingIntent);
        builder.addAction(actionBuilder.build());
        builder.setContentText(getString(R.string.gaming_mode_running));
        builder.setSmallIcon(R.drawable.ic_notification_game);

        Notification mGamingNotification = builder.build();
        startForeground(NOTIFICATION_ID, mGamingNotification);

        Toast.makeText(this, R.string.gaming_mode_on, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mMenuOverlay = Settings.System.getInt(getContentResolver(),
                Settings.System.GAMING_MODE_USE_OVERLAY_MENU, 1) == 1;
        if (mMenuOverlay) {
            mDanmakuController.init();
            mDanmakuService.init(this, mDanmakuController);
            mFloatingViewController.init();
            mCallViewController.init();
        }
        Settings.System.putInt(getContentResolver(), Settings.System.GAMING_MODE_ACTIVE, 1);
        return super.onStartCommand(intent, flags, startId);
    }

    private void registerNotificationListener() {
        final ComponentName componentName = new ComponentName(this, GamingService.class);
        try {
            mDanmakuService.registerAsSystemService(this, componentName, UserHandle.USER_CURRENT);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while registering danmaku service");
        }
    }

    private void unregisterNotificationListener() {
        try {
            mDanmakuService.unregisterAsSystemService();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while unregistering danmaku service");
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mGamingModeOffReceiver);
        unregisterNotificationListener();
        if (mMenuOverlay) {
            mDanmakuController.destroy();
            mCallViewController.destroy();
            mFloatingViewController.destroy();
        }
        Settings.System.putInt(getContentResolver(), Settings.System.GAMING_MODE_ACTIVE, 0);
        Toast.makeText(this, R.string.gaming_mode_off, Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mMenuOverlay) {
            mDanmakuController.updateConfiguration(newConfig);
            mFloatingViewController.updateConfiguration(newConfig);
            mCallViewController.updateConfiguration(newConfig);
        }
    }

    private void createNotificationChannel(String channelName) {
        NotificationChannel channel = new NotificationChannel(GamingService.CHANNEL_GAMING_MODE_STATUS,
                channelName, NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}
