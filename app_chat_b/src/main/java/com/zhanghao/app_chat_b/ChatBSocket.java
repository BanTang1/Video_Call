package com.zhanghao.app_chat_b;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

/**
 * Socket 客户端，用于接收
 */
public class ChatBSocket {
    private final String TAG = "zh___SocketPull";

    // 修改为指定的服务器地址
    private final String websocket_url = "ws://192.168.101.111:9522";
    private WebSocketClient webSocket;
    private WebSocketCallback webSocketCallback;


    public interface WebSocketCallback {
        void onMessage(ByteBuffer message);
    }


    public ChatBSocket() {
        this(null);
    }

    public ChatBSocket(WebSocketCallback webSocketCallback) {
        this.webSocketCallback = webSocketCallback;
    }

    public void start() {
        initWebSocket();
    }

    /**
     * 初始化WebSocket客户端
     */
    private void initWebSocket() {
        webSocket = new WebSocketClient(URI.create(websocket_url)) {

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i(TAG, "onOpen: ");
            }

            @Override
            public void onMessage(String message) {

            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                if (webSocketCallback != null) {
                    webSocketCallback.onMessage(bytes);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.e(TAG, "onClose: ");
                initWebSocket();
            }

            @Override
            public void onError(Exception ex) {
                Log.e(TAG, "onError: ex = " + ex.getMessage());
            }
        };
        webSocket.connect();
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

    public void close() {
        webSocket.close();
        webSocket = null;
    }

}
