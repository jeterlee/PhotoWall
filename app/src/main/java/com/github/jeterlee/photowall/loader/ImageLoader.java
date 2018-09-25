package com.github.jeterlee.photowall.loader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * <pre>
 * Title: ImageLoader
 * Description: 对图片进行管理的工具类。
 * </pre>
 *
 * @author <a href="https://www.github.com/jeterlee"></a>
 * @date 2018/9/18 0018
 */

public class ImageLoader {

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth) {
        // 源图片的宽度
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (width > reqWidth) {
            // 计算出实际宽度和目标宽度的比率
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = widthRatio;
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(String pathName, int reqWidth) {
        // 第一次解析将inJustDecodeBounds设置为true（图片不加载到内存中，仅获取图片的信息），来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);
        // 调用上面定义的方法计算inSampleSize值
        options.inSampleSize = calculateInSampleSize(options, reqWidth);
        // 使用获取到的inSampleSize值再次解析图片（正式加载图片）
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }
}
