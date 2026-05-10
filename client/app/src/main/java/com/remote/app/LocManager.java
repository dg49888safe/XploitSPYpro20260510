package com.remote.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 定位管理器 - 获取设备 GPS 和网络定位
 * 
 * 兼容 Android 10+ 后台定位限制
 * Android 14+ (API 34+) 更新
 */
public class LocManager implements LocationListener {

    private final Context mContext;
    private boolean isGPSEnabled = false;
    private boolean isNetworkEnabled = false;
    private boolean canGetLocation = false;
    private Location location;
    private double latitude = 0;
    private double longitude = 0;
    private float accuracy = 0;
    private double altitude = 0;
    private float speed = 0;
    private long lastUpdateTime = 0;

    // 定位更新参数
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10米
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;  // 1分钟

    protected LocationManager locationManager;

    /**
     * 构造函数 - 空构造
     */
    public LocManager() {
        this.mContext = null;
    }

    /**
     * 构造函数 - 带上下文
     */
    public LocManager(Context context) {
        this.mContext = context;
        getLocation();
    }

    /**
     * 获取定位信息
     * 支持 Android 10+ 后台定位限制
     */
    public Location getLocation() {
        try {
            if (mContext == null) {
                Log.e(Config.LOG_TAG, "Context 为空");
                return null;
            }

            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            
            if (locationManager == null) {
                Log.e(Config.LOG_TAG, "LocationManager 获取失败");
                return null;
            }

            // ========== 检查提供商 ==========
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Log.d(Config.LOG_TAG, "GPS 启用: " + isGPSEnabled + ", 网络启用: " + isNetworkEnabled);

            if (!isGPSEnabled && !isNetworkEnabled) {
                Log.w(Config.LOG_TAG, "GPS 和网络定位都未启用");
                this.canGetLocation = false;
                return null;
            }

            this.canGetLocation = true;

            // ========== 权限检查 ==========
            // Android 6.0+ 需要检查运行时权限
            if (!hasLocationPermissions()) {
                Log.w(Config.LOG_TAG, "缺少定位权限");
                this.canGetLocation = false;
                return null;
            }

            // ========== 尝试从网络提供商获取 ==========
            if (isNetworkEnabled) {
                try {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null) {
                        updateLocationData(location);
                        Log.d(Config.LOG_TAG, "从网络提供商获取定位: " + latitude + ", " + longitude);
                    }
                } catch (SecurityException e) {
                    Log.w(Config.LOG_TAG, "网络定位权限异常: " + e.getMessage());
                }
            }

            // ========== 尝试从 GPS 提供商获取 ==========
            if (isGPSEnabled && location == null) {
                try {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        updateLocationData(location);
                        Log.d(Config.LOG_TAG, "从 GPS 提供商获取定位: " + latitude + ", " + longitude);
                    }
                } catch (SecurityException e) {
                    Log.w(Config.LOG_TAG, "GPS 定位权限异常: " + e.getMessage());
                }
            }

            // ========== 尝试从被动提供商获取 ==========
            if (location == null) {
                try {
                    location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    if (location != null) {
                        updateLocationData(location);
                        Log.d(Config.LOG_TAG, "从被动提供商获取定位: " + latitude + ", " + longitude);
                    }
                } catch (SecurityException e) {
                    Log.w(Config.LOG_TAG, "被动定位权限异常: " + e.getMessage());
                }
            }

