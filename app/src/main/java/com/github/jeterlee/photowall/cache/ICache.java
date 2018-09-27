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
     * 对应的索引key存放对象
     *
     * @param key 索引key
     * @param e   对象
     * @param <E> 对象
     */
    <E> void put(String key, E e);

    /**
     * 根据索引key获取对应对象
     *
     * @param key 索引key
     * @param <E> 对象
     * @return 对象
     */
    <E> E get(String key);

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
     * @return {@code true}: 移除成功<br>{@code false}: 移除失败
     */
    boolean remove(String key);

    /**
     * 清除所有缓存
     */
    void clear();
}
