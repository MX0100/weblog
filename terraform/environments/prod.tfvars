project_name = "weblog"
environment  = "prod"
aws_region   = "us-west-2"

# EC2 配置
ec2_instance_type = "t3.small"
ec2_key_pair_name = "weblog-prod-key"  # 需要在AWS中创建此密钥对

# Database configuration for production
db_name           = "weblog_prod"
db_username       = "weblog_admin"
db_instance_class = "db.t3.medium"

# S3 配置
s3_bucket_name = "weblog-prod-frontend-static"  # 需要是全局唯一的名称

# CloudFront 配置
cloudfront_price_class = "PriceClass_100"

# 安全配置
allowed_cidr_blocks = ["0.0.0.0/0"]  # 生产环境应限制为特定IP范围

# Alert email (required for production)
alert_email = "alerts@company.com"  # 请更新为实际的邮箱地址

# 通用标签
common_tags = {
  Environment = "prod"
  Project     = "weblog"
  ManagedBy   = "terraform"
} 