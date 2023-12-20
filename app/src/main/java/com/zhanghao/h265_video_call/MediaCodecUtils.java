package com.zhanghao.h265_video_call;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;

/**
 * MediaCode 工具类， 统一管理编码器和解码器
 */
public class MediaCodecUtils {

    /**
     * 此处分辨率最好由CameraX 中的 ImageProxy image传递，以确保以正确的分辨率编码
     * 此Demo手动指定
     */
    private int width = 480;
    private int height = 640;


    public MediaCodec getEnMediaCodec() {
        try {
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            MediaCodec mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
            return mediaCodec;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MediaCodec getDeMediaCodec(Surface surface) {
        try {
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
            format.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            MediaCodec mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            mediaCodec.configure(format,
                    surface,
                    null, 0);
            mediaCodec.start();
            return mediaCodec;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
