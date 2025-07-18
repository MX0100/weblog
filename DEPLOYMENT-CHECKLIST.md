# WeBlog 部署前检查清单

## 🔧 AWS 准备工作

### 1. AWS 凭证配置

- [ ] 配置 AWS CLI: `aws configure`
- [ ] 确认 AWS 访问密钥和秘密密钥已设置
- [ ] 确认有足够的权限创建 EC2、RDS、S3、CloudFront 等资源

### 2. 创建 EC2 密钥对

```bash
# 在 AWS Console 中创建密钥对，或使用 CLI：
aws ec2 create-key-pair --key-name weblog-prod-key --query 'KeyMaterial' --output text > weblog-prod-key.pem
chmod 400 weblog-prod-key.pem
```

### 3. 更新配置文件

#### Terraform 生产环境配置 (`terraform/environments/prod.tfvars`)

- [ ] 更新 `ec2_key_pair_name` 为实际创建的密钥对名称
- [ ] 更新 `s3_bucket_name` 为全局唯一的名称
- [ ] 更新 `alert_email` 为实际的邮箱地址
- [ ] 限制 `allowed_cidr_blocks` 为特定 IP 范围（可选但推荐）

#### 后端生产配置 (`WeBlog_backend/src/main/resources/application-prod.properties`)

- [ ] 配置邮件服务器信息（如果需要邮件功能）
- [ ] 检查数据库连接配置
- [ ] 确认 actuator 端点配置合适

## 🛠️ 部署步骤

### 方式一：一键部署（推荐）

```bash
# 给脚本执行权限
chmod +x deploy-multi-env.sh

# 查看部署计划
./deploy-multi-env.sh prod plan

# 执行部署
./deploy-multi-env.sh prod apply
```

### 方式二：手动部署

```bash
# 1. 部署基础设施
cd terraform
terraform init
terraform workspace select prod || terraform workspace new prod
terraform apply -var-file="environments/prod.tfvars"

# 2. 构建和推送镜像
# (脚本会自动处理)

# 3. 更新服务
# (脚本会自动处理)
```

## 🔍 部署后验证

### 1. 基础设施检查

- [ ] EC2 实例运行正常
- [ ] RDS 数据库可连接
- [ ] Load Balancer 健康检查通过
- [ ] CloudFront 分发正常工作

### 2. 应用检查

- [ ] 后端 API 响应正常: `https://your-alb-url/api/actuator/health`
- [ ] 前端页面加载正常
- [ ] 用户注册/登录功能正常
- [ ] WebSocket 连接正常

### 3. 监控检查

- [ ] CloudWatch 监控正常
- [ ] 日志正确输出到 CloudWatch Logs
- [ ] 告警配置正确

## 🚨 常见问题解决

### 1. EC2 实例无法访问

- 检查安全组配置
- 确认密钥对正确
- 查看用户数据脚本执行日志

### 2. 数据库连接失败

- 检查 RDS 安全组
- 确认数据库凭证正确
- 检查网络连接

### 3. 前端页面无法加载

- 检查 S3 存储桶策略
- 确认 CloudFront 配置
- 查看浏览器开发者工具错误

### 4. API 请求失败

- 检查 Load Balancer 目标组健康状态
- 查看后端应用日志
- 确认安全组配置

## 🔒 安全建议

### 生产环境安全配置

- [ ] 限制 SSH 访问 IP 范围
- [ ] 定期更新系统和依赖
- [ ] 启用 RDS 加密
- [ ] 配置 CloudFront WAF（如需要）
- [ ] 定期备份数据库

### 敏感信息管理

- [ ] 使用 AWS Secrets Manager 存储数据库密码
- [ ] 不在代码中硬编码敏感信息
- [ ] 定期轮换访问密钥

## 💰 成本优化

### 开发/测试环境

- 使用 t3.micro 实例
- 使用 db.t3.micro 数据库
- 非工作时间可以停止实例

### 生产环境

- 考虑预留实例节省成本
- 配置自动伸缩
- 监控并优化不必要的资源

## 📊 监控和维护

### 日常监控

- [ ] 检查 CloudWatch 告警
- [ ] 查看应用性能指标
- [ ] 监控成本使用情况

### 定期维护

- [ ] 更新应用版本
- [ ] 清理旧的 Docker 镜像
- [ ] 备份重要数据
- [ ] 审查安全配置

## 📞 支持联系

如遇到部署问题，请检查：

1. AWS CloudWatch 日志
2. ECS 任务定义和服务状态
3. Load Balancer 目标组健康状态
4. RDS 连接状态

---

**注意**: 首次部署可能需要 10-15 分钟才能完全启动所有服务。请耐心等待并通过 AWS Console 监控部署进度。
