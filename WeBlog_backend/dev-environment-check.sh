#!/bin/bash

# ======================================
# 开发环境健康检查脚本
# ======================================
# 检查后端、前端、数据库、PgAdmin等所有服务

echo "🔍 开始检查WeBlog开发环境..."
echo "========================================"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查结果统计
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

# 检查函数
check_service() {
    local service_name="$1"
    local check_command="$2"
    local expected_result="$3"
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -e "\n${BLUE}🔍 检查 $service_name...${NC}"
    
    if eval "$check_command" >/dev/null 2>&1; then
        echo -e "${GREEN}✅ $service_name 正常${NC}"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    else
        echo -e "${RED}❌ $service_name 异常${NC}"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        return 1
    fi
}

# 检查HTTP服务
check_http_service() {
    local service_name="$1"
    local url="$2"
    local expected_status="$3"
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -e "\n${BLUE}🔍 检查 $service_name ($url)...${NC}"
    
    local status_code=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
    
    if [ "$status_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ $service_name 正常 (状态码: $status_code)${NC}"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    else
        echo -e "${RED}❌ $service_name 异常 (状态码: $status_code, 期望: $expected_status)${NC}"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        return 1
    fi
}

echo "========================================"
echo "🐳 Docker 服务检查"
echo "========================================"

# 1. 检查Docker是否运行
check_service "Docker 守护进程" "docker info"

# 2. 检查Docker Compose是否可用
check_service "Docker Compose" "docker-compose --version"

# 3. 检查容器状态
echo -e "\n${BLUE}📦 检查容器状态...${NC}"
docker-compose -f compose.yaml -f docker-compose.dev.yml ps

echo "========================================"
echo "🗄️ 数据库服务检查"
echo "========================================"

# 4. 检查PostgreSQL容器
check_service "PostgreSQL 容器" "docker-compose -f compose.yaml -f docker-compose.dev.yml exec -T postgres pg_isready -U weblog"

# 5. 检查数据库连接
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${BLUE}🔍 检查数据库连接...${NC}"
if docker-compose -f compose.yaml -f docker-compose.dev.yml exec -T postgres psql -U weblog -d weblog -c "SELECT 1;" >/dev/null 2>&1; then
    echo -e "${GREEN}✅ 数据库连接正常${NC}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
else
    echo -e "${RED}❌ 数据库连接失败${NC}"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
fi

# 6. 检查数据库表
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${BLUE}🔍 检查数据库表结构...${NC}"
TABLES=$(docker-compose -f compose.yaml -f docker-compose.dev.yml exec -T postgres psql -U weblog -d weblog -t -c "SELECT tablename FROM pg_tables WHERE schemaname = 'public';" 2>/dev/null | wc -l)
if [ "$TABLES" -gt 0 ]; then
    echo -e "${GREEN}✅ 数据库表存在 ($TABLES 个表)${NC}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
    # 显示表列表
    echo -e "${YELLOW}📋 数据库表列表:${NC}"
    docker-compose -f compose.yaml -f docker-compose.dev.yml exec -T postgres psql -U weblog -d weblog -c "\dt"
else
    echo -e "${RED}❌ 没有发现数据库表${NC}"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
fi

echo "========================================"
echo "☕ 后端服务检查"
echo "========================================"

# 7. 检查后端容器
check_service "后端容器" "docker-compose -f compose.yaml -f docker-compose.dev.yml ps weblog-app | grep -q 'Up'"

# 8. 检查后端健康检查
check_http_service "后端健康检查" "http://localhost:8080/actuator/health" "200"

# 9. 检查后端API端点
check_http_service "后端API根路径" "http://localhost:8080/" "200"

# 10. 检查Spring Boot Actuator端点
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${BLUE}🔍 检查Spring Boot Actuator端点...${NC}"
ENDPOINTS=$(curl -s http://localhost:8080/actuator 2>/dev/null | grep -o '"[^"]*"' | wc -l)
if [ "$ENDPOINTS" -gt 0 ]; then
    echo -e "${GREEN}✅ Actuator端点正常 ($ENDPOINTS 个端点)${NC}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
else
    echo -e "${RED}❌ Actuator端点异常${NC}"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
fi

echo "========================================"
echo "🌐 前端服务检查"
echo "========================================"

# 11. 检查Node.js
check_service "Node.js" "node --version"

# 12. 检查npm
check_service "npm" "npm --version"

# 13. 检查前端依赖
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${BLUE}🔍 检查前端依赖...${NC}"
cd ../WeBlog-frontend
if [ -d "node_modules" ] && [ -f "package-lock.json" ]; then
    echo -e "${GREEN}✅ 前端依赖已安装${NC}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
else
    echo -e "${RED}❌ 前端依赖未安装${NC}"
    echo -e "${YELLOW}💡 请运行: cd WeBlog-frontend && npm install${NC}"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
fi
cd ../WeBlog_backend

echo "========================================"
echo "🔧 PgAdmin4 检查"
echo "========================================"

# 14. 检查PgAdmin4服务状态
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${BLUE}🔍 检查PgAdmin4容器状态...${NC}"
if docker-compose -f compose.yaml -f docker-compose.dev.yml --profile tools ps pgadmin | grep -q 'Up'; then
    echo -e "${GREEN}✅ PgAdmin4 容器运行中${NC}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
    
    # 检查PgAdmin4 Web界面
    check_http_service "PgAdmin4 Web界面" "http://localhost:5050" "200"
else
    echo -e "${YELLOW}⚠️ PgAdmin4 未启动 (需要使用 --profile tools)${NC}"
    echo -e "${YELLOW}💡 启动命令: docker-compose -f compose.yaml -f docker-compose.dev.yml --profile tools up -d pgadmin${NC}"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
fi

echo "========================================"
echo "🔗 网络连通性检查"
echo "========================================"

# 15. 检查容器间网络
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${BLUE}🔍 检查容器间网络连通性...${NC}"
if docker-compose -f compose.yaml -f docker-compose.dev.yml exec -T weblog-app ping -c 1 postgres >/dev/null 2>&1; then
    echo -e "${GREEN}✅ 后端到数据库网络正常${NC}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
else
    echo -e "${RED}❌ 后端到数据库网络异常${NC}"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
fi

echo "========================================"
echo "📋 检查总结"
echo "========================================"

echo -e "总检查项: ${BLUE}$TOTAL_CHECKS${NC}"
echo -e "通过: ${GREEN}$PASSED_CHECKS${NC}"
echo -e "失败: ${RED}$FAILED_CHECKS${NC}"

if [ $FAILED_CHECKS -eq 0 ]; then
    echo -e "\n${GREEN}🎉 所有检查通过！开发环境运行正常！${NC}"
    exit 0
else
    echo -e "\n${RED}⚠️ 发现 $FAILED_CHECKS 个问题，请检查上述失败项${NC}"
    exit 1
fi 