package com.zhanghao.h265_video_call;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * 服务端Socket，用于推流
 */
public class ChatASocket {

    private final String TAG = "zh___SocketPush";
    private WebSocket webSocket;
    private WebSocketCallback webSocketCallback;

    public ChatASocket() {
        this(null);
    }

    public ChatASocket(WebSocketCallback webSocketCallback) {
        this.webSocketCallback = webSocketCallback;
    }

    public interface WebSocketCallback {
        void onMessage(ByteBuffer message);
    }

    private WebSocketServer webSocketServer = new WebSocketServer(new InetSocketAddress(9522)) {
        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
            ChatASocket.this.webSocket = webSocket;
        }

        @Override
        public void onClose(WebSocket webSocket, int i, String s, boolean b) {
            Log.i(TAG, "onClose: 关闭 socket ");
        }

        @Override
        public void onMessage(WebSocket webSocket, String s) {
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            super.onMessage(conn, message);
            if (webSocketCallback != null) {
                webSocketCallback.onMessage(message);
            }
        }

        @Override
        public void onError(WebSocket webSocket, Exception e) {
            Log.i(TAG, "onError:  " + e.toString());
        }

        @Override
        public void onStart() {

        }
    };

    /**
     * 开始推流，初始化 webSocketServer ，并启动编码线程
     */
    public void start() {
        webSocketServer.start();
    }

    /**
     * 发送码流数据
     *
     * @param bytes 码流数据
     */
    public void sendData(byte[] bytes) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(bytes);
        }
    }

    /**
     * 关闭闭 socket 服务 , 同时停止编码线程
     */
    public void close() {
        try {
            webSocket.close();
            webSocketServer.stop();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
