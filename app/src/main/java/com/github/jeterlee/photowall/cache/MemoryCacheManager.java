package com.github.jeterlee.photowall.cache;

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

    private LruCache<String, Object> cache;

    public MemoryCacheManager() {
        // 获取应用程序最大可用内存（默认为程序最大可用内存的 1/8）
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
    public <E> void put(String key, E e) {
        try {
            if (cache.get(key) != null) {
                cache.remove(key);
            }
            cache.put(key, e);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public <E> E get(String key) {
        try {
            //noinspection unchecked
            return (E) cache.get(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean contains(String key) {
        try {
            return cache.get(key) != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean remove(String key) {
        try {
            if (cache.get(key) != null) {
                cache.remove(key);
            }
            return cache.get(key) == null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void clear() {
        try {
            cache.evictAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long size() {
        try {
            return cache.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}
