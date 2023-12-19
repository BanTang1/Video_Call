package com.zhanghao.app_chat_b;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

/**
 * CameraX 工具类
 */
public class CameraXUtils {
    private static final String TAG = "zh___CameraXUtils";
    private Context context;
    private CameraSelector cameraSelector;
    private ImageAnalysis imageAnalysis;
    private CameraXCallback cameraXCallback;
    private PreviewView previewView;

    private int width = 720;
    private int height = 1080;

    public CameraXUtils(Context context, PreviewView previewView) {
        this(context, null, previewView);
    }

    public CameraXUtils(Context context) {
        this(context, null, null);
    }

    public CameraXUtils(Context context, CameraXCallback callback, PreviewView previewView) {
        this.context = context;
        this.previewView = previewView;
        this.cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)    // 默认使用前置摄像头
                .build();
        this.cameraXCallback = callback;
    }

    /**
     * 启动相机
     */
    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bind(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));
    }

    /**
     * 绑定定相机到Activity的生命周期中，
     * 同时时绑定相机的相关配置：预览 分析
     *
     * @param cameraProvider
     */
    private void bind(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new android.util.Size(width, height))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), this::processImage);

        Preview preview = new Preview.Builder().build();
        if (previewView != null) {
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        }

        cameraProvider.bindToLifecycle((MainActivity) context, cameraSelector, preview, imageAnalysis);
    }

    /**
     * 提取并处理YUV数据， 数据格式为 YUV_420_888
     *
     * @param image
     */
    private void processImage(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];

        buffer.get(data);

        if (cameraXCallback != null) {
            cameraXCallback.onImageAvailable(data);
        }
        image.close();
    }

    /**
     * 停止相机
     */
    public void stopCamera() {
        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
        }
    }

    /**
     * 相机的YUV_420_888 数据回调接口
     */
    public interface CameraXCallback {
        void onImageAvailable(byte[] imageData);
    }

}


