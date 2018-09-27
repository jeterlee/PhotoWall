package com.github.jeterlee.photowall.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * <pre>
 * Title: Util
 * Description: 帮助类
 * </pre>
 *
 * @author <a href="https://www.github.com/jeterlee"></a>
 * @date 2018/9/19 0019
 */

public class Util {
    /**
     * 建立HTTP请求，并获输出流（写入缓存）
     *
     * @param urlString    URL地址
     * @param outputStream 输出流
     * @return 解析是否成功
     */
    public static boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static InputStream getInputStream(String urlString) {
        try {
            // 创建url对象
            URL url = new URL(urlString);
            // 打开一个连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 设置它的请求方式
            connection.setRequestMethod("GET");
            // 设置它的请求超时时间
            connection.setConnectTimeout(5000);
            // 设置超时读取时间
            connection.setReadTimeout(3000);
            // 得到服务区返回的结果吗
            int code = connection.getResponseCode();
            // 利用结果吗判断
            // 服务器返回的数据是以流的形式  返回的
            if (code == 200) {
                return connection.getInputStream();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
