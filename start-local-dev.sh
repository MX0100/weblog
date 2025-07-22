#!/bin/bash

# ======================================
# WeBlog 本地开发环境一键启动脚本
# ======================================

set -e

echo "🚀 启动WeBlog本地开发环境..."

# 进入后端目录
cd WeBlog_backend

# 启动后端服务
echo "📦 启动后端服务..."
docker-compose -f compose.yaml -f docker-compose.dev.yml up -d

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
echo "🔍 检查服务状态..."
docker-compose ps

# 检查应用健康状态
echo "💊 检查应用健康状态..."
for i in {1..10}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ 后端服务启动成功！"
        break
    else
        echo "⏳ 等待后端服务启动... ($i/10)"
        sleep 3
    fi
done

# 显示访问信息
echo ""
echo "🎉 本地开发环境启动完成！"
echo ""
echo "📍 服务访问地址："
echo "   🌐 前端应用: http://localhost:5173 (需要手动启动: cd WeBlog-frontend && npm run dev)"
echo "   🔧 后端API: http://localhost:8080"
echo "   💊 健康检查: http://localhost:8080/actuator/health"
echo "   🗄️ 数据库: localhost:5432 (weblog/password)"
echo "   🔍 pgAdmin: http://localhost:5050 (admin@weblog.dev/admin)"
echo ""
echo "👥 测试账户："
echo "   alice/123456, bob/123456, charlie/123456"
echo "   diana/123456, edward/123456, fiona/123456"
echo ""
echo "🛠️ 常用命令："
echo "   查看日志: docker-compose -f compose.yaml -f docker-compose.dev.yml logs -f weblog-app"
echo "   停止服务: docker-compose -f compose.yaml -f docker-compose.dev.yml down"
echo "   重启服务: docker-compose -f compose.yaml -f docker-compose.dev.yml restart"
echo ""

# 返回根目录
cd ..

echo "准备启动前端? (y/n)"
read -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🌐 启动前端开发服务器..."
    cd WeBlog-frontend
    
    # 检查是否需要安装依赖
    if [ ! -d "node_modules" ]; then
        echo "📦 安装前端依赖..."
        npm install
    fi
    
    echo "🚀 启动前端服务器..."
    npm run dev
fi 