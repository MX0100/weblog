# WeBlog EC2+S3+CloudFront 部署指南

## 🏗️ 架构概览

这是一个针对 AWS Free Tier 优化的部署架构：

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   CloudFront    │────│      S3         │    │      EC2        │
│  (CDN + HTTPS)  │    │ (静态前端文件)   │    │ (Docker + 后端)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                                               ┌─────────────────┐
                                               │   RDS PgSQL     │
                                               │ (db.t3.micro)   │
                                               └─────────────────┘
```

### ✅ 优势

- **Free Tier 友好**: EC2 t3.micro + RDS db.t3.micro
- **WebSocket 支持**: 前端直连 EC2，避免 CloudFront 代理限制
- **高性能**: CloudFront 全球 CDN 加速静态资源
- **简单架构**: 易于理解和维护
- **自动扩展**: 支持未来升级到更强配置

### 💰 成本估算

- **Free Tier 内**: $0-5/月
- **超出后**: ~$15-25/月（小规模使用）

## 🚀 快速部署

### 前置条件

1. **AWS CLI**

   ```bash
   # 安装并配置
   aws configure
   ```

2. **Terraform** (>= 1.0)

   ```bash
   # 下载: https://www.terraform.io/downloads.html
   terraform version
   ```

3. **Docker**

   ```bash
   docker version
   ```

4. **Node.js** (>= 16)
   ```bash
   node --version
   npm --version
   ```

### 创建 Key Pair

```bash
# 在AWS中创建Key Pair
aws ec2 create-key-pair --key-name weblog-test-key --region us-west-2 --query 'KeyMaterial' --output text > weblog-test-key.pem

# Windows用户可以用GUI在EC2控制台创建
```

### 一键部署

```bash
# 使用部署脚本
./deploy-ec2.sh test us-west-2 weblog-test-key

# 参数说明:
# 1. environment: test/staging/prod
# 2. aws-region: us-west-2
# 3. key-pair-name: 你的EC2 Key Pair名称
```

## 🔧 手动部署步骤

如果你想了解详细过程或自定义配置：

### 1. 配置 Terraform 变量

编辑 `terraform/environments/test.tfvars`:

```hcl
environment = "test"
region = "us-west-2"

# EC2配置
ec2_instance_type = "t3.micro"
ec2_key_pair_name = "your-key-name"

# RDS配置
db_instance_class = "db.t3.micro"
db_allocated_storage = 20
db_name = "weblog_test"

# S3配置
s3_bucket_name = "weblog-test-frontend-unique-suffix"
```

### 2. 部署基础设施

```bash
cd terraform
terraform init
terraform plan -var-file="environments/test.tfvars"
terraform apply -var-file="environments/test.tfvars"
```

### 3. 获取部署信息

```bash
# 获取重要输出
terraform output ec2_public_ip
terraform output s3_bucket_name
terraform output cloudfront_domain_name
terraform output database_password
```

### 4. 构建和部署应用

```bash
# 构建后端镜像
cd ../WeBlog_backend
docker build -f Dockerfile.prod -t mx0100/weblog-backend:latest .

# 构建前端
cd ../WeBlog-frontend
npm install

# 设置环境变量
export VITE_API_BASE_URL=http://YOUR_EC2_IP:8080
export VITE_WS_BASE_URL=ws://YOUR_EC2_IP:8080

npm run build

# 上传到S3
aws s3 sync dist/ s3://YOUR_S3_BUCKET/ --region us-west-2
```

## 📊 部署后验证

### 1. 检查后端健康状态

```bash
curl http://YOUR_EC2_IP:8080/actuator/health
```

### 2. 访问前端

- CloudFront URL: `https://YOUR_CLOUDFRONT_DOMAIN`
- S3 Direct URL: `http://YOUR_S3_BUCKET.s3-website-us-west-2.amazonaws.com`

### 3. 测试 WebSocket 连接

在浏览器控制台测试：

```javascript
const ws = new WebSocket("ws://YOUR_EC2_IP:8080/ws");
ws.onopen = () => console.log("WebSocket connected");
```

## 🔧 运维管理

### SSH 连接 EC2

```bash
ssh -i your-key.pem ec2-user@YOUR_EC2_IP
```

### 查看应用日志

```bash
# SSH到EC2后
docker logs weblog-backend
```

### 重启应用

```bash
# SSH到EC2后
cd /opt/weblog
docker-compose -f docker-compose.prod.yml restart
```

### 更新应用

#### 更新后端:

```bash
# 构建新镜像
docker build -f Dockerfile.prod -t mx0100/weblog-backend:latest .

# SSH到EC2，拉取并重启
ssh -i your-key.pem ec2-user@YOUR_EC2_IP
cd /opt/weblog
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```

#### 更新前端:

```bash
# 重新构建
npm run build

# 上传到S3
aws s3 sync dist/ s3://YOUR_S3_BUCKET/ --region us-west-2

# 清除CloudFront缓存
aws cloudfront create-invalidation --distribution-id YOUR_DISTRIBUTION_ID --paths "/*"
```

## 🛡️ 安全考虑

### 生产环境建议:

1. **限制访问 IP**

   ```hcl
   allowed_cidr_blocks = ["YOUR_OFFICE_IP/32"]
   ```

2. **启用 HTTPS**

   - EC2 配置 SSL 证书（Let's Encrypt）
   - 更新安全组规则

3. **数据库安全**

   - 更强的密码策略
   - 备份加密

4. **网络隔离**
   - 将 RDS 迁移到私有子网
   - 使用 NAT Gateway（会产生费用）

## 📈 扩展路径

### 升级到生产配置:

1. **计算资源**

   - EC2: t3.micro → t3.small/medium
   - RDS: db.t3.micro → db.t3.small

2. **高可用**

   - 多 AZ 部署
   - Application Load Balancer
   - Auto Scaling Group

3. **监控增强**
   - CloudWatch 详细监控
   - ELK/Grafana 日志分析
   - 自定义指标

## 🆘 故障排除

### 常见问题:

1. **EC2 无法访问**

   - 检查安全组规则
   - 确认 Key Pair 正确
   - 验证 AWS 凭证

2. **数据库连接失败**

   - 检查数据库密码
   - 验证网络连接
   - 查看应用日志

3. **前端无法访问后端**

   - 检查 CORS 配置
   - 验证 API URL 环境变量
   - 测试网络连接

4. **WebSocket 连接失败**
   - 确认端口 8080 开放
   - 检查防火墙设置
   - 验证 WebSocket URL

### 获取帮助:

```bash
# 查看Terraform输出
terraform output

# 查看AWS资源
aws ec2 describe-instances --region us-west-2
aws s3 ls
aws rds describe-db-instances --region us-west-2

# 查看日志
ssh -i your-key.pem ec2-user@YOUR_EC2_IP
sudo tail -f /var/log/weblog-setup.log
docker logs weblog-backend
```

## 📝 维护清单

### 每周检查:

- [ ] EC2 实例状态
- [ ] 应用健康检查
- [ ] 数据库备份
- [ ] CloudWatch 指标

### 每月检查:

- [ ] AWS 费用账单
- [ ] 安全更新
- [ ] 备份测试
- [ ] 性能优化

### 季度检查:

- [ ] 架构评估
- [ ] 成本优化
- [ ] 安全审计
- [ ] 容量规划
