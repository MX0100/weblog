# WeBlog EC2+S3+CloudFront PowerShell部署脚本
# 适用于AWS Free Tier测试环境

param(
    [string]$Environment = "test",
    [string]$AwsRegion = "us-west-2",
    [Parameter(Mandatory=$true)]
    [string]$KeyPairName
)

# 颜色定义
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Cyan"

Write-Host "🚀 WeBlog EC2架构部署脚本" -ForegroundColor $Blue
Write-Host "环境: $Environment" -ForegroundColor $Blue
Write-Host "区域: $AwsRegion" -ForegroundColor $Blue
Write-Host "Key Pair: $KeyPairName" -ForegroundColor $Blue
Write-Host ""

# 设置错误时停止
$ErrorActionPreference = "Stop"

# 检查依赖
Write-Host "🔍 检查依赖..." -ForegroundColor $Blue

# 检查AWS CLI
try {
    aws --version | Out-Null
    Write-Host "✅ AWS CLI 已安装" -ForegroundColor $Green
} catch {
    Write-Host "❌ AWS CLI 未安装" -ForegroundColor $Red
    Write-Host "请从 https://aws.amazon.com/cli/ 下载安装" -ForegroundColor $Yellow
    exit 1
}

# 检查Terraform
try {
    terraform version | Out-Null
    Write-Host "✅ Terraform 已安装" -ForegroundColor $Green
} catch {
    Write-Host "❌ Terraform 未安装" -ForegroundColor $Red
    Write-Host "请从 https://www.terraform.io/downloads.html 下载安装" -ForegroundColor $Yellow
    exit 1
}

# 检查Docker
try {
    docker version | Out-Null
    Write-Host "✅ Docker 已安装" -ForegroundColor $Green
} catch {
    Write-Host "❌ Docker 未安装" -ForegroundColor $Red
    Write-Host "请安装 Docker Desktop for Windows" -ForegroundColor $Yellow
    exit 1
}

# 检查Node.js
try {
    npm --version | Out-Null
    Write-Host "✅ Node.js/npm 已安装" -ForegroundColor $Green
} catch {
    Write-Host "❌ Node.js/npm 未安装" -ForegroundColor $Red
    Write-Host "请从 https://nodejs.org/ 下载安装" -ForegroundColor $Yellow
    exit 1
}

# 检查AWS凭证
Write-Host "🔐 检查AWS凭证..." -ForegroundColor $Blue
try {
    $AwsAccountId = aws sts get-caller-identity --query Account --output text
    Write-Host "✅ AWS账户: $AwsAccountId" -ForegroundColor $Green
} catch {
    Write-Host "❌ AWS凭证未配置或无效" -ForegroundColor $Red
    Write-Host "请运行: aws configure" -ForegroundColor $Yellow
    exit 1
}

# 检查Key Pair
Write-Host "🔑 检查Key Pair..." -ForegroundColor $Blue
try {
    aws ec2 describe-key-pairs --key-names $KeyPairName --region $AwsRegion | Out-Null
    Write-Host "✅ Key Pair '$KeyPairName' 存在" -ForegroundColor $Green
} catch {
    Write-Host "❌ Key Pair '$KeyPairName' 在区域 '$AwsRegion' 不存在" -ForegroundColor $Red
    Write-Host "请在AWS控制台创建Key Pair，或运行:" -ForegroundColor $Yellow
    Write-Host "aws ec2 create-key-pair --key-name $KeyPairName --region $AwsRegion --query 'KeyMaterial' --output text > $KeyPairName.pem" -ForegroundColor $Yellow
    exit 1
}

# 准备Terraform变量文件
Write-Host "📝 准备Terraform配置..." -ForegroundColor $Blue
Set-Location terraform

