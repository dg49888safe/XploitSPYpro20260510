# Windows 部署指南

## 优势
- 更易于本地调试和测试
- 可以直接修改代码并立即生效
- 方便查看日志和排查问题

---

## 部署步骤

### 1. 安装 Node.js

```powershell
# 方法1: 官网下载安装
# 访问 https://nodejs.org/ 下载 LTS 版本 (推荐 18.x 或 16.x)

# 方法2: 使用 nvm-windows
# 下载安装 nvm: https://github.com/coreybutler/nvm-windows
nvm install 18
nvm use 18
```

### 2. 安装 Java (ApkTool 需要)

```powershell
# 下载 OpenJDK 11 或 17
# https://adoptium.net/

# 验证安装
java -version
```

### 3. 启动服务器

```powershell
# 进入项目目录
cd C:\Users\Administrator\Downloads\XploitSPYpro-main202605100042\XploitSPYpro-main\server

# 安装依赖
npm install

# 启动服务 (端口 8080)
$env:PORT=8080
node index.js
```

### 4. 访问 Web 界面

浏览器打开: `http://localhost:8080`

---

## 调试技巧

### 实时查看日志

```powershell
# PowerShell 窗口保持开启，可以看到所有调试输出
```

### 修改代码自动生效

```powershell
# 按 Ctrl+C 停止服务
# 修改代码后重新运行
node index.js
```

### 检查 APK 模板

```powershell
cd server\app\factory

# 反编译检查
java -jar apktool.jar d -f -o temp_check app-release.apk

# 查看 URL
Select-String -Path "temp_check\smali\com\remote\app\h.smali" -Pattern "http://"

# 清理
Remove-Item -Recurse -Force temp_check
```

---

## 常见问题

### 1. 端口被占用

```powershell
# 查找占用 8080 的进程
netstat -ano | findstr :8080

# 使用其他端口
$env:PORT=3000
node index.js
```

### 2. 防火墙阻止

```powershell
# 添加防火墙规则 (管理员权限)
netsh advfirewall firewall add rule name="XploitSPY" dir=in action=allow protocol=TCP localport=8080
```

---

## 验证 APK 构建

```powershell
# 构建后检查生成的 APK
cd server\assets\webpublic

# 使用 7-Zip 或 PowerShell 解压检查
Expand-Archive -Path build-aligned-signed.apk -DestinationPath temp_apk -Force

# 检查 classes.dex 中的 URL
# 或者上传到手机安装测试
```
