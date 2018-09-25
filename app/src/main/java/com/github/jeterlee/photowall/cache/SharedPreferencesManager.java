package com.github.jeterlee.photowall.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * <pre>
 * Title: SharedPreferencesManager
 * Description: SharedPreferences存储，支持对象加密存储（Base64加密）
 * </pre>
 *
 * @author <a href="https://www.github.com/jeterlee"></a>
 * @date 2018/9/20 0020
 */

public class SharedPreferencesManager implements ICache {
    private static final String TAG = "SpManager";
    /**
     * 默认SharedPreferences缓存目录
     */
    private static final String SP_CACHE_NAME = "sp_cache";
    private SharedPreferences sp;

    public SharedPreferencesManager(Context context) {
        this(context, SP_CACHE_NAME);
    }

    public SharedPreferencesManager(Context context, String fileName) {
        sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    public SharedPreferences getSharedPreferences() {
        if (sp == null) {
            throw new NullPointerException("SharedPreferences == null");
        }
        return sp;
    }

    @Override
    public void put(String key, Object ser) {
        try {
            Log.i(TAG, key + " put: " + ser);
            if (ser == null) {
                sp.edit().remove(key).apply();
            } else {
                byte[] bytes = SharedPreferencesManager.objectToByte(ser);
                bytes = Base64.encode(bytes, Base64.DEFAULT);
                put(key, HexUtil.encodeHexStr(bytes));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object get(String key) {
        try {
            String hex = get(key, null);
            if (hex == null) {
                return null;
            }
            byte[] bytes = HexUtil.decodeHex(hex.toCharArray());
            bytes = Base64.decode(bytes, Base64.DEFAULT);
            Object obj = SharedPreferencesManager.byteToObject(bytes);
            Log.i(TAG, key + " get: " + obj);
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean contains(String key) {
        return sp.contains(key);
    }

    @Override
    public void remove(String key) {
        sp.edit().remove(key).apply();
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public void clear() {
        sp.edit().clear().apply();
    }

    public void put(String key, String value) {
        if (value == null) {
            sp.edit().remove(key).apply();
        } else {
            sp.edit().putString(key, value).apply();
        }
    }

    public void put(String key, boolean value) {
        sp.edit().putBoolean(key, value).apply();
    }

    public void put(String key, float value) {
        sp.edit().putFloat(key, value).apply();
    }

    public void put(String key, long value) {
        sp.edit().putLong(key, value).apply();
    }

    public void putInt(String key, int value) {
        sp.edit().putInt(key, value).apply();
    }

    public String get(String key, String defValue) {
        return sp.getString(key, defValue);
    }

    public boolean get(String key, boolean defValue) {
        return sp.getBoolean(key, defValue);
    }

    public float get(String key, float defValue) {
        return sp.getFloat(key, defValue);
    }

    public int getInt(String key, int defValue) {
        return sp.getInt(key, defValue);
    }

    public long get(String key, long defValue) {
        return sp.getLong(key, defValue);
    }

    /**
     * byte[] 转为 对象
     *
     * @param bytes byte[] 字节数组
     * @return 对象
     */
    public static Object byteToObject(byte[] bytes) throws Exception {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return ois.readObject();
        } finally {
            if (ois != null) {
                ois.close();
            }
        }
    }

    /**
     * 对象 转为 byte[]
     *
     * @param obj 对象
     * @return byte[] 字节数组
     */
    public static byte[] objectToByte(Object obj) throws Exception {
        ObjectOutputStream oos = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            return bos.toByteArray();
        } finally {
            if (oos != null) {
                oos.close();
            }
        }
    }
}
