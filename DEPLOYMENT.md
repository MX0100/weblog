# WeBlog AWS 部署指南

这是一个完整的生产级 AWS 部署指南，包含所有优化功能：NAT Gateway、RDS 数据库、CloudFront CDN、监控告警和 CI/CD 管道。

## 🏗️ 架构概述

### 基础设施组件

- **VPC** - 10.0.0.0/16 网络，包含公有和私有子网
- **NAT Gateway** - 为私有子网提供出站网络访问
- **Application Load Balancer** - 请求分发和健康检查
- **ECS Fargate** - 无服务器容器运行时
- **RDS PostgreSQL** - 托管数据库服务
- **ECR** - Docker 镜像仓库
- **CloudFront** - 全球内容分发网络
- **CloudWatch** - 监控、日志和告警
- **Secrets Manager** - 安全凭证管理

### 网络架构

```
Internet Gateway
        |
   Public Subnets (ALB)
        |
   Private Subnets (ECS Tasks, RDS)
        |
   NAT Gateway (出站访问)
```

## 🚀 部署选项

### 1. 一键部署（推荐用于测试）

```bash
# 给脚本执行权限
chmod +x deploy.sh

# 运行一键部署脚本
./deploy.sh
```

### 2. 多环境部署（推荐用于生产）

```bash
# 给脚本执行权限
chmod +x deploy-multi-env.sh

# 部署开发环境
./deploy-multi-env.sh dev

# 部署 staging 环境
./deploy-multi-env.sh staging

# 部署生产环境
./deploy-multi-env.sh prod

# 只查看计划（不执行）
./deploy-multi-env.sh prod plan

# 销毁环境
./deploy-multi-env.sh dev destroy
```

### 3. 手动分步部署

```bash
# 1. 配置 AWS 凭证
aws configure

# 2. 初始化 Terraform
cd terraform
terraform init

# 3. 选择工作区（环境）
terraform workspace select prod || terraform workspace new prod

# 4. 查看部署计划
terraform plan -var-file="environments/prod.tfvars"

# 5. 部署基础设施
terraform apply -var-file="environments/prod.tfvars"

# 6. 获取 ECR 仓库信息
FRONTEND_REPO=$(terraform output -raw ecr_frontend_repository_url)
BACKEND_REPO=$(terraform output -raw ecr_backend_repository_url)

# 7. 登录 ECR
aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin $FRONTEND_REPO

# 8. 构建并推送镜像
cd ../WeBlog-frontend
docker build -t weblog-frontend .
docker tag weblog-frontend:latest $FRONTEND_REPO:latest
docker push $FRONTEND_REPO:latest

cd ../WeBlog_backend
docker build -f Dockerfile.prod -t weblog-backend .
docker tag weblog-backend:latest $BACKEND_REPO:latest
docker push $BACKEND_REPO:latest

# 9. 更新 ECS 服务
aws ecs update-service --cluster weblog-cluster --service weblog-frontend-service --force-new-deployment --region us-west-2
aws ecs update-service --cluster weblog-cluster --service weblog-backend-service --force-new-deployment --region us-west-2
```

## 🔧 环境配置

### 环境配置文件

每个环境都有独立的配置文件：

- `terraform/environments/dev.tfvars` - 开发环境
- `terraform/environments/staging.tfvars` - 测试环境
- `terraform/environments/prod.tfvars` - 生产环境

### 主要配置项

```hcl
project_name = "weblog"
environment  = "prod"
aws_region   = "us-west-2"

# 数据库配置
db_name           = "weblog_prod"
db_username       = "weblog_admin"
db_instance_class = "db.t3.medium"

# 告警邮箱
alert_email = "alerts@company.com"
```

## 🔄 CI/CD 管道

### GitHub Actions 工作流

1. **持续集成** (`.github/workflows/ci.yml`)

   - 代码检查和测试
   - 安全扫描
   - 构建验证

2. **开发环境部署** (`.github/workflows/deploy-dev.yml`)

   - 自动部署到开发环境
   - 在 `develop` 分支推送时触发

3. **生产环境部署** (`.github/workflows/deploy-prod.yml`)
   - 生产环境部署
   - 在 `main` 分支推送或发布时触发
   - 包含安全检查和健康检查

### 设置 GitHub Secrets

在 GitHub 仓库设置中添加以下 Secrets：

