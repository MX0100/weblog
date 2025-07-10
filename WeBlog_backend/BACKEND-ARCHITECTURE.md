# WeBlog 企业级后端架构文档

## 🏗️ 架构概览

WeBlog 采用现代化的企业级 Spring Boot 架构，遵循最佳实践和设计原则：

- **分层架构**: Controller → Service → Repository → Entity
- **保护性编程**: 全面的空值检查和边界条件处理
- **高内聚低耦合**: 模块化设计，职责分离
- **企业级安全**: JWT 认证 + Spring Security
- **API 标准化**: RESTful 设计 + 统一响应格式

## 📁 项目结构

```
src/main/java/io/github/mx0100/weblog/
├── common/                    # 公共组件
│   ├── ApiResponse.java       # 统一响应格式
│   └── ResponseCode.java      # 错误码枚举
├── config/                    # 配置类
│   ├── JwtConfig.java         # JWT 配置
│   └── SecurityConfig.java    # Spring Security 配置
├── controller/                # 控制器层
│   ├── AuthController.java    # 认证控制器
│   ├── UserController.java    # 用户控制器
│   ├── PostController.java    # 帖子控制器
│   └── CommentController.java # 评论控制器
├── dto/                       # 数据传输对象
│   ├── request/              # 请求 DTO
│   └── response/             # 响应 DTO
├── entity/                    # 实体类
│   ├── User.java             # 用户实体
│   ├── Post.java             # 帖子实体
│   ├── Comment.java          # 评论实体
│   └── enums/
│       └── Gender.java       # 性别枚举 (LGBTQ+ 友好)
├── exception/                 # 异常处理
│   └── GlobalExceptionHandler.java
├── repository/                # 数据访问层
│   ├── UserRepository.java
│   ├── PostRepository.java
│   └── CommentRepository.java
├── security/                  # 安全组件
│   ├── JwtAuthenticationFilter.java
│   └── UserPrincipal.java
├── service/                   # 业务逻辑层
│   ├── UserService.java
│   ├── PostService.java
│   └── CommentService.java
├── utils/                     # 工具类
│   ├── BeanUtils.java        # 对象映射工具
│   ├── JwtUtils.java         # JWT 工具
│   ├── PasswordUtils.java    # 密码工具
│   └── TimeUtils.java        # 时间工具
└── WeBlogApplication.java     # 启动类
```

## 🔧 核心技术栈

- **Spring Boot 3.x**: 核心框架
- **Spring Security**: 安全认证
- **Spring Data JPA**: 数据持久化
- **PostgreSQL**: 主数据库
- **Redis**: 缓存 (配置就绪)
- **JWT**: 无状态认证
- **BCrypt**: 密码加密
- **Validation**: 参数校验
- **Lombok**: 代码简化

## 🛡️ 安全设计

### JWT 认证机制

- **Token 过期时间**: 24 小时
- **密钥配置**: 可配置化
- **无状态设计**: 支持水平扩展

### 权限控制

- **用户权限**: 只能修改自己的信息
- **帖子权限**: 只有作者可以修改/删除
- **评论权限**: 作者或帖子作者可以删除

### 密码安全

- **BCrypt 加密**: 不可逆加密
- **强密码策略**: 可配置验证规则

## 📊 数据模型设计

### 用户模型 (User)

```java
- userId: Long (主键)
- username: String (唯一)
- password: String (BCrypt 加密)
- nickname: String
- gender: Gender (LGBTQ+ 友好枚举)
- createdAt: LocalDateTime (UTC)
- updatedAt: LocalDateTime (UTC)
```

### 帖子模型 (Post)

```java
- postId: Long (主键)
- authorId: Long (外键)
- content: String
- comments: List<Long> (评论ID数组)
- commentsCount: Integer
- createdAt: LocalDateTime (UTC)
- updatedAt: LocalDateTime (UTC)
```

### 评论模型 (Comment)

```java
- commentId: Long (主键)
- postId: Long (外键)
- authorId: Long (外键)
- content: String
- createdAt: LocalDateTime (UTC)
```

## 🔄 API 设计

### 认证 API

- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录

### 用户管理 API

- `GET /api/users/{userId}` - 获取用户信息
- `PUT /api/users/{userId}` - 更新用户信息
- `PUT /api/users/{userId}/password` - 修改密码

### 帖子管理 API

- `POST /api/posts` - 创建帖子
- `GET /api/posts` - 获取帖子列表 (分页)
- `GET /api/posts/{postId}` - 获取单个帖子
- `PUT /api/posts/{postId}` - 更新帖子
- `DELETE /api/posts/{postId}` - 删除帖子

