package com.github.jeterlee.photowall.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <pre>
 * Title: CacheManager
 * Description: 统一的内存缓存管理类，简化了内存缓存与硬盘缓存同时使用时的操作。
 * </pre>
 *
 * @author <a href="https://www.github.com/jeterlee"></a>
 * @date 2018/9/20 0020
 */

public class CacheManager implements ICache {
    private static final String TAG = "CacheManager";

    /**
     * 设置内存缓存的最大值，单位：byte
     */
    private int maxMemoryCacheSize = 0;
    /**
     * 设置硬盘缓存的最大值，单位：byte
     */
    private int maxDiskCacheSize = 0;
    /**
     * 设置自定义的硬盘缓存文件夹名称
     */
    private String diskCacheDirName = "";
    private File diskCacheFileDir;
    private Context context;
    private int cacheMode;
    private MemoryCacheManager memoryCacheManager;
    private DiskCacheManager diskCacheManager;

    private CacheManager() {
    }

    public static CacheManager getInstance() {
        return CacheManagerHolder.instance;
    }

    private static class CacheManagerHolder {
        private static CacheManager instance = new CacheManager();
    }

    public void init(Context context) {
        init(context, CacheMode.ALL_ALLOW);
    }

    public void init(Context context, @CacheMode.Mode int cacheMode) {
        this.context = context;
        this.cacheMode = cacheMode;
        switch (cacheMode) {
            case CacheMode.ALL_ALLOW:
            default:
                initMemoryCacheManager();
                initDiskCacheManager();
                break;
            case CacheMode.ONLY_MEMORY:
                initMemoryCacheManager();
                break;
            case CacheMode.ONLY_DISK:
                initDiskCacheManager();
                break;
        }
    }

    /**
     * 初始化内存缓存管理
     */
    private void initMemoryCacheManager() {
        if (maxMemoryCacheSize > 0) {
            memoryCacheManager = new MemoryCacheManager(maxDiskCacheSize);
        } else {
            memoryCacheManager = new MemoryCacheManager();
        }
    }

