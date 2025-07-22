# 🔐 企业级密钥管理策略

## 📋 密钥分类与管理原则

### 🏗️ **密钥分类**

| 类型           | 示例                 | 存储位置            | 轮换频率 |
| -------------- | -------------------- | ------------------- | -------- |
| **应用密钥**   | JWT Secret, API Keys | AWS Secrets Manager | 90 天    |
| **数据库凭证** | DB Username/Password | AWS Secrets Manager | 30 天    |
| **第三方服务** | 邮件服务密码         | AWS Secrets Manager | 手动     |
| **基础设施**   | EC2 Key Pairs        | AWS Key Management  | 年度     |

### 🎯 **管理原则**

1. **永不明文存储**：所有密钥都必须加密存储
2. **最小权限原则**：应用只能访问必需的密钥
3. **定期轮换**：自动化密钥轮换流程
4. **审计日志**：记录所有密钥访问
5. **环境隔离**：不同环境使用不同密钥

## 🏛️ **AWS Secrets Manager 集成**

### **1. 创建密钥**

```bash
# 创建数据库凭证
aws secretsmanager create-secret \
  --name "weblog/prod/database" \
  --description "WeBlog Production Database Credentials" \
  --secret-string '{
    "username": "weblog_prod",
    "password": "GENERATED_STRONG_PASSWORD",
    "engine": "postgresql",
    "host": "weblog-prod-db.cluster-xyz.us-east-1.rds.amazonaws.com",
    "port": 5432,
    "database": "weblog"
  }'

# 创建应用密钥
aws secretsmanager create-secret \
  --name "weblog/prod/application" \
  --description "WeBlog Production Application Secrets" \
  --secret-string '{
    "jwt_secret": "GENERATED_256_BIT_KEY",
    "mail_password": "APP_SPECIFIC_PASSWORD"
  }'

# 创建AWS服务密钥
aws secretsmanager create-secret \
  --name "weblog/prod/aws-services" \
  --description "WeBlog AWS Services Configuration" \
  --secret-string '{
    "s3_bucket": "weblog-prod-assets",
    "cloudfront_domain": "cdn.weblog.com"
  }'
```

### **2. IAM 权限配置**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::ACCOUNT:role/weblog-ec2-role"
      },
      "Action": ["secretsmanager:GetSecretValue"],
      "Resource": [
        "arn:aws:secretsmanager:us-east-1:ACCOUNT:secret:weblog/prod/*"
      ]
    }
  ]
}
```

## 🐳 **Docker 与环境变量集成**

### **生产环境启动脚本**

```bash
#!/bin/bash

# secrets-loader.sh - 从AWS Secrets Manager加载密钥
set -e

AWS_REGION="${AWS_REGION:-us-east-1}"

# 获取数据库凭证
DB_SECRET=$(aws secretsmanager get-secret-value \
  --secret-id "weblog/prod/database" \
  --region "${AWS_REGION}" \
  --query 'SecretString' \
  --output text)

export DB_HOST=$(echo "$DB_SECRET" | jq -r '.host')
export DB_USERNAME=$(echo "$DB_SECRET" | jq -r '.username')
export DB_PASSWORD=$(echo "$DB_SECRET" | jq -r '.password')
export DB_NAME=$(echo "$DB_SECRET" | jq -r '.database')

# 获取应用密钥
APP_SECRET=$(aws secretsmanager get-secret-value \
  --secret-id "weblog/prod/application" \
  --region "${AWS_REGION}" \
  --query 'SecretString' \
  --output text)

export JWT_SECRET=$(echo "$APP_SECRET" | jq -r '.jwt_secret')
export MAIL_PASSWORD=$(echo "$APP_SECRET" | jq -r '.mail_password')

# 获取AWS服务配置
AWS_SECRET=$(aws secretsmanager get-secret-value \
  --secret-id "weblog/prod/aws-services" \
  --region "${AWS_REGION}" \
  --query 'SecretString' \
  --output text)

export S3_BUCKET=$(echo "$AWS_SECRET" | jq -r '.s3_bucket')
export CLOUDFRONT_DOMAIN=$(echo "$AWS_SECRET" | jq -r '.cloudfront_domain')

# 启动应用
exec "$@"
```

### **增强的 Dockerfile**

```dockerfile
# 在生产Dockerfile中添加密钥加载
FROM eclipse-temurin:17-jre-alpine AS runtime

# 安装必要工具
RUN apk add --no-cache curl jq aws-cli

# 复制密钥加载脚本
COPY secrets-loader.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/secrets-loader.sh

# ... 其他构建步骤 ...

# 使用密钥加载器启动
ENTRYPOINT ["secrets-loader.sh", "java", "-jar", "app.jar"]
```

## 🔄 **自动化密钥轮换**

### **Lambda 函数 - 密钥轮换**

```python
import boto3
import json
import random
import string

def lambda_handler(event, context):
    """自动轮换JWT密钥"""

    secrets_client = boto3.client('secretsmanager')

    # 生成新的JWT密钥
    new_jwt_secret = generate_jwt_secret()

    # 获取当前密钥
    current_secret = secrets_client.get_secret_value(
        SecretId='weblog/prod/application'
    )

    secret_data = json.loads(current_secret['SecretString'])
    secret_data['jwt_secret'] = new_jwt_secret

    # 更新密钥
    secrets_client.update_secret(
        SecretId='weblog/prod/application',
        SecretString=json.dumps(secret_data)
    )

    # 触发应用重启 (通过EC2或ECS)
    trigger_app_restart()

    return {
        'statusCode': 200,
        'body': json.dumps('JWT secret rotated successfully')
    }

