#!/bin/bash

# WeBlog AWS 部署脚本
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目配置
PROJECT_NAME="weblog"
AWS_REGION="us-west-2"

echo -e "${GREEN}开始 WeBlog AWS 部署流程...${NC}"

# 检查必要工具
echo -e "${YELLOW}检查必要工具...${NC}"
command -v aws >/dev/null 2>&1 || { echo -e "${RED}请先安装 AWS CLI${NC}" >&2; exit 1; }
command -v terraform >/dev/null 2>&1 || { echo -e "${RED}请先安装 Terraform${NC}" >&2; exit 1; }
command -v docker >/dev/null 2>&1 || { echo -e "${RED}请先安装 Docker${NC}" >&2; exit 1; }

# 第一步：初始化 Terraform
echo -e "${YELLOW}1. 初始化 Terraform...${NC}"
cd terraform
terraform init

# 第二步：规划部署
echo -e "${YELLOW}2. 规划 Terraform 部署...${NC}"
terraform plan

# 第三步：部署基础设施
echo -e "${YELLOW}3. 部署 AWS 基础设施...${NC}"
terraform apply -auto-approve

# 获取 ECR 仓库 URL
FRONTEND_REPO=$(terraform output -raw ecr_frontend_repository_url)
BACKEND_REPO=$(terraform output -raw ecr_backend_repository_url)

echo -e "${GREEN}基础设施部署完成！${NC}"
echo -e "前端 ECR 仓库: ${FRONTEND_REPO}"
echo -e "后端 ECR 仓库: ${BACKEND_REPO}"

cd ..

# 第四步：登录 ECR
echo -e "${YELLOW}4. 登录 ECR...${NC}"
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $FRONTEND_REPO

# 第五步：构建并推送前端镜像
echo -e "${YELLOW}5. 构建并推送前端镜像...${NC}"
cd WeBlog-frontend
docker build -t $PROJECT_NAME-frontend .
docker tag $PROJECT_NAME-frontend:latest $FRONTEND_REPO:latest
docker push $FRONTEND_REPO:latest
cd ..

# 第六步：构建并推送后端镜像
echo -e "${YELLOW}6. 构建并推送后端镜像...${NC}"
cd WeBlog_backend
docker build -f Dockerfile.prod -t $PROJECT_NAME-backend .
docker tag $PROJECT_NAME-backend:latest $BACKEND_REPO:latest
docker push $BACKEND_REPO:latest
cd ..

# 第七步：更新 ECS 服务
echo -e "${YELLOW}7. 更新 ECS 服务...${NC}"
aws ecs update-service --cluster $PROJECT_NAME-cluster --service $PROJECT_NAME-frontend-service --force-new-deployment --region $AWS_REGION
aws ecs update-service --cluster $PROJECT_NAME-cluster --service $PROJECT_NAME-backend-service --force-new-deployment --region $AWS_REGION

# 获取负载均衡器 URL
cd terraform
LOAD_BALANCER_URL=$(terraform output -raw load_balancer_url)
cd ..

echo -e "${GREEN}==================================${NC}"
echo -e "${GREEN}部署完成！${NC}"
echo -e "${GREEN}==================================${NC}"
echo -e "应用 URL: ${LOAD_BALANCER_URL}"
echo -e "${YELLOW}注意：服务可能需要几分钟才能完全启动${NC}"
echo -e "${YELLOW}可以通过 AWS Console 查看 ECS 服务状态${NC}" 