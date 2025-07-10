# WeBlog API 设计文档

## 概述

WeBlog 是一个博客系统，支持用户注册、登录、发帖、评论等功能。

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **认证方式**: JWT Token (放在 Header 的 Authorization: Bearer {token})

## 数据模型

### 用户实体 (User)

```json
{
  "userId": 1,
  "username": "johndoe",
  "nickname": "John Doe",
  "password": "password123",
  "gender": "MALE",
  "profileimg": "https://example.com/avatar.jpg",
  "hobby": ["编程", "阅读", "旅游"],
  "relationshipStatus": "COUPLED",
  "partnerId": 2,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**字段说明**：

- `profileimg`: 用户头像 URL
- `hobby`: 用户兴趣爱好列表
- `relationshipStatus`: 关系状态 (SINGLE, COUPLED)
- `partnerId`: 配对的情侣 ID（单身时为 null）

**🎯 情侣博客系统设计**：

- 使用独立的用户关系表管理配对关系，提供更好的性能和数据一致性
- 帖子仍然属于个人，但情侣双方可以互相看到对方的帖子
- 实现私密博客：只有情侣之间可以看到彼此的内容，外人无法访问

**性别选项说明**：

- `MALE`: 男性
- `FEMALE`: 女性
- `NON_BINARY`: 非二元性别
- `TRANSGENDER_MALE`: 跨性别男性
- `TRANSGENDER_FEMALE`: 跨性别女性
- `GENDERFLUID`: 流动性别
- `AGENDER`: 无性别
- `OTHER`: 其他
- `PREFER_NOT_TO_SAY`: 不愿透露

### 帖子实体 (Post)

```json
{
  "postId": 1,
  "userId": 1,
  "content": "这是一篇博客内容...",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**🎯 情侣博客帖子设计**：

- `userId`: 帖子的作者 ID，明确帖子归属
- 帖子仍然属于个人，但实现情侣间的私密共享
- 可见性规则：只有情侣双方可以看到彼此的帖子
- 通过用户关系表来控制帖子的访问权限

### 评论实体 (Comment)

```json
{
  "commentId": 1,
  "postId": 1,
  "userId": 2,
  "content": "很好的文章！",
  "createdAt": "2024-01-15T11:00:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

**🎯 情侣博客评论设计**：

- `postId`: 关联评论所属的帖子
- `userId`: 评论的作者 ID
- 评论权限：只有情侣双方可以对彼此的帖子进行评论
- 评论也遵循私密性规则，只有情侣双方可见

### 用户关系实体 (UserRelationship)

```json
{
  "id": 1,
  "user1Id": 1,
  "user2Id": 2,
  "requesterUserId": 1,
  "relationshipType": "COUPLE",
  "status": "PENDING",
  "createdAt": "2024-01-15T10:30:00",
  "endedAt": null
}
```

**🎯 用户关系表设计详解**：

- `user1Id`: 用户 1 的 ID（总是较小的 ID，用于数据库索引优化）
- `user2Id`: 用户 2 的 ID（总是较大的 ID，用于数据库索引优化）
- `requesterUserId`: 配对请求发送者的用户 ID（保存原始请求发送者信息）
- `relationshipType`: 关系类型（目前为 COUPLE）
- `status`: 关系状态（PENDING, ACTIVE, INACTIVE）
- `endedAt`: 关系结束时间（拒绝或分手时设置）

**🔑 核心设计逻辑**：

1. **ID 排序机制**：

   - 系统自动确保 `user1Id < user2Id`，避免重复关系记录
   - 例如：用户 3 向用户 1 发送请求，存储为 `user1Id=1, user2Id=3`

2. **请求发送者标识**：

   - `requesterUserId` 字段保存原始请求发送者的 ID
   - 即使 ID 被重新排序，仍能准确识别谁是请求发送者

3. **状态管理**：
   - `PENDING`: 配对请求待处理
   - `ACTIVE`: 配对成功，情侣关系建立
   - `INACTIVE`: 配对被拒绝或关系已结束

**优点**：

- 🚀 高性能查询，支持索引优化
- 🔒 数据一致性强，单一数据源
- 📈 支持完整的配对请求流程
- 🛠️ 易于维护和扩展
- 🎯 准确的请求发送者追踪

## API 接口

### 1. 认证相关

#### 1.1 用户注册

- **URL**: `POST /api/auth/register`
- **描述**: 用户注册

**请求体**:

```json
{
  "username": "johndoe",
  "password": "password123",
  "nickname": "John Doe",
  "gender": "MALE",
  "profileimg": "https://example.com/avatar.jpg",
  "hobby": ["编程", "阅读", "旅游"]
}
```

**响应**:

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "username": "johndoe",
    "nickname": "John Doe",
    "gender": "MALE",
    "profileimg": "https://example.com/avatar.jpg",
    "hobby": ["编程", "阅读", "旅游"],
    "relationshipStatus": "SINGLE",
    "partnerId": null,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

#### 1.2 用户登录

- **URL**: `POST /api/auth/login`
- **描述**: 用户登录

**请求体**:

```json
{
  "username": "johndoe",
  "password": "password123"
}
```

**响应**:

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "userId": 1,
      "username": "johndoe",
      "nickname": "John Doe",
      "gender": "MALE",
      "profileimg": "https://example.com/avatar.jpg",
      "hobby": ["编程", "阅读", "旅游"],
      "relationshipStatus": "COUPLED",
      "partnerId": 2,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  }
}
```

### 2. 用户相关

#### 2.1 获取用户信息

- **URL**: `GET /api/users/{userId}`
- **描述**: 根据用户 ID 获取用户信息（包含配对状态）
- **认证**: 需要

**响应**:

```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "userId": 1,
    "username": "johndoe",
    "nickname": "John Doe",
    "gender": "MALE",
    "profileimg": "https://example.com/avatar.jpg",
    "hobby": ["编程", "阅读", "旅游"],
    "relationshipStatus": "COUPLED",
    "partnerId": 2,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**配对状态说明**:

- `relationshipStatus = "SINGLE"`: 用户处于单身状态，`partnerId = null`
- `relationshipStatus = "COUPLED"`: 用户已配对，`partnerId` 为配对对象的用户 ID

#### 2.2 更新用户信息

- **URL**: `PUT /api/users/{userId}`
- **描述**: 更新用户信息
- **认证**: 需要

**请求体**:

```json
{
  "nickname": "John Smith",
  "gender": "MALE",
  "profileimg": "https://example.com/new-avatar.jpg",
  "hobby": ["编程", "阅读", "旅游", "摄影"]
}
```

**响应**:

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "userId": 1,
    "username": "johndoe",
    "nickname": "John Smith",
    "gender": "MALE",
    "profileimg": "https://example.com/new-avatar.jpg",
    "hobby": ["编程", "阅读", "旅游", "摄影"],
    "relationshipStatus": "SINGLE",
    "partnerId": null,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T12:00:00"
  }
}
```

#### 2.3 修改密码

- **URL**: `PUT /api/users/{userId}/password`
- **描述**: 修改用户密码
- **认证**: 需要（只能修改自己的密码）

**请求体**:

```json
{
  "oldPassword": "oldpassword123",
  "newPassword": "newpassword456"
}
```

**响应**:

```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null
}
```

### 3. 帖子相关

#### 3.1 获取帖子列表（分页）

- **URL**: `GET /api/posts`
- **描述**: 分页获取帖子列表（根据用户配对状态进行过滤）
- **参数**:
  - `page`: 页码，从 0 开始 (默认: 0)
  - `size`: 每页大小 (默认: 10)
  - `sort`: 排序字段 (默认: createdAt,desc)

**过滤逻辑**:

- **单身用户** (relationshipStatus = "SINGLE")：只返回自己的帖子
- **配对用户** (relationshipStatus = "COUPLED")：返回自己和情侣的帖子

**响应**:

```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "content": [
      {
        "postId": 1,
        "userId": 1,
        "content": "这是一篇博客内容...",
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

#### 3.2 获取单个帖子详情

- **URL**: `GET /api/posts/{postId}`
- **描述**: 根据帖子 ID 获取详情（需要权限检查）

**权限检查逻辑**:

- 帖子作者本人：可以访问
- 帖子作者的情侣：可以访问
- 其他用户：返回 403 权限不足

**响应**:

```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "postId": 1,
    "userId": 1,
    "content": "这是一篇博客内容...",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

#### 3.3 创建帖子

- **URL**: `POST /api/posts`
- **描述**: 创建新帖子
- **认证**: 需要

**请求体**:

```json
{
  "content": "这是我的第一篇博客..."
}
```

**响应**:

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "postId": 1,
    "userId": 1,
    "content": "这是我的第一篇博客...",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

#### 3.4 更新帖子

- **URL**: `PUT /api/posts/{postId}`
- **描述**: 更新帖子内容
- **认证**: 需要（只能更新自己的帖子）

**请求体**:

```json
{
  "content": "更新后的博客内容..."
}
```

**响应**:

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "postId": 1,
    "userId": 1,
    "content": "更新后的博客内容...",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T12:00:00"
  }
}
```

#### 3.5 删除帖子

- **URL**: `DELETE /api/posts/{postId}`
- **描述**: 删除帖子
- **认证**: 需要（只能删除自己的帖子）

**响应**:

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

### 4. 评论相关

#### 4.1 获取帖子的评论

- **URL**: `GET /api/posts/{postId}/comments`
- **描述**: 获取指定帖子的评论列表（只有情侣双方可以看到评论）
- **参数**:
  - `page`: 页码，从 0 开始 (默认: 0)
  - `size`: 每页大小 (默认: 20)

**响应**:

```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "content": [
      {
        "commentId": 1,
        "postId": 1,
        "userId": 2,
        "content": "很好的文章！",
        "createdAt": "2024-01-15T11:00:00",
        "updatedAt": "2024-01-15T11:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

#### 4.2 添加评论

- **URL**: `POST /api/posts/{postId}/comments`
- **描述**: 为指定帖子添加评论（只有情侣双方可以评论）
- **认证**: 需要

**请求体**:

```json
{
  "content": "很好的文章！"
}
```

**响应**:

```json
{
  "code": 200,
  "message": "评论成功",
  "data": {
    "commentId": 1,
    "postId": 1,
    "userId": 2,
    "content": "很好的文章！",
    "createdAt": "2024-01-15T11:00:00",
    "updatedAt": "2024-01-15T11:00:00"
  }
}
```

**说明**: 创建评论成功后，系统会自动建立评论与帖子的关联关系。

#### 4.3 删除评论

- **URL**: `DELETE /api/comments/{commentId}`
- **描述**: 删除评论
- **认证**: 需要（只能删除自己的评论）

**响应**:

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

**说明**: 删除评论成功后，系统会自动清理相关的关联关系。

#### 4.4 批量获取评论

- **URL**: `POST /api/comments/batch`
- **描述**: 根据评论 ID 数组批量获取评论详情
- **认证**: 需要（只能获取有权限访问的评论）

**请求体**:

```json
{
  "commentIds": [1, 2, 3, 4, 5]
}
```

**响应**:

```json
{
  "code": 200,
  "message": "获取成功",
  "data": [
    {
      "commentId": 1,
      "postId": 1,
      "userId": 2,
      "content": "很好的文章！",
      "createdAt": "2024-01-15T11:00:00",
      "updatedAt": "2024-01-15T11:00:00"
    },
    {
      "commentId": 2,
      "postId": 1,
      "userId": 3,
      "content": "我也觉得很有用！",
      "createdAt": "2024-01-15T11:30:00",
      "updatedAt": "2024-01-15T11:30:00"
    }
  ]
}
```

**说明**:

- 此接口用于根据评论 ID 数组批量加载评论详情
- 返回的评论顺序与请求的 ID 顺序保持一致
- 如果某个 ID 对应的评论不存在或无权限访问，则在返回数组中跳过该项
- 只返回用户有权限访问的评论（即情侣双方的评论）

## 统一响应格式

所有 API 响应都使用以下统一格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

## 错误码说明

| 错误码 | 说明                       |
| ------ | -------------------------- |
| 200    | 操作成功                   |
| 400    | 请求参数错误               |
| 401    | 未认证或 token 过期        |
| 403    | 权限不足                   |
| 404    | 资源不存在                 |
| 409    | 数据冲突（如用户名已存在） |
| 500    | 服务器内部错误             |

## 错误响应示例

```json
{
  "code": 401,
  "message": "用户未登录或 token 已过期",
  "data": null
}
```

## 字段约束

### 用户相关

- `username`: 3-20 字符，只能包含字母、数字、下划线
- `password`: 6-50 字符
- `nickname`: 1-50 字符
- `gender`: 枚举值 [MALE, FEMALE, NON_BINARY, TRANSGENDER_MALE, TRANSGENDER_FEMALE, GENDERFLUID, AGENDER, OTHER, PREFER_NOT_TO_SAY]
- `relationshipStatus`: 关系状态，枚举值 [SINGLE, COUPLED]
- `partnerId`: 配对对象的用户 ID，单身时为 null
- `profileimg`: 头像 URL，支持 http/https 协议
- `hobby`: 兴趣爱好数组，每个元素 1-20 字符，数组长度不超过 10
- `oldPassword`: 修改密码时需要提供原密码进行验证
- `newPassword`: 新密码，需符合密码规则（6-50 字符）

### 帖子相关

- `content`: 1-10000 字符

### 评论相关

- `content`: 1-500 字符

## 情侣博客系统业务规则

### 用户关系管理

#### 1. 用户配对逻辑

- 用户注册时 `relationshipStatus` 为 "SINGLE"，`partnerId` 为 null
- 配对关系通过独立的 `user_relationships` 表管理
- 配对关系是双向的，必须双方都同意才能建立
- 每个用户在任何时间只能与一个用户配对

#### 2. 帖子可见性规则

- **私密性**：帖子只对情侣双方可见，其他用户无法访问
- **归属性**：帖子仍然属于个人，通过 `userId` 字段标识作者
- **权限检查**：
  ```
  访问帖子的权限判断：
  1. 如果请求者是帖子作者 → 允许访问
  2. 如果请求者是帖子作者的情侣 → 允许访问
  3. 其他情况 → 拒绝访问
  ```

#### 3. 评论权限规则

- 只有情侣双方可以对彼此的帖子进行评论
- 评论也遵循私密性，只有情侣双方可见
- 外人无法看到或评论私密帖子

### 需要权限控制的 API 接口

#### 需要检查情侣关系的接口：

- `GET /api/posts` - 获取帖子列表
  - **单身用户** (relationshipStatus = "SINGLE"): 只返回自己的帖子
  - **配对用户** (relationshipStatus = "COUPLED"): 返回自己和情侣的帖子
- `GET /api/posts/{postId}` - 获取帖子详情
  - **权限检查**: 只有帖子作者或其情侣可以访问
- `PUT /api/posts/{postId}` - 更新帖子
  - **权限限制**: 只能更新自己的帖子
- `DELETE /api/posts/{postId}` - 删除帖子
  - **权限限制**: 只能删除自己的帖子
- `GET /api/posts/{postId}/comments` - 获取帖子评论
  - **权限检查**: 只有有权限访问帖子的用户才能看到评论
- `POST /api/posts/{postId}/comments` - 添加评论
  - **权限检查**: 只有有权限访问帖子的用户才能评论
- `DELETE /api/comments/{commentId}` - 删除评论
  - **权限限制**: 只能删除自己的评论

### 常用业务查询示例

#### 获取用户可见的帖子列表（根据配对状态过滤）

```sql
-- 获取用户的配对关系
SELECT partner_id FROM (
    SELECT
        CASE
            WHEN user1_id = ? THEN user2_id
            WHEN user2_id = ? THEN user1_id
        END as partner_id
    FROM user_relationships
    WHERE (user1_id = ? OR user2_id = ?)
    AND status = 'ACTIVE'
) sub WHERE partner_id IS NOT NULL;

-- 根据配对状态返回帖子
SELECT * FROM posts
WHERE user_id IN (
    -- 用户自己
    SELECT ?
    UNION ALL
    -- 用户的配对情侣（如果存在）
    SELECT partner_id FROM (
        SELECT
            CASE
                WHEN user1_id = ? THEN user2_id
                WHEN user2_id = ? THEN user1_id
            END as partner_id
        FROM user_relationships
        WHERE (user1_id = ? OR user2_id = ?)
        AND status = 'ACTIVE'
    ) sub WHERE partner_id IS NOT NULL
)
ORDER BY created_at DESC;
```

**Java 实现逻辑示例**:

```java
public List<Post> getPostsForUser(Long userId) {
    List<Long> visibleUserIds = new ArrayList<>();

    // 添加用户自己的ID
    visibleUserIds.add(userId);

    // 获取配对的情侣ID
    Optional<Long> partnerId = getPartnerUserId(userId);
    if (partnerId.isPresent()) {
        visibleUserIds.add(partnerId.get());
    }

    return postRepository.findByUserIdInOrderByCreatedAtDesc(visibleUserIds);
}

private Optional<Long> getPartnerUserId(Long userId) {
    return userRelationshipRepository
        .findActiveRelationshipByUserId(userId)
        .map(rel -> rel.getOtherUserId(userId));
}
```

#### 检查用户是否有权限访问帖子

```sql
-- 检查权限：用户是否可以访问指定帖子
SELECT COUNT(*) FROM posts p
WHERE p.post_id = ?
  AND p.user_id IN (
    -- 用户自己
    SELECT ?
    UNION ALL
    -- 用户的配对情侣（如果存在）
    SELECT partner_id FROM (
        SELECT
            CASE
                WHEN user1_id = ? THEN user2_id
                WHEN user2_id = ? THEN user1_id
            END as partner_id
        FROM user_relationships
        WHERE (user1_id = ? OR user2_id = ?)
        AND status = 'ACTIVE'
    ) sub WHERE partner_id IS NOT NULL
  );
```

**Java 实现逻辑示例**:

```java
public boolean canUserAccessPost(Long userId, Long postId) {
    Post post = postRepository.findById(postId);
    if (post == null) return false;

    // 帖子作者本人可以访问
    if (post.getUserId().equals(userId)) {
        return true;
    }

    // 检查是否为帖子作者的情侣
    Optional<Long> partnerId = getPartnerUserId(userId);
    if (partnerId.isPresent() && post.getUserId().equals(partnerId.get())) {
        return true;
    }

    return false;
}

private Optional<Long> getPartnerUserId(Long userId) {
    return userRelationshipRepository
        .findActiveRelationshipByUserId(userId)
        .map(rel -> rel.getOtherUserId(userId));
}
```

## 数据库设计架构

### 用户关系表 (user_relationships)

```sql
-- 用户关系表
CREATE TABLE user_relationships (
    id BIGSERIAL PRIMARY KEY,
    user1_id BIGINT NOT NULL REFERENCES users(user_id),
    user2_id BIGINT NOT NULL REFERENCES users(user_id),
    requester_user_id BIGINT NOT NULL REFERENCES users(user_id),
    relationship_type VARCHAR(20) NOT NULL DEFAULT 'COUPLE',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ACTIVE, INACTIVE
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMP,
    UNIQUE(user1_id, user2_id),
    CHECK(user1_id < user2_id) -- 确保user1_id总是小于user2_id，避免重复
);

-- 性能优化索引
CREATE INDEX idx_user_relationships_user1 ON user_relationships(user1_id);
CREATE INDEX idx_user_relationships_user2 ON user_relationships(user2_id);
CREATE INDEX idx_user_relationships_requester ON user_relationships(requester_user_id);
CREATE INDEX idx_user_relationships_status ON user_relationships(status);
CREATE INDEX idx_user_relationships_status_requester ON user_relationships(status, requester_user_id);
```

**🔑 关键设计说明**:

1. **requester_user_id 字段**:

   - 保存配对请求的原始发送者 ID
   - 即使 user1_id 和 user2_id 被重新排序，仍能准确识别请求发送者

2. **CHECK 约束**:

   - `CHECK(user1_id < user2_id)` 确保数据库层面的一致性
   - 避免出现 (A,B) 和 (B,A) 两条重复记录

3. **索引优化**:

   - 添加 `requester_user_id` 索引，优化查询性能
   - 复合索引 `(status, requester_user_id)` 优化配对请求查询

4. **状态管理**:
   - 默认状态为 `PENDING`，配对请求创建时的初始状态
   - 支持完整的状态转换：PENDING → ACTIVE/INACTIVE

### 设计优点

- 🚀 **高性能查询**: 支持索引优化，查询速度快
- 🔒 **数据一致性**: 单一数据源，避免数据同步问题
- 📈 **扩展性强**: 支持关系历史、状态管理、多种关系类型
- 🛠️ **易于维护**: 清晰的数据结构，便于调试和维护
- 📊 **支持分析**: 可以方便地进行关系分析和统计

### 核心查询逻辑

```java
// 获取用户可见的帖子
public List<Post> getVisiblePosts(Long userId) {
    // 获取用户的配对关系
    Optional<Long> partnerId = getPartnerUserId(userId);

    List<Long> visibleUserIds = new ArrayList<>();
    visibleUserIds.add(userId);

    if (partnerId.isPresent()) {
        visibleUserIds.add(partnerId.get());
    }

    return postRepository.findByUserIdInOrderByCreatedAtDesc(visibleUserIds);
}

private Optional<Long> getPartnerUserId(Long userId) {
    return userRelationshipRepository
        .findActiveRelationshipByUserId(userId)
        .map(rel -> rel.getOtherUserId(userId));
}

// 获取用户收到的配对请求
public List<UserRelationship> getPendingRequests(Long userId) {
    return userRelationshipRepository
        .findPendingRelationshipsByTargetUserId(userId);
}

// 获取用户发送的配对请求
public List<UserRelationship> getSentRequests(Long userId) {
    return userRelationshipRepository
        .findPendingRelationshipsByRequesterUserId(userId);
}

// 发送配对请求
public void sendPairRequest(Long fromUserId, Long toUserId) {
    // 检查是否已有关系
    if (userRelationshipRepository.existsPendingRelationshipBetweenUsers(fromUserId, toUserId)) {
        throw new IllegalStateException("已有待处理的配对请求");
    }

    // 创建新的配对请求
    UserRelationship relationship = new UserRelationship();
    relationship.setUser1Id(fromUserId);  // 会在 @PrePersist 中自动排序
    relationship.setUser2Id(toUserId);
    relationship.setRequesterUserId(fromUserId);  // 保存原始请求发送者
    relationship.setStatus(RelationshipStatus.PENDING);

    userRelationshipRepository.save(relationship);

    // 发送通知
    notificationService.sendPairRequestNotification(fromUserId, toUserId);
}
```

### 5. 配对管理相关

#### 5.1 发送配对请求

- **URL**: `POST /api/users/{userId}/pair-request`
- **描述**: 向指定用户发送配对请求（创建 PENDING 状态的关系）
- **认证**: 需要（只能为自己发送请求）

**请求体**:

```json
{
  "targetUserId": 2
}
```

**响应**:

```json
{
  "code": 200,
  "message": "配对请求已发送",
  "data": null
}
```

**详细业务逻辑**:

1. **数据处理流程**:

   - 假设用户 3 向用户 1 发送请求
   - 系统创建关系记录：`user1Id=1, user2Id=3, requesterUserId=3`
   - 自动排序确保 `user1Id < user2Id`，但 `requesterUserId` 保持为 3

2. **状态设置**:

   - 新建关系状态为 `PENDING`
   - 发送通知给目标用户（当前通过日志记录，可扩展为 WebSocket）

3. **查看机制**:
   - 目标用户（用户 1）通过 `/pending-requests` 查看收到的请求
   - 发送者（用户 3）通过 `/sent-requests` 查看发送的请求

**限制条件**:

- 不能向自己发送请求
- 不能向已有活跃关系的用户发送请求
- 不能重复发送请求（如果已有 PENDING 关系）
- 每个用户同时只能有一个 ACTIVE 关系

#### 5.2 查看待处理的配对请求

- **URL**: `GET /api/users/{userId}/pending-requests`
- **描述**: 查看收到的配对请求（其他用户发送给我的请求）
- **认证**: 需要（只能查看自己的待处理请求）

**查询逻辑**:

- 查找所有状态为 `PENDING` 且目标用户为当前用户的关系
- 通过 `requesterUserId != userId` 确定这是别人发送给我的请求

**响应**:

```json
{
  "code": 200,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "partnerId": 3,
      "partnerUsername": "alice",
      "partnerNickname": "Alice Smith",
      "relationshipType": "COUPLE",
      "status": "PENDING",
      "createdAt": "2024-01-15T10:30:00",
      "endedAt": null
    }
  ]
}
```

**说明**: `partnerId` 为发送请求的用户 ID，即 `requesterUserId`

#### 5.3 查看发送的配对请求

- **URL**: `GET /api/users/{userId}/sent-requests`
- **描述**: 查看发送的配对请求（我发送给其他用户的请求）
- **认证**: 需要（只能查看自己发送的请求）

**查询逻辑**:

- 查找所有状态为 `PENDING` 且请求发送者为当前用户的关系
- 通过 `requesterUserId = userId` 确定这是我发送的请求

**响应**:

```json
{
  "code": 200,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "partnerId": 4,
      "partnerUsername": "bob",
      "partnerNickname": "Bob Chen",
      "relationshipType": "COUPLE",
      "status": "PENDING",
      "createdAt": "2024-01-15T10:30:00",
      "endedAt": null
    }
  ]
}
```

**说明**: `partnerId` 为接收请求的用户 ID，即目标用户 ID

#### 5.4 接受配对请求

- **URL**: `POST /api/users/{userId}/accept-pair`
- **描述**: 接受配对请求并建立情侣关系（PENDING → ACTIVE）
- **认证**: 需要（只能接受发给自己的请求）

**请求体**:

```json
{
  "requestUserId": 1
}
```

**响应**:

```json
{
  "code": 200,
  "message": "配对成功",
  "data": {
    "id": 1,
    "user1Id": 1,
    "user2Id": 2,
    "requesterUserId": 1,
    "relationshipType": "COUPLE",
    "status": "ACTIVE",
    "createdAt": "2024-01-15T10:30:00",
    "endedAt": null
  }
}
```

**业务逻辑**:

- 将关系状态从 `PENDING` 改为 `ACTIVE`
- 发送通知给原请求发送者
- 双方的 relationshipStatus 变为 "COUPLED"
- 双方可以看到彼此的帖子并互相评论

#### 5.5 拒绝配对请求

- **URL**: `POST /api/users/{userId}/reject-pair`
- **描述**: 拒绝配对请求（PENDING → INACTIVE）
- **认证**: 需要（只能拒绝发给自己的请求）

**请求体**:

```json
{
  "requestUserId": 1
}
```

**响应**:

```json
{
  "code": 200,
  "message": "配对请求已拒绝",
  "data": null
}
```

**业务逻辑**:

- 将关系状态从 `PENDING` 改为 `INACTIVE`
- 设置 `ended_at` 时间戳
- 发送通知给原请求发送者
- 保留拒绝记录用于历史查询

#### 5.6 解除配对关系

- **URL**: `DELETE /api/users/{userId}/unpair`
- **描述**: 解除当前的配对关系（ACTIVE → INACTIVE）
- **认证**: 需要（只能解除自己的配对关系）

**响应**:

```json
{
  "code": 200,
  "message": "配对关系已解除",
  "data": null
}
```

**解除配对后的数据更新**:

- 将 user_relationships 记录状态设为 INACTIVE
- 设置 ended_at 时间戳
- 双方的 relationshipStatus 变为 "SINGLE"
- 双方只能看到自己的帖子
- 保留配对历史记录用于分析

#### 5.7 获取配对历史

- **URL**: `GET /api/users/{userId}/relationship-history`
- **描述**: 获取用户的配对历史记录（包括所有状态的关系）
- **认证**: 需要（只能查看自己的历史）

**响应**:

```json
{
  "code": 200,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "partnerId": 2,
      "partnerUsername": "bob",
      "partnerNickname": "Bob Smith",
      "relationshipType": "COUPLE",
      "status": "INACTIVE",
      "createdAt": "2024-01-15T10:30:00",
      "endedAt": "2024-02-15T10:30:00"
    },
    {
      "id": 2,
      "partnerId": 3,
      "partnerUsername": "charlie",
      "partnerNickname": "Charlie Chen",
      "relationshipType": "COUPLE",
      "status": "ACTIVE",
      "createdAt": "2024-02-20T10:30:00",
      "endedAt": null
    }
  ]
}
```

### 配对请求流程图

```
用户A (ID=3) 发送配对请求给用户B (ID=1)
             ↓
创建关系记录: user1Id=1, user2Id=3, requesterUserId=3, status=PENDING
             ↓
系统自动排序: user1Id < user2Id (确保 1 < 3)
             ↓
通知用户B: "用户A向您发送了配对请求"
             ↓
┌─────────────────────────────────────────────────────────────┐
│                    双方查看请求状态                          │
├─────────────────────────────────────────────────────────────┤
│ 用户B查看 pending-requests  │  用户A查看 sent-requests     │
│ (收到的请求)                │  (发送的请求)                │
│ 看到来自用户A的请求          │  看到发送给用户B的请求        │
└─────────────────────────────────────────────────────────────┘
             ↓
用户B选择处理方式
             ↓
          ┌─────────────────────────┼─────────────────────────┐
          ↓                         ↓                         ↓
      接受请求                   拒绝请求                  忽略请求
    (PENDING→ACTIVE)           (PENDING→INACTIVE)          (保持PENDING)
          ↓                         ↓                         ↓
      建立情侣关系                设置ended_at              请求继续待处理
      通知用户A成功              通知用户A被拒绝            用户A可继续查看状态
      双方relationshipStatus      关系记录保留用于历史
      变为"COUPLED"
```

### 配对请求状态管理

#### 关系状态说明

- **PENDING**: 待处理状态，配对请求已发送但未处理
- **ACTIVE**: 活跃状态，配对成功，双方为情侣关系
- **INACTIVE**: 非活跃状态，配对被拒绝或关系已结束

#### 重要实现细节

1. **数据库索引优化与业务逻辑的平衡**:

   - **索引优化**: 系统自动确保 `user1Id < user2Id`，避免重复关系记录
   - **业务逻辑**: 通过 `requesterUserId` 字段保留原始请求发送者信息
   - **实际示例**: 用户 3 向用户 1 发送请求 → 存储为 `user1Id=1, user2Id=3, requesterUserId=3`

2. **请求发送者识别机制**:

   - 系统通过 `requesterUserId` 字段明确标识谁是配对请求的发送者
   - 即使 `user1Id` 和 `user2Id` 因数据库索引优化被重新排序，原始请求者信息仍然保留
   - 这解决了"谁发送给谁"的业务问题

3. **API 接口权限区分逻辑**:

   - `GET /api/users/{userId}/pending-requests`: 查询条件 `requesterUserId != userId`
     - 含义：查看**收到的**配对请求（别人发送给我的）
   - `GET /api/users/{userId}/sent-requests`: 查询条件 `requesterUserId = userId`
     - 含义：查看**发送的**配对请求（我发送给别人的）

4. **状态管理与通知机制**:

   - **状态转换**: PENDING → ACTIVE（接受）或 PENDING → INACTIVE（拒绝）
   - **通知机制**: 当前通过日志记录通知，可扩展为 WebSocket 实时通知
   - **前端轮询**: 需要通过轮询 API 或实时通知获取配对请求状态更新

5. **数据一致性与约束**:

   - 用户只能有一个 ACTIVE 关系
   - 同一对用户之间不能有多个 PENDING 请求
   - 所有状态变更都有完整的审计记录
   - 通过数据库唯一约束 `UNIQUE(user1_id, user2_id)` 确保数据完整性

6. **通知机制设计**:
   - **当前实现**: 通过日志记录通知事件，方便调试和审计
   - **扩展能力**: 可升级为 WebSocket 实时通知、邮件通知等
   - **通知类型**: 配对请求通知、请求接受通知、请求拒绝通知
   - **前端实现**: 需要通过轮询或 WebSocket 获取通知更新

## 配对系统设计总结

### 核心设计思路

WeBlog 的配对系统采用了**数据库优化与业务逻辑分离**的设计思路：

1. **数据库层面优化**:

   - 使用 `user1Id < user2Id` 约束避免重复记录
   - 通过唯一索引 `UNIQUE(user1_id, user2_id)` 确保数据一致性
   - 优化查询性能，支持大规模用户场景

2. **业务逻辑层面完整**:
   - 通过 `requesterUserId` 字段保留业务语义
   - 实现真正的请求-响应机制（PENDING 状态）
   - 支持完整的配对生命周期管理

### 技术特点

- ✅ **性能优化**: 数据库索引优化，查询效率高
- ✅ **业务完整**: 真正的请求-响应机制，符合用户期望
- ✅ **数据一致**: 严格的约束和状态管理
- ✅ **可扩展性**: 支持通知机制扩展和关系历史记录
- ✅ **权限控制**: 完整的权限检查和数据隔离
- ✅ **审计能力**: 完整的操作记录和状态变更历史

### 解决的核心问题

1. **如何在数据库优化的前提下保留业务语义**？

   - 通过 `requesterUserId` 字段解决"谁发送给谁"的问题

2. **如何实现真正的配对请求机制**？

   - 使用 PENDING 状态实现请求-等待-响应的完整流程

3. **如何确保数据一致性**？

   - 数据库约束 + 业务逻辑双重保障

4. **如何支持复杂的配对场景**？
   - 状态管理、通知机制、权限控制的完整实现

### 最佳实践

这个配对系统的设计体现了几个重要的最佳实践：

- **数据库设计与业务逻辑的平衡**：既要性能优化，又要业务完整
- **状态机设计**：清晰的状态转换和约束
- **权限控制**：细粒度的权限管理和数据隔离
- **可扩展性**：支持未来功能扩展和性能优化

项目现已实现完整的配对请求功能，支持发送、接受、拒绝配对请求的完整流程，并提供了清晰的 API 接口和文档。

## 6. WebSocket 实时通知系统

### 6.1 系统概述

WeBlog 实现了统一的 WebSocket 通知系统，类似于微信朋友圈的通知机制。所有的用户交互行为（配对请求、新帖子、新评论等）都通过统一的通知通道实时推送给相关用户。

### 6.2 通知系统架构

```
业务操作层          通知服务层          WebSocket层        前端展示层
    ↓                  ↓                  ↓                ↓
配对请求发送  →    NotificationService  →  WebSocket      →  弹出通知
帖子发布     →    NotificationService  →  WebSocket      →  弹出通知
评论发布     →    NotificationService  →  WebSocket      →  弹出通知
配对接受     →    NotificationService  →  WebSocket      →  弹出通知
帖子删除     →    NotificationService  →  WebSocket      →  弹出通知
```

**设计原则**：

- 📡 **统一通道**：所有通知都通过一个 WebSocket 连接
- 🔔 **实时推送**：用户行为立即通知相关用户
- 🎯 **精准投递**：只向目标用户发送相关通知
- 🔄 **异步处理**：通知发送不影响主业务流程

### 6.3 WebSocket 连接管理

#### 6.3.1 建立连接

- **WebSocket URL**: `ws://localhost:8080/ws/notifications`
- **认证方式**: 连接时需要传递 JWT token
- **连接参数**:
  ```javascript
  const ws = new WebSocket(
    "ws://localhost:8080/ws/notifications?token=your_jwt_token"
  );
  ```

#### 6.3.2 连接状态管理

```javascript
// 连接建立
ws.onopen = function (event) {
  console.log("WebSocket连接已建立");
};

// 接收通知
ws.onmessage = function (event) {
  const notification = JSON.parse(event.data);
  handleNotification(notification);
};

// 连接关闭
ws.onclose = function (event) {
  console.log("WebSocket连接已关闭");
  // 实现重连逻辑
};

// 连接错误
ws.onerror = function (error) {
  console.error("WebSocket错误:", error);
};
```

### 6.4 通知消息格式

#### 6.4.1 统一消息结构

```json
{
  "type": "PAIR_REQUEST",
  "fromUserId": 1,
  "toUserId": 2,
  "message": "Alice 向你发送了配对请求",
  "data": {
    "fromUserName": "Alice",
    "fromUserNickname": "爱丽丝",
    "relationshipId": 123
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

**字段说明**：

- `type`: 通知类型，用于前端区分处理方式
- `fromUserId`: 触发通知的用户 ID（发送者）
- `toUserId`: 接收通知的用户 ID（接收者）
- `message`: 人类可读的通知消息
- `data`: 通知相关的附加数据
- `timestamp`: 通知发送时间

#### 6.4.2 通知类型定义

| 通知类型                | 说明           | 触发场景         |
| ----------------------- | -------------- | ---------------- |
| `PAIR_REQUEST`          | 配对请求       | 用户发送配对请求 |
| `PAIR_REQUEST_ACCEPTED` | 配对请求被接受 | 用户接受配对请求 |
| `PAIR_REQUEST_REJECTED` | 配对请求被拒绝 | 用户拒绝配对请求 |
| `NEW_POST`              | 新帖子发布     | 情侣发布新帖子   |
| `NEW_COMMENT`           | 新评论         | 帖子收到新评论   |
| `POST_DELETED`          | 帖子删除       | 情侣删除帖子     |
| `COMMENT_DELETED`       | 评论删除       | 评论被删除       |
| `RELATIONSHIP_ENDED`    | 关系结束       | 情侣关系解除     |

### 6.5 具体通知场景

#### 6.5.1 配对请求通知

**触发场景**: 用户 A 向用户 B 发送配对请求

**通知接收者**: 用户 B

**消息示例**:

```json
{
  "type": "PAIR_REQUEST",
  "fromUserId": 1,
  "toUserId": 2,
  "message": "Alice 向你发送了配对请求",
  "data": {
    "fromUserName": "alice",
    "fromUserNickname": "Alice",
    "relationshipId": 123
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

#### 6.5.2 新帖子通知

**触发场景**: 用户 A 发布新帖子

**通知接收者**: 用户 A 的情侣

**消息示例**:

```json
{
  "type": "NEW_POST",
  "fromUserId": 1,
  "toUserId": 2,
  "message": "Alice 发布了新帖子",
  "data": {
    "fromUserName": "alice",
    "fromUserNickname": "Alice",
    "postId": 456,
    "postContent": "今天天气真好..."
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

#### 6.5.3 新评论通知

**触发场景**: 用户 B 对用户 A 的帖子发表评论

**通知接收者**: 用户 A（帖子作者）

**消息示例**:

```json
{
  "type": "NEW_COMMENT",
  "fromUserId": 2,
  "toUserId": 1,
  "message": "Bob 评论了你的帖子",
  "data": {
    "fromUserName": "bob",
    "fromUserNickname": "Bob",
    "postId": 456,
    "commentId": 789,
    "commentContent": "很棒的分享！"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

### 6.6 前端集成指南

#### 6.6.1 WebSocket 连接管理

```javascript
class NotificationManager {
  constructor(token) {
    this.token = token;
    this.ws = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 3000;
  }

  connect() {
    this.ws = new WebSocket(
      `ws://localhost:8080/ws/notifications?token=${this.token}`
    );

    this.ws.onopen = (event) => {
      console.log("WebSocket连接已建立");
      this.reconnectAttempts = 0;
    };

    this.ws.onmessage = (event) => {
      const notification = JSON.parse(event.data);
      this.handleNotification(notification);
    };

    this.ws.onclose = (event) => {
      console.log("WebSocket连接已关闭");
      this.attemptReconnect();
    };

    this.ws.onerror = (error) => {
      console.error("WebSocket错误:", error);
    };
  }

  handleNotification(notification) {
    switch (notification.type) {
      case "PAIR_REQUEST":
        this.showPairRequestNotification(notification);
        break;
      case "NEW_POST":
        this.showNewPostNotification(notification);
        break;
      case "NEW_COMMENT":
        this.showNewCommentNotification(notification);
        break;
      default:
        console.log("未知通知类型:", notification.type);
    }
  }

  showPairRequestNotification(notification) {
    // 显示配对请求通知
    this.showToast(notification.message, "info");
    // 可以触发页面刷新待处理请求列表
    this.refreshPendingRequests();
  }

  showNewPostNotification(notification) {
    // 显示新帖子通知
    this.showToast(notification.message, "success");
    // 可以更新帖子列表
    this.refreshPostList();
  }

  showNewCommentNotification(notification) {
    // 显示新评论通知
    this.showToast(notification.message, "info");
    // 可以更新评论数
    this.updateCommentCount(notification.data.postId);
  }

  attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(
        `尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`
      );

      setTimeout(() => {
        this.connect();
      }, this.reconnectDelay);
    } else {
      console.error("WebSocket重连失败，已达到最大重连次数");
    }
  }

  showToast(message, type = "info") {
    // 实现Toast通知显示
    // 可以使用UI库如Element UI、Ant Design等
    console.log(`[${type.toUpperCase()}] ${message}`);
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
    }
  }
}

