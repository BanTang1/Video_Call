//package com.zhanghao.h265_video_call;
//
//import android.content.Context;
//import android.util.Log;
//
//import androidx.camera.core.CameraSelector;
//import androidx.camera.core.ImageAnalysis;
//import androidx.camera.core.ImageProxy;
//import androidx.camera.lifecycle.ProcessCameraProvider;
//import androidx.core.content.ContextCompat;
//
//import com.google.common.util.concurrent.ListenableFuture;
//
//import java.nio.ByteBuffer;
//import java.util.concurrent.ExecutionException;
//
//public class CameraXUtils {
//    private static final String TAG = "zh___CameraXUtils";
//    private Context context;
//    private CameraSelector cameraSelector;
//    private ImageAnalysis imageAnalysis;
//    private CameraXCallback cameraXCallback;
//
//    public CameraXUtils(Context context, CameraXCallback callback) {
//        this.context = context;
//        this.cameraSelector = new CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//                .build();
//        this.cameraXCallback = callback;
//    }
//
//    public void startCamera() {
//        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
//
//        cameraProviderFuture.addListener(() -> {
//            try {
//                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
//                bindImageAnalysis(cameraProvider);
//            } catch (ExecutionException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        }, ContextCompat.getMainExecutor(context));
//    }
//
//    private void bindImageAnalysis(ProcessCameraProvider cameraProvider) {
//        imageAnalysis = new ImageAnalysis.Builder()
//                .setTargetResolution(new android.util.Size(640, 480))
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build();
//
//        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), this::processImage);
//
//        cameraProvider.bindToLifecycle(null, cameraSelector, imageAnalysis);
//    }
//
//    private void processImage(ImageProxy image) {
//        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//        byte[] data = new byte[buffer.remaining()];
//        buffer.get(data);
//
//        int format = image.getFormat();
//        Log.i(TAG, "processImage: format = " + format);
//
//        // 调用回调方法，将 YUV 数据传递给处理逻辑
//        cameraXCallback.onImageAvailable(data);
//
//        image.close();
//    }
//
//    public void stopCamera() {
//        if (imageAnalysis != null) {
//            imageAnalysis.clearAnalyzer();
//        }
//    }
//
//    public interface CameraXCallback {
//        void onImageAvailable(byte[] imageData);
//    }
//}
//
//
