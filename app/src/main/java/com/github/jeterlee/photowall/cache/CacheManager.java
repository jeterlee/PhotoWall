package com.github.jeterlee.photowall.cache;

import android.content.Context;
import android.support.annotation.IntDef;
import android.text.TextUtils;

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
    /**
     * 只使用内存缓存（LruCache）
     */
    public static final int ONLY_MEMORY = 1;
    /**
     * 只使用硬盘缓存（DiskLruCache）
     */
    public static final int ONLY_DISK = 2;
    /**
     * 同时使用内存缓存（LruCache）与硬盘缓存（DiskLruCache）
     */
    public static final int ALL_ALLOW = 0;
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
    /**
     * 设置硬盘缓存的有效时间，默认为永久不过期，单位：ms
     */
    private long diskCacheTime = DiskCacheManager.CACHE_NEVER_EXPIRE;
    private Context context;
    private int cacheMode;
    private MemoryCacheManager memoryCacheManager;
    private DiskCacheManager diskCacheManager;

    @IntDef({ALL_ALLOW, ONLY_MEMORY, ONLY_DISK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CacheMode {
    }

    private CacheManager() {
    }

    public static CacheManager getInstance() {
        return CacheManagerHolder.instance;
    }

    private static class CacheManagerHolder {
        private static CacheManager instance = new CacheManager();
    }

    public void init(Context context) {
        init(context, CacheManager.ALL_ALLOW);
    }

    public void init(Context context, @CacheMode int cacheMode) {
        this.context = context;
        this.cacheMode = cacheMode;
        switch (cacheMode) {
            case CacheManager.ALL_ALLOW:
            default:
                initMemoryCacheManager();
                initDiskCacheManager();
                break;
            case CacheManager.ONLY_MEMORY:
                initMemoryCacheManager();
                break;
            case CacheManager.ONLY_DISK:
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
        if (maxDiskCacheSize > 0) {
            if (!TextUtils.isEmpty(diskCacheDirName)) {
                if (diskCacheTime > 0) {
                    diskCacheManager = new DiskCacheManager(context, DiskCacheManager.getDiskCacheDir(context, diskCacheDirName)
                            , maxDiskCacheSize).setCacheTime(diskCacheTime);
                } else {
                    diskCacheManager = new DiskCacheManager(context, DiskCacheManager.getDiskCacheDir(context, diskCacheDirName)
                            , maxDiskCacheSize);
                }
            } else {
                if (diskCacheTime > 0) {
                    diskCacheManager = new DiskCacheManager(context, maxDiskCacheSize)
                            .setCacheTime(diskCacheTime);
                } else {
                    diskCacheManager = new DiskCacheManager(context, maxDiskCacheSize);
                }
            }
        } else if (!TextUtils.isEmpty(diskCacheDirName)) {
            if (diskCacheTime > 0) {
                diskCacheManager = new DiskCacheManager(context, diskCacheDirName)
                        .setCacheTime(diskCacheTime);
            } else {
                diskCacheManager = new DiskCacheManager(context, diskCacheDirName);
            }
        } else {
            if (diskCacheTime > 0) {
                diskCacheManager = new DiskCacheManager(context)
                        .setCacheTime(diskCacheTime);
            } else {
                diskCacheManager = new DiskCacheManager(context);
            }
        }
    }

    /**
     * 索引key对应的value写入缓存
     *
     * @param key   缓存索引
     * @param value 缓存内容
     */
    @Override
    public void put(String key, Object value) {
        switch (cacheMode) {
            case CacheManager.ALL_ALLOW:
            default:
                if (memoryCacheManager != null && diskCacheManager != null) {
                    // 设置硬盘缓存成功后，再设置内存缓存
                    diskCacheManager.put(key, value);
                    memoryCacheManager.put(key, value);
                }
                break;
            case CacheManager.ONLY_MEMORY:
                if (memoryCacheManager != null) {
                    memoryCacheManager.put(key, value);
                }
                break;
            case CacheManager.ONLY_DISK:
                if (diskCacheManager != null) {
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
    public Object get(String key) {
        Object object = null;
        switch (cacheMode) {
            case CacheManager.ALL_ALLOW:
            default:
                if (memoryCacheManager != null && diskCacheManager != null) {
                    object = memoryCacheManager.get(key);
                    if (object == null) {
                        // 如果硬盘缓存内容存在，内存缓存不存在。则在获取硬盘缓存后，将内容写入内存缓存
                        object = diskCacheManager.get(key);
                        memoryCacheManager.put(key, object);
                    }
                }
                break;
            case CacheManager.ONLY_MEMORY:
                if (memoryCacheManager != null) {
                    object = memoryCacheManager.get(key);
                }
                break;
            case CacheManager.ONLY_DISK:
                if (diskCacheManager != null) {
                    object = diskCacheManager.get(key);
                }
                break;
        }
        return object;
    }

    /**
     * 判断是否有对应索引key的缓存
     *
     * @param key 缓存索引key
     * @return {@code true}: 存在<br>{@code false}: 不存在
     */
    @Override
    public boolean contains(String key) {
        boolean isContain = false;
        switch (cacheMode) {
            case CacheManager.ALL_ALLOW:
            default:
                if (memoryCacheManager != null && diskCacheManager != null) {
                    // 硬盘缓存或内存缓存中寻找
                    return memoryCacheManager.contains(key) || diskCacheManager.contains(key);
                }
                break;
            case CacheManager.ONLY_MEMORY:
                if (memoryCacheManager != null) {
                    isContain = memoryCacheManager.contains(key);
                }
                break;
            case CacheManager.ONLY_DISK:
                if (diskCacheManager != null) {
                    isContain = diskCacheManager.contains(key);
                }
                break;
        }
        return isContain;
    }

    /**
     * 移除一条索引key对应的缓存
     *
     * @param key 索引
     */
    @Override
    public void remove(String key) {
        switch (cacheMode) {
            case CacheManager.ALL_ALLOW:
            default:
                if (memoryCacheManager != null && diskCacheManager != null) {
                    memoryCacheManager.remove(key);
                    diskCacheManager.remove(key);
                }
                break;
            case CacheManager.ONLY_MEMORY:
                if (memoryCacheManager != null) {
                    memoryCacheManager.remove(key);
                }
                break;
            case CacheManager.ONLY_DISK:
                if (diskCacheManager != null) {
                    diskCacheManager.remove(key);
                }
                break;
        }
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    @Override
    public long size() {
        int size = 0;
        switch (cacheMode) {
            case CacheManager.ALL_ALLOW:
            default:
                if (memoryCacheManager != null) {
                    size += memoryCacheManager.size();
                }
                if (diskCacheManager != null) {
                    size += diskCacheManager.size();
                }
                break;
            case CacheManager.ONLY_MEMORY:
                if (memoryCacheManager != null) {
                    size += memoryCacheManager.size();
                }
                break;
            case CacheManager.ONLY_DISK:
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
            case CacheManager.ALL_ALLOW:
            default:
                if (memoryCacheManager != null && diskCacheManager != null) {
                    memoryCacheManager.clear();
                    diskCacheManager.clear();
                }
                break;
            case CacheManager.ONLY_MEMORY:
                if (memoryCacheManager != null) {
                    memoryCacheManager.clear();
                }
                break;
            case CacheManager.ONLY_DISK:
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
            case CacheManager.ALL_ALLOW:
            default:
                if (memoryCacheManager != null && diskCacheManager != null) {
                    diskCacheManager.flushCache();
                }
                break;
            case CacheManager.ONLY_MEMORY:
                break;
            case CacheManager.ONLY_DISK:
                if (diskCacheManager != null) {
                    diskCacheManager.flushCache();
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
        if (diskCacheManager != null) {
            diskCacheManager.close();
        }
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
     * 设置硬盘缓存自定义的文件名
     *
     * @param dirName 自定义文件名
     * @return CacheManager
     */
    public CacheManager setDiskCacheDirName(String dirName) {
        this.diskCacheDirName = dirName;
        return this;
    }

    /**
     * 设置硬盘缓存的有效时间
     *
     * @param cacheTime 有效时间，单位：ms
     * @return CacheManager
     */
    public CacheManager setDiskCacheTime(long cacheTime) {
        this.diskCacheTime = cacheTime;
        return this;
    }
}