$TfvarsFile = "environments\$Environment.tfvars"
if (-not (Test-Path $TfvarsFile)) {
    Write-Host "⚠️  创建 $TfvarsFile..." -ForegroundColor $Yellow
    $Timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $Username = $env:USERNAME
    $CurrentDate = Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ"
    
    @"
# $($Environment.ToUpper()) Environment Configuration
environment = "$Environment"
region = "$AwsRegion"

# EC2 Configuration
ec2_instance_type = "t3.micro"
ec2_key_pair_name = "$KeyPairName"

# RDS Configuration  
db_instance_class = "db.t3.micro"
db_allocated_storage = 20
db_engine_version = "15.4"
db_name = "weblog_$Environment"
db_username = "weblog_user"

# S3 & CloudFront
s3_bucket_name = "weblog-$Environment-frontend-$Timestamp"
cloudfront_price_class = "PriceClass_100"

# Security
allowed_cidr_blocks = ["0.0.0.0/0"]

# Tags
common_tags = {
  Environment = "$Environment"
  Project     = "weblog"
  DeployedBy  = "$Username"
  DeployedAt  = "$CurrentDate"
}
"@ | Out-File -FilePath $TfvarsFile -Encoding UTF8
}

# 使用EC2架构配置
Write-Host "🔄 更新Terraform配置为EC2架构..." -ForegroundColor $Blue
if (Test-Path "main-ec2.tf") {
    Copy-Item "main-ec2.tf" "main.tf" -Force
    Write-Host "✅ 已使用EC2架构配置" -ForegroundColor $Green
}

if (Test-Path "outputs-ec2.tf") {
    Copy-Item "outputs-ec2.tf" "outputs.tf" -Force
    Write-Host "✅ 已更新输出配置" -ForegroundColor $Green
}

# Terraform 初始化
Write-Host "🔧 初始化Terraform..." -ForegroundColor $Blue
terraform init

# Terraform 计划
Write-Host "📋 生成Terraform计划..." -ForegroundColor $Blue
terraform plan -var-file="$TfvarsFile" -out="$Environment.tfplan"

# 确认部署
Write-Host "⚠️  准备部署到AWS，这将创建以下资源:" -ForegroundColor $Yellow
Write-Host "   - EC2 t3.micro 实例" -ForegroundColor $Yellow
Write-Host "   - RDS db.t3.micro 数据库" -ForegroundColor $Yellow
Write-Host "   - S3存储桶 + CloudFront分发" -ForegroundColor $Yellow
Write-Host "   - VPC, 子网, 安全组等网络资源" -ForegroundColor $Yellow
Write-Host ""

$Choice = Read-Host "是否继续部署? (y/N)"
if ($Choice -ne "y" -and $Choice -ne "Y") {
    Write-Host "🚫 部署已取消" -ForegroundColor $Yellow
    exit 0
}

# Terraform 应用
Write-Host "🚀 执行Terraform部署..." -ForegroundColor $Blue
terraform apply $Environment.tfplan

# 获取输出
Write-Host "📊 获取部署信息..." -ForegroundColor $Blue
$Ec2Ip = terraform output -raw ec2_public_ip
$S3Bucket = terraform output -raw s3_bucket_name
$CloudfrontDomain = terraform output -raw cloudfront_domain_name
$DbPassword = terraform output -raw database_password

Write-Host "🎉 基础设施部署完成！" -ForegroundColor $Green
Write-Host ""
Write-Host "📋 部署信息:" -ForegroundColor $Blue
Write-Host "  - EC2 Public IP: $Ec2Ip" -ForegroundColor $Green
Write-Host "  - S3 Bucket: $S3Bucket" -ForegroundColor $Green
Write-Host "  - CloudFront URL: https://$CloudfrontDomain" -ForegroundColor $Green
Write-Host "  - API URL: http://$Ec2Ip`:8080" -ForegroundColor $Green
Write-Host "  - WebSocket URL: ws://$Ec2Ip`:8080" -ForegroundColor $Green

# 回到项目根目录
Set-Location ..

# 构建后端Docker镜像
Write-Host "🐳 构建后端Docker镜像..." -ForegroundColor $Blue
Set-Location WeBlog_backend

docker build -f Dockerfile.prod -t mx0100/weblog-backend:latest .

Write-Host "✅ 后端镜像构建完成" -ForegroundColor $Green

# 等待EC2实例就绪
Write-Host "⏳ 等待EC2实例启动和Docker安装完成..." -ForegroundColor $Blue
Write-Host "这可能需要3-5分钟..." -ForegroundColor $Yellow

