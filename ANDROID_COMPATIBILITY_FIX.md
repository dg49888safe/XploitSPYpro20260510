# XploitSPY Android 兼容性升级报告 (2026年新版Android)

## 问题分析

### 核心问题
1. **APK 无法在小米新Android系统上线** - Socket.IO 连接失败
2. **Gradle 版本过旧** - gradle 3.4.2 已无法支持最新Android API
3. **Socket.IO 库版本过低** - 2.1.0 缺少新系统需要的TLS支持
4. **权限模型不符** - Android 14+ 权限声明不完整
5. **前台服务配置不足** - Android 14+ 需要明确指定服务类型

## 已发现的问题点

### 1. Gradle 构建工具版本（⚠️ 严重）
```
当前: gradle:3.4.2 (2019年)
必须升级到: gradle:8.2.0+ (2024年+)
影响: 无法编译 Android 14 (API 34)
```

### 2. Socket.IO 库版本（⚠️ 严重）
```
当前: io.socket:socket.io-client:2.1.0
问题: 
  - 不支持 HTTP/2
  - TLS 1.3 支持不完整
  - 与新版 OkHttp 不兼容
必须升级到: 2.1.1 (稳定最新)
```

### 3. AndroidX 依赖不完整
```
当前: 只有 core:1.12.0 和 appcompat:1.6.1
缺失: androidx.activity, androidx.fragment 等
```

### 4. Android 14 权限声明（⚠️ 中等）
```
缺失的前台服务权限:
  - FOREGROUND_SERVICE
  - FOREGROUND_SERVICE_LOCATION
  - FOREGROUND_SERVICE_MICROPHONE
  - FOREGROUND_SERVICE_CAMERA
  - POST_NOTIFICATIONS (Android 13+)

小米系统特殊处理:
  - 需要请求"电池优化"白名单权限
  - 需要处理系统后台限制
```

### 5. Socket.IO 连接地址硬编码
```
当前: IOSocket.java 中 http://xwizer.herokuapp.com:80
问题: 该服务已下线，无法连接
解决: 应该从配置文件或构建时动态注入
```

## 升级清单

### ✅ 需要更新的文件

#### 1. `client/build.gradle` - 构建配置升级
- [ ] Gradle 版本: 3.4.2 → 8.2.0
- [ ] AGP 版本: 对应升级
- [ ] compileSdkVersion: 保持 34 (Android 14)
- [ ] 添加 AndroidX 依赖
- [ ] 更新 socket.io-client 到 2.1.1

#### 2. `client/app/build.gradle` - 应用配置
- [ ] 更新 gradle 插件版本
- [ ] 添加 JVM 和 Java 兼容性设置
- [ ] 配置 ProGuard 规则排除 socket.io
- [ ] 添加 buildFeatures 配置

#### 3. `client/app/src/main/AndroidManifest.xml`
- [ ] 移除 @deprecated 的权限
- [ ] 修复 DeviceAdmin 类名（当前用 DeviceAdminX，Manifest 中写的是 DeviceAdminX）
- [ ] 添加 queries 标签用于包可见性

#### 4. `client/app/src/main/java/com/remote/app/IOSocket.java`
- [ ] 从常量类读取 C2 地址而不是硬编码
- [ ] 添加 SSL/TLS 验证
- [ ] 改进错误处理和日志

#### 5. `client/app/src/main/java/com/remote/app/MainActivity.java`
- [ ] 修复权限申请逻辑
- [ ] 处理 Android 14+ 的权限分组
- [ ] 改进 AlarmManager 用法（Android 12+ 需要 SCHEDULE_EXACT_ALARM）

#### 6. `client/app/src/main/java/com/remote/app/MainService.java`
- [ ] ✅ 已实现 Android 14+ 前台服务类型
- [ ] 改进通知通道配置

#### 7. `client/app/src/main/java/com/remote/app/LocManager.java` (需要查看)
- [ ] 处理 Android 10+ 后台定位限制
- [ ] 添加前台服务检查

#### 8. `client/app/src/main/java/com/remote/app/FileManager.java` (需要查看)
- [ ] 使用 SAF (Storage Access Framework)
- [ ] 处理 Android 11+ 分区存储

#### 9. 新增: `client/app/src/main/java/com/remote/app/Constants.java`
- [ ] 服务器地址配置
- [ ] API 路径常量

#### 10. 服务端 - `server/includes/const.js`
- [ ] 添加更新日志提示客户端必须升级

## 小米系统特殊适配

小米 MIUI 系统有额外的限制：

1. **后台限制** - 需要在电池管理中加白名单
   - 应用名称 (`app_name`)
   - 联系方式 (manifest)

2. **权限控制** - MIUI 有额外权限层
   - 定位权限需要二次确认
   - 通话记录权限特殊处理

3. **WebSocket** - 可能被网络过滤
   - 需要降级到 HTTP Long Polling

## 升级步骤（优先级）

### 第1阶段（紧急）
1. 更新 `client/build.gradle` - Gradle 版本升级
2. 修复 `IOSocket.java` - 硬编码地址和 TLS 支持
3. 修复 `MainActivity.java` - 权限申请逻辑

### 第2阶段（重要）
4. 更新 `AndroidManifest.xml` - 权限声明规范化
5. 创建 `Constants.java` - 配置管理
6. 优化 `FileManager.java` - 存储权限兼容性

### 第3阶段（完善）
7. 测试小米 MIUI 系统
8. 配置反向代理处理 WebSocket
9. 添加容错和日志

## 详细修复代码

见下面的文件修改部分。

