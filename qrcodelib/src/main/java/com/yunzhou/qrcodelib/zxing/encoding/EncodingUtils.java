package com.yunzhou.qrcodelib.zxing.encoding;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.yunzhou.qrcodelib.zxing.utils.BitmapFlex;

import java.util.HashMap;
import java.util.Map;

/**
 * 二维码生成工具类
 */
public class EncodingUtils {

    //允许logo最大是100x100
    private static final int LOGO_MAX_SIZE = 100;

    /**
     * 创建二维码
     *
     * @param content   content
     * @param widthPix  widthPix
     * @param heightPix heightPix
     * @param format    二维码格式
     * @param logoBm    logoBm
     * @return 二维码
     */
    public static Bitmap create2DCode(String content, int widthPix, int heightPix, BarcodeFormat format,
                                      Bitmap logoBm) {
        try {
            if (content == null || "".equals(content)) {
                return null;
            }
            // 配置参数
            Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            // 容错级别
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            // 设置二维码边距
            hints.put(EncodeHintType.MARGIN, 0);
            // 图像数据转换，使用了矩阵转换
            BitMatrix bitMatrix = new MultiFormatWriter().encode(content, format, widthPix,
                    heightPix, hints);

            int bitWidth = bitMatrix.getWidth();
            int bitHeight = bitMatrix.getHeight();
            int[] pixels = new int[bitWidth * bitHeight];
            // 下面这里按照二维码的算法，逐个生成二维码的图片，
            // 两个for循环是图片横列扫描的结果
            for (int y = 0; y < bitHeight; y++) {
                for (int x = 0; x < bitWidth; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * bitWidth + x] = 0xff000000;
                    } else {
                        pixels[y * bitWidth + x] = 0xffffffff;
                    }
                }
            }
            // 生成二维码图片的格式，使用ARGB_8888
            Bitmap bitmap = Bitmap.createBitmap(bitWidth, bitHeight, Bitmap.Config.RGB_565);
            bitmap.setPixels(pixels, 0, bitWidth, 0, 0, bitWidth, bitHeight);

            // 因为bitmap可能并不等于预先设置的width和height，需要进行等比缩放，
            // 尤其是BarcodeFormat.DATA_MATRIX格式，小的不可想象
            if(bitWidth != widthPix){
                float wMultiple = ((float) bitWidth) / (float) widthPix;//生成的bitmap的宽除以预期的宽
                float hMultiple = ((float) bitHeight) / (float) heightPix;//生成的bitmap的高除以预期的高

                if (wMultiple > hMultiple) {//说明宽超出范围更多，以宽的比例为标准进行缩放。
                    int dstWidth = widthPix;// bitWidth / wMultiple
                    int dstHeight = (int) (bitHeight / wMultiple);

                    bitmap = BitmapFlex.flex(bitmap, dstWidth, dstHeight);//等间采样算法进行缩放
                } else {//说明相当或高超出范围更多，以高的比例为标准进行缩放。
                    int dstHeight = heightPix;// bitHeight / hMultiple
                    int dstWidth = (int) (bitWidth / hMultiple);

                    bitmap = BitmapFlex.flex(bitmap, dstWidth, dstHeight);//等间采样算法进行缩放
                }
            }
            // 是否添加logo
            if (logoBm != null) {
                bitmap = addLogo(bitmap, logoBm);
            }
            //必须使用compress方法将bitmap保存到文件中再进行读取。直接返回的bitmap是没有任何压缩的，内存消耗巨大！
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 在二维码中间添加Logo图案
     */
    private static Bitmap addLogo(Bitmap src, Bitmap logo) {
        if (src == null) {
            return null;
        }
        if (logo == null) {
            return src;
        }
        //获取图片的宽高
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();
        if (srcWidth == 0 || srcHeight == 0) {
            return null;
        }
        if (logoWidth == 0 || logoHeight == 0) {
            return src;
        }
        //logo大小为二维码整体大小的1/5
        float scaleFactor = srcWidth * 1.0f / 5 / logoWidth;
        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(src, 0, 0, null);
            canvas.scale(scaleFactor, scaleFactor, srcWidth / 2, srcHeight / 2);
            canvas.drawBitmap(logo, (srcWidth - logoWidth) / 2, (srcHeight - logoHeight) / 2, null);
            canvas.save(Canvas.ALL_SAVE_FLAG);
            canvas.restore();
        } catch (Exception e) {
            bitmap = null;
            e.getStackTrace();
        }
        return bitmap;
    }

    /**
     * 创建QR二维码
     *
     * @param content   content
     * @param widthPix  widthPix
     * @param heightPix heightPix
     * @return 二维码
     */
    public static Bitmap createQRCode(String content, int widthPix, int heightPix){
        return create2DCode(content, widthPix, heightPix, BarcodeFormat.QR_CODE, null);
    }

    public static Bitmap createQRCode(Context context, String content, int widthPix, int heightPix, int resId){
        Bitmap logoBitmap = getScaleBitmap(context, resId);
        return create2DCode(content, widthPix, heightPix, BarcodeFormat.QR_CODE, logoBitmap);
    }

    /**
     * 创建DataMatrix生成的二维码
     * @param content
     * @param widthPix
     * @param heightPix
     * @return
     */
    public static Bitmap createDataMatrix(String content, int widthPix, int heightPix){
        return create2DCode(content, widthPix, heightPix, BarcodeFormat.DATA_MATRIX, null);
    }

    public static Bitmap createDataMatrix(Context context, String content, int widthPix, int heightPix, int resId){
        Bitmap logoBitmap = getScaleBitmap(context, resId);
        return create2DCode(content, widthPix, heightPix, BarcodeFormat.DATA_MATRIX, logoBitmap);
    }


    /**
     * 根据资源id获取logo图片，并根据需求压缩
     * @param context
     * @param resId
     * @return
     */
    private static Bitmap getScaleBitmap(Context context, int resId){
        Bitmap logoBitmap = null;
        /**
         * 避免图片过大导致过高的内存消耗，这边对大图进行了压缩操作
         */
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(context.getResources(), resId, opts);

            int inSampleSize = 1;
            if(opts.outWidth > LOGO_MAX_SIZE || opts.outHeight > LOGO_MAX_SIZE){
                if(opts.outWidth > opts.outHeight){
                    inSampleSize = Math.round(opts.outHeight / LOGO_MAX_SIZE);
                }else{
                    inSampleSize = Math.round(opts.outWidth / LOGO_MAX_SIZE);
                }
            }
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = inSampleSize;
            logoBitmap = BitmapFactory.decodeResource(context.getResources(), resId, opts);
        }catch (Exception e){
            e.printStackTrace();
            logoBitmap = null;
        }
        return logoBitmap;
    }

}
