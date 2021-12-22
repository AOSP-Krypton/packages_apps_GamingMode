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

package org.exthmui.game.misc;

import android.provider.Settings;

public class Constants {

    public static class Broadcasts {

        // Local broadcast
        public static final String BROADCAST_CONFIG_CHANGED = "org.exthmui.game.CONFIG_CHANGED";

        /*
        * GAMING_ACTION 需要携带的extra: String target, value
        */
        public static final String BROADCAST_GAMING_ACTION = "org.exthmui.game.GAMING_ACTION";
    }

    public static class ConfigKeys {
        // 禁用自动亮度
        public static final String DISABLE_AUTO_BRIGHTNESS = Settings.System.GAMING_MODE_DISABLE_AUTO_BRIGHTNESS;

        // 请勿打扰模式
        public static final String DISABLE_RINGTONE = Settings.System.GAMING_MODE_DISABLE_RINGTONE;

        // 屏蔽手势
        public static final String DISABLE_GESTURE = Settings.System.GAMING_MODE_DISABLE_GESTURE;

        // 性能配置
        public static final String CHANGE_PERFORMANCE_LEVEL = Settings.System.GAMING_MODE_CHANGE_PERFORMANCE_LEVEL;
        public static final String PERFORMANCE_LEVEL = Settings.System.GAMING_MODE_PERFORMANCE_LEVEL;

        public static final String MENU_OPACITY = Settings.System.GAMING_MODE_MENU_OPACITY;

        public static final String MENU_OVERLAY = Settings.System.GAMING_MODE_USE_OVERLAY_MENU;
    }

    public static class ConfigDefaultValues {
        // 请勿打扰模式
        public static final boolean DISABLE_RINGTONE = true;

        // 关闭自动亮度
        public static final boolean DISABLE_AUTO_BRIGHTNESS = true;

        // 屏蔽手势
        public static final boolean DISABLE_GESTURE = true;

        // 性能配置
        public static final boolean CHANGE_PERFORMANCE_LEVEL = true;
        public static final int PERFORMANCE_LEVEL = 5;

        // Opacity of Gaming Menu (in percent)
        public static final int MENU_OPACITY = 75;

        // Whether to show menu overlay or not (1/0)
        public static final int MENU_OVERLAY = 1;
    }

    public static class GamingActionTargets {
        // 请勿打扰模式
        public static final String DISABLE_RINGTONE = ConfigKeys.DISABLE_RINGTONE;
        // 屏蔽手势
        public static final String DISABLE_GESTURE = ConfigKeys.DISABLE_GESTURE;
        // 自动亮度
        public static final String DISABLE_AUTO_BRIGHTNESS = ConfigKeys.DISABLE_AUTO_BRIGHTNESS;
        // 性能配置
        public static final String PERFORMANCE_LEVEL = ConfigKeys.PERFORMANCE_LEVEL;
    }

    public static class LocalConfigKeys {
        // 悬浮球位置
        public static final String FLOATING_BUTTON_COORDINATE_HORIZONTAL_X = "floating_button_horizontal_x";
        public static final String FLOATING_BUTTON_COORDINATE_HORIZONTAL_Y = "floating_button_horizontal_y";
        public static final String FLOATING_BUTTON_COORDINATE_VERTICAL_X = "floating_button_vertical_x";
        public static final String FLOATING_BUTTON_COORDINATE_VERTICAL_Y = "floating_button_vertical_y";
        // 屏幕录制选项
        public static final String SCREEN_RECORDING_AUDIO_SOURCE = "screen_recording_audio_source";
        public static final String SCREEN_RECORDING_SHOW_TAP = "screen_recording_show_tap";
    }

    public static class LocalConfigDefaultValues {
        public static final int SCREEN_RECORDING_AUDIO_SOURCE = 3;
        public static final boolean SCREEN_RECORDING_SHOW_TAP = false;
    }
}