```
AWS_ACCESS_KEY_ID - AWS 访问密钥 ID
AWS_SECRET_ACCESS_KEY - AWS 秘密访问密钥
```

### 分支策略

- `develop` - 开发分支，自动部署到开发环境
- `main` - 主分支，用于生产环境部署
- `feature/*` - 功能分支，触发 CI 检查

## 📊 监控和告警

### CloudWatch 告警

系统自动配置以下告警：

- ALB 目标健康状态
- ALB 响应时间过长
- ECS CPU 和内存使用率过高
- RDS CPU 使用率和连接数
- RDS 存储空间不足

### 监控面板

- **CloudWatch Dashboard** - 实时性能指标
- **ECS 服务监控** - 容器健康状态
- **RDS 性能洞察** - 数据库性能分析

### 日志管理

- **应用日志** - CloudWatch Logs
- **访问日志** - ALB 访问日志
- **容器日志** - ECS 任务日志

## 🔒 安全配置

### 网络安全

- 私有子网运行应用容器
- NAT Gateway 控制出站流量
- 安全组限制端口访问
- VPC 隔离网络流量

### 数据安全

- RDS 加密存储
- Secrets Manager 管理数据库密码
- ECR 镜像安全扫描
- CloudFront HTTPS 强制重定向

### 访问控制

- IAM 角色最小权限原则
- ECS 任务执行角色
- RDS 增强监控角色

## 💰 成本优化

### 资源配置建议

**开发环境：**

- ECS: 0.25 vCPU, 0.5GB 内存
- RDS: db.t3.micro
- NAT Gateway: 单个实例

**生产环境：**

- ECS: 0.5+ vCPU, 1GB+ 内存
- RDS: db.t3.medium+
- NAT Gateway: 多 AZ 高可用

### 成本控制

- 使用 Fargate Spot 实例（开发环境）
- RDS 预留实例（生产环境）
- CloudFront 缓存优化
- 定期清理未使用资源

## 🛠️ 运维操作

### 扩容操作

```bash
# 修改 ECS 服务期望容器数量
aws ecs update-service --cluster weblog-cluster --service weblog-backend-service --desired-count 3

# 修改 RDS 实例类型
# 在 terraform/environments/prod.tfvars 中修改 db_instance_class
# 然后运行 terraform apply
```

### 备份恢复

```bash
# 创建 RDS 快照
aws rds create-db-snapshot --db-instance-identifier weblog-database --db-snapshot-identifier weblog-backup-$(date +%Y%m%d)

# 查看备份
aws rds describe-db-snapshots --db-instance-identifier weblog-database
```

### 故障排除

```bash
# 查看 ECS 服务状态
aws ecs describe-services --cluster weblog-cluster --services weblog-backend-service

# 查看容器日志
aws logs get-log-events --log-group-name /ecs/weblog --log-stream-name backend/backend/TASK_ID

# 查看 ALB 目标健康状态
aws elbv2 describe-target-health --target-group-arn TARGET_GROUP_ARN
```

## 🚨 故障恢复

### 自动恢复机制

- ECS 服务自动重启失败的任务
- ALB 健康检查自动移除不健康目标
- RDS 自动故障转移（Multi-AZ）
- CloudWatch 告警自动通知

### 手动恢复步骤

1. 检查 CloudWatch 告警
2. 查看 ECS 服务状态
3. 检查应用日志
4. 必要时重新部署服务

## 📚 相关文档

- [AWS ECS 文档](https://docs.aws.amazon.com/ecs/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

## ❓ 常见问题

### Q: 如何更新应用版本？

A: 推送代码到相应分支，GitHub Actions 会自动部署。或者使用 `deploy-multi-env.sh` 脚本手动部署。

### Q: 如何查看应用日志？

A: 在 AWS Console 的 CloudWatch Logs 中查看 `/ecs/weblog` 日志组。

### Q: 如何连接数据库？

A: 数据库在私有子网中，需要通过堡垒机或 VPN 连接。生产环境不建议直接连接。

### Q: 如何设置自定义域名？

A: 在 CloudFront 分发中配置自定义域名，并在 Route 53 中添加 CNAME 记录。

### Q: 如何扩展到多个地区？

A: 复制 Terraform 配置到其他地区，修改 `aws_region` 变量即可。

---

🎉 **恭喜！你现在拥有了一个完整的生产级 AWS 部署系统！**
