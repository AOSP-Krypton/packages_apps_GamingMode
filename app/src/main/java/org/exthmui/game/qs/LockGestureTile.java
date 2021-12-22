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

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.android.internal.statusbar.IStatusBarService;

import org.exthmui.game.R;

public class LockGestureTile extends TileBase {

    private static final String TAG = "LockGestureTile";
    private final IStatusBarService mStatusBarService;

    public LockGestureTile(Context context) {
        super(context, R.string.qs_lock_gesture, R.drawable.ic_qs_disable_gesture);
        mStatusBarService = IStatusBarService.Stub.asInterface(
            ServiceManager.getService(Context.STATUS_BAR_SERVICE));
    }

    @Override
    protected void handleClick(boolean isSelected) {
        super.handleClick(isSelected);
        try {
            mStatusBarService.setBlockedGesturalNavigation(isSelected);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to toggle gesture");
        }
    }
}
