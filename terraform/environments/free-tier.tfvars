project_name = "weblog"
environment  = "dev"
aws_region   = "us-west-2"

# EC2 配置 - 免费套餐
ec2_instance_type = "t3.micro"  # 免费套餐
ec2_key_pair_name = "weblog-prod-key"  # 使用现有密钥对

# 免费套餐数据库配置
db_name           = "weblog_dev"
db_username       = "weblog_admin"
db_instance_class = "db.t3.micro"  # 免费套餐
db_allocated_storage = 20  # 免费套餐最大20GB

# S3 配置 - 使用唯一名称
s3_bucket_name = "weblog-frontend-free-tier-20250721"

# CloudFront 配置
cloudfront_price_class = "PriceClass_100"

# 安全配置
allowed_cidr_blocks = ["0.0.0.0/0"]

# 告警邮箱
alert_email = "lucaswang0402@gmail.com"

# 通用标签
common_tags = {
  Environment = "dev"
  Project     = "weblog"
  ManagedBy   = "terraform"
  CostOptimized = "true"
} 