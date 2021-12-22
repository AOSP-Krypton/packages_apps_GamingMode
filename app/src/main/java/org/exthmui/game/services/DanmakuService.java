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
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

import org.exthmui.game.controller.DanmakuController;

public class DanmakuService extends NotificationListenerService {

    private final Map<String, String> mLastNotificationMap = new HashMap<>();

    private boolean mUseFilter;
    private String[] mNotificationBlacklist;

    private DanmakuController mDanmakuController;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (mDanmakuController == null || !mDanmakuController.shouldShowDanmaku()) return;
        Bundle extras = sbn.getNotification().extras;
        if (!isInBlackList(sbn.getPackageName())){
            String lastNotification = mLastNotificationMap.getOrDefault(sbn.getPackageName(), "");
            String title = extras.getString(Notification.EXTRA_TITLE);
            if (TextUtils.isEmpty(title)) title = extras.getString(Notification.EXTRA_TITLE_BIG);
            String text = extras.getString(Notification.EXTRA_TEXT);
            StringBuilder builder = new StringBuilder();
            if (!TextUtils.isEmpty(title)) {
                builder.append("[");
                builder.append(title);
                builder.append("] ");
            }
            if (!TextUtils.isEmpty(text)) {
                builder.append(text);
            }
            String danmakuText = builder.toString();
            if (!TextUtils.isEmpty(danmakuText) && (!mUseFilter || compareDanmaku(danmakuText, lastNotification))) {
                mDanmakuController.showDanmaku(danmakuText);
            }
            mLastNotificationMap.put(sbn.getPackageName(), danmakuText);
        }
    }

    public void init(Context context, DanmakuController controller) {
        mDanmakuController = controller;
        final String blacklist = Settings.System.getString(context.getContentResolver(),
                Settings.System.GAMING_MODE_DANMAKU_APP_BLACKLIST);
        if (blacklist != null && !blacklist.isEmpty()) {
            mNotificationBlacklist = blacklist.split(",");
        }
        mUseFilter = Settings.System.getInt(context.getContentResolver(),
                Settings.System.GAMING_MODE_DANMAKU_DYNAMIC_NOTIFICATION_FILTER, 1) == 1;
    }

    private boolean isInBlackList(String packageName) {
        if (mNotificationBlacklist == null || mNotificationBlacklist.length == 0) return false;
        for (String str : mNotificationBlacklist) {
            if (TextUtils.equals(str, packageName)) {
                return true;
            }
        }
        return false;
    }

    private boolean compareDanmaku(String a, String b) {
        String tA = a.replaceAll("[\\d]+\\.[\\d]+|[\\d]", "");
        String tB = b.replaceAll("[\\d]+\\.[\\d]+|[\\d]", "");
        int filterThreshold = 3;
        if (TextUtils.isEmpty(tA) || TextUtils.isEmpty(tB)) {
            return levenshtein(a, b) > filterThreshold;
        } else {
            return levenshtein(tA, tB) > filterThreshold;
        }
    }

    // 最小编辑距离
    private static int levenshtein(CharSequence a, CharSequence b) {
        if (TextUtils.isEmpty(a)) {
            return TextUtils.isEmpty(b) ? 0 : b.length();
        } else if (TextUtils.isEmpty(b)) {
            return TextUtils.isEmpty(a) ? 0 : a.length();
        }
        final int lenA = a.length(), lenB = b.length();
        int[][] dp = new int[lenA+1][lenB+1];
        int flag = 0;
        for (int i = 0; i <= lenA; i++) {
            for (int j = 0; j <= lenB; j++) dp[i][j] = lenA + lenB;
        }
        for(int i=1; i <= lenA; i++) dp[i][0] = i;
        for(int j=1; j <= lenB; j++) dp[0][j] = j;
        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                if (a.charAt(i-1) == b.charAt(j-1)) {
                    flag = 0;
                } else {
                    flag = 1;
                }
                dp[i][j] = Math.min(dp[i-1][j-1] + flag, Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1));
            }
        }
        return dp[lenA][lenB];
    }
}
