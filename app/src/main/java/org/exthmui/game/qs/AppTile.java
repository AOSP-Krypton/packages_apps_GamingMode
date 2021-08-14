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

package org.exthmui.game.qs;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.LinearLayout;

import org.exthmui.game.R;

public class AppTile extends TileBase {

    public AppTile(Context context, String packageName) {
        super(context, packageName, packageName, null);
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            int size = (int) context.getResources().getDimension(R.dimen.app_qs_icon_size);
            int padding = (int) context.getResources().getDimension(R.dimen.app_qs_icon_padding);
            qsText.setVisibility(View.GONE);
            qsIcon.setPadding(padding,padding,padding,padding);
            qsIcon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
            qsIcon.setImageDrawable(applicationInfo.loadIcon(pm));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }
}
