#!/bin/bash

# ======================================
# Production Deployment Script
# ======================================
# This script deploys WeBlog to production environment
# Supports both AWS EC2 and Docker deployment modes

set -e  # Exit on any error

# ======================================
# Configuration Variables
# ======================================
APP_NAME="weblog"
DOCKER_IMAGE="weblog-backend"
DOCKER_TAG="${DOCKER_TAG:-latest}"
DEPLOY_MODE="${DEPLOY_MODE:-ec2}"  # ec2 or docker

# AWS Configuration
AWS_REGION="${AWS_REGION:-us-east-1}"
EC2_INSTANCE_ID="${EC2_INSTANCE_ID}"
KEY_FILE="${KEY_FILE:-weblog-test-key.pem}"

# Database Configuration (Production RDS)
DB_HOST="${DB_HOST}"
DB_NAME="${DB_NAME:-weblog}"
DB_USERNAME="${DB_USERNAME}"
DB_PASSWORD="${DB_PASSWORD}"

# Application Configuration
JWT_SECRET="${JWT_SECRET}"
CORS_ORIGINS="${CORS_ORIGINS:-https://yourdomain.com}"

# AWS Services Configuration
S3_BUCKET="${S3_BUCKET}"
CLOUDFRONT_DOMAIN="${CLOUDFRONT_DOMAIN}"

# ======================================
# Logging Functions
# ======================================
log_info() {
    echo "ℹ️  [INFO] $1"
}

log_success() {
    echo "✅ [SUCCESS] $1"
}

log_error() {
    echo "❌ [ERROR] $1"
    exit 1
}

log_warning() {
    echo "⚠️  [WARNING] $1"
}

# ======================================
# Validation Functions
# ======================================
validate_env_vars() {
    log_info "Validating environment variables..."
    
    local required_vars=(
        "DB_HOST"
        "DB_USERNAME" 
        "DB_PASSWORD"
        "JWT_SECRET"
    )
    
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var}" ]]; then
            log_error "Required environment variable $var is not set"
        fi
    done
    
    if [[ "$DEPLOY_MODE" == "ec2" && -z "$EC2_INSTANCE_ID" ]]; then
        log_error "EC2_INSTANCE_ID is required for EC2 deployment"
    fi
    
    log_success "Environment validation passed"
}

# ======================================
# Build Functions
# ======================================
build_backend() {
    log_info "Building backend Docker image..."
    
    cd WeBlog_backend
    
    # Build production image
    docker build \
        -f Dockerfile \
        -t "${DOCKER_IMAGE}:${DOCKER_TAG}" \
        .
    
    log_success "Backend Docker image built: ${DOCKER_IMAGE}:${DOCKER_TAG}"
    cd ..
}

build_frontend() {
    log_info "Building frontend for production..."
    
    cd WeBlog-frontend
    
    # Set production environment variables
    export VITE_API_BASE_URL="https://${CLOUDFRONT_DOMAIN}"
    export VITE_WS_URL="wss://${CLOUDFRONT_DOMAIN}/ws"
    export VITE_ENV_NAME="production"
    export VITE_ENABLE_DEBUG="false"
    export VITE_ENABLE_ANALYTICS="true"
    export VITE_CLOUDFRONT_DOMAIN="${CLOUDFRONT_DOMAIN}"
    export VITE_S3_BUCKET="${S3_BUCKET}"
    export VITE_AWS_REGION="${AWS_REGION}"
    
    # Install dependencies and build
    npm ci --only=production
    npm run build
    
    log_success "Frontend built successfully"
    cd ..
}

# ======================================
# Deployment Functions
# ======================================
deploy_to_ec2() {
    log_info "Deploying to EC2 instance: ${EC2_INSTANCE_ID}"
    
    # Create deployment package
    create_deployment_package
    
    # Upload and execute on EC2
    local ec2_ip=$(aws ec2 describe-instances \
        --instance-ids "${EC2_INSTANCE_ID}" \
        --query 'Reservations[0].Instances[0].PublicIpAddress' \
        --output text \
        --region "${AWS_REGION}")
    
    if [[ "$ec2_ip" == "None" || -z "$ec2_ip" ]]; then
        log_error "Could not get EC2 instance IP address"
    fi
    
    log_info "Deploying to EC2 IP: ${ec2_ip}"
    
    # Copy deployment package
    scp -i "${KEY_FILE}" \
        -o StrictHostKeyChecking=no \
        weblog-deploy.tar.gz \
        "ec2-user@${ec2_ip}:/tmp/"
    
    # Execute deployment script on EC2
    ssh -i "${KEY_FILE}" \
        -o StrictHostKeyChecking=no \
        "ec2-user@${ec2_ip}" \
        "cd /tmp && tar -xzf weblog-deploy.tar.gz && chmod +x deploy-on-ec2.sh && ./deploy-on-ec2.sh"
    
    log_success "Deployment to EC2 completed"
}

