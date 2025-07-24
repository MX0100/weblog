# WeBlog EC2+S3+CloudFront Deployment Guide

## 🏗️ Architecture Overview

This is a deployment architecture optimized for AWS Free Tier:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   CloudFront    │────│      S3         │    │      EC2        │
│  (CDN + HTTPS)  │    │(Static Frontend)│    │(Docker + Backend) │                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                                               ┌─────────────────┐
                                               │   RDS PgSQL     │
                                               │ (db.t3.micro)   │
                                               └─────────────────┘
```

### ✅ Advantages

- **Free Tier Friendly**: EC2 t3.micro + RDS db.t3.micro
- **WebSocket Support**: Frontend connects directly to EC2, avoiding CloudFront proxy limitations
- **High Performance**: CloudFront global CDN accelerates static assets
- **Simple Architecture**: Easy to understand and maintain
- **Auto Scaling**: Supports future upgrades to higher specs

### 💰 Cost Estimate

- **Within Free Tier**: $0-5/month
- **Beyond Free Tier**: ~$15-25/month (for small scale usage)

## 🚀 Quick Deployment

### Prerequisites

1. **AWS CLI**

   ```bash
   # Install and configure
   aws configure
   ```

2. **Terraform** (>= 1.0)

   ```bash
   # Download: https://www.terraform.io/downloads.html
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

### Create Key Pair

```bash
# Create Key Pair in AWS
aws ec2 create-key-pair --key-name weblog-test-key --region us-west-2 --query 'KeyMaterial' --output text > weblog-test-key.pem

# Windows users can use the EC2 console GUI to create
```

### One-Click Deployment

```bash
# Use deployment script
./deploy-ec2.sh test us-west-2 weblog-test-key

# Parameter description:
# 1. environment: test/staging/prod
# 2. aws-region: us-west-2
# 3. key-pair-name: your EC2 Key Pair name
```

## 🔧 Manual Deployment Steps

If you want to understand the process or customize config:

### 1. Configure Terraform Variables

Edit `terraform/environments/test.tfvars`:

```hcl
environment = "test"
region = "us-west-2"

# EC2 config
ec2_instance_type = "t3.micro"
ec2_key_pair_name = "your-key-name"

# RDS config
db_instance_class = "db.t3.micro"
db_allocated_storage = 20
db_name = "weblog_test"

# S3 config
s3_bucket_name = "weblog-test-frontend-unique-suffix"
```

### 2. Deploy Infrastructure

```bash
cd terraform
terraform init
terraform plan -var-file="environments/test.tfvars"
terraform apply -var-file="environments/test.tfvars"
```

### 3. Get Deployment Info

```bash
# Get important outputs
terraform output ec2_public_ip
terraform output s3_bucket_name
terraform output cloudfront_domain_name
terraform output database_password
```

### 4. Build and Deploy Application

```bash
# Build backend image
cd ../WeBlog_backend
docker build -f Dockerfile.prod -t mx0100/weblog-backend:latest .

# Build frontend
cd ../WeBlog-frontend
npm install

# Set environment variables
export VITE_API_BASE_URL=http://YOUR_EC2_IP:8080
export VITE_WS_BASE_URL=ws://YOUR_EC2_IP:8080

npm run build

# Upload to S3
aws s3 sync dist/ s3://YOUR_S3_BUCKET/ --region us-west-2
```

## 📊 Post-Deployment Verification

### 1. Check Backend Health

```bash
curl http://YOUR_EC2_IP:8080/actuator/health
```

### 2. Access Frontend

- CloudFront URL: `https://YOUR_CLOUDFRONT_DOMAIN`
- S3 Direct URL: `http://YOUR_S3_BUCKET.s3-website-us-west-2.amazonaws.com`

### 3. Test WebSocket Connection

In browser console:

```javascript
const ws = new WebSocket("ws://YOUR_EC2_IP:8080/ws");
ws.onopen = () => console.log("WebSocket connected");
```

## 🔧 Operations & Maintenance

### SSH to EC2

```bash
ssh -i your-key.pem ec2-user@YOUR_EC2_IP
```

### View Application Logs

```bash
# After SSH to EC2
docker logs weblog-backend
```

### Restart Application

```bash
# After SSH to EC2
cd /opt/weblog
docker-compose -f docker-compose.prod.yml restart
```

### Update Application

#### Update Backend:

```bash
# Build new image
docker build -f Dockerfile.prod -t mx0100/weblog-backend:latest .

# SSH to EC2, pull and restart
ssh -i your-key.pem ec2-user@YOUR_EC2_IP
cd /opt/weblog
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```

#### Update Frontend:

```bash
# Rebuild
npm run build

# Upload to S3
aws s3 sync dist/ s3://YOUR_S3_BUCKET/ --region us-west-2

# Invalidate CloudFront cache
aws cloudfront create-invalidation --distribution-id YOUR_DISTRIBUTION_ID --paths "/*"
```

## 🛡️ Security Considerations

### Production Recommendations:

1. **Restrict Access IP**

   ```hcl
   allowed_cidr_blocks = ["YOUR_OFFICE_IP/32"]
   ```

2. **Enable HTTPS**

   - Configure SSL certificate on EC2 (Let's Encrypt)
   - Update security group rules

3. **Database Security**

   - Stronger password policy
   - Encrypted backups

4. **Network Isolation**
   - Move RDS to private subnet
   - Use NAT Gateway (may incur cost)

## 📈 Upgrade Path

### Upgrade to Production Config:

1. **Compute Resources**

   - EC2: t3.micro → t3.small/medium
   - RDS: db.t3.micro → db.t3.small

2. **High Availability**

   - Multi-AZ deployment
   - Application Load Balancer
   - Auto Scaling Group

3. **Enhanced Monitoring**
   - Detailed CloudWatch monitoring
   - ELK/Grafana log analysis
   - Custom metrics

## 🆘 Troubleshooting

### Common Issues:

1. **EC2 Not Accessible**

   - Check security group rules
   - Confirm Key Pair is correct
   - Validate AWS credentials

2. **Database Connection Failure**

   - Check DB password
   - Validate network connectivity
   - View application logs

3. **Frontend Cannot Access Backend**

   - Check CORS config
   - Validate API URL env variable
   - Test network connectivity

4. **WebSocket Connection Failure**
   - Confirm port 8080 is open
   - Check firewall settings
   - Validate WebSocket URL

### Getting Help:

```bash
# View Terraform outputs
terraform output

# View AWS resources
aws ec2 describe-instances --region us-west-2
aws s3 ls
aws rds describe-db-instances --region us-west-2

# View logs
ssh -i your-key.pem ec2-user@YOUR_EC2_IP
sudo tail -f /var/log/weblog-setup.log
docker logs weblog-backend
```

## 📝 Maintenance Checklist

### Weekly:

- [ ] EC2 instance status
- [ ] Application health check
- [ ] Database backups
- [ ] CloudWatch metrics

### Monthly:

- [ ] AWS billing
- [ ] Security updates
- [ ] Backup testing
- [ ] Performance optimization

### Quarterly:

- [ ] Architecture review
- [ ] Cost optimization
- [ ] Security audit
- [ ] Capacity planning
