package com.yunzhou.qrcodeutils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.Result;
import com.yunzhou.qrcodelib.zxing.QRCodeManager;
import com.yunzhou.qrcodelib.zxing.activity.CaptureActivity;
import com.yunzhou.qrcodelib.zxing.decode.DecodeBitmap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView mQrResultView;
    private ImageView mQrCodeView;
    private TextView mScanResultView;

    private Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mQrResultView = (TextView) findViewById(R.id.qr_code_result);
        mQrCodeView = (ImageView) findViewById(R.id.img_qrcode);
        mScanResultView = (TextView) findViewById(R.id.scan_qr_code_result);

        findViewById(R.id.start_qr_scan).setOnClickListener(this);
        findViewById(R.id.create).setOnClickListener(this);
        findViewById(R.id.create_logo).setOnClickListener(this);
        findViewById(R.id.scan_qr_code).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.start_qr_scan:
                scan4QRCode();
                break;
            case R.id.create:
                createQrCode();
                break;
            case R.id.create_logo:
                createQrCodeWithLogo();
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

    private void createQrCodeWithLogo() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLUE);
        canvas.drawCircle(50, 50, 50, paint);

        mBitmap = QRCodeManager.getInstance().createQRCode("二维码内容", 300, 300, bitmap);
        mQrCodeView.setImageBitmap(mBitmap);
    }

    private void createQrCode() {
        mBitmap = QRCodeManager.getInstance().createQRCode("二维码内容", 300, 300);
        mQrCodeView.setImageBitmap(mBitmap);
    }

    private void scan4QRCode() {
        //打开扫描界面扫描条形码或二维码
        Intent openCameraIntent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(openCameraIntent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString("result");
            mQrResultView.setText(scanResult);
        }
    }
}
