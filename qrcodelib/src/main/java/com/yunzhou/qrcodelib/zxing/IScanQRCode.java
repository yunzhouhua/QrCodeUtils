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

    /**
     * 获取扫码区域
     * @return
     */
    Rect getCropRect();

    /**
     * 获取相机管理类，该类主要管理相机启动，预览，关闭等
     * @return
     */
    CameraManager getCameraManager();

    /**
     * 获取处理扫码状态信息的Handler，即CaptureActivityHandler对象
     * @return
     */
    Handler getHandler();

    /**
     * 扫码成功结果回调
     * @param rawResult
     * @param bundle
     */
    void handleDecode(Result rawResult, Bundle bundle);
}
