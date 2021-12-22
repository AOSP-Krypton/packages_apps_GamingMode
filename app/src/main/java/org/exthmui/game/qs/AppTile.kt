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
package org.exthmui.game.qs

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.ActivityTaskManager
import android.app.WindowConfiguration
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.RemoteException
import android.os.UserHandle
import android.util.Log
import android.view.WindowManager

import org.exthmui.game.R

@SuppressLint("ViewConstructor")
class AppTile(
    context: Context,
    private val packageName: String,
) : TileBase(context, 0, 0) {
    private val activityTaskManager = ActivityTaskManager.getInstance()
    private val packageManager: PackageManager = context.packageManager
    private val activityOptions = ActivityOptions.makeBasic()
    private var packageInstalled: Boolean

    init {
        var ai: ApplicationInfo? = null
        try {
            ai = packageManager.getApplicationInfo(packageName, 0)
            packageInstalled = true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package $packageName not found")
            packageInstalled = false
        }
        qsText.visibility = GONE
        if (packageInstalled) {
            setToggleable(false)
            val size = resources.getDimensionPixelSize(R.dimen.app_qs_icon_size)
            val padding = resources.getDimensionPixelSize(R.dimen.app_qs_icon_padding)
            qsIcon.setPadding(padding, padding, padding, padding)
            qsIcon.layoutParams = LayoutParams(size, size)
            qsIcon.setImageDrawable(ai!!.loadIcon(packageManager))
            activityOptions.launchWindowingMode = WindowConfiguration.WINDOWING_MODE_FREEFORM
        } else {
            qsIcon.visibility = GONE
        }
    }

    override fun handleClick(isSelected: Boolean) {
        if (packageInstalled) startActivity()
    }

    private fun startActivity() {
        val wm: WindowManager = context.getSystemService(WindowManager::class.java)
        val bounds: Rect = wm.currentWindowMetrics.bounds
        val isPortrait = resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
        val width = if (isPortrait) bounds.width() - 100 else bounds.height() - 100
        val height = width * if (isPortrait) 4 / 3 else 3 / 4
        activityOptions.launchBounds = Rect(50, 50, 50 + width, 50 + height)
        try {
            val recentTaskInfoList: List<ActivityManager.RecentTaskInfo> =
                activityTaskManager.getRecentTasks(
                    100,
                    ActivityManager.RECENT_IGNORE_UNAVAILABLE,
                    UserHandle.USER_CURRENT
                )
            recentTaskInfoList.firstOrNull { packageName == it.topActivity?.packageName }
                ?.takeIf { it.isRunning }
                ?.let {
                    ActivityManager.getService()
                        .startActivityFromRecents(it.taskId, activityOptions.toBundle())
                }
        } catch (e: RemoteException) {
            Log.e(TAG, "RemoteException while launching app", e)
        }
        val startAppIntent: Intent? = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(
                Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            setPackage(null)
        }
        context.startActivityAsUser(startAppIntent, activityOptions.toBundle(), UserHandle.CURRENT)
    }

    companion object {
        private const val TAG = "AppTile"
    }
}