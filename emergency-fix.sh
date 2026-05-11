#!/bin/bash
# 紧急修复 - 完整版

cd /home/test2/XploitSPYpro-main202605100042/XploitSPYpro-main/server/app/factory

echo "=== 当前目录文件 ==="
ls -la *.apk 2>/dev/null || echo "无 APK 文件"

echo ""
echo "=== 步骤1: 检查原APK ==="
if [ ! -f "app-release-old.apk" ]; then
    echo "错误: 未找到 app-release-old.apk"
    echo "请先执行: mv app-release.apk app-release-old.apk"
    exit 1
fi

echo ""
echo "=== 步骤2: 查找签名后的APK ==="
SIGNED_APK=$(ls -t *signed*.apk 2>/dev/null | head -1)
if [ -z "$SIGNED_APK" ]; then
    echo "未找到签名后的APK，尝试其他模式..."
    SIGNED_APK=$(ls -t app-release-*.apk 2>/dev/null | grep -v "old" | head -1)
fi

if [ -z "$SIGNED_APK" ]; then
    echo "错误: 未找到任何新生成的APK"
    echo "当前目录文件:"
    ls -la
    exit 1
fi

echo "找到签名APK: $SIGNED_APK"

echo ""
echo "=== 步骤3: 替换为新的APK模板 ==="
mv "$SIGNED_APK" app-release.apk
echo "✓ 已重命名为 app-release.apk"

echo ""
echo "=== 步骤4: 更新decompiled目录 ==="
rm -rf decompiled
java -jar apktool.jar d -f -o decompiled app-release.apk
if [ $? -eq 0 ]; then
    echo "✓ decompiled目录更新成功"
else
    echo "✗ decompiled更新失败"
    exit 1
fi

echo ""
echo "=== 步骤5: 清理临时文件 ==="
rm -rf temp_decompiled app-release-fixed.apk app-release-old.apk 2>/dev/null
echo "✓ 清理完成"

echo ""
echo "=== 步骤6: 验证新APK模板 ==="
echo "检查APK中的URL:"
unzip -p app-release.apk classes.dex | strings | grep "http://" | head -3

echo ""
echo "=========================================="
echo "修复完成! 请重启服务器:"
echo "  cd /home/test2/XploitSPYpro-main202605100042/XploitSPYpro-main/server"
echo "  PORT=8080 node index.js"
echo "=========================================="