            // ========== 注册定位监听 ==========
            if (hasLocationPermissions()) {
                try {
                    // 注册网络定位监听
                    if (isNetworkEnabled) {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            this
                        );
                        Log.d(Config.LOG_TAG, "已注册网络定位监听");
                    }

                    // 注册 GPS 定位监听
                    if (isGPSEnabled) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            this
                        );
                        Log.d(Config.LOG_TAG, "已注册 GPS 定位监听");
                    }
                } catch (SecurityException e) {
                    Log.e(Config.LOG_TAG, "注册定位监听异常: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "获取定位异常: " + e.getMessage());
            e.printStackTrace();
        }

        return location;
    }

    /**
     * 更新定位数据
     */
    private void updateLocationData(Location loc) {
        if (loc != null) {
            latitude = loc.getLatitude();
            longitude = loc.getLongitude();
            altitude = loc.getAltitude();
            accuracy = loc.getAccuracy();
            speed = loc.getSpeed();
            lastUpdateTime = loc.getTime();
        }
    }

    /**
     * 检查是否拥有定位权限
     * 支持 Android 10+ 后台定位权限
     */
    private boolean hasLocationPermissions() {
        // 前台定位权限（Android 6.0+）
        boolean hasFineLocation = ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        
        boolean hasCoarseLocation = ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // 检查前台定位权限
        if (!hasFineLocation && !hasCoarseLocation) {
            Log.w(Config.LOG_TAG, "缺少前台定位权限");
            return false;
        }

        // 后台定位权限（Android 10+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean hasBackgroundLocation = ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
            
            if (!hasBackgroundLocation) {
                Log.w(Config.LOG_TAG, "缺少 Android 10+ 后台定位权限");
                // 仍然可以获取前台定位，但后台无法获取
            }
        }

        return true;
    }

    /**
     * 检查是否可以获取定位
     */
    public boolean canGetLocation() {
        return this.canGetLocation && location != null;
    }

    /**
     * 获取定位数据 JSON 对象
     */
    public JSONObject getData() {
        JSONObject data = new JSONObject();
        try {
            if (location != null) {
                data.put("enabled", true);
                data.put("latitude", latitude);
                data.put("longitude", longitude);
                data.put("altitude", altitude);
                data.put("accuracy", accuracy);
                data.put("speed", speed);
                data.put("timestamp", lastUpdateTime);
                data.put("provider", location.getProvider());
                
                // 添加定位精度评级
                if (accuracy < 10) {
                    data.put("accuracy_level", "高精度");
                } else if (accuracy < 50) {
                    data.put("accuracy_level", "中精度");
                } else {
                    data.put("accuracy_level", "低精度");
                }
            } else {
                data.put("enabled", false);
                data.put("error", "无法获取定位信息");
            }
        } catch (JSONException e) {
            Log.e(Config.LOG_TAG, "构建定位数据 JSON 异常: " + e.getMessage());
        }
        return data;
    }

    /**
     * 定位改变监听
     */
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            updateLocationData(location);
            Log.d(Config.LOG_TAG, "定位已更新: " + latitude + ", " + longitude + " (精度: " + accuracy + "m)");
            
            // 发送更新
            try {
                IOSocket.getInstance().getIoSocket().emit(Config.MessageType.LOCATION, getData());
            } catch (Exception e) {
                Log.e(Config.LOG_TAG, "发送定位更新异常: " + e.getMessage());
            }
        }
    }

    /**
     * 提供商禁用监听
     */
    @Override
    public void onProviderDisabled(String provider) {
        Log.w(Config.LOG_TAG, "定位提供商已禁用: " + provider);
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            isGPSEnabled = false;
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            isNetworkEnabled = false;
        }
    }

    /**
     * 提供商启用监听
     */
    @Override
    public void onProviderEnabled(String provider) {
        Log.d(Config.LOG_TAG, "定位提供商已启用: " + provider);
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            isGPSEnabled = true;
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            isNetworkEnabled = true;
        }
    }

    /**
     * 提供商状态改变监听
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(Config.LOG_TAG, "定位提供商状态改变: " + provider + " -> " + status);
    }

    /**
     * 移除定位监听
     */
    public void removeLocationUpdates() {
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(this);
                Log.d(Config.LOG_TAG, "已移除定位监听");
            }
        } catch (Exception e) {
            Log.e(Config.LOG_TAG, "移除定位监听异常: " + e.getMessage());
        }
    }
}
