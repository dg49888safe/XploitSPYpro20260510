#!/bin/bash
# 自动修复 apktool.yml 配置脚本

PROJECT_DIR="/home/XploitSPYpro-main20260510-main"
DEcompiled_DIR="$PROJECT_DIR/server/app/factory/decompiled"
APKTOOL_YML="$DEcompiled_DIR/apktool.yml"

echo "=========================================="
echo "修复 apktool.yml 配置"
echo "=========================================="

# 检查目录
if [ ! -d "$DEcompiled_DIR" ]; then
    echo "错误: decompiled 目录不存在: $DEcompiled_DIR"
    exit 1
fi

cd "$DEcompiled_DIR"

# 备份原文件
if [ -f "$APKTOOL_YML" ]; then
    cp "$APKTOOL_YML" "$APKTOOL_YML.backup.$(date +%Y%m%d%H%M%S)"
    echo "已备份原文件"
fi

# 写入正确的配置
cat > "$APKTOOL_YML" << 'EOF'
!!brut.androlib.meta.MetaInfo
apkFileName: app-release.apk
compressionType: false
doNotCompress:
- resources.arsc
- png
isFrameworkApk: false
packageInfo:
  forcedPackageId: '127'
  renameManifestPackage: null
sdkInfo:
  minSdkVersion: '21'
  targetSdkVersion: '34'
sharedLibrary: false
sparseResources: false
unknownFiles: {}
usesFramework:
  ids:
  - 1
  tag: null
version: 2.10.0
versionInfo:
  versionCode: 3
  versionName: '3.0'
EOF

echo ""
echo "✓ apktool.yml 已更新"
echo ""
echo "当前配置:"
echo "------------------------------------------"
grep -E "(minSdkVersion|targetSdkVersion|versionCode|versionName|version:)" "$APKTOOL_YML" | head -6
echo "------------------------------------------"
echo ""
echo "配置说明:"
echo "  - minSdkVersion: '21'   (最低 Android 5.0)"
echo "  - targetSdkVersion: '34' (支持 Android 14)"
echo "  - versionCode: 3        (整数格式)"
echo "  - versionName: '3.0'"
echo "  - ApkTool: 2.10.0"
echo ""
echo "=========================================="
echo "修复完成!"
echo "=========================================="
echo ""
echo "下一步: 重启服务器并测试构建"
echo "  cd $PROJECT_DIR/server"
echo "  PORT=8080 node index.js"