    /**
     * 初始化硬盘缓存管理
     */
    private void initDiskCacheManager() {
        try {
            if (maxDiskCacheSize > 0) {
                if (!TextUtils.isEmpty(diskCacheDirName)) {
                    diskCacheManager = new DiskCacheManager(context, diskCacheDirName, maxDiskCacheSize);
                } else if (diskCacheFileDir != null) {
                    diskCacheManager = new DiskCacheManager(context, diskCacheFileDir, maxDiskCacheSize);
                } else {
                    diskCacheManager = new DiskCacheManager(context, maxDiskCacheSize);
                }
            } else if (!TextUtils.isEmpty(diskCacheDirName)) {
                diskCacheManager = new DiskCacheManager(context, diskCacheDirName);
            } else if (diskCacheFileDir != null) {
                diskCacheManager = new DiskCacheManager(context, diskCacheFileDir);
            } else {
                diskCacheManager = new DiskCacheManager(context);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 索引key对应的value写入缓存
     *
     * @param key   缓存索引
     * @param value 缓存内容
     */
    @Override
    public <E> void put(String key, E value) {
        switch (cacheMode) {
            case CacheMode.ALL_ALLOW:
            default:
                if (memoryCacheManager != null && diskCacheManager != null) {
                    // 设置硬盘缓存成功后，再设置内存缓存
                    Log.i(TAG, "put: set disk cache!");
                    if (value instanceof String) {
                        diskCacheManager.put(key, (String) value);
                    } else if (value instanceof JSONObject) {
                        diskCacheManager.put(key, (JSONObject) value);
                    } else if (value instanceof JSONArray) {
                        diskCacheManager.put(key, (JSONArray) value);
                    } else if (value instanceof byte[]) {
                        diskCacheManager.put(key, (byte[]) value);
                    } else if (value instanceof Bitmap) {
                        diskCacheManager.put(key, (Bitmap) value);
                    } else if (value instanceof Drawable) {
                        diskCacheManager.put(key, (Drawable) value);
                    } else {
                        diskCacheManager.put(key, value);
                    }
                    Log.i(TAG, "put: set memory cache!");
                    memoryCacheManager.put(key, value);
                }
                break;
            case CacheMode.ONLY_MEMORY:
                if (memoryCacheManager != null) {
                    Log.i(TAG, "put: set disk cache!");
                    memoryCacheManager.put(key, value);
                }
                break;
            case CacheMode.ONLY_DISK:
                if (diskCacheManager != null) {
                    Log.i(TAG, "put: set memory cache!");
                    diskCacheManager.put(key, value);
                }
                break;
        }
    }

    /**
     * 获取索引key对应的缓存内容
     *
     * @param key 缓存索引key
     * @return key索引对应的缓存数据
     */
    @Override
    public <E> E get(String key) {
        return get(key, CacheType.SERIALIZABLE_TYPE);
    }

    public <E> E get(String key, @CacheType.Types int cacheType) {
        E e = null;
        switch (cacheMode) {
            case CacheMode.ALL_ALLOW:
            default:
                if (memoryCacheManager != null && diskCacheManager != null) {
                    e = memoryCacheManager.get(key);
                    if (e == null) {
                        // 如果硬盘缓存内容存在，内存缓存不存在。则在获取硬盘缓存后，将内容写入内存缓存
                        // 设置硬盘缓存成功后，再设置内存缓存
                        e = getByCacheType(key, cacheType);
                        if (e != null) {
                            Log.i(TAG, "get: use disk cache & put memory cache!");
                            memoryCacheManager.put(key, e);
                        } else {
                            Log.i(TAG, "get: no memory cache & no disk cache!");
                        }
                    } else {
                        Log.i(TAG, "get: use memory cache!");
                    }
                }
                break;
            case CacheMode.ONLY_MEMORY:
                if (memoryCacheManager != null) {
                    e = memoryCacheManager.get(key);
                    if (e != null) {
                        Log.i(TAG, "get: use memory cache!");
                    }
                }
                break;
            case CacheMode.ONLY_DISK:
                if (diskCacheManager != null) {
                    e = getByCacheType(key, cacheType);
                    if (e != null) {
                        Log.i(TAG, "get: use disk cache!");
                    }
                }
                break;
        }
        return e;
    }

    private <E> E getByCacheType(String key, @CacheType.Types int cacheType) {
        E e;
        switch (cacheType) {
            case CacheType.STRING_TYPE:
                // noinspection unchecked
                e = (E) diskCacheManager.getAsString(key);
                break;
            case CacheType.JSON_OBJECT_TYPE:
                // noinspection unchecked
                e = (E) diskCacheManager.getAsJSONObject(key);
                break;
            case CacheType.JSON_ARRAY_TYPE:
                // noinspection unchecked
                e = (E) diskCacheManager.getAsJSONArray(key);
                break;
            case CacheType.BYTES_TYPE:
                // noinspection unchecked
                e = (E) diskCacheManager.getAsBinary(key);
                break;
            case CacheType.BITMAP_TYPE:
                // noinspection unchecked
                e = (E) diskCacheManager.getAsBitmap(key);
                break;
            case CacheType.DRAWABLE_TYPE:
                // noinspection unchecked
                e = (E) diskCacheManager.getAsDrawable(key);
                break;
            case CacheType.SERIALIZABLE_TYPE:
            default:
                // noinspection unchecked
                e = diskCacheManager.get(key);
                break;
        }
        return e;
    }

    /**
     * 判断是否有对应索引key的缓存
     *
     * @param key 缓存索引key
     * @return {@code true}: 存在<br>{@code false}: 不存在
     */
    @Override
    public boolean contains(String key) {
        switch (cacheMode) {
            case CacheMode.ALL_ALLOW:
            default:
                if (memoryCacheManager != null && diskCacheManager != null) {
                    // 硬盘缓存或内存缓存中寻找
                    return memoryCacheManager.contains(key) || diskCacheManager.contains(key);
                }
                break;
            case CacheMode.ONLY_MEMORY:
                if (memoryCacheManager != null) {
                    return memoryCacheManager.contains(key);
                }
                break;
            case CacheMode.ONLY_DISK:
                if (diskCacheManager != null) {
                    return diskCacheManager.contains(key);
                }
                break;
        }
        return false;
    }

    /**
     * 移除一条索引key对应的缓存
     *
     * @param key 索引
     * @return {@code true}: 移除成功<br>{@code false}: 移除失败
     */
    @Override
    public boolean remove(String key) {
        switch (cacheMode) {
            case CacheMode.ALL_ALLOW:
            default:
                if (memoryCacheManager != null && diskCacheManager != null) {
                    return memoryCacheManager.remove(key) && diskCacheManager.remove(key);
                }
                break;
            case CacheMode.ONLY_MEMORY:
                if (memoryCacheManager != null) {
                    return memoryCacheManager.remove(key);
                }
                break;
            case CacheMode.ONLY_DISK:
                if (diskCacheManager != null) {
                    return diskCacheManager.remove(key);
                }
                break;
        }
        return false;
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    public long size() {
        int size = 0;
        switch (cacheMode) {
            case CacheMode.ALL_ALLOW:
            default:
                if (memoryCacheManager != null) {
                    size += memoryCacheManager.size();
                }
                if (diskCacheManager != null) {
                    size += diskCacheManager.size();
                }
                break;
            case CacheMode.ONLY_MEMORY:
                if (memoryCacheManager != null) {
                    size += memoryCacheManager.size();
                }
                break;
            case CacheMode.ONLY_DISK:
                if (diskCacheManager != null) {
                    size += diskCacheManager.size();
                }
                break;
        }
        return size;
    }

    /**
     * 删除所有缓存
     */
    @Override
    public void clear() {
        switch (cacheMode) {
            case CacheMode.ALL_ALLOW:
            default:
                if (memoryCacheManager != null && diskCacheManager != null) {
                    memoryCacheManager.clear();
                    diskCacheManager.clear();
                }
                break;
            case CacheMode.ONLY_MEMORY:
                if (memoryCacheManager != null) {
                    memoryCacheManager.clear();
                }
                break;
            case CacheMode.ONLY_DISK:
                if (diskCacheManager != null) {
                    diskCacheManager.clear();
                }
                break;
        }
    }

    /**
     * 缓存数据同步
     */
    public void flush() {
        switch (cacheMode) {
            case CacheMode.ALL_ALLOW:
            default:
                if (memoryCacheManager != null && diskCacheManager != null) {
                    diskCacheManager.flush();
                }
                break;
            case CacheMode.ONLY_MEMORY:
                break;
            case CacheMode.ONLY_DISK:
                if (diskCacheManager != null) {
                    diskCacheManager.flush();
                }
                break;
        }
    }

    /**
     * 删除特定文件名的缓存文件
     *
     * @param dirName 文件名
     */
    public void deleteCacheFile(String dirName) {
        if (diskCacheManager != null) {
            diskCacheManager.deleteCacheFile(context, dirName);
        }
    }

    public void close() {
        try {
            if (diskCacheManager != null) {
                diskCacheManager.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DiskLruCache.Editor getEditor(@NonNull String key) {
        return diskCacheManager != null ? diskCacheManager.getEditor(key) : null;
    }

    public DiskLruCache.Snapshot getSnapshot(@NonNull String key) {
        return diskCacheManager != null ? diskCacheManager.getSnapshot(key) : null;
    }

    /**
     * 设置内存缓存的最大值
     *
     * @param maxSize 内存缓存最大值，单位：byte
     * @return CacheManager
     */
    public CacheManager setMaxMemoryCacheSize(int maxSize) {
        this.maxMemoryCacheSize = maxSize / 1024;
        return this;
    }

    /**
     * 设置硬盘缓存的最大值
     *
     * @param maxSize 硬盘缓存最大值，单位：byte
     * @return CacheManager
     */
    public CacheManager setMaxDiskCacheSize(int maxSize) {
        this.maxDiskCacheSize = maxSize;
        return this;
    }

    /**
     * 设置硬盘缓存自定义的文件名（setDiskCacheDirName 和 setDiskCacheFileDir 只能调用其一 ）
     *
     * @param dirName 自定义文件名
     * @return CacheManager
     */
    public CacheManager setDiskCacheDirName(String dirName) {
        this.diskCacheDirName = dirName;
        return this;
    }

    /**
     * 设置硬盘缓存自定义的文件（setDiskCacheDirName 和 setDiskCacheFileDir 只能调用其一 ）
     *
     * @param file 自定义文件
     * @return CacheManager
     */
    public CacheManager setDiskCacheFileDir(File file) {
        this.diskCacheFileDir = file;
        return this;
    }

    static class CacheMode {
        /**
         * 只使用内存缓存（LruCache）
         */
        static final int ONLY_MEMORY = 1;
        /**
         * 只使用硬盘缓存（DiskLruCache）
         */
        static final int ONLY_DISK = 2;
        /**
         * 同时使用内存缓存（LruCache）与硬盘缓存（DiskLruCache）
         */
        static final int ALL_ALLOW = 0;

        @IntDef({ALL_ALLOW, ONLY_MEMORY, ONLY_DISK})
        @Retention(RetentionPolicy.SOURCE)
        @interface Mode {
        }
    }

    public static class CacheType {
        public static final int STRING_TYPE = 0;
        public static final int JSON_OBJECT_TYPE = 1;
        public static final int JSON_ARRAY_TYPE = 2;
        public static final int BYTES_TYPE = 3;
        public static final int BITMAP_TYPE = 4;
        public static final int DRAWABLE_TYPE = 5;
        public static final int SERIALIZABLE_TYPE = 6;

        @IntDef({STRING_TYPE, JSON_OBJECT_TYPE, JSON_ARRAY_TYPE,
                BYTES_TYPE, BITMAP_TYPE, DRAWABLE_TYPE, SERIALIZABLE_TYPE})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Types {
        }
    }
}
