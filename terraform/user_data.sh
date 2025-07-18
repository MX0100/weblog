#!/bin/bash

# 更新系统
yum update -y

# 安装Docker
yum install -y docker
systemctl start docker
systemctl enable docker

# 将ec2-user添加到docker组
usermod -a -G docker ec2-user

# 安装Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# 安装git
yum install -y git

# 安装CloudWatch agent
yum install -y amazon-cloudwatch-agent

# 创建应用目录
mkdir -p /opt/weblog
cd /opt/weblog

# 创建Docker Compose文件
cat > docker-compose.prod.yml << EOF
version: '3.8'

services:
  weblog-backend:
    image: mx0100/weblog-backend:latest
    container_name: weblog-backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=${db_host}
      - DB_NAME=${db_name}
      - DB_USERNAME=${db_username}
      - DB_PASSWORD=${db_password}
      - JAVA_OPTS=-Xms256m -Xmx512m -Dspring.devtools.restart.enabled=false
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    logging:
      driver: "awslogs"
      options:
        awslogs-group: "/aws/ec2/weblog-${environment}"
        awslogs-region: "us-west-2"
        awslogs-stream: "weblog-backend"
EOF

# 拉取并启动应用
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d

# 设置开机自启动
cat > /etc/systemd/system/weblog.service << EOF
[Unit]
Description=WeBlog Application
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/weblog
ExecStart=/usr/local/bin/docker-compose -f docker-compose.prod.yml up -d
ExecStop=/usr/local/bin/docker-compose -f docker-compose.prod.yml down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOF

systemctl enable weblog.service

# 配置CloudWatch Agent
cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json << EOF
{
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/lib/docker/containers/*/*-json.log",
            "log_group_name": "/aws/ec2/weblog-${environment}",
            "log_stream_name": "docker-containers"
          }
        ]
      }
    }
  },
  "metrics": {
    "namespace": "WebLog/${environment}",
    "metrics_collected": {
      "cpu": {
        "measurement": [
          "cpu_usage_idle",
          "cpu_usage_iowait",
          "cpu_usage_user",
          "cpu_usage_system"
        ],
        "metrics_collection_interval": 60
      },
      "disk": {
        "measurement": [
          "used_percent"
        ],
        "metrics_collection_interval": 60,
        "resources": [
          "*"
        ]
      },
      "diskio": {
        "measurement": [
          "io_time"
        ],
        "metrics_collection_interval": 60,
        "resources": [
          "*"
        ]
      },
      "mem": {
        "measurement": [
          "mem_used_percent"
        ],
        "metrics_collection_interval": 60
      }
    }
  }
}
EOF

# 启动CloudWatch Agent
/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json

# 创建健康检查脚本
cat > /opt/weblog/health-check.sh << EOF
#!/bin/bash
curl -f http://localhost:8080/actuator/health || docker-compose -f /opt/weblog/docker-compose.prod.yml restart weblog-backend
EOF

chmod +x /opt/weblog/health-check.sh

# 添加到crontab进行健康检查
echo "*/5 * * * * /opt/weblog/health-check.sh" | crontab -

# 创建日志
echo "EC2 setup completed at $(date)" >> /var/log/weblog-setup.log 