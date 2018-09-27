package com.github.jeterlee.photowall;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.GridView;

import com.github.jeterlee.photowall.adapter.PhotoWallAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <pre>
 * Title: MainActivity
 * Description: 照片墙主活动，使用GridView展示照片墙
 * </pre>
 *
 * @author <a href="https://www.github.com/jeterlee"></a>
 * @date 2018/9/25 0025
 */
public class MainActivity extends AppCompatActivity {
    private GridView mPhotoWall;
    private PhotoWallAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gridview_show);
        mPhotoWall = findViewById(R.id.photo_wall);
        List<String> data = new ArrayList<>();
        Collections.addAll(data, Images.imageUrls);

        mAdapter = new PhotoWallAdapter(this, data);
        mPhotoWall.setAdapter(mAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAdapter.flush();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 退出程序时结束所有的下载任务
        mAdapter.cancelAllTasks();
    }
}
