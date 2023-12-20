package com.zhanghao.h265_video_call;

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
//                .setTargetResolution(new android.util.Size(width, height))    // 使用默认分辨率
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
     * 提取并处理YUV数据， 整理数据格式为 YUV420P
     *
     * @param image
     */
    private void processImage(ImageProxy image) {
        ByteBuffer bufferY = image.getPlanes()[0].getBuffer();
        ByteBuffer bufferU = image.getPlanes()[1].getBuffer();
        ByteBuffer bufferV = image.getPlanes()[2].getBuffer();

        byte[] dataY = new byte[bufferY.remaining()];
        byte[] dataU = new byte[bufferU.remaining()];
        byte[] dataV = new byte[bufferV.remaining()];

        bufferY.get(dataY);
        bufferU.get(dataU);
        bufferV.get(dataV);

        byte[] yuvData = new byte[dataY.length + dataU.length + dataV.length];
        System.arraycopy(dataY, 0, yuvData, 0, dataY.length);
        System.arraycopy(dataU, 0, yuvData, dataY.length, dataU.length);
        System.arraycopy(dataV, 0, yuvData, dataY.length + dataU.length, dataV.length);

        // 数据旋转 需与编解码器的分辨率保持一致
        byte[] rotateYuvData = rotateYUV420PCounterClockwise90(yuvData, image.getWidth(), image.getHeight());

        // 数据的实际宽高
        int width = image.getWidth();
        int height = image.getHeight();
//        Log.i(TAG, "processImage: width = " + width);
//        Log.i(TAG, "processImage: height = " + height);

        if (cameraXCallback != null) {
            cameraXCallback.onImageAvailable(rotateYuvData);
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


    /**
     * 顺时针旋转90度
     * @param input YUV420P
     * @param width
     * @param height
     * @return
     */
    private byte[] rotateYUV420PClockwise90(byte[] input, int width, int height) {
        byte[] output = new byte[width * height * 3 / 2];

        // 旋转Y分量
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                output[i * height + height - 1 - j] = input[j * width + i];
            }
        }

        // 旋转Cb和Cr分量
        int offset = width * height;
        for (int i = 0; i < width / 2; i++) {
            for (int j = 0; j < height / 2; j++) {
                output[offset + i * height / 2 + height / 2 - 1 - j] = input[offset + j * width / 2 + i];
                output[offset + width * height / 4 + i * height / 2 + height / 2 - 1 - j] = input[offset + width * height / 4 + j * width / 2 + i];
            }
        }

        return output;
    }


    /**
     * 逆时针旋转90度
     * @param input     YUV420P
     * @param width
     * @param height
     * @return
     */
    private byte[] rotateYUV420PCounterClockwise90(byte[] input, int width, int height) {
        byte[] output = new byte[width * height * 3 / 2];

        // 旋转Y分量
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                output[(width - 1 - i) * height + j] = input[j * width + i];
            }
        }

        // 旋转Cb和Cr分量
        int offset = width * height;
        for (int i = 0; i < width / 2; i++) {
            for (int j = 0; j < height / 2; j++) {
                output[offset + (width / 2 - 1 - i) * height / 2 + j] = input[offset + j * width / 2 + i];
                output[offset + width * height / 4 + (width / 2 - 1 - i) * height / 2 + j] = input[offset + width * height / 4 + j * width / 2 + i];
            }
        }

        return output;
    }
}


