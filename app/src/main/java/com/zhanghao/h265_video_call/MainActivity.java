package com.zhanghao.h265_video_call;

import android.Manifest;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.zhanghao.h265_video_call.databinding.MainLayoutBinding;

/**
 * 视屏A端
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private MainLayoutBinding binding;

    /**
     * 权限结果回调
     */
    private ActivityResultLauncher<String> requestPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (result) {
                // Permission granted
            }
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = MainLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestPermission.launch(Manifest.permission.CAMERA);

    }
}
