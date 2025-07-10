# WeBlog 项目

这是一个使用 Spring Boot 3.5.3 构建的博客应用，使用 PostgreSQL 数据库和 Redis 缓存。

## 技术栈

- **Java 17**
- **Spring Boot 3.5.3**
- **PostgreSQL 15**
- **Redis 7**
- **Docker & Docker Compose**

## 快速开始

### 本地开发

1. 启动数据库和 Redis 服务：

```bash
docker-compose up postgres redis -d
```

2. 运行应用：

```bash
./gradlew bootRun
```

3. 访问应用：
   - 应用地址：http://localhost:8080
   - 数据库：localhost:5432 (weblog/password)
   - Redis：localhost:6379

### Docker 部署

1. 构建并启动所有服务：

```bash
docker-compose up -d
```

2. 停止服务：

```bash
docker-compose down
```

3. 查看日志：

```bash
docker-compose logs -f weblog-app
```

## EC2 部署

在 EC2 实例上只需要安装 Docker 和 Docker Compose：

```bash
# 安装 Docker
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 克隆项目并启动
git clone <your-repo-url>
cd WeBlog
docker-compose up -d
```

## 配置说明

- **本地开发配置**：`application.properties`
- **Docker 环境配置**：`application-docker.properties`
- **数据库**：自动创建表结构 (hibernate.ddl-auto=update)
- **邮件配置**：需要在配置文件中填写实际的邮件服务器信息

## 数据持久化

数据库和 Redis 数据都会持久化到 Docker volumes 中，重启容器不会丢失数据。
