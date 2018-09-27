package com.github.jeterlee.photowall.cache;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <pre>
 * Title: DiskCacheHelper
 * Description: 磁盘缓存管理，使用DisLruCache算法。
 * </pre>
 *
 * @author <a href="https://www.github.com/jeterlee"></a>
 * @date 2018/9/20 0020
 */

public class DiskCacheHelper implements ICache {
    private static final String TAG = "DiskCacheHelper";
    private static final String TAG_CACHE = "@createTime{createTime_v}expireMills{expireMills_v}@";
    private static final String REGEX = "@createTime\\{(\\d{1,})\\}expireMills\\{((-)?\\d{1,})\\}@";
    /**
     * 默认磁盘缓存目录
     */
    private static final String DISK_CACHE_DIR = "disk_cache";
    /**
     * 最小的磁盘缓存大小：5MB
     */
    public static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024;
    /**
     * 最大的磁盘缓存大小：20MB
     */
    public static final int MAX_DISK_CACHE_SIZE = 20 * 1024 * 1024;
    /**
     * 永久不过期
     */
    public static final long CACHE_NEVER_EXPIRE = -1;
    private DiskLruCache cache;
    private Pattern compile;
    private long cacheTime = CACHE_NEVER_EXPIRE;

    public DiskCacheHelper(Context context) {
        this(context, getDiskCacheDir(context, DISK_CACHE_DIR),
                calculateDiskCacheSize(getDiskCacheDir(context, DISK_CACHE_DIR)));
    }

    public DiskCacheHelper(Context context, long diskMaxSize) {
        this(context, getDiskCacheDir(context, DISK_CACHE_DIR), diskMaxSize);
    }

    public DiskCacheHelper(Context context, String dirName) {
        this(context, getDiskCacheDir(context, dirName),
                calculateDiskCacheSize(getDiskCacheDir(context, DISK_CACHE_DIR)));
    }

    public DiskCacheHelper(Context context, File diskDir, long diskMaxSize) {
        compile = Pattern.compile(REGEX);
        try {
            cache = DiskLruCache.open(diskDir, getAppVersionCode(context), 1, diskMaxSize);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    public DiskCacheHelper setCacheTime(long cacheTime) {
        this.cacheTime = cacheTime;
        return this;
    }

    public synchronized void put(@NonNull String key, String value) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
            return;
        }

        String name = getMd5Key(key);
        try {
            if (!TextUtils.isEmpty(get(name))) {
                cache.remove(name);
            }
            DiskLruCache.Editor editor = cache.edit(name);
            StringBuilder content = new StringBuilder(value);
            content.append(TAG_CACHE.replace("createTime_v", "" +
                    Calendar.getInstance().getTimeInMillis())
                    .replace("expireMills_v", "" + cacheTime));
            editor.set(0, content.toString());
            editor.commit();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void put(@NonNull String key, Object value) {
        if (value instanceof Bitmap) {
            put(key, convertIconToString((Bitmap) value));
        }
    }

    /**
     * 图片转成string
     *
     * @param bitmap
     * @return
     */
    public String convertIconToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();// outputstream
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bytes = baos.toByteArray();// 转为byte数组
        return Base64.encodeToString(bytes, Base64.DEFAULT);

    }


    @Override
    public String get(@NonNull String key) {
        try {
            String md5Key = getMd5Key(key);
            DiskLruCache.Snapshot snapshot = cache.get(md5Key);
            if (snapshot != null) {
                String content = snapshot.getString(0);
                if (!TextUtils.isEmpty(content)) {
                    Matcher matcher = compile.matcher(content);
                    long createTime = 0;
                    long expireMills = 0;
                    while (matcher.find()) {
                        createTime = Long.parseLong(matcher.group(1));
                        expireMills = Long.parseLong(matcher.group(2));
                    }
                    int index = content.indexOf("@createTime");

                    if ((createTime + expireMills > Calendar.getInstance().getTimeInMillis())
                            || expireMills == CACHE_NEVER_EXPIRE) {
                        return content.substring(0, index);
                    } else {
                        cache.remove(md5Key);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
        return null;
    }

    public Bitmap getBitmap(@NonNull String key) {
        return convertStringToIcon(get(key));
    }

    /**
     * string转成bitmap
     *
     * @param str
     */
    public Bitmap convertStringToIcon(String str) {
        // OutputStream out;
        Bitmap bitmap = null;
        try {
            // out = new FileOutputStream("/sdcard/aa.jpg");
            byte[] bytes = Base64.decode(str, Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            // bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean remove(@NonNull String key) {
        try {
            return cache.remove(getMd5Key(key));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
        return false;
    }

    @Override
    public boolean contains(@NonNull String key) {
        try {
            DiskLruCache.Snapshot snapshot = cache.get(getMd5Key(key));
            return snapshot != null;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
        return false;
    }

    public long size() {
        return cache.size();
    }

    @Override
    public void clear() {
        try {
            cache.delete();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    public DiskLruCache.Editor Editor(@NonNull String key) {
        try {
            return cache.edit(getMd5Key(key));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DiskLruCache.Snapshot Snapshot(@NonNull String key) {
        try {
            return cache.get(getMd5Key(key));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将缓存记录同步到journal文件中
     */
    public void flushCache() {
        try {
            cache.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    public void deleteCacheFile(Context context, String dirName) {
        try {
            DiskLruCache.deleteContents(packDiskCacheDir(context, dirName));
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    public void close() {
        try {
            cache.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    public String getMd5Key(@NonNull String key) {
        return hashKeyForDisk(key);
    }

    public static long calculateDiskCacheSize(File dir) {
        long size = 0;
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long available = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
            size = available / 50;
        } catch (IllegalArgumentException ignored) {
            Log.e(TAG, ignored.getMessage());
        }
        return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
    }

    public static File getDiskCacheDir(Context context, String dirName) {
        File cacheDir = packDiskCacheDir(context, dirName);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDir;
    }

    @NonNull
    public static File packDiskCacheDir(Context context, String dirName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = Objects.requireNonNull(context.getExternalCacheDir()).getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + dirName);
    }

    private int getAppVersionCode(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    private String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = HexUtil.encodeHexStr(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    // private String bytesToHexString(byte[] bytes) {
    //     StringBuilder sb = new StringBuilder();
    //     for (int i : bytes) {
    //         String hex = Integer.toHexString(0xFF & bytes[i]);
    //         if (hex.length() == 1) {
    //             sb.append('0');
    //         }
    //         sb.append(hex);
    //     }
    //     return sb.toString();
    // }
}
