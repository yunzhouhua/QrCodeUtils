# QrCodeUtils
基于ZXing3.3.0封装的用于二维码扫描/生成的工具库

1. 扫描二维码
2. 生成二维码
3. 识别图中二维码


![image](https://github.com/yunzhouhua/QrCodeUtils/blob/master/README/imgs/%E4%BA%8C%E7%BB%B4%E7%A0%81.gif)


### 一.扫描二维码

1.默认方式

库中默认实现了一个二维码扫描ui-CaptureActivity

```java
//1.启动CaptureActivity，进入二维码扫描页面
Intent openCameraIntent = new Intent(MainActivity.this, CaptureActivity.class);
//requestCode根据自己喜好来
startActivityForResult(openCameraIntent, 0);

//2.onActivityResult中接收数据
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  super.onActivityResult(requestCode, resultCode, data);
  if (resultCode == RESULT_OK && requestCode == 0) {
    Bundle bundle = data.getExtras();
    String scanResult = bundle.getString("result");
    Log.e(TAG, "二维码内容：" + scanResult);
    //二维码图片
    byte[] byteResult = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
    Bitmap bitmap = BitmapFactory.decodeByteArray(byteResult, 0, byteResult.length);
    imageView.setImageBitmap(bitmap);
  }
}
```



2.局部自定义

对于二维码扫描页面，提供了一些自定义的配置项，在启动Activity的时候通过bundle向Intent传入对应的数据

| Key                 | Type    | Describe                         |
| ------------------- | ------- | -------------------------------- |
| SCAN_TITLE          | String  | 页面标题，默认 二维码/条码                   |
| SCAN_TITLE_COLOR    | int     | 标题字体颜色，默认 Color.WHITE            |
| SCAN_TITLE_BG_COLOR | int     | 标题栏背景色，默认 Color.BLACK            |
| SCAN_DIST_COLOR     | int     | 扫描区域颜色，边框/扫描条。默认 #65e102         |
| SCAN_DESCRIBE       | String  | 扫描区域下方描述字符，默认 将二维码/条码放入框内，即可自动扫描 |
| SCAN_DESCRIBE_COLOR | int     | 扫描区域下方描述字符字体颜色，默认 Color.WHITE    |
| SCAN_NEED_BEEP      | boolean | 扫描成功是否需要蜂鸣器，默认 false             |



3.完全自定义

如果库中实现的CaptureActivity无法满足需求，可以自定义扫码页面，步骤为以下几步：

(1)创建自定义的Activity，加载我们需要的布局文件，布局中必须有SurfaceView。

(2)Activity实现SurfaceHolder.Callback，让相机预览显示在SurfaceView中

(3)Activity实现IScanQRCode,IScanQRCode中的四个方法，参照CaptureActivity对应的实现即可

### 二.QR格式二维码生成

1.生成普通二维码

```java
/*
 * 生成一个300*300内容为“二维码内容”的QR二维码
 */
Bitmap mBitmap = EncodingUtils.createQRCode("二维码内容", 300, 300);
```

2.生成带Logo的二维码

```java
//Bitmap形式生成
Bitmap mBitmap = EncodingUtils.createQRCode("二维码内容", 300, 300, bitmap);

//资源Id形式生成
Bitmap mBitmap = EncodingUtils.createQRCode(this, "二维码内容", 300, 300, R.mipmap.ic_launcher);
```



### 三.DataMatrix格式二维码生成

1.生成普通二维码

```java
/*
 * 生成一个300*300内容为“二维码内容”的DataMatrix二维码
 */
Bitmap mBitmap = EncodingUtils.createDataMatrix("二维码内容", 300, 300);
```

2.生成带Logo的二维码

```java
//Bitmap形式生成
Bitmap mBitmap = EncodingUtils.createDataMatrix("二维码内容", 300, 300, bitmap);

//资源Id形式生成
Bitmap mBitmap = EncodingUtils.createDataMatrix(this, "二维码内容", 300, 300, R.mipmap.ic_launcher);
```



### 三.识别图中二维码

```java
/*
* mBitmap是我们需要识别的图片资源，如果需要识别文件，需要将文件内容读取到Bitmap对象再进一步操作
*/
Result result = DecodeBitmap.scanningImage(mBitmap);
String codeResult = DecodeBitmap.parseReuslt(result.toString());
```