def generate_jwt_secret():
    """生成256位安全密钥"""
    return ''.join(random.choices(
        string.ascii_letters + string.digits,
        k=64
    ))

def trigger_app_restart():
    """触发应用重启以加载新密钥"""
    # 实现应用重启逻辑
    pass
```

## 📊 **密钥监控与审计**

### **CloudWatch 监控**

```yaml
# cloudwatch-alerts.yml
AWSTemplateFormatVersion: "2010-09-09"
Resources:
  SecretAccessAlert:
    Type: AWS::CloudWatch::Alarm
    Properties:
      AlarmName: "weblog-secret-access-anomaly"
      MetricName: "SecretRetrievals"
      Namespace: "AWS/SecretsManager"
      Statistic: Sum
      Period: 300
      EvaluationPeriods: 2
      Threshold: 100
      ComparisonOperator: GreaterThanThreshold
      AlarmActions:
        - !Ref SecurityNotificationTopic
```

### **密钥访问日志**

```bash
# 查询密钥访问日志
aws logs filter-log-events \
  --log-group-name "/aws/secretsmanager/weblog" \
  --start-time $(date -d "1 hour ago" +%s)000 \
  --filter-pattern "{ $.eventName = GetSecretValue }"
```

## 🎯 **生产环境部署流程**

### **1. 预部署检查**

```bash
#!/bin/bash
# pre-deploy-check.sh

check_secrets() {
    local secrets=(
        "weblog/prod/database"
        "weblog/prod/application"
        "weblog/prod/aws-services"
    )

    for secret in "${secrets[@]}"; do
        if ! aws secretsmanager get-secret-value --secret-id "$secret" >/dev/null 2>&1; then
            echo "❌ Secret $secret not found"
            return 1
        fi
        echo "✅ Secret $secret verified"
    done
}
```

### **2. 安全部署**

```bash
#!/bin/bash
# secure-deploy.sh

# 加载密钥
source secrets-loader.sh

# 验证密钥完整性
if [[ -z "$DB_HOST" || -z "$JWT_SECRET" ]]; then
    echo "❌ Required secrets not loaded"
    exit 1
fi

# 部署应用
docker run -d \
  --name weblog-app \
  -p 8080:8080 \
  -e ENV=production \
  -e DB_HOST="$DB_HOST" \
  -e DB_USERNAME="$DB_USERNAME" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e JWT_SECRET="$JWT_SECRET" \
  weblog-backend:latest
```

## 🛡️ **安全最佳实践**

### **环境隔离**

```yaml
# 不同环境使用不同的密钥前缀
Development: weblog/dev/*
Staging: weblog/staging/*
Production: weblog/prod/*
```

### **密钥版本管理**

```bash
# 创建带版本的密钥
aws secretsmanager put-secret-value \
  --secret-id "weblog/prod/application" \
  --secret-string '{"jwt_secret":"new_key"}' \
  --version-stage "AWSPENDING"

# 测试新版本
test_application_with_new_secret

# 促进到当前版本
aws secretsmanager update-secret-version-stage \
  --secret-id "weblog/prod/application" \
  --version-stage "AWSCURRENT" \
  --move-to-version-id "new-version-id"
```

### **紧急密钥撤销**

```bash
#!/bin/bash
# emergency-revoke.sh

# 立即撤销密钥
aws secretsmanager put-secret-value \
  --secret-id "weblog/prod/application" \
  --secret-string '{"jwt_secret":"REVOKED"}' \
  --version-stage "AWSCURRENT"

# 强制应用重启
aws ec2 reboot-instances --instance-ids "$EC2_INSTANCE_ID"

echo "🚨 Emergency secret revocation completed"
```

## 📝 **开发团队使用指南**

### **本地开发**

```bash
# 开发人员永远不应该有生产密钥访问权限
# 使用本地环境变量或开发专用密钥

export JWT_SECRET="dev-secret-key"
export DB_PASSWORD="dev-password"
```

### **测试环境**

```bash
# 测试环境使用专用密钥
aws secretsmanager get-secret-value \
  --secret-id "weblog/test/application" \
  --region us-east-1
```

### **生产访问**

```bash
# 只有授权的DevOps人员可以访问生产密钥
# 使用临时凭证和MFA认证
aws sts assume-role \
  --role-arn "arn:aws:iam::ACCOUNT:role/weblog-prod-access" \
  --role-session-name "prod-deploy-$(date +%s)" \
  --serial-number "arn:aws:iam::ACCOUNT:mfa/username" \
  --token-code "123456"
```

这个密钥管理策略确保了：

1. ✅ **零明文密钥**：所有敏感信息都加密存储
2. ✅ **自动轮换**：定期更新密钥增强安全性
3. ✅ **审计追踪**：完整的访问日志记录
4. ✅ **环境隔离**：不同环境使用独立密钥
5. ✅ **紧急响应**：快速撤销和恢复机制
