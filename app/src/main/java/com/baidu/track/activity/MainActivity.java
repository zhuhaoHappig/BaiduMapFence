package com.baidu.track.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.baidu.track.R;
import com.baidu.track.TrackApplication;
import com.baidu.track.utils.BitmapUtil;
import com.baidu.track.utils.CommonUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        BitmapUtil.init();
    }

    private void init() {

        Button trace = (Button) findViewById(R.id.btn_createfence);
        Button query = (Button) findViewById(R.id.btn_fencealarm);

        trace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CreateFenceActivity.class);
                startActivity(intent);
            }
        });

        query.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,FenceAlarmActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 适配android M，检查权限
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isNeedRequestPermissions(permissions)) {
            requestPermissions(permissions.toArray(new String[permissions.size()]), 0);
        }
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_main;
    }

    private boolean isNeedRequestPermissions(List<String> permissions) {
        // 定位精确位置
        addPermission(permissions, Manifest.permission.ACCESS_FINE_LOCATION);
        // 存储权限
        addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // 读取手机状态
        addPermission(permissions, Manifest.permission.READ_PHONE_STATE);
        return permissions.size() > 0;
    }

    private void addPermission(List<String> permissionsList, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BitmapUtil.clear();
    }
}
