/*
 * Copyright (C) 2020 The exTHmUI Open Source Project
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
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.internal.statusbar.IStatusBarService;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

import org.exthmui.game.R;
import org.exthmui.game.controller.CallViewController;
import org.exthmui.game.controller.DanmakuController;
import org.exthmui.game.controller.FloatingViewController;
import org.exthmui.game.misc.Constants;

@AndroidEntryPoint(Service.class)
public class GamingService extends Hilt_GamingService {

    private static final String TAG = "GamingService";

    private static final String SYS_BROADCAST_GAMING_MODE_OFF = "exthmui.intent.action.GAMING_MODE_OFF";
    private static final String CHANNEL_GAMING_MODE_STATUS = "gaming_mode_status";

    private static final int NOTIFICATION_ID = 1;

    private String mCurrentPackage;

    private final Bundle mCurrentConfig = new Bundle();

    private AudioManager mAudioManager;
    private IStatusBarService mStatusBarService;

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

    private final BroadcastReceiver mGamingActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String target = intent.getStringExtra("target");
            Intent configChangedIntent = new Intent(Constants.Broadcasts.BROADCAST_CONFIG_CHANGED);
            if (Constants.GamingActionTargets.DISABLE_RINGTONE.equals(target)) {
                setDisableRingtone(intent.getBooleanExtra("value", Constants.ConfigDefaultValues.DISABLE_RINGTONE));
            } else {
                return;
            }
            configChangedIntent.putExtras(mCurrentConfig);
            LocalBroadcastManager.getInstance(GamingService.this).sendBroadcast(configChangedIntent);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(getString(R.string.channel_gaming_mode_status));

        registerNotificationListener();
        checkFreeFormSettings();

        mAudioManager = getSystemService(AudioManager.class);
        mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService(STATUS_BAR_SERVICE));

        registerReceiver(mGamingModeOffReceiver, new IntentFilter(SYS_BROADCAST_GAMING_MODE_OFF));
        LocalBroadcastManager.getInstance(this).registerReceiver(mGamingActionReceiver, new IntentFilter(Constants.Broadcasts.BROADCAST_GAMING_ACTION));


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
        if (intent != null && !TextUtils.equals(intent.getStringExtra("package"), mCurrentPackage)) {
            mCurrentPackage = intent.getStringExtra("package");
            updateConfig();
        }

        mMenuOverlay = getIntSetting(Constants.ConfigKeys.MENU_OVERLAY, Constants.ConfigDefaultValues.MENU_OVERLAY) == 1;
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

    private void checkFreeFormSettings() {
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVELOPMENT_ENABLE_FREEFORM_WINDOWS_SUPPORT, 1);
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVELOPMENT_FORCE_RESIZABLE_ACTIVITIES, 1);
    }

    private void updateConfig() {
        // gesture
        boolean disableGesture = getBooleanSetting(Constants.ConfigKeys.DISABLE_GESTURE, Constants.ConfigDefaultValues.DISABLE_GESTURE);
        setDisableGesture(disableGesture);

        // quick-start apps
        setAutoRotation(false);

        // misc
        boolean disableRingtone = getBooleanSetting(Constants.ConfigKeys.DISABLE_RINGTONE, Constants.ConfigDefaultValues.DISABLE_RINGTONE);
        setDisableRingtone(disableRingtone);
        boolean disableAutoBrightness = getBooleanSetting(Constants.ConfigKeys.DISABLE_AUTO_BRIGHTNESS, Constants.ConfigDefaultValues.DISABLE_AUTO_BRIGHTNESS);
        setDisableAutoBrightness(disableAutoBrightness, false);

        // enabled overlay menu
        mCurrentConfig.putInt(Constants.ConfigKeys.MENU_OVERLAY, getIntSetting(Constants.ConfigKeys.MENU_OVERLAY, Constants.ConfigDefaultValues.MENU_OVERLAY));

        Intent intent = new Intent(Constants.Broadcasts.BROADCAST_CONFIG_CHANGED);
        intent.putExtras(mCurrentConfig);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void setDisableGesture(boolean disable) {
        mCurrentConfig.putBoolean(Constants.ConfigKeys.DISABLE_GESTURE, disable);
        try {
            mStatusBarService.setBlockedGesturalNavigation(disable);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to disable/enable gesture!", e);
        }
    }

    private void setDisableRingtone(boolean disable) {
        if (!mCurrentConfig.containsKey("old_ringer_mode")) {
            mCurrentConfig.putInt("old_ringer_mode", mAudioManager.getRingerModeInternal());
        }
        int oldRingerMode = mCurrentConfig.getInt("old_ringer_mode", AudioManager.RINGER_MODE_NORMAL);
        mAudioManager.setRingerModeInternal(disable ? AudioManager.RINGER_MODE_SILENT : oldRingerMode);
        mCurrentConfig.putBoolean(Constants.ConfigKeys.DISABLE_RINGTONE, disable);
    }

    private void setDisableAutoBrightness(boolean disable, boolean restore) {
        if (!mCurrentConfig.containsKey("old_auto_brightness")) {
            int oldValue = getIntSetting(Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            mCurrentConfig.putInt("old_auto_brightness", oldValue);
        }
        if (!restore) {
            if (disable) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            } else {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            }
            mCurrentConfig.putBoolean(Constants.ConfigKeys.DISABLE_AUTO_BRIGHTNESS, disable);
        } else {
            int oldValue = mCurrentConfig.getInt("old_auto_brightness");
            mCurrentConfig.putBoolean(Constants.ConfigKeys.DISABLE_AUTO_BRIGHTNESS, oldValue == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, oldValue);
        }
    }

    private void setAutoRotation(boolean restore) {
        if (!mCurrentConfig.containsKey("old_auto_rotation")) {
            mCurrentConfig.putInt("old_auto_rotation", getIntSetting(Settings.System.ACCELEROMETER_ROTATION, 0));
        }
        if (!restore) {
            Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
        } else {
            int oldValue = mCurrentConfig.getInt("old_auto_rotation");
            Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, oldValue);
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
        setDisableGesture(false);
        setDisableAutoBrightness(false, true);
        setDisableRingtone(false);
        setAutoRotation(true);
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

    private boolean getBooleanSetting(String key, boolean def) {
        return Settings.System.getInt(getContentResolver(), key, def ? 1 : 0) != 0;
    }

    private int getIntSetting(String key, int def) {
        return Settings.System.getInt(getContentResolver(), key, def);
    }
}
