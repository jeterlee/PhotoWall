package com.github.jeterlee.photowall.cache;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * <pre>
 * Title: DiskCacheManager
 * Description: 磁盘缓存管理，使用DisLruCache算法。
 * </pre>
 *
 * @author <a href="https://www.github.com/jeterlee"></a>
 * @date 2018/9/26 0026
 */

public class DiskCacheManager implements ICacheManager {
    private static final String TAG = "DiskCacheManager";
    /**
     * 默认磁盘缓存目录
     */
    private static final String DISK_CACHE_DIR = "disk_cache";
    /**
     * 最小的磁盘缓存大小：5MB
     */
    private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024;
    /**
     * 最大的磁盘缓存大小：20MB
     */
    private static final int MAX_DISK_CACHE_SIZE = 20 * 1024 * 1024;
    private static final int DEFAULT_APP_VERSION = 1;
    /**
     * The default valueCount when open DiskLruCache.
     */
    private static final int DEFAULT_VALUE_COUNT = 1;
    private DiskLruCache mDiskLruCache;

    public DiskCacheManager(Context context) throws IOException {
        mDiskLruCache = generateCache(context, DISK_CACHE_DIR,
                Utils.calculateDiskCacheSize(Utils.getDiskCacheDir(context, DISK_CACHE_DIR)));
    }

    public DiskCacheManager(Context context, long maxCount) throws IOException {
        mDiskLruCache = generateCache(context, DISK_CACHE_DIR, maxCount);
    }

    public DiskCacheManager(Context context, String dirName) throws IOException {
        mDiskLruCache = generateCache(context, dirName,
                Utils.calculateDiskCacheSize(Utils.getDiskCacheDir(context, DISK_CACHE_DIR)));
    }

    public DiskCacheManager(Context context, String dirName, long maxCount) throws IOException {
        mDiskLruCache = generateCache(context, dirName, maxCount);
    }

    /**
     * custom cache dir
     */
    public DiskCacheManager(Context context, File dir) throws IOException {
        mDiskLruCache = generateCache(context, dir,
                Utils.calculateDiskCacheSize(Utils.getDiskCacheDir(context, DISK_CACHE_DIR)));
    }

    public DiskCacheManager(Context context, File dir, long maxCount) throws IOException {
        mDiskLruCache = generateCache(context, dir, maxCount);
    }

