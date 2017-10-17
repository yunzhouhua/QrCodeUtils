package com.yunzhou.qrcodelib.zxing;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;

import com.google.zxing.Result;
import com.yunzhou.qrcodelib.zxing.camera.CameraManager;

/**
 * Created by huayunzhou on 2017/10/17.
 */

public interface IScanQRCode {

    Rect getCropRect();
    CameraManager getCameraManager();
    Handler getHandler();
    void handleDecode(Result rawResult, Bundle bundle);
}
