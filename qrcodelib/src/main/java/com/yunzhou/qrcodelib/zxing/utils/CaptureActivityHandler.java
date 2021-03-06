/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yunzhou.qrcodelib.zxing.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.yunzhou.qrcodelib.R;
import com.yunzhou.qrcodelib.zxing.IScanQRCode;
import com.yunzhou.qrcodelib.zxing.camera.CameraManager;
import com.yunzhou.qrcodelib.zxing.decode.DecodeThread;

import java.util.Collection;
import java.util.Map;

/**
 * This class handles all the messaging which comprises the state machine for
 * capture.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public class CaptureActivityHandler extends Handler {

    private final IScanQRCode activity;
    private final DecodeThread decodeThread;
    private final CameraManager cameraManager;
    private State state;

    public CaptureActivityHandler(IScanQRCode activity,
//                                  Collection<BarcodeFormat> decodeFormats,
                                  Map<DecodeHintType,?> baseHints,
                                  String characterSet,
                                  CameraManager cameraManager,
                                  int decodeMode) {
        this.activity = activity;
        decodeThread = new DecodeThread(activity, baseHints, characterSet, decodeMode);
        decodeThread.start();
        state = State.SUCCESS;

        // Start ourselves capturing previews and decoding.
        this.cameraManager = cameraManager;
        cameraManager.startPreview();
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == R.id.zxing_restart_preview) {
            restartPreviewAndDecode();

        } else if (message.what == R.id.zxing_decode_succeeded) {
            state = State.SUCCESS;
            Bundle bundle = message.getData();

            activity.handleDecode((Result) message.obj, bundle);

        } else if (message.what == R.id.zxing_decode_failed) {// We're decoding as fast as possible, so when one
            // decode fails,
            // start another.
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.zxing_decode);

        } else if (message.what == R.id.zxing_return_scan_result) {
            ((Activity)activity).setResult(Activity.RESULT_OK, (Intent) message.obj);
            ((Activity)activity).finish();

        }
    }

    public void quitSynchronously() {
        state = State.DONE;
        cameraManager.stopPreview();
        Message quit = Message.obtain(decodeThread.getHandler(), R.id.zxing_quit);
        quit.sendToTarget();
        try {
            // Wait at most half a second; should be enough time, and onPause()
            // will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.zxing_decode_succeeded);
        removeMessages(R.id.zxing_decode_failed);
    }

    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.zxing_decode);
        }
    }

    private enum State {
        PREVIEW, SUCCESS, DONE
    }

}