// 使用示例
const notificationManager = new NotificationManager(userToken);
notificationManager.connect();

// 页面卸载时断开连接
window.addEventListener("beforeunload", () => {
  notificationManager.disconnect();
});
```

#### 6.6.2 UI 通知显示

```javascript
// 使用流行的通知库，如 react-hot-toast 或 vue-toastification
import { toast } from "react-hot-toast";

function showNotification(notification) {
  const options = {
    duration: 4000,
    position: "top-right",
    icon: getNotificationIcon(notification.type),
  };

  switch (notification.type) {
    case "PAIR_REQUEST":
      toast(notification.message, { ...options, icon: "💕" });
      break;
    case "NEW_POST":
      toast(notification.message, { ...options, icon: "📝" });
      break;
    case "NEW_COMMENT":
      toast(notification.message, { ...options, icon: "💬" });
      break;
    default:
      toast(notification.message, options);
  }
}
```

### 6.7 后端实现集成点

#### 6.7.1 与现有服务的集成

通知系统与现有业务服务的集成点：

1. **PostService**:

   ```java
   // 发布帖子后通知情侣
   public Post createPost(PostCreateRequest request) {
       Post post = createPostLogic(request);
       notificationService.sendNewPostNotification(post);
       return post;
   }
   ```

2. **CommentService**:

   ```java
   // 发表评论后通知帖子作者
   public Comment createComment(CommentCreateRequest request) {
       Comment comment = createCommentLogic(request);
       notificationService.sendNewCommentNotification(comment);
       return comment;
   }
   ```

3. **UserRelationshipService**:
   ```java
   // 现有的配对通知逻辑保持不变
   public void sendPairRequest(Long fromUserId, Long toUserId) {
       // 配对逻辑...
       notificationService.sendPairRequestNotification(fromUserId, toUserId);
   }
   ```

#### 6.7.2 通知服务接口

```java
public interface NotificationService {
    // 配对相关通知
    void sendPairRequestNotification(Long fromUserId, Long toUserId);
    void sendPairRequestAcceptedNotification(Long requestUserId, Long acceptUserId);
    void sendPairRequestRejectedNotification(Long requestUserId, Long rejectUserId);

