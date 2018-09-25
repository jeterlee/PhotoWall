package com.github.jeterlee.photowall.cache;

import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;

/**
 * <pre>
 * Title: MemoryCacheManager
 * Description: 内存缓存管理，使用LruCache算法。
 * </pre>
 *
 * @author <a href="https://www.github.com/jeterlee"></a>
 * @date 2018/9/20 0020
 */

public class MemoryCacheManager implements ICache {
    private static final String TAG = "MemoryCacheManager";
    private LruCache<String, Object> cache;
    private static MemoryCacheManager instance;

    public MemoryCacheManager() {
        // 获取应用程序最大可用内存（默认为程序最大可用内存的1/8）
        this((int) Runtime.getRuntime().maxMemory() / 1024 / 8);
    }

    /**
     * 设置自定义大小的LruCache
     *
     * @param maxSize 自定义大小，单位：byte
     */
    public MemoryCacheManager(int maxSize) {
        cache = new LruCache<>(maxSize * 1024);
    }

    @Override
    public synchronized void put(@NonNull String key, Object value) {
        if (cache.get(key) != null) {
            cache.remove(key);
        }
        cache.put(key, value);
    }

    @Override
    public Object get(@NonNull String key) {
        return cache.get(key);
    }

    @Override
    public void remove(@NonNull String key) {
        if (cache.get(key) != null) {
            cache.remove(key);
        }
    }

    @Override
    public boolean contains(@NonNull String key) {
        return cache.get(key) != null;
    }

    @Override
    public long size() {
        return cache.size();
    }

    @Override
    public void clear() {
        cache.evictAll();
    }
}
