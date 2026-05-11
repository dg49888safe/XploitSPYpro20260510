# XploitSPY 中英双语支持

**更新日期**: 2026年5月11日  
**提交**: `83f582a`

---

## 功能概述

项目现已支持 **中文** 和 **英文** 双语切换，方便不同语言背景的用户使用。

## 切换语言方法

### 1. 网站导航栏切换（推荐）

所有后台管理页面右上角都有语言切换按钮：
- 🌐 English / 简体中文

点击即可在中文和英文之间切换，选择会自动保存在 cookie 中（30天有效）。

### 2. URL 参数切换

在任意页面 URL 后添加 `?lang=zh` 或 `?lang=en`：

```
# 中文版本
http://your-server:8080/?lang=zh
http://your-server:8080/login?lang=zh
http://your-server:8080/builder?lang=zh

# 英文版本（默认）
http://your-server:8080/?lang=en
http://your-server:8080/login?lang=en
```

### 3. 自动检测

系统会根据浏览器语言自动选择：
- 中文浏览器 → 显示简体中文
- 其他语言 → 显示英文

---

## 支持多语言的页面

| 页面 | 路径 | 支持状态 |
|------|------|----------|
| 首页/免责声明 | `/` | ✅ 完整支持 |
| 登录页 | `/login` | ✅ 完整支持 |
| 设备列表 | `/panel` | ✅ 完整支持 |
| APK构建 | `/builder` | ✅ 完整支持 |
| 设备管理 | `/manage/:id/*` | ✅ 完整支持 |
| 修改密码 | `/changepass` | ✅ 完整支持 |
| 事件日志 | `/logs` | ✅ 完整支持 |

---

## 技术实现

### 核心文件

```
server/
├── includes/
│   └── i18n.js          # 多语言中间件
├── locales/
│   ├── en.json          # 英文语言包
│   └── zh.json          # 中文语言包
├── assets/views/
│   └── partials/
│       ├── head_i18n.ejs      # 国际化头部
│       └── header_i18n.ejs    # 国际化导航栏
```

### 语言包结构

```json
{
  "lang": "语言名称",
  "nav": {
    "devices": "设备管理",
    "apkBuilder": "APK构建",
    ...
  },
  "welcome": {
    "title": "标题",
    "subtitle": "副标题",
    ...
  },
  "login": {
    "title": "登录标题",
    "username": "用户名",
    ...
  }
}
```

### 在模板中使用

```ejs
<!-- 使用翻译函数 -->
<%= t('nav.devices') %>
<%= t('welcome.title') %>

<!-- 条件判断 -->
<%= locale === 'zh' ? '中文文本' : 'English Text' %>

<!-- 切换链接 -->
<a href="?lang=<%= locale === 'en' ? 'zh' : 'en' %>">
    🌐 <%= locale === 'en' ? '简体中文' : 'English' %>
</a>
```

---

## 添加新语言

1. 在 `server/locales/` 创建新语言文件，如 `fr.json`
2. 复制 `en.json` 内容并翻译
3. 重启服务器即可自动加载

---

## 服务器端更新步骤

```bash
cd /home/XploitSPYpro20260510-main
git pull

# 重启服务
PORT=8080 node server/index.js
```

---

## 注意事项

1. **首次访问**：默认显示英文
2. **语言持久化**：切换后保存在 cookie 中，30天内有效
3. **设备页面**：管理菜单保持英文（设备功能名称）
4. **构建日志**：根据语言显示不同的加载文本

---

**贡献**：欢迎提交更多语言翻译！
