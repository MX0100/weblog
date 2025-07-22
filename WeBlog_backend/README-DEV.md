# 🔧 WeBlog 后端开发指南

本文档提供 WeBlog 后端开发环境的详细设置和使用说明。

## 🚀 快速开始

### 📋 前置条件

确保已安装以下工具：

- Docker & Docker Compose
- Git
- 支持 Bash 的终端

### 🏗️ 环境启动

```bash
# 克隆项目
git clone <项目地址>
cd WeBlog/WeBlog_backend

# 构建并启动开发环境
docker-compose -f compose.yaml -f docker-compose.dev.yml up --build

# 后台运行
docker-compose -f compose.yaml -f docker-compose.dev.yml up --build -d
```

## 📋 功能特性

### ✅ 热部署支持

- **Spring Boot DevTools**：代码修改后自动重启应用
- **LiveReload**：前端资源修改后自动刷新浏览器
- **源码挂载**：容器内代码与宿主机实时同步

### 🗄️ 数据库支持

- **PostgreSQL 15**：生产级关系型数据库
- **pgAdmin 4**：Web 界面数据库管理工具
- **数据持久化**：开发数据保存在 Docker 卷中

### 📊 监控调试

- **Actuator 端点**：健康检查和应用监控
- **详细日志**：多级别日志输出
- **调试模式**：支持远程调试连接

## 🔧 开发工作流

### 📝 代码开发

1. 修改源代码
2. DevTools 自动检测变化
3. 应用自动重启（热部署）
4. 浏览器自动刷新（LiveReload）

### 🗄️ 数据库操作

- 访问 `http://localhost:5050` 使用 pgAdmin
- 默认登录：`admin@weblog.dev` / `admin`
- 连接信息：`postgres:5432` / `weblog` / `password`

### 📋 常用命令

```bash
# 查看应用日志
docker-compose -f compose.yaml -f docker-compose.dev.yml logs -f weblog-app

# 查看所有服务日志
docker-compose -f compose.yaml -f docker-compose.dev.yml logs -f

# 停止所有服务
docker-compose -f compose.yaml -f docker-compose.dev.yml down

# 重启特定服务
docker-compose -f compose.yaml -f docker-compose.dev.yml restart weblog-app
```

## 🌐 服务端口

| 服务             | 端口  | 用途           |
| ---------------- | ----- | -------------- |
| Spring Boot 应用 | 8080  | 主要 API 服务  |
| LiveReload       | 35729 | 前端热重载     |
| PostgreSQL       | 5432  | 数据库连接     |
| pgAdmin          | 5050  | 数据库管理界面 |

## 🔍 API 测试

### 健康检查

```bash
curl http://localhost:8080/actuator/health
```

### 用户注册

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "nickname": "测试用户"
  }'
```

### 用户登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

## 🛠️ 故障排除

### 常见问题

**问题：容器启动失败**

```bash
# 检查容器状态
docker-compose -f compose.yaml -f docker-compose.dev.yml ps

# 查看错误日志
docker-compose -f compose.yaml -f docker-compose.dev.yml logs weblog-app

# 重新构建
docker-compose -f compose.yaml -f docker-compose.dev.yml build --no-cache weblog-app
```

**问题：数据库连接失败**

```bash
# 检查PostgreSQL状态
docker-compose -f compose.yaml -f docker-compose.dev.yml ps postgres

# 重启数据库
docker-compose -f compose.yaml -f docker-compose.dev.yml restart postgres
```

**问题：热重载不工作**

- 确保源码正确挂载到容器
- 检查 DevTools 配置是否启用
- 验证文件监控权限

### 🔄 完全重置

```bash
# 停止并删除所有容器、网络和卷
docker-compose -f compose.yaml -f docker-compose.dev.yml down -v

# 清理镜像
docker-compose -f compose.yaml -f docker-compose.dev.yml build --no-cache

# 重新启动
docker-compose -f compose.yaml -f docker-compose.dev.yml up -d
```

## 📊 性能优化

### 🚀 开发环境优化

- **Gradle 缓存**：依赖包缓存在 Docker 卷中
- **增量编译**：只编译修改的文件
- **内存设置**：JVM 内存针对开发环境优化

### 📈 监控指标

- 应用启动时间：通常 < 30 秒
- 热重载时间：通常 < 10 秒
- 内存使用：通常 < 512MB
