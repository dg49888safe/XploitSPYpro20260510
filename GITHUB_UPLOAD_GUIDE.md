# XploitSPYpro 2026 - GitHub 上传指南

## 快速上传（推荐）

### 方法 1：使用 PowerShell 脚本（最简单）

1. **在项目根目录打开 PowerShell**
   - 在项目根目录右键 → "在此处打开 PowerShell"

2. **执行上传脚本**
   ```powershell
   .\upload_to_github.ps1
   ```

3. **按提示操作**
   - 脚本会自动初始化 Git、配置远程、添加文件、提交、推送
   - 首次推送时可能需要输入 GitHub 认证信息

4. **验证上传成功**
   - 访问 https://github.com/dg49888/XploitSPYpro20260510
   - 确认看到你的代码

---

## 手动上传（如果脚本失败）

### 前置条件
- 已安装 Git（https://git-scm.com）
- 已配置 GitHub 认证（SSH 密钥或 Personal Access Token）

### 步骤

1. **打开项目根目录的 PowerShell/CMD**

2. **初始化 Git 仓库**
   ```powershell
   git init
   ```

3. **配置 Git 用户信息**
   ```powershell
   git config user.name "Your Name"
   git config user.email "your.email@example.com"
   ```

4. **添加远程仓库**
   ```powershell
   git remote add origin https://github.com/dg49888/XploitSPYpro20260510.git
   ```

5. **添加所有文件**
   ```powershell
   git add -A
   ```

6. **提交代码**
   ```powershell
   git commit -m "Update: XploitSPYpro 2026 version with Android 14 compatibility"
   ```

7. **推送到 GitHub**
   ```powershell
   git push -u origin master
   ```
   
   或者（如果默认分支是 main）：
   ```powershell
   git push -u origin main
   ```

---

## 常见问题解决

### 问题 1：认证失败
**症状**：`fatal: Authentication failed`

**解决方案**：
- 使用 SSH 密钥：先配置 GitHub SSH 密钥
- 或者使用 Personal Access Token（推荐）：
  1. GitHub → Settings → Developer settings → Personal access tokens
  2. 生成新 token，勾选 `repo` 权限
  3. 使用 token 作为密码推送

### 问题 2：分支名称错误
**症状**：`error: src refspec main does not match any`

**解决方案**：
```powershell
# 检查当前分支
git branch

# 如果只有 master，推送到 master
git push -u origin master

# 如果只有 main，推送到 main
git push -u origin main
```

### 问题 3：远程仓库已存在
**症状**：`fatal: remote origin already exists`

**解决方案**：
```powershell
# 删除旧的远程配置
git remote remove origin

# 重新添加
git remote add origin https://github.com/dg49888/XploitSPYpro20260510.git
```

### 问题 4：文件过大被拒绝
**症状**：`error: File too large`

**解决方案**：
- 编辑 `.gitignore` 排除大文件（如 `.apk`, `build/`）
- 或使用 Git LFS

---

## 使用 Windsurf 上传（如果有集成）

如果 Windsurf 有 GitHub 集成功能：

1. 打开源代码管理面板
2. 初始化仓库 → 配置远程
3. 暂存所有文件 → 提交
4. 推送到远程

具体步骤依赖你的 Windsurf 版本。

---

## 验证上传完成

1. 访问 https://github.com/dg49888/XploitSPYpro20260510
2. 确认以下内容存在：
   - `client/` 目录（Android 项目）
   - `server/` 目录（Node.js 服务端）
   - `INSTALL.md`（安装说明）
   - `README.md`（项目说明）

3. 查看提交历史：
   ```powershell
   git log --oneline
   ```

---

## 后续维护

### 推送新的更改
```powershell
git add -A
git commit -m "Your commit message"
git push origin master
```

### 查看状态
```powershell
git status
```

### 查看提交历史
```powershell
git log --oneline -10
```

---

有任何问题，请参考 Git 官方文档：https://git-scm.com/doc

