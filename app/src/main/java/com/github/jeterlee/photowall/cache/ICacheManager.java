package com.github.jeterlee.photowall.cache;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * <pre>
 * Title: ICacheManager
 * Description: 缓存管理的接口
 * </pre>
 *
 * @author <a href="https://www.github.com/jeterlee"></a>
 * @date 2018/9/26 0026
 */

public interface ICacheManager extends ICache {
    /**
     * 对应的索引key存放String
     *
     * @param key    索引key
     * @param string String
     */
    void put(String key, String string);

    /**
     * 对应的索引key存放JSONObject
     *
     * @param key        索引key
     * @param jsonObject JSONObject
     */
    void put(String key, JSONObject jsonObject);

    /**
     * 对应的索引key存放JSONArray
     *
     * @param key       索引key
     * @param jsonArray JSONArray
     */
    void put(String key, JSONArray jsonArray);

    /**
     * 对应的索引key存放byte[]
     *
     * @param key   索引key
     * @param bytes byte[]
     */
    void put(String key, byte[] bytes);

    /**
     * 对应的索引key存放序列化对象
     *
     * @param key            索引key
     * @param e              对象
     * @param <Serializable> 序列化对象
     */
    @Override
    <Serializable> void put(String key, Serializable e);

    /**
     * 对应的索引key存放bitmap
     *
     * @param key    索引key
     * @param bitmap Bitmap
     */
    void put(String key, Bitmap bitmap);

    /**
     * 对应的索引key存放drawable
     *
     * @param key      索引key
     * @param drawable Drawable
     */
    void put(String key, Drawable drawable);

    /**
     * 根据索引key获取对应的String
     *
     * @param key 索引key
     * @return String
     */
    String getAsString(String key);

    /**
     * 根据索引key获取对应的JSONObject
     *
     * @param key 索引key
     * @return JSONObject
     */
    JSONObject getAsJSONObject(String key);

    /**
     * 根据索引key获取对应的JSONArray
     *
     * @param key 索引key
     * @return JSONArray
     */
    JSONArray getAsJSONArray(String key);

    /**
     * 根据索引key获取对应的byte[]
     *
     * @param key 索引key
     * @return byte[]
     */
    byte[] getAsBinary(String key);

    /**
     * 根据索引key获取对应的序列化对象
     *
     * @param key            索引key
     * @param <Serializable> 序列化对象
     * @return 序列化对象
     */
    @Override
    <Serializable> Serializable get(String key);

    /**
     * 根据索引key获取对应的Bitmap
     *
     * @param key 索引key
     * @return Bitmap
     */
    Bitmap getAsBitmap(String key);

    /**
     * 根据索引key获取对应的Drawable
     *
     * @param key 索引key
     * @return Drawable
     */
    Drawable getAsDrawable(String key);
}
