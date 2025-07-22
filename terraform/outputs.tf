# VPC信息
output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "vpc_cidr_block" {
  description = "VPC CIDR block"
  value       = aws_vpc.main.cidr_block
}

# EC2实例信息
output "ec2_instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.web_server.id
}

output "ec2_public_ip" {
  description = "EC2 instance public IP"
  value       = aws_instance.web_server.public_ip
}

output "ec2_public_dns" {
  description = "EC2 instance public DNS"
  value       = aws_instance.web_server.public_dns
}

output "backend_url" {
  description = "Backend API URL"
  value       = "http://${aws_instance.web_server.public_ip}:8080"
}

output "backend_websocket_url" {
  description = "Backend WebSocket URL"
  value       = "ws://${aws_instance.web_server.public_ip}:8080"
}

# S3和CloudFront信息
output "s3_bucket_name" {
  description = "S3 bucket name for frontend"
  value       = aws_s3_bucket.frontend.bucket
}

output "s3_bucket_website_endpoint" {
  description = "S3 bucket website endpoint"
  value       = aws_s3_bucket_website_configuration.frontend.website_endpoint
}

output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID"
  value       = aws_cloudfront_distribution.frontend.id
}

output "cloudfront_domain_name" {
  description = "CloudFront distribution domain name"
  value       = aws_cloudfront_distribution.frontend.domain_name
}

output "frontend_url" {
  description = "Frontend URL via CloudFront"
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

# RDS信息
output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.main.endpoint
}

output "rds_port" {
  description = "RDS instance port"
  value       = aws_db_instance.main.port
}

output "database_name" {
  description = "Database name"
  value       = aws_db_instance.main.db_name
}

output "database_username" {
  description = "Database username"
  value       = aws_db_instance.main.username
}

# 密码输出（敏感信息）
output "database_password" {
  description = "Database password"
  value       = random_password.db_password.result
  sensitive   = true
}

# 安全组信息
output "web_server_security_group_id" {
  description = "Web server security group ID"
  value       = aws_security_group.web_server.id
}

output "rds_security_group_id" {
  description = "RDS security group ID"
  value       = aws_security_group.rds.id
}

# CloudWatch日志组
output "cloudwatch_log_group_name" {
  description = "CloudWatch log group name"
  value       = aws_cloudwatch_log_group.application.name
}

# ======================================
# GitHub Actions CI/CD 输出信息
# ======================================
output "github_actions_access_key_id" {
  description = "GitHub Actions IAM user access key ID"
  value       = aws_iam_access_key.github_actions.id
}

output "github_actions_secret_access_key" {
  description = "GitHub Actions IAM user secret access key"
  value       = aws_iam_access_key.github_actions.secret
  sensitive   = true
}

output "s3_bucket_for_frontend" {
  description = "S3 bucket name for CI/CD frontend deployment"
  value       = aws_s3_bucket.frontend.bucket
}

output "ec2_instance_for_backend" {
  description = "EC2 instance ID for CI/CD backend deployment"
  value       = aws_instance.web_server.id
}

# 部署说明
output "deployment_instructions" {
  description = "Deployment instructions"
  value = <<-EOT
    📋 部署完成！以下是重要信息：

    🌐 前端访问:
      - CloudFront URL: https://${aws_cloudfront_distribution.frontend.domain_name}
      - S3 Direct URL: http://${aws_s3_bucket_website_configuration.frontend.website_endpoint}

    🔧 后端API:
      - API Base URL: http://${aws_instance.web_server.public_ip}:8080
      - WebSocket URL: ws://${aws_instance.web_server.public_ip}:8080
      - Health Check: http://${aws_instance.web_server.public_ip}:8080/actuator/health

    🗄️ 数据库:
      - Endpoint: ${aws_db_instance.main.endpoint}
      - Database: ${aws_db_instance.main.db_name}
      - Username: ${aws_db_instance.main.username}
      - Password: [敏感信息，使用 terraform output database_password 查看]

    🖥️ EC2实例:
      - Instance ID: ${aws_instance.web_server.id}
      - Public IP: ${aws_instance.web_server.public_ip}
      - SSH: ssh -i your-key.pem ec2-user@${aws_instance.web_server.public_ip}

    📊 监控:
      - CloudWatch Logs: /aws/ec2/weblog-${var.environment}

    🚀 下一步:
      1. 构建并上传前端到S3: aws s3 sync ./dist s3://${aws_s3_bucket.frontend.bucket}/
      2. 推送Docker镜像到Registry: docker push mx0100/weblog-backend:latest
      3. 重启EC2上的应用: ssh到EC2后运行 'docker-compose -f /opt/weblog/docker-compose.prod.yml pull && docker-compose -f /opt/weblog/docker-compose.prod.yml up -d'

    🔧 GitHub Actions 配置:
      - AWS_ACCESS_KEY_ID: ${aws_iam_access_key.github_actions.id}
      - AWS_SECRET_ACCESS_KEY: [敏感信息，使用 terraform output github_actions_secret_access_key 查看]
      - S3_BUCKET: ${aws_s3_bucket.frontend.bucket}
      - CLOUDFRONT_DOMAIN: ${aws_cloudfront_distribution.frontend.domain_name}
  EOT
}

# 环境变量配置（用于前端构建）
output "frontend_env_variables" {
  description = "Environment variables for frontend build"
  value = {
    VITE_API_BASE_URL = "https://${aws_cloudfront_distribution.frontend.domain_name}"
    VITE_WS_BASE_URL  = "wss://${aws_cloudfront_distribution.frontend.domain_name}"
    VITE_ENVIRONMENT  = var.environment
  }
}

# 成本估算（测试环境）
output "estimated_monthly_cost" {
  description = "Estimated monthly cost in USD (Free Tier eligible)"
  value = <<-EOT
    💰 预估月费用 (Free Tier):

    ✅ 免费资源:
      - EC2 t3.micro: $0 (750小时/月 Free Tier)
      - RDS db.t3.micro: $0 (750小时/月 Free Tier)
      - S3存储: $0 (前5GB免费)
      - CloudFront: $0 (前1TB免费)
      - VPC, IGW, 路由表: $0

    💸 可能产生费用:
      - 数据传输 (如果超过免费额度): ~$0.09/GB
      - S3请求 (如果超过免费额度): ~$0.0004/1000请求
      - RDS存储 (超过20GB): ~$0.115/GB/月

    📅 Free Tier限制:
      - EC2: 12个月免费
      - RDS: 12个月免费  
      - S3: 永久免费额度
      - CloudFront: 永久免费额度

    总计预估: $0-5/月 (Free Tier内)
    EOT
} 