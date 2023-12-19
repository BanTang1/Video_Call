package com.zhanghao.app_chat_b;

import android.Manifest;
import android.media.MediaCodec;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.zhanghao.app_chat_b.databinding.MainLayoutBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * 视屏A端
 */
public class MainActivity extends AppCompatActivity implements CameraXUtils.CameraXCallback, ChatBSocket.WebSocketCallback {
    private static final String TAG = "zh___MainActivity";
    private MainLayoutBinding binding;
    private CameraXUtils cameraXUtils;
    private MediaCodecUtils mediaCodecUtils;
    private MediaCodec enMediaCodec, deMediaCodec;

    private ChatBSocket socketPull;

    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private byte[] configData;      // VPS SPS PPS


    /**
     * 权限结果回调
     */
    private ActivityResultLauncher<String> requestPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (result) {
                // Permission granted
                init();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraXUtils.stopCamera();
        deMediaCodec.stop();
        enMediaCodec.stop();
        deMediaCodec.release();
        enMediaCodec.release();
        if (handlerThread != null) {
            handlerThread.quitSafely();
            try {
                handlerThread.join();
                handlerThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        socketPull.close();
    }

    /**
     * 1. 初始化本地相机
     * 2. 初始化硬编解码器
     * 3. 初始化WebSocket
     * 4. 初始化编解码线程
     */
    private void init() {
        cameraXUtils = new CameraXUtils(this, this, binding.previewView);
        cameraXUtils.startCamera();

        mediaCodecUtils = new MediaCodecUtils();
        enMediaCodec = mediaCodecUtils.getEnMediaCodec();
        binding.remoteSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                deMediaCodec = mediaCodecUtils.getDeMediaCodec(surfaceHolder.getSurface());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

            }
        });

        socketPull = new ChatBSocket();
        socketPull.start();

        handlerThread = new HandlerThread("ImageProcessingThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
    }


    /**
     * CameraX 工具类的数据回调,
     * 也就是摄像头捕捉的画面
     * 将YUV数据编码为H265码流
     *
     * @param localData YUV_420_888
     */
    @Override
    public void onImageAvailable(byte[] localData) {
        if (backgroundHandler != null) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    // 在子线程中执行编码操作
                    encodeYUVToH265(localData);
                }
            });
        };
    }

    /**
     * Socket对端发送过来的数据，也就是对方摄像头的画面
     *
     * @param remoteData
     */
    @Override
    public void onMessage(ByteBuffer remoteData) {
        byte[] data = new byte[remoteData.remaining()];
        remoteData.get(data);
        deCode(data);
    }

    /**
     * 开始解码
     *
     * @param data
     */
    public void deCode(byte[] data) {
        // 提交数据到解码器
        int inputBufferID = deMediaCodec.dequeueInputBuffer(10000);
        if (inputBufferID >= 0) {
            ByteBuffer inputBuffer = deMediaCodec.getInputBuffer(inputBufferID);
            assert inputBuffer != null;
            inputBuffer.clear();
            inputBuffer.put(data);
            deMediaCodec.queueInputBuffer(inputBufferID, 0, data.length, System.currentTimeMillis(), 0);
        }

        // 从解码器获取数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferID = deMediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
        // 即使只提交了一份数据到解码器的输入缓冲区，但解码器的输出并不一定是一一对应的，它可能在一次解码操作中产生多个输出缓冲区
        while (outputBufferID >= 0) {
            deMediaCodec.releaseOutputBuffer(outputBufferID, true);
            outputBufferID = deMediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
        }
    }

    /**
     * 实际处理YUV编码为H265的地方
     *
     * @param imageData
     */
    private void encodeYUVToH265(byte[] imageData) {
        int inputBufferIndex = enMediaCodec.dequeueInputBuffer(10000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = enMediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(imageData);
            enMediaCodec.queueInputBuffer(inputBufferIndex, 0, imageData.length, System.currentTimeMillis(), 0);
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = enMediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = enMediaCodec.getOutputBuffer(outputBufferIndex);
            byte[] outputData = new byte[bufferInfo.size];
            outputBuffer.get(outputData);
            handleData(bufferInfo, outputBuffer);
            writeFile(outputData);
            enMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = enMediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
        }
    }

    /**
     * 处理码流数据：保证VPS SPS PPS 在I帧之前一定存在
     *
     * @param bufferInfo   存储有关媒体数据缓冲区的信息
     * @param outputBuffer 操作原始字节的缓冲区
     */
    private void handleData(MediaCodec.BufferInfo bufferInfo, ByteBuffer outputBuffer) {
        switch (bufferInfo.flags) {
            // 保存VPS SPS PPS 信息
            case MediaCodec.BUFFER_FLAG_CODEC_CONFIG:
                configData = new byte[bufferInfo.size];
                outputBuffer.rewind();
                outputBuffer.get(configData);
                break;
            // I帧
            case MediaCodec.BUFFER_FLAG_KEY_FRAME:
                ByteBuffer configAndIFrame = ByteBuffer.allocate(bufferInfo.size + configData.length);
                configAndIFrame.put(configData);
                outputBuffer.rewind();
                configAndIFrame.put(outputBuffer);
                socketPull.sendData(configAndIFrame.array());
                break;
            // 其余信息正常发送
            default:
                byte[] otherData = new byte[bufferInfo.size];
                outputBuffer.rewind();
                outputBuffer.get(otherData);
                socketPull.sendData(otherData);
                break;
        }
    }


    /**
     * 将录屏的画面编码为H265码流，并写入私有目录
     *
     * @param data 码流字节数据
     */
    private void writeFile(byte[] data) {
        try {
            if (data == null) return;
            // 获取应用程序私有目录
            File privateDir = getExternalFilesDir(null);
            // 创建一个新的文件，你可以自定义文件名和格式
            File outputFile = new File(privateDir, "outputData.h265");

            // 创建文件输出流
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile, true);
            fileOutputStream.write(data);

            // 关闭文件输出流
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
