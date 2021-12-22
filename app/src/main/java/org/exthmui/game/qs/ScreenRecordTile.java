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

package org.exthmui.game.qs;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;

import androidx.preference.PreferenceManager;

import com.android.systemui.screenrecord.IRemoteRecording;
import com.android.systemui.screenrecord.IRecordingCallback;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;
import org.exthmui.game.ui.ScreenRecordConfDialogActivity;

public class ScreenRecordTile extends TileBase implements View.OnLongClickListener {

    private static final String TAG = "ScreenRecordTile";
    private final RecordingCallback mCallback = new RecordingCallback();
    private final Context mContext;
    private final SharedPreferences mPreferences;

    private IRemoteRecording mBinder;
    private final boolean mIsBound;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName,IBinder iBinder) {
            mBinder = IRemoteRecording.Stub.asInterface(iBinder);
            try {
                mBinder.addRecordingCallback(mCallback);
                setSelected(mBinder.isRecording());
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while binder transaction");
            }
        } 
        
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBinder = null;
        }
    };

    public ScreenRecordTile(Context context) {
        super(context, R.string.qs_screen_record, R.drawable.ic_qs_screenrecord);
        setOnLongClickListener(this);
        mContext = context.getApplicationContext();
        final Intent intent = new Intent();
        intent.setAction("com.android.systemui.screenrecord.RecordingService");
        intent.setPackage("com.android.systemui");
        mIsBound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    public void handleClick(boolean isSelected) {
        super.handleClick(isSelected);
        if (mBinder == null) {
            return;
        }
        try {
            if (isSelected()) {
                mBinder.stopRecording();
            } else {
                boolean showTap = mPreferences.getBoolean(Constants.LocalConfigKeys.SCREEN_RECORDING_SHOW_TAP, Constants.LocalConfigDefaultValues.SCREEN_RECORDING_SHOW_TAP);
                int audioSource = mPreferences.getInt(Constants.LocalConfigKeys.SCREEN_RECORDING_AUDIO_SOURCE, Constants.LocalConfigDefaultValues.SCREEN_RECORDING_AUDIO_SOURCE);
                mBinder.startRecording(audioSource, showTap);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (!isSelected()) {
            final Intent intent = new Intent(mContext, ScreenRecordConfDialogActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        if (mBinder != null) {
            try {
                mBinder.removeRecordingCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while removing binder callback");
            }
        }
        if (mIsBound) mContext.unbindService(mServiceConnection);
    }

    private class RecordingCallback extends IRecordingCallback.Stub {
        @Override
        public void onRecordingStart() {
            setSelected(true);
        }

        @Override
        public void onRecordingEnd() {
            setSelected(false);
        }
    }
}
