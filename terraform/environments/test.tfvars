# Test Environment Configuration
environment = "test"
region = "us-west-2"

# EC2 Configuration
ec2_instance_type = "t3.micro"
ec2_key_pair_name = "weblog-test-key"  # 你需要在AWS创建这个key pair

# RDS Configuration  
db_instance_class = "db.t3.micro"
db_allocated_storage = 20
db_engine_version = "15.4"
db_name = "weblog_test"
db_username = "weblog_user"
# db_password will be generated automatically

# S3 & CloudFront
s3_bucket_name = "weblog-test-frontend"
cloudfront_price_class = "PriceClass_100"  # 只用北美和欧洲，最便宜

# Security
allowed_cidr_blocks = ["0.0.0.0/0"]  # 测试环境允许所有IP，生产环境需要限制

# Tags
common_tags = {
  Environment = "test"
  Project     = "weblog"
  Owner       = "dev-team"
} 