package com.github.jeterlee.photowall.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.github.jeterlee.photowall.R;
import com.github.jeterlee.photowall.cache.CacheManager;
import com.github.jeterlee.photowall.utils.Util;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <pre>
 * Title: PhotoWallAdapter
 * Description: GridView 的适配器，负责异步从网络上下载图片展示在照片墙上
 * </pre>
 *
 * @author https://www.github.com/jeterlee
 * @date 2018/9/19 0019
 */

public class PhotoWallAdapter extends BaseAdapter {
    private static final String TAG = "PhotoWallAdapter";
    private Context context;
    /**
     * 记录所有正在下载或等待下载的任务
     */
    private Set<BitmapWorkerTask> taskCollection;
    private List<String> data;
    private CacheManager cacheManager;

    public PhotoWallAdapter(Context context, List<String> data) {
        this.context = context;
        this.data = data;
        taskCollection = new HashSet<>();
        cacheManager = CacheManager.getInstance();
        cacheManager.init(context);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view;
        ViewHolder viewHolder;
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.photo_layout, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.imageView = view.findViewById(R.id.photo);
            view.setTag(viewHolder);
        } else {
            // 直接复用
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.imageView.setImageResource(R.drawable.empty_photo);
        loadBitmaps(viewHolder.imageView, data.get(position));
        return view;
    }

    private class ViewHolder {
        ImageView imageView;
    }

    /**
     * 加载 Bitmap 对象。此方法会在 LruCache 中检查所有屏幕中可见的 ImageView 的Bitmap对象，
     * 如果发现任何一个ImageView的Bitmap对象不在缓存中，就先到磁盘继续寻找磁盘缓存，如果还是没有找到，
     * 最后就会开启异步线程去下载图片。
     */
    private void loadBitmaps(ImageView imageView, String imageUrl) {
        try {
            Bitmap bitmap = cacheManager.get(imageUrl, CacheManager.CacheType.BITMAP_TYPE);
            if (bitmap == null) {
                Log.i(TAG, "loadBitmaps: 网络下载图片");
                BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask();
                bitmapWorkerTask.setImageView(imageView);
                taskCollection.add(bitmapWorkerTask);
                bitmapWorkerTask.execute(imageUrl);
            } else {
                if (imageView != null) {
                    Log.i(TAG, "loadBitmaps: 缓存读取图片");
                    imageView.setImageBitmap(bitmap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消所有正在下载或等待下载的任务
     */
    public void cancelAllTasks() {
        if (taskCollection != null) {
            for (BitmapWorkerTask bitmapWorkerTask : taskCollection) {
                bitmapWorkerTask.cancel(false);
            }
        }
    }

    public void flush() {
        cacheManager.flush();
    }


    /**
     * 异步下载图片的任务
     */
    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            InputStream inputStream = Util.getInputStream(params[0]);
            Bitmap bitmap = null;
            if (inputStream != null) {
                bitmap = BitmapFactory.decodeStream(inputStream);
            }
            if (bitmap != null) {
                Log.i(TAG, "doInBackground: 存储图片缓存");
                cacheManager.put(params[0], bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (getImageView() != null && bitmap != null) {
                getImageView().setImageBitmap(bitmap);
            }
            taskCollection.remove(this);
        }

        ImageView imageView;

        void setImageView(ImageView imageView) {
            this.imageView = imageView;
        }

        ImageView getImageView() {
            return this.imageView;
        }
    }
}
