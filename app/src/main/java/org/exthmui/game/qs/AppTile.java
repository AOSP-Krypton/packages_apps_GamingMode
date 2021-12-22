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

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.util.List;

import org.exthmui.game.R;

@SuppressLint("ViewConstructor")
public class AppTile extends TileBase {

    private static final String TAG = "AppTile";
    private final String mPackageName;
    private final PackageManager mPackageManager;

    private ActivityOptions mActivityOptions;
    private boolean packageInstalled;

    public AppTile(Context context, String packageName) {
        super(context, 0, 0);
        mPackageName = packageName;
        mPackageManager = context.getPackageManager();
        final ApplicationInfo ai;
        try {
            ai = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package " + packageName + " not found");
            return;
        }
        packageInstalled = true;

        setToggleable(false);
        final int size = getResources().getDimensionPixelSize(R.dimen.app_qs_icon_size);
        final int padding = getResources().getDimensionPixelSize(R.dimen.app_qs_icon_padding);
        qsText.setVisibility(View.GONE);
        qsIcon.setPadding(padding, padding, padding, padding);
        qsIcon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        qsIcon.setImageDrawable(ai.loadIcon(mPackageManager));

        mActivityOptions = ActivityOptions.makeBasic();
        mActivityOptions.setLaunchWindowingMode(WindowConfiguration.WINDOWING_MODE_FREEFORM);
    }

    @Override
    protected void handleClick(boolean isSelected) {
        if (packageInstalled) startActivity();
    }

    private void startActivity() {
        final WindowManager wm = getContext().getSystemService(WindowManager.class);
        final Rect bounds = wm.getCurrentWindowMetrics().getBounds();
        final boolean isPortrait = getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT;
        final int width = isPortrait ? (bounds.width() - 100) : (bounds.height() - 100);
        final int height = width * (isPortrait ? (4 / 3) : (3 / 4));
        mActivityOptions.setLaunchBounds(new Rect(50, 50, 50 + width, 50 + height));

        final ActivityManager am = getContext().getSystemService(ActivityManager.class);
        try {
            final List<ActivityManager.RecentTaskInfo> recentTaskInfoList = am.getRecentTasks(100, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
            for (ActivityManager.RecentTaskInfo info : recentTaskInfoList) {
                if (info.isRunning && info.topActivity != null && mPackageName.equals(info.topActivity.getPackageName())) {
                    am.getService().startActivityFromRecents(info.taskId, mActivityOptions.toBundle());
                    return;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while launching app", e);
        }

        final Intent startAppIntent = mPackageManager.getLaunchIntentForPackage(mPackageName);
        startAppIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startAppIntent.setPackage(null);
        getContext().startActivityAsUser(startAppIntent, mActivityOptions.toBundle(), UserHandle.CURRENT);
    }
}