# 等待EC2健康检查
for ($i = 1; $i -le 30; $i++) {
    try {
        $Response = Invoke-WebRequest -Uri "http://$Ec2Ip`:8080/actuator/health" -Method GET -TimeoutSec 10
        if ($Response.StatusCode -eq 200) {
            Write-Host "✅ 后端服务已启动" -ForegroundColor $Green
            break
        }
    } catch {
        # 忽略错误继续尝试
    }
    Write-Host "等待后端启动... ($i/30)" -ForegroundColor $Yellow
    Start-Sleep -Seconds 30
}

# 构建前端
Write-Host "🌐 构建前端..." -ForegroundColor $Blue
Set-Location ..\WeBlog-frontend

# 安装依赖
npm install

# 设置环境变量并构建
@"
VITE_API_BASE_URL=http://$Ec2Ip`:8080
VITE_WS_BASE_URL=ws://$Ec2Ip`:8080
VITE_ENVIRONMENT=$Environment
"@ | Out-File -FilePath ".env.production" -Encoding UTF8

# 构建前端
npm run build

# 上传到S3
Write-Host "📤 上传前端到S3..." -ForegroundColor $Blue
aws s3 sync dist/ "s3://$S3Bucket/" --region $AwsRegion

# 清除CloudFront缓存
Write-Host "🗑️  清除CloudFront缓存..." -ForegroundColor $Blue
Set-Location ..\terraform
$DistributionId = terraform output -raw cloudfront_distribution_id
aws cloudfront create-invalidation --distribution-id $DistributionId --paths "/*" --region $AwsRegion

Write-Host ""
Write-Host "🎉 部署完成！" -ForegroundColor $Green
Write-Host ""
Write-Host "🌐 访问地址:" -ForegroundColor $Blue
Write-Host "  - 前端: https://$CloudfrontDomain" -ForegroundColor $Green
Write-Host "  - 后端API: http://$Ec2Ip`:8080" -ForegroundColor $Green
Write-Host "  - 健康检查: http://$Ec2Ip`:8080/actuator/health" -ForegroundColor $Green
Write-Host ""
Write-Host "🔧 管理命令:" -ForegroundColor $Blue
Write-Host "  - SSH到EC2: ssh -i $KeyPairName.pem ec2-user@$Ec2Ip" -ForegroundColor $Yellow
Write-Host "  - 查看应用日志: ssh到EC2后运行 'docker logs weblog-backend'" -ForegroundColor $Yellow
Write-Host "  - 重启应用: ssh到EC2后运行 'cd /opt/weblog && docker-compose -f docker-compose.prod.yml restart'" -ForegroundColor $Yellow
Write-Host ""
Write-Host "💰 成本估算:" -ForegroundColor $Blue
Write-Host "  - 当前配置在AWS Free Tier内: $0-5/月" -ForegroundColor $Green
Write-Host ""
Write-Host "📝 注意: CloudFront分发可能需要10-15分钟才能完全生效" -ForegroundColor $Yellow

# 保存重要信息到文件
$DeploymentInfo = @"
# WeBlog 部署信息 - $(Get-Date)

## 访问地址
- 前端: https://$CloudfrontDomain
- 后端API: http://$Ec2Ip:8080
- 健康检查: http://$Ec2Ip:8080/actuator/health

## AWS资源
- EC2 Public IP: $Ec2Ip
- S3 Bucket: $S3Bucket
- CloudFront Distribution: $DistributionId
- Database Password: $DbPassword

## SSH连接
ssh -i $KeyPairName.pem ec2-user@$Ec2Ip

## 管理命令
- 查看日志: docker logs weblog-backend
- 重启应用: cd /opt/weblog && docker-compose -f docker-compose.prod.yml restart
- 更新应用: docker-compose -f docker-compose.prod.yml pull && docker-compose -f docker-compose.prod.yml up -d
"@

$DeploymentInfo | Out-File -FilePath "..\deployment-info.txt" -Encoding UTF8
Write-Host "📄 部署信息已保存到 deployment-info.txt" -ForegroundColor $Blue 