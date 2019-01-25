package com.yunzhou.qrcodeutils;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.yunzhou.qrcodelib.zxing.activity.CaptureActivity;
import com.yunzhou.qrcodelib.zxing.decode.DecodeBitmap;
import com.yunzhou.qrcodelib.zxing.decode.DecodeThread;
import com.yunzhou.qrcodelib.zxing.encoding.EncodingUtils;
import com.yunzhou.qrcodelib.zxing.utils.IsChineseOrNot;

import java.io.UnsupportedEncodingException;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "MainActivity";

    private TextView mQrResultView;
    private ImageView mQrCodeView;
    private TextView mScanResultView;
    private EditText mQrEditView;

    private Bitmap mBitmap;

    RxPermissions rxPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rxPermissions = new RxPermissions(this);

        mQrResultView = findViewById(R.id.qr_code_result);
        mQrCodeView = findViewById(R.id.img_qrcode);
        mScanResultView = findViewById(R.id.scan_qr_code_result);
        mQrEditView = findViewById(R.id.qr_code_text);

        findViewById(R.id.start_qr_scan).setOnClickListener(this);
        findViewById(R.id.create).setOnClickListener(this);
        findViewById(R.id.create_logo).setOnClickListener(this);
        findViewById(R.id.dm_create).setOnClickListener(this);
        findViewById(R.id.dm_create_logo).setOnClickListener(this);
        findViewById(R.id.scan_qr_code).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.start_qr_scan:
                scan4QRCode();
                break;
            case R.id.create:
                createQrCode(1);
                break;
            case R.id.create_logo:
                createQrCodeWithLogo(1);
                break;
            case R.id.dm_create:
                createQrCode(2);
                break;
            case R.id.dm_create_logo:
                createQrCodeWithLogo(2);
                break;
            case R.id.scan_qr_code:
                if(mBitmap != null) {
                    Result result = DecodeBitmap.scanningImage(mBitmap);
                    String codeResult = DecodeBitmap.parseReuslt(result.toString());
                    mScanResultView.setText("二维码扫描结果:" + codeResult);
                }
                break;
        }
    }

    private void createQrCodeWithLogo(int flag) {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLUE);
        canvas.drawCircle(50, 50, 50, paint);

        //mBitmap = QRCodeManager.getInstance().createQRCode("二维码内容", 300, 300, bitmap);
        if(flag == 1) {
            mBitmap = EncodingUtils.createQRCode(this, mQrEditView.getText().toString(), 300, 300, R.mipmap.ic_launcher);
        }else if(flag == 2){
            mBitmap = EncodingUtils.createDataMatrix(this, mQrEditView.getText().toString(), 300, 300, R.mipmap.ic_launcher);
        }
        mQrCodeView.setImageBitmap(mBitmap);
    }

    private void createQrCode(int flag) {
        if(flag == 1) {
            mBitmap = EncodingUtils.createQRCode(mQrEditView.getText().toString(), 300, 300);
        }else if(flag == 2){
            mBitmap = EncodingUtils.createDataMatrix(mQrEditView.getText().toString(), 300, 300);
        }
        mQrCodeView.setImageBitmap(mBitmap);
    }

    private void scan4QRCode() {
        rxPermissions
                .request(Manifest.permission.CAMERA)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if(aBoolean){
                            //打开扫描界面扫描条形码或二维码
                            Intent openCameraIntent = new Intent(MainActivity.this, CaptureActivity.class);
                            startActivityForResult(openCameraIntent, 0);
                        }else{
                            Toast.makeText(MainActivity.this, "请打开相机权限", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString("result");
            try {
                Log.e(TAG, "utf-8 : " + new String(scanResult.getBytes("ISO-8859-1"), "utf-8"));
                Log.e(TAG, "GB2312: " + new String(scanResult.getBytes("ISO-8859-1"), "GB2312"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            // 展示文字
            String GB_Str = "";
            boolean is_cN = false;
            String result = null;
            try {
                result = new String(scanResult.getBytes("ISO-8859-1"), "UTF-8");
                is_cN = IsChineseOrNot.isChineseCharacter(result);
                //防止有人特意使用乱码来生成二维码来判断的情况
                boolean b = IsChineseOrNot.isSpecialCharacter(scanResult);
                if (b) {
                    is_cN = true;
                }
//                            System.out.println("是为:"+is_cN);
                if (!is_cN) {
                    result = new String(scanResult.getBytes("ISO-8859-1"), "GB2312");
//                                System.out.println("这是转了GB2312的"+GB_Str);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            mQrResultView.setText(result);

            // 展示图片
            byte[] byteResult = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
            Bitmap bitmap = BitmapFactory.decodeByteArray(byteResult, 0, byteResult.length);
            mQrCodeView.setImageBitmap(bitmap);
        }
    }
}
