# XploitSPYpro 2026 版本 - GitHub 上传脚本
# 功能：初始化 Git 仓库，配置远程，提交代码，推送到 GitHub

# ========== 配置信息 ==========
$GITHUB_REPO = "https://github.com/dg49888/XploitSPYpro20260510.git"
$COMMIT_MESSAGE = "Update: XploitSPYpro 2026 version with Android 14 compatibility"
$PROJECT_DIR = Get-Location

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "XploitSPYpro 2026 - GitHub 上传脚本" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ========== 检查项目目录 ==========
Write-Host "[1/6] 检查项目目录..." -ForegroundColor Yellow
if (-Not (Test-Path "$PROJECT_DIR\client") -or -Not (Test-Path "$PROJECT_DIR\server")) {
    Write-Host "错误：找不到 client 或 server 目录，请确保在项目根目录运行此脚本" -ForegroundColor Red
    exit 1
}
Write-Host "✓ 项目结构正确" -ForegroundColor Green
Write-Host ""

# ========== 初始化 Git 仓库 ==========
Write-Host "[2/6] 初始化 Git 仓库..." -ForegroundColor Yellow
if (Test-Path "$PROJECT_DIR\.git") {
    Write-Host "✓ Git 仓库已存在，跳过初始化" -ForegroundColor Green
} else {
    git init
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Git 仓库初始化成功" -ForegroundColor Green
    } else {
        Write-Host "错误：Git 初始化失败，请确保已安装 Git" -ForegroundColor Red
        exit 1
    }
}
Write-Host ""

# ========== 配置 .gitignore ==========
Write-Host "[3/6] 配置 .gitignore..." -ForegroundColor Yellow
$gitignore_content = @"
# Gradle
.gradle/
build/
*.apk
*.aar
*.jks

# Android Studio
.idea/
*.iml
*.iws
*.ipr
local.properties

# VS Code
.vscode/

# macOS
.DS_Store

# Node
node_modules/
npm-debug.log
yarn-error.log

# 备份文件
*.bak
*.backup
*.old

# 密钥和敏感信息
*.key
*.pem
maindb.json

# 构建输出
output/
dist/
*.log
"@

$gitignore_content | Out-File -FilePath "$PROJECT_DIR\.gitignore" -Encoding UTF8 -Force
Write-Host "✓ .gitignore 已配置" -ForegroundColor Green
Write-Host ""

# ========== 配置远程仓库 ==========
Write-Host "[4/6] 配置远程仓库..." -ForegroundColor Yellow
$existing_remote = git remote get-url origin 2>$null
if ($existing_remote -eq $GITHUB_REPO) {
    Write-Host "✓ 远程仓库已配置" -ForegroundColor Green
} else {
    git remote remove origin 2>$null
    git remote add origin $GITHUB_REPO
    Write-Host "✓ 远程仓库已配置: $GITHUB_REPO" -ForegroundColor Green
}
Write-Host ""

# ========== 添加所有文件 ==========
Write-Host "[5/6] 添加项目文件到暂存区..." -ForegroundColor Yellow
git add -A
Write-Host "✓ 文件已添加" -ForegroundColor Green

# 显示要提交的文件数量
$file_count = git diff --cached --name-only | Measure-Object | Select-Object -ExpandProperty Count
Write-Host "  准备提交 $file_count 个文件" -ForegroundColor Cyan
Write-Host ""

# ========== 提交代码 ==========
Write-Host "[6/6] 提交代码并推送到 GitHub..." -ForegroundColor Yellow

# 检查是否有未提交的更改
$status = git status --short
if ($status) {
    git commit -m $COMMIT_MESSAGE
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 代码已提交" -ForegroundColor Green
    } else {
        Write-Host "错误：代码提交失败" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "⚠ 没有新的更改需要提交" -ForegroundColor Yellow
}

# 推送到 GitHub
Write-Host ""
Write-Host "正在推送到 GitHub..." -ForegroundColor Cyan
Write-Host "（首次推送可能需要认证）" -ForegroundColor Gray

git push -u origin master 2>&1
$push_exit_code = $LASTEXITCODE

if ($push_exit_code -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✓ 上传成功！" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "仓库地址: $GITHUB_REPO" -ForegroundColor Cyan
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "✗ 推送失败" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "可能的原因:" -ForegroundColor Yellow
    Write-Host "1. GitHub 认证失败 - 请检查 SSH key 或 Personal Access Token" -ForegroundColor Gray
    Write-Host "2. 分支名称 - 尝试推送到 'main' 分支: git push -u origin main" -ForegroundColor Gray
    Write-Host "3. 网络问题 - 检查网络连接" -ForegroundColor Gray
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "提交详情:" -ForegroundColor Cyan
git log -1 --oneline
Write-Host ""
Write-Host "完成！按 Enter 键关闭窗口..."
Read-Host
