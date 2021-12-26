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

package org.exthmui.game.controller

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioSystem
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.ImageView

import androidx.core.app.ActivityCompat

import dagger.hilt.android.qualifiers.ApplicationContext

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import javax.inject.Inject
import javax.inject.Singleton

import org.exthmui.game.R

@Singleton
class CallViewController @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewController(context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)
    private val telecomManager = context.getSystemService(TelecomManager::class.java)

    private val autoAnswerCall = Settings.System.getInt(
        context.contentResolver,
        Settings.System.GAMING_MODE_AUTO_ANSWER_CALL, 0
    ) == 1

    private var executor: ExecutorService? = null

    private var callControlButton: ImageView? = null

    private val telephonyCallback = Callback()

    private var callStatus: Int = TelephonyManager.CALL_STATE_OFFHOOK

    private val handler = Handler(Looper.getMainLooper())

    fun initView(button: ImageView?) {
        callControlButton = button
        callControlButton?.setOnClickListener {
            if (!checkPermission()) return@setOnClickListener
            @Suppress("DEPRECATION")
            if (callStatus == TelephonyManager.CALL_STATE_OFFHOOK)
                telecomManager.endCall()
            else
                telecomManager.acceptRingingCall()
        }
        if (callControlButton != null) {
            executor = Executors.newSingleThreadExecutor()
            telephonyManager.registerTelephonyCallback(executor!!, telephonyCallback)
        }
    }

    override fun onDestroy() {
        telephonyManager.unregisterTelephonyCallback(telephonyCallback)
        executor?.shutdownNow()
        callControlButton = null
    }

    private fun isHeadsetPluggedIn(): Boolean {
        val audioDeviceInfoArr: Array<AudioDeviceInfo> =
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return audioDeviceInfoArr.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
    }

    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "App does not have required permission ANSWER_PHONE_CALLS")
            return false
        }
        return true
    }

    private inner class Callback : TelephonyCallback(), TelephonyCallback.CallStateListener {
        private var previousState = -1
        private var previousAudioMode = audioManager.mode

        override fun onCallStateChanged(state: Int) {
            if (!autoAnswerCall || !checkPermission()) return

            @Suppress("DEPRECATION")
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    telecomManager.acceptRingingCall()
                    handler.post {
                        callControlButton?.setImageResource(R.drawable.ic_call_accept)
                        callControlButton?.visibility = View.VISIBLE
                    }
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (previousState == TelephonyManager.CALL_STATE_RINGING) {
                        previousAudioMode = audioManager.mode
                        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                        if (isHeadsetPluggedIn()) {
                            audioManager.isSpeakerphoneOn = false
                            AudioSystem.setForceUse(
                                AudioSystem.FOR_COMMUNICATION,
                                AudioSystem.FORCE_NONE
                            )
                        } else {
                            audioManager.isSpeakerphoneOn = true
                            AudioSystem.setForceUse(
                                AudioSystem.FOR_COMMUNICATION,
                                AudioSystem.FORCE_SPEAKER
                            )
                        }
                        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    }
                    handler.post {
                        callControlButton?.setImageResource(R.drawable.ic_call_end)
                        callControlButton?.visibility = View.VISIBLE
                    }
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (previousState == TelephonyManager.CALL_STATE_OFFHOOK) {
                        audioManager.mode = previousAudioMode
                        AudioSystem.setForceUse(
                            AudioSystem.FOR_COMMUNICATION,
                            AudioSystem.FORCE_NONE
                        )
                        audioManager.isSpeakerphoneOn = false
                    }
                    handler.post {
                        callControlButton?.visibility = View.GONE
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CallViewController"
    }
}