create_deployment_package() {
    log_info "Creating deployment package..."
    
    # Create temporary deployment directory
    rm -rf deploy-temp
    mkdir deploy-temp
    
    # Copy necessary files
    cp docker-compose.prod.yml deploy-temp/
    cp WeBlog_backend/Dockerfile deploy-temp/
    
    # Create environment file for production
    cat > deploy-temp/.env.prod << EOF
# Production Environment Configuration
ENV=production
DOCKER_IMAGE=${DOCKER_IMAGE}:${DOCKER_TAG}

# Database Configuration
DB_HOST=${DB_HOST}
DB_NAME=${DB_NAME}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}

# Security Configuration
JWT_SECRET=${JWT_SECRET}
CORS_ORIGINS=${CORS_ORIGINS}

# AWS Configuration
S3_BUCKET=${S3_BUCKET}
CLOUDFRONT_DOMAIN=${CLOUDFRONT_DOMAIN}
AWS_REGION=${AWS_REGION}
EOF
    
    # Create EC2 deployment script
    cat > deploy-temp/deploy-on-ec2.sh << 'EOF'
#!/bin/bash
set -e

log_info() { echo "ℹ️  [INFO] $1"; }
log_success() { echo "✅ [SUCCESS] $1"; }
log_error() { echo "❌ [ERROR] $1"; exit 1; }

log_info "Starting deployment on EC2..."

# Stop existing containers
sudo docker-compose -f docker-compose.prod.yml --env-file .env.prod down || true

# Pull latest images
sudo docker-compose -f docker-compose.prod.yml --env-file .env.prod pull

# Start services
sudo docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Wait for health check
log_info "Waiting for application to start..."
sleep 30

# Verify deployment
if curl -f http://localhost:8080/actuator/health; then
    log_success "Application is healthy"
else
    log_error "Application health check failed"
fi

log_success "Deployment completed successfully"
EOF
    
    # Create production docker-compose
    cat > deploy-temp/docker-compose.prod.yml << 'EOF'
services:
  weblog-app:
    image: ${DOCKER_IMAGE}
    ports:
      - "8080:8080"
    environment:
      - ENV=production
      - DB_HOST=${DB_HOST}
      - DB_NAME=${DB_NAME}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - CORS_ORIGINS=${CORS_ORIGINS}
      - S3_BUCKET=${S3_BUCKET}
      - CLOUDFRONT_DOMAIN=${CLOUDFRONT_DOMAIN}
      - AWS_REGION=${AWS_REGION}
      - INIT_MODE=never
      - DDL_AUTO=validate
      - SHOW_SQL=false
      - LOG_LEVEL=WARN
      - APP_LOG_LEVEL=INFO
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
EOF
    
    # Create deployment package
    tar -czf weblog-deploy.tar.gz -C deploy-temp .
    
    log_success "Deployment package created"
}

deploy_frontend_to_s3() {
    log_info "Deploying frontend to S3..."
    
    if [[ -z "$S3_BUCKET" ]]; then
        log_warning "S3_BUCKET not set, skipping frontend deployment"
        return
    fi
    
    # Sync build files to S3
    aws s3 sync WeBlog-frontend/dist/ "s3://${S3_BUCKET}/" \
        --delete \
        --region "${AWS_REGION}"
    
    # Invalidate CloudFront cache
    if [[ -n "$CLOUDFRONT_DOMAIN" ]]; then
        local distribution_id=$(aws cloudfront list-distributions \
            --query "DistributionList.Items[?contains(Aliases.Items, '${CLOUDFRONT_DOMAIN}')].Id" \
            --output text \
            --region "${AWS_REGION}")
        
        if [[ -n "$distribution_id" ]]; then
            aws cloudfront create-invalidation \
                --distribution-id "$distribution_id" \
                --paths "/*" \
                --region "${AWS_REGION}"
            log_success "CloudFront cache invalidated"
        fi
    fi
    
    log_success "Frontend deployed to S3"
}

# ======================================
# Main Deployment Flow
# ======================================
main() {
    log_info "Starting WeBlog production deployment..."
    log_info "Deploy mode: ${DEPLOY_MODE}"
    
    # Validate prerequisites
    validate_env_vars
    
    # Build applications
    build_backend
    build_frontend
    
    # Deploy based on mode
    case "$DEPLOY_MODE" in
        "ec2")
            deploy_to_ec2
            ;;
        "docker")
            log_info "Docker deployment mode selected"
            create_deployment_package
            log_success "Deployment package ready. Use docker-compose to deploy."
            ;;
        *)
            log_error "Unknown deploy mode: $DEPLOY_MODE"
            ;;
    esac
    
    # Deploy frontend to S3
    deploy_frontend_to_s3
    
    # Cleanup
    rm -rf deploy-temp weblog-deploy.tar.gz 2>/dev/null || true
    
    log_success "🎉 WeBlog production deployment completed successfully!"
    
    if [[ -n "$CLOUDFRONT_DOMAIN" ]]; then
        log_info "🌐 Application URL: https://${CLOUDFRONT_DOMAIN}"
    fi
}

# ======================================
# Script Entry Point
# ======================================
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi 