package com.zhanghao.h265_video_call;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

/**
 * 本地相机捕捉到的画面
 */
public class LocalSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "zh___LocalSurfaceView";
    private Surface surface;
    private Context context;

    public LocalSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        getHolder().addCallback(this);
//        new CameraXUtils(context, new CameraXUtils.CameraXCallback() {
//            @Override
//            public void onImageAvailable(byte[] imageData) {
//
//            }
//        });
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        surface = holder.getSurface();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }
}