### 评论管理 API

- `POST /api/posts/{postId}/comments` - 创建评论
- `GET /api/posts/{postId}/comments` - 获取帖子评论
- `POST /api/comments/batch` - 批量获取评论
- `DELETE /api/comments/{commentId}` - 删除评论

## ⚡ 性能优化

### 查询优化

- **批量查询**: 避免 N+1 问题
- **分页查询**: 帖子列表分页 (每页 10 条)
- **索引优化**: 关键字段建索引

### 缓存策略

- **Redis 配置**: 准备就绪，可扩展缓存
- **查询缓存**: JPA 二级缓存配置

### 数据库优化

- **连接池**: HikariCP 默认配置
- **事务管理**: 声明式事务

## 🏗️ 企业级特性

### 代码质量

- **保护性编程**: 全面的空值检查
- **异常处理**: 全局异常处理器
- **日志记录**: 结构化日志输出
- **英文注释**: 国际化友好

### 可维护性

- **模块化设计**: 职责分离
- **工具类提取**: 可复用组件
- **配置外部化**: 支持多环境

### 可扩展性

- **无状态设计**: 支持水平扩展
- **微服务友好**: 可拆分为微服务
- **API 版本化**: 预留版本管理

## 🐳 部署配置

### Docker 支持

- **多阶段构建**: 优化镜像大小
- **健康检查**: 容器状态监控
- **环境变量**: 配置外部化

### Docker Compose

- **PostgreSQL**: 数据库服务
- **Redis**: 缓存服务
- **应用服务**: Spring Boot 应用

## 🧪 测试策略

### 自动化测试

- **API 测试脚本**: 完整功能验证
- **单元测试**: Repository 层测试
- **集成测试**: 端到端测试

### 测试数据

- **初始化脚本**: data.sql 提供测试数据
- **默认用户**: admin/alice/bob/charlie
- **默认密码**: 123456

## 📈 监控和日志

### 日志配置

- **结构化日志**: JSON 格式输出
- **分级日志**: DEBUG/INFO/WARN/ERROR
- **SQL 日志**: 开发环境 SQL 跟踪

### 健康检查

- **Spring Actuator**: 应用状态监控
- **数据库连接**: 连接状态检查
- **Redis 连接**: 缓存状态检查

## 🚀 快速开始

### 1. 环境要求

- Java 17+
- PostgreSQL 15+
- Redis 7+ (可选)
- Docker & Docker Compose

### 2. 启动步骤

```bash
# 启动数据库和缓存
docker-compose up -d postgres redis

# 启动应用
./gradlew bootRun

# 运行 API 测试
./test-api.sh
```

### 3. 默认配置

- **应用端口**: 8080
- **数据库**: localhost:5432/weblog
- **Redis**: localhost:6379
- **默认用户**: admin/123456

## 🔮 扩展计划

### 功能扩展

- **文件上传**: 图片/附件支持
- **实时通知**: WebSocket 集成
- **搜索功能**: Elasticsearch 集成
- **内容审核**: AI 内容过滤

### 技术升级

- **响应式编程**: Spring WebFlux
- **微服务拆分**: Spring Cloud
- **消息队列**: RabbitMQ/Kafka
- **分布式缓存**: Redis Cluster

## 📚 最佳实践

### 开发规范

1. **命名规范**: 驼峰命名，语义化
2. **注释标准**: JavaDoc + 英文注释
3. **异常处理**: 统一异常响应
4. **日志记录**: 关键操作日志

### 安全规范

1. **输入验证**: 所有参数校验
2. **权限检查**: 每个操作验证权限
3. **密码管理**: BCrypt + 强密码策略
4. **敏感数据**: 不记录敏感信息

### 性能规范

1. **查询优化**: 避免 N+1 问题
2. **分页处理**: 大数据集分页
3. **缓存策略**: 热点数据缓存
4. **连接管理**: 数据库连接池

---

## 🎯 总结

WeBlog 后端系统是一个完整的企业级博客平台，具备：

✅ **完整功能**: 用户管理、帖子管理、评论系统
✅ **企业级架构**: 分层设计、模块化、可扩展
✅ **安全可靠**: JWT 认证、权限控制、数据加密
✅ **性能优化**: 查询优化、缓存策略、分页处理
✅ **可维护性**: 代码规范、异常处理、日志记录
✅ **部署友好**: Docker 支持、多环境配置
✅ **测试完备**: API 测试、初始化数据

该系统可以直接用于生产环境，也为后续功能扩展提供了坚实的基础。