    // 帖子相关通知
    void sendNewPostNotification(Post post);
    void sendPostDeletedNotification(Post post);

    // 评论相关通知
    void sendNewCommentNotification(Comment comment);
    void sendCommentDeletedNotification(Comment comment);

    // 关系相关通知
    void sendRelationshipEndedNotification(Long userId1, Long userId2);
}
```

### 6.8 技术优势

#### 6.8.1 用户体验提升

- ✅ **即时反馈**: 用户操作立即得到通知反馈
- ✅ **减少轮询**: 无需频繁查询 API 获取更新
- ✅ **统一体验**: 所有通知都在一个地方展示
- ✅ **个性化**: 根据通知类型展示不同的 UI 效果

#### 6.8.2 系统性能优化

- ✅ **减少 HTTP 请求**: 避免频繁的 API 轮询
- ✅ **服务器资源节约**: 推送比轮询更高效
- ✅ **实时性**: WebSocket 提供毫秒级的通知延迟
- ✅ **可扩展**: 易于添加新的通知类型

#### 6.8.3 架构优势

- ✅ **解耦合**: 业务逻辑与通知逻辑分离
- ✅ **可维护**: 统一的通知处理机制
- ✅ **容错性**: WebSocket 断开不影响核心功能
- ✅ **测试友好**: 可以独立测试通知功能

### 6.9 扩展能力

#### 6.9.1 未来功能扩展

- 📱 **移动端推送**: 集成 FCM/APNs 推送服务
- 💾 **通知历史**: 持久化通知记录
- 🔕 **通知设置**: 用户可自定义通知偏好
- 📊 **通知统计**: 通知发送和阅读统计
- 🎨 **富媒体通知**: 支持图片、链接等富媒体内容

#### 6.9.2 高级功能

- 🏷️ **通知分组**: 按类型或时间分组展示
- 🔍 **通知搜索**: 搜索历史通知
- ⭐ **重要通知**: 标记重要通知
- 🔔 **免打扰模式**: 设置免打扰时间段

这个统一的 WebSocket 通知系统为 WeBlog 提供了类似现代社交应用的实时通知体验，大大提升了用户的使用体验和系统的互动性。
