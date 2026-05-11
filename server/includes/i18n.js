/*
 * i18n - 多语言支持模块
 * 支持英文和简体中文
 */

const fs = require('fs');
const path = require('path');

// 加载语言文件
const locales = {};
const localesDir = path.join(__dirname, '..', 'locales');

// 自动加载所有语言文件
fs.readdirSync(localesDir).forEach(file => {
    if (file.endsWith('.json')) {
        const lang = path.basename(file, '.json');
        locales[lang] = JSON.parse(fs.readFileSync(path.join(localesDir, file), 'utf8'));
        console.log(`[i18n] 加载语言: ${lang}`);
    }
});

// i18n 中间件
function i18nMiddleware(req, res, next) {
    // 从查询参数、cookie 或请求头获取语言设置
    let lang = req.query.lang || req.cookies?.lang || req.headers['accept-language']?.split(',')[0]?.split('-')[0];
    
    // 验证语言是否支持，默认使用英语
    if (!locales[lang]) {
        lang = 'en';
    }
    
    // 将语言设置保存到请求对象
    req.locale = lang;
    req.i18n = locales[lang];
    
    // 翻译函数
    req.t = function(key) {
        const keys = key.split('.');
        let value = locales[lang];
        for (const k of keys) {
            value = value?.[k];
            if (!value) break;
        }
        return value || key;
    };
    
    // 设置 cookie（如果通过查询参数切换）
    if (req.query.lang) {
        res.cookie('lang', lang, { maxAge: 30 * 24 * 60 * 60 * 1000 }); // 30天
    }
    
    // 将 i18n 传递给视图
    res.locals.locale = lang;
    res.locals.i18n = locales[lang];
    res.locals.t = req.t;
    res.locals.availableLangs = Object.keys(locales).map(code => ({
        code,
        name: locales[code].lang
    }));
    
    next();
}

module.exports = i18nMiddleware;
module.exports.locales = locales;
