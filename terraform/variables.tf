# 通用变量
variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-west-2"
}

variable "common_tags" {
  description = "Common tags for all resources"
  type        = map(string)
  default     = {}
}

# EC2变量
variable "ec2_instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.micro"
}

variable "ec2_key_pair_name" {
  description = "Name of the EC2 Key Pair for SSH access"
  type        = string
}

# RDS变量
variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GB"
  type        = number
  default     = 20
}

variable "db_engine_version" {
  description = "PostgreSQL engine version"
  type        = string
  default     = "15.8"
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "weblog"
}

variable "db_username" {
  description = "Database username"
  type        = string
  default     = "weblog_user"
}

# S3和CloudFront变量
variable "s3_bucket_name" {
  description = "S3 bucket name for frontend static files"
  type        = string
}

variable "cloudfront_price_class" {
  description = "CloudFront price class"
  type        = string
  default     = "PriceClass_100"
  
  validation {
    condition = contains([
      "PriceClass_All",
      "PriceClass_200", 
      "PriceClass_100"
    ], var.cloudfront_price_class)
    error_message = "Price class must be PriceClass_All, PriceClass_200, or PriceClass_100."
  }
}

# 安全变量
variable "allowed_cidr_blocks" {
  description = "CIDR blocks allowed to access the application"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# 监控告警变量
variable "alert_email" {
  description = "Email address for CloudWatch alerts"
  type        = string
  default     = ""
}

# 以下是旧配置的变量，保留以避免破坏性更改
variable "aws_region" {
  description = "AWS region (deprecated, use region instead)"
  type        = string
  default     = "us-west-2"
}

variable "project_name" {
  description = "Project name (deprecated, use environment instead)"
  type        = string
  default     = "weblog"
}

variable "backend_cpu" {
  description = "Backend CPU units (deprecated)"
  type        = number
  default     = 256
}

variable "backend_memory" {
  description = "Backend memory in MiB (deprecated)"
  type        = number
  default     = 512
}

variable "frontend_cpu" {
  description = "Frontend CPU units (deprecated)"
  type        = number
  default     = 256
}

variable "frontend_memory" {
  description = "Frontend memory in MiB (deprecated)"
  type        = number
  default     = 512
}

variable "db_port" {
  description = "Database port (deprecated)"
  type        = number
  default     = 5432
}

variable "backend_port" {
  description = "Backend port (deprecated)"
  type        = number
  default     = 8080
}

variable "frontend_port" {
  description = "Frontend port (deprecated)"
  type        = number
  default     = 80
} 