package com.het.cbeauty.cameralearndemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.het.cbeauty.cameralearndemo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding mBinding;

    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        checkPerission();
        mBinding.btnTakePhoto.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), CameraActivity3.class));
        });
    }

    private void checkPerission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            for (int i = 0; i < permissions.length; i++) {
                int permission = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[i]);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, 321);
                    break;
                }
            }
        }
    }
}