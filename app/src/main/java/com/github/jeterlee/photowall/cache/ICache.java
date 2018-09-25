package com.github.jeterlee.photowall.cache;

/**
 * <pre>
 * Title: ICache
 * Description: 缓存接口
 * </pre>
 *
 * @author <a href="https://www.github.com/jeterlee"></a>
 * @date 2018/9/20 0020
 */

public interface ICache {
    /**
     * 写入索引key对应的缓存
     *
     * @param key   索引key
     * @param value 缓存内容
     */
    void put(String key, Object value);

    /**
     * 根据索引key获取对应的缓存
     *
     * @param key 索引key
     * @return 对应索引key的缓存内容
     */
    Object get(String key);

    /**
     * 判断是否有对应索引key的缓存
     *
     * @param key 索引key
     * @return {@code true}: 存在<br>{@code false}: 不存在
     */
    boolean contains(String key);

    /**
     * 根据索引key移除对应的缓存
     *
     * @param key 索引key
     */
    void remove(String key);

    /**
     * 缓存大小
     *
     * @return 缓存大小
     */
    long size();

    /**
     * 清除所有缓存
     */
    void clear();
}
