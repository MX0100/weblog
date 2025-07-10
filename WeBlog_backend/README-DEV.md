# WeBlog Development Environment Setup Guide

## 🚀 快速开始

### 方式一：使用启动脚本（推荐）

**Windows 用户：**

```bash
.\dev-start.bat
```

**Linux/macOS 用户：**

```bash
./dev-start.sh
```

### 方式二：手动启动

```bash
# 构建并启动开发环境
docker-compose -f compose.dev.yaml up --build

# 后台运行
docker-compose -f compose.dev.yaml up --build -d
```

## 📋 功能特性

### ✅ 热部署支持

- ✨ **代码修改自动重启**：修改 Java 代码后，应用会自动重启
- 🔄 **LiveReload**：支持浏览器自动刷新
- ⚡ **快速重启**：重启时间优化，通常在 3-5 秒内完成

### 🛠️ 开发工具集成

- 🗄️ **PostgreSQL 数据库**：端口 5432
- 🔴 **Redis 缓存**：端口 6380
- 🌐 **pgAdmin**：http://localhost:5050
- 📊 **详细日志**：SQL 查询、调试信息等

### 📁 源码挂载

- `./src` → `/app/src`：源码目录实时同步
- `./build` → `/app/build`：构建输出目录
- Gradle 缓存持久化，加速后续构建

## 🎯 使用说明

### 开发流程

1. **启动开发环境**

   ```bash
   .\dev-start.bat  # Windows
   ./dev-start.sh   # Linux/macOS
   ```

2. **修改代码**

   - 在 IDE 中正常编辑 `src/` 目录下的 Java 文件
   - 保存文件后，应用会自动检测变化并重启

3. **查看日志**

   ```bash
   # 查看应用日志
   docker-compose -f compose.dev.yaml logs -f weblog-app-dev

   # 查看所有服务日志
   docker-compose -f compose.dev.yaml logs -f
   ```

4. **停止环境**
   ```bash
   docker-compose -f compose.dev.yaml down
   ```

### 数据库管理

- **pgAdmin 访问**：http://localhost:5050

  - 邮箱：`admin@weblog.com`
  - 密码：`admin123`

- **直接连接数据库**：
  - 主机：`localhost`
  - 端口：`5432`
  - 数据库：`weblog`
  - 用户名：`weblog`
  - 密码：`password`

### 性能优化

#### Gradle 缓存

开发环境使用了持久化的 Gradle 缓存卷，首次构建后后续启动会更快。

#### 重启优化

DevTools 配置了最优的重启参数：

- `poll-interval`: 1000ms（检查文件变化间隔）
- `quiet-period`: 400ms（变化检测静默期）

## 🔧 自定义配置

### 修改热部署设置

编辑 `src/main/resources/application-dev.properties`：

```properties
# 调整重启间隔（毫秒）
spring.devtools.restart.poll-interval=1000
spring.devtools.restart.quiet-period=400

# 禁用热部署
spring.devtools.restart.enabled=false
```

### 添加排除路径

如果某些文件变化不需要重启应用，可以配置排除：

```properties
# 排除静态资源变化
spring.devtools.restart.exclude=static/**,public/**,templates/**
```

## 🐛 故障排除

### 应用无法启动

```bash
# 查看详细错误日志
docker-compose -f compose.dev.yaml logs weblog-app-dev

# 重新构建镜像
docker-compose -f compose.dev.yaml build --no-cache weblog-app-dev
```

### 热部署不工作

1. 确认文件已保存
2. 检查是否在排除路径中
3. 查看容器日志确认是否检测到变化
4. 重启开发环境

### 端口冲突

如果遇到端口冲突，修改 `compose.dev.yaml` 中的端口映射：

```yaml
ports:
  - "8081:8080" # 将主机端口改为 8081
```

### 数据库连接问题

```bash
# 检查数据库容器状态
docker-compose -f compose.dev.yaml ps postgres

# 重启数据库
docker-compose -f compose.dev.yaml restart postgres
```

## 📚 相关链接

- [Spring Boot DevTools 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.devtools)
- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [项目主 README](./README.md)

## 💡 提示

- 第一次启动会比较慢，因为需要下载依赖和构建镜像
- 建议在 IDE 中配置自动保存，以获得最佳的热部署体验
- 可以配合前端开发服务器一起使用，实现全栈热部署
