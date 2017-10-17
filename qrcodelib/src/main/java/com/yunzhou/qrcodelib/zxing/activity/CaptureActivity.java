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
package com.yunzhou.qrcodelib.zxing.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.yunzhou.qrcodelib.R;
import com.yunzhou.qrcodelib.zxing.IScanQRCode;
import com.yunzhou.qrcodelib.zxing.camera.CameraManager;
import com.yunzhou.qrcodelib.zxing.decode.DecodeThread;
import com.yunzhou.qrcodelib.zxing.utils.BeepManager;
import com.yunzhou.qrcodelib.zxing.utils.CaptureActivityHandler;
import com.yunzhou.qrcodelib.zxing.utils.InactivityTimer;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */

public class CaptureActivity extends Activity implements
        SurfaceHolder.Callback, IScanQRCode, View.OnClickListener {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    public static final String SCAN_TITLE = "scan_title";
    public static final String SCAN_TITLE_COLOR = "scan_title_color";
    public static final String SCAN_TITLE_BG_COLOR = "scan_title_bg_color";
    public static final String SCAN_DIST_COLOR = "scan_dist_color";
    public static final String SCAN_DESCRIBE = "scan_describe";
    public static final String SCAN_DESCRIBE_COLOR = "scan_describe_color";
    public static final String SCAN_NEED_BEEP = "scan_need_beep";

    private RelativeLayout mTitleBarView = null;
    private TextView mTitleView = null;
    private SurfaceView scanPreview = null;
    private RelativeLayout scanContainer = null;
    private RelativeLayout scanCropView = null;
    private TextView mScanDescribeView = null;
    private ImageView scanLine = null;
    private ImageView ivBack = null;

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;

    //View 配置相关
    private String mTitle;
    private int mTitleColor;
    private int mTitleBgColor;
    private int mScanColor;
    private String mScanDescribe;
    private int mScanDescColor;
    private boolean mNeedBeep;


    private Rect mCropRect = null;
    private boolean isHasSurface = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_capture);
        initData();
        initViews();

        ivBack.setTag(123);
        ivBack.setOnClickListener(this);
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 1.0f);
        animation.setDuration(4500);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        scanLine.startAnimation(animation);
    }

    private void initData() {
        mTitle = getIntent().getStringExtra(SCAN_TITLE);
        mTitleColor = getIntent().getIntExtra(SCAN_TITLE_COLOR, Color.WHITE);
        mTitleBgColor = getIntent().getIntExtra(SCAN_TITLE_BG_COLOR, Color.BLACK);
        mScanColor = getIntent().getIntExtra(SCAN_DIST_COLOR, Color.parseColor("#65e102"));
        mScanDescribe = getIntent().getStringExtra(SCAN_DESCRIBE);
        mScanDescColor = getIntent().getIntExtra(SCAN_DESCRIBE_COLOR, Color.WHITE);
        mNeedBeep = getIntent().getBooleanExtra(SCAN_NEED_BEEP, false);
    }

    private void initViews() {
        mTitleBarView = (RelativeLayout) findViewById(R.id.scan_title_bar);
        mTitleView = (TextView) findViewById(R.id.scan_title);
        scanPreview = (SurfaceView) findViewById(R.id.capture_preview);
        scanContainer = (RelativeLayout) findViewById(R.id.capture_container);
        scanCropView = (RelativeLayout) findViewById(R.id.capture_crop_view);
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);
        ivBack = (ImageView) findViewById(R.id.iv_back);
        mScanDescribeView = (TextView) findViewById(R.id.capture_mask_bottom);

        if(!TextUtils.isEmpty(mTitle)){
            mTitleView.setText(mTitle);
        }
        if(!TextUtils.isEmpty(mScanDescribe)){
            mScanDescribeView.setText(mScanDescribe);
        }
        mTitleView.setTextColor(mTitleColor);
        mScanDescribeView.setTextColor(mScanDescColor);
        mTitleBarView.setBackgroundColor(mTitleBgColor);

        scanCropView.setBackground(getTintDrawable(R.drawable.zxing_code_bg, mScanColor));
        scanLine.setBackground(getTintDrawable(R.drawable.zxing_scan_line, mScanColor));


    }

    private Drawable getTintDrawable(int resId, int color){
        Drawable original = ContextCompat.getDrawable(this, resId);
        Drawable wrappedDrawable = DrawableCompat.wrap(original);
        DrawableCompat.setTint(wrappedDrawable, color);
        return wrappedDrawable;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());
        handler = null;
        if (isHasSurface) {
            initCamera(scanPreview.getHolder());
        } else {
            scanPreview.getHolder().addCallback(this);
        }

        inactivityTimer.onResume();

    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    /**
     * 扫描设备二维码成功
     *
     * @param rawResult
     * @param bundle
     */
    private void scanDeviceSuccess(String rawResult, Bundle bundle) {
        Intent resultIntent = new Intent();
        bundle.putString("result", rawResult);
        resultIntent.putExtras(bundle);
        this.setResult(RESULT_OK, resultIntent);
        CaptureActivity.this.finish();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG,
                    "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager, DecodeThread.ALL_MODE);
            }

            initCrop();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        // camera error
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(getString(R.string.zxing_prompt));
//        builder.setMessage(getString(R.string.zxing_camera_error));
//        builder.setPositiveButton(getString(R.string.zxing_confirm), new DialogInterface.OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                finish();
//            }
//
//        });
//        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
//
//            @Override
//            public void onCancel(DialogInterface dialog) {
//                finish();
//            }
//        });
//        builder.show();
        Toast.makeText(this, "相机初始化异常", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.zxing_restart_preview, delayMS);
        }
    }

    @Override
    public Rect getCropRect() {
        return mCropRect;
    }

    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     *
     * @param rawResult The contents of the barcode.
     * @param bundle    The extras
     */
    @Override
    public void handleDecode(Result rawResult, Bundle bundle) {
        inactivityTimer.onActivity();
        if(mNeedBeep){
            beepManager.playBeepSoundAndVibrate();
        }
        Intent resultIntent = new Intent();
        bundle.putInt("width", mCropRect.width());
        bundle.putInt("height", mCropRect.height());
        bundle.putString("result", rawResult.getText());
        resultIntent.putExtras(bundle);
        this.setResult(RESULT_OK, resultIntent);
        CaptureActivity.this.finish();

    }

    /**
     * 初始化截取的矩形区域
     */
    private void initCrop() {
        int cameraWidth = cameraManager.getCameraResolution().y;
        int cameraHeight = cameraManager.getCameraResolution().x;

        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        /** 获取布局容器的宽高 */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = cropLeft * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = cropTop * cameraHeight / containerHeight;

        /** 计算最终截取的矩形的宽度 */
        int width = cropWidth * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的高度 */
        int height = cropHeight * cameraHeight / containerHeight;

        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(x, y, width + x, height + y);
    }

    private int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void onClick(View v) {
        switch (Integer.parseInt(v.getTag().toString())) {
            case 123:
                Intent resultIntent = new Intent();
                this.setResult(RESULT_CANCELED, resultIntent);
                CaptureActivity.this.finish();
                break;
        }
    }


}