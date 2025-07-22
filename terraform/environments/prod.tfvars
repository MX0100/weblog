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
db_engine_version = "15.12"

# S3 配置 - 使用唯一名称避免冲突
s3_bucket_name = "weblog-frontend-20250721-3566"

# CloudFront 配置
cloudfront_price_class = "PriceClass_100"

# 安全配置
allowed_cidr_blocks = ["0.0.0.0/0"]  # 生产环境应限制为特定IP范围

# Alert email - 请更新为你的实际邮箱
alert_email = "lucaswang0402@gmail.com"

# 通用标签
common_tags = {
  Environment = "prod"
  Project     = "weblog"
  ManagedBy   = "terraform"
} 