    private DiskLruCache generateCache(Context context, File dir, long maxCount) throws IOException {
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException(dir + " is not a directory or does not exists. ");
        }
        return DiskLruCache.open(dir, Utils.getAppVersionCode(context),
                DEFAULT_VALUE_COUNT, maxCount);
    }

    private DiskLruCache generateCache(Context context, String dirName, long maxCount) throws IOException {
        return DiskLruCache.open(Utils.getDiskCacheDir(context, dirName),
                Utils.getAppVersionCode(context),
                DEFAULT_VALUE_COUNT, maxCount);
    }

    // <editor-fold desc="String 数据 读写">

    @Override
    public void put(String key, String value) {
        DiskLruCache.Editor edit = null;
        BufferedWriter bw = null;
        try {
            edit = editor(key);
            if (edit == null) {
                return;
            }
            OutputStream os = edit.newOutputStream(0);
            bw = new BufferedWriter(new OutputStreamWriter(os));
            bw.write(value);
            edit.commit();// write CLEAN
        } catch (IOException e) {
            e.printStackTrace();
            try {
                // s
                edit.abort();// write REMOVE
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getAsString(String key) {
        InputStream inputStream;
        inputStream = getInputStream(key);
        if (inputStream == null) {
            return null;
        }
        String str = null;
        try {
            str = Utils.readFully(new InputStreamReader(inputStream, Utils.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            try {
                inputStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return str;
    }

    @Override
    public void put(String key, JSONObject jsonObject) {
        put(key, jsonObject.toString());
    }

    @Override
    public JSONObject getAsJSONObject(String key) {
        String val = getAsString(key);
        try {
            if (val != null) {
                return new JSONObject(val);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    // </editor-fold>

    // <editor-fold desc="JSONArray 数据 读写">

    @Override
    public void put(String key, JSONArray jsonArray) {
        put(key, jsonArray.toString());
    }

    @Override
    public JSONArray getAsJSONArray(String key) {
        String jsonString = getAsString(key);
        try {
            return new JSONArray(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // </editor-fold>

    // <editor-fold desc="byte 数据 读写">

    /**
     * 保存 byte数据 到 缓存中
     *
     * @param key   保存的key
     * @param value 保存的数据
     */
    @Override
    public void put(String key, byte[] value) {
        OutputStream out = null;
        DiskLruCache.Editor editor = null;
        try {
            editor = editor(key);
            if (editor == null) {
                return;
            }
            out = editor.newOutputStream(0);
            out.write(value);
            out.flush();
            editor.commit();// write CLEAN
        } catch (Exception e) {
            e.printStackTrace();
            try {
                editor.abort();// write REMOVE
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public byte[] getAsBinary(String key) {
        byte[] res = null;
        InputStream is = getInputStream(key);
        if (is == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[256];
            int len = 0;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            res = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    // </editor-fold>

    // <editor-fold desc="序列化 数据 读写">

    @Override
    public <Serializable> void put(String key, Serializable value) {
        DiskLruCache.Editor editor = editor(key);
        ObjectOutputStream oos = null;
        if (editor == null) {
            return;
        }
        try {
            OutputStream os = editor.newOutputStream(0);
            oos = new ObjectOutputStream(os);
            oos.writeObject(value);
            oos.flush();
            editor.commit();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                editor.abort();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public <T> T get(String key) {
        T t = null;
        InputStream is = getInputStream(key);
        ObjectInputStream ois = null;
        if (is == null) {
            return null;
        }
        try {
            ois = new ObjectInputStream(is);
            // noinspection unchecked
            t = (T) ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return t;
    }

    // </editor-fold>

    // <editor-fold desc="bitmap 数据 读写">

    @Override
    public void put(String key, Bitmap bitmap) {
        put(key, Utils.bitmapToBytes(bitmap));
    }

    @Override
    public Bitmap getAsBitmap(String key) {
        byte[] bytes = getAsBinary(key);
        if (bytes == null) {
            return null;
        }
        return Utils.bytesToBitmap(bytes);
    }

    // </editor-fold>

    // <editor-fold desc="drawable 数据 读写">

    @Override
    public void put(String key, Drawable value) {
        put(key, Utils.drawableToBitmap(value));
    }

    @Override
    public Drawable getAsDrawable(String key) {
        byte[] bytes = getAsBinary(key);
        if (bytes == null) {
            return null;
        }
        return Utils.bitmapToDrawable(Utils.bytesToBitmap(bytes));
    }

    // </editor-fold>

    // <editor-fold desc="other methods">

    @Override
    public boolean contains(@NonNull String key) {
        try {
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(Utils.hashKeyForDisk(key));
            return snapshot != null;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
        return false;
    }

    @Override
    public boolean remove(String key) {
        try {
            return mDiskLruCache.remove(Utils.hashKeyForDisk(key));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void clear() {
        try {
            mDiskLruCache.delete();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    public void deleteCacheFile(Context context, String dirName) {
        try {
            DiskLruCache.deleteContents(Utils.packDiskCacheDir(context, dirName));
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    public void close() throws IOException {
        try {
            mDiskLruCache.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将缓存记录同步到journal文件中
     */
    public void flush() {
        try {
            mDiskLruCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    public DiskLruCache.Editor getEditor(String key) {
        try {
            return mDiskLruCache.edit(Utils.hashKeyForDisk(key));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DiskLruCache.Snapshot getSnapshot(@NonNull String key) {
        try {
            return mDiskLruCache.get(Utils.hashKeyForDisk(key));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isClosed() {
        return mDiskLruCache.isClosed();
    }

    public long size() {
        return mDiskLruCache.size();
    }

    public long getMaxSize() {
        return mDiskLruCache.maxSize();
    }

    public File getDirectory() {
        return mDiskLruCache.getDirectory();
    }

    // </editor-fold>

    // <editor-fold desc="遇到文件比较大的，可以直接通过流读写">

    /**
     * basic editor
     */
    public DiskLruCache.Editor editor(String key) {
        try {
            key = Utils.hashKeyForDisk(key);
            // write DIRTY
            DiskLruCache.Editor edit = mDiskLruCache.edit(key);
            // edit maybe null :the entry is editing
            if (edit == null) {
                Log.w(TAG, "the entry specified key:" + key + " is editing by other . ");
            }
            return edit;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * basic get
     */
    public InputStream getInputStream(String key) {
        try {
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(Utils.hashKeyForDisk(key));
            // not find entry , or entry.readable = false
            if (snapshot == null) {
                Log.e(TAG, "not find entry , or entry.readable = false");
                return null;
            }
            // write READ
            return snapshot.getInputStream(0);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // </editor-fold>

    private static class Utils {
        private static final Charset US_ASCII = Charset.forName("US-ASCII");
        private static final Charset UTF_8 = Charset.forName("UTF-8");

        @NonNull
        private static String readFully(Reader reader) throws IOException {
            try {
                StringWriter writer = new StringWriter();
                char[] buffer = new char[1024];
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, count);
                }
                return writer.toString();
            } finally {
                reader.close();
            }
        }

        private static long calculateDiskCacheSize(File dir) {
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

        private static File getDiskCacheDir(Context context, String dirName) {
            File cacheDir = packDiskCacheDir(context, dirName);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            return cacheDir;
        }

        @NonNull
        private static File packDiskCacheDir(Context context, String dirName) {
            String cachePath;
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    || !Environment.isExternalStorageRemovable()) {
                cachePath = Objects.requireNonNull(context.getExternalCacheDir()).getPath();
            } else {
                cachePath = context.getCacheDir().getPath();
            }
            return new File(cachePath + File.separator + dirName);
        }

        private static byte[] bitmapToBytes(Bitmap bm) {
            if (bm == null) {
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
            return baos.toByteArray();
        }

        private static Bitmap bytesToBitmap(byte[] b) {
            if (b.length == 0) {
                return null;
            }
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        }

        private static Bitmap drawableToBitmap(Drawable drawable) {
            if (drawable == null) {
                return null;
            }

            int w = drawable.getIntrinsicWidth();
            int h = drawable.getIntrinsicHeight();

            Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                    : Bitmap.Config.RGB_565;

            Bitmap bitmap = Bitmap.createBitmap(w, h, config);

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, w, h);

            drawable.draw(canvas);
            return bitmap;
        }

        @SuppressWarnings("deprecation")
        private static Drawable bitmapToDrawable(Bitmap bm) {
            if (bm == null) {
                return null;
            }
            return new BitmapDrawable(bm);
        }

        private static String hashKeyForDisk(String key) {
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

        private static int getAppVersionCode(Context context) {
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                return info.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return DEFAULT_APP_VERSION;
        }
    }
}



