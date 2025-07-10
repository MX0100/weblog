# WebSocket 连接指南

## 🔗 连接信息

### 服务端地址

```
ws://localhost:8080/ws/notifications
```

### 生产环境

```
wss://yourdomain.com/ws/notifications
```

## 🔐 认证方式

### 方式 1：查询参数认证 (推荐)

```javascript
const token = localStorage.getItem("jwtToken");
const ws = new WebSocket(`ws://localhost:8080/ws/notifications?token=${token}`);
```

### 方式 2：Header 认证

```javascript
const token = localStorage.getItem("jwtToken");
const ws = new WebSocket("ws://localhost:8080/ws/notifications", [], {
  headers: {
    Authorization: `Bearer ${token}`,
  },
});
```

## 📱 完整的前端连接示例

### 基础连接

```javascript
class WebSocketConnection {
  constructor() {
    this.ws = null;
    this.token = localStorage.getItem("jwtToken");
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 2000;
  }

  connect() {
    if (!this.token) {
      console.error("❌ 没有找到JWT Token，请先登录");
      return;
    }

    try {
      // 使用查询参数方式传递token
      this.ws = new WebSocket(
        `ws://localhost:8080/ws/notifications?token=${this.token}`
      );

      this.ws.onopen = this.onOpen.bind(this);
      this.ws.onmessage = this.onMessage.bind(this);
      this.ws.onclose = this.onClose.bind(this);
      this.ws.onerror = this.onError.bind(this);
    } catch (error) {
      console.error("❌ WebSocket连接失败:", error);
    }
  }

  onOpen(event) {
    console.log("🔗 WebSocket连接成功");
    this.reconnectAttempts = 0;

    // 可以在这里发送心跳或其他初始化消息
    this.sendHeartbeat();
  }

  onMessage(event) {
    console.log("📬 收到WebSocket消息:", event.data);

    try {
      const notification = JSON.parse(event.data);
      this.handleNotification(notification);
    } catch (error) {
      console.error("❌ 解析消息失败:", error);
    }
  }

  onClose(event) {
    console.log("📱 WebSocket连接关闭:", event.code, event.reason);

    // 自动重连
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(
        `🔄 尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`
      );

      setTimeout(() => {
        this.connect();
      }, this.reconnectDelay);
    } else {
      console.error("❌ 重连失败，已达到最大重连次数");
    }
  }

  onError(event) {
    console.error("❌ WebSocket错误:", event);
  }

  handleNotification(notification) {
    console.log("🔔 处理通知:", notification);

    // 根据通知类型处理
    switch (notification.type) {
      case "NEW_POST":
        this.handleNewPost(notification);
        break;
      case "NEW_COMMENT":
        this.handleNewComment(notification);
        break;
      case "PAIR_REQUEST":
        this.handlePairRequest(notification);
        break;
      case "PAIR_ACCEPTED":
        this.handlePairAccepted(notification);
        break;
      default:
        console.log("🔔 未知通知类型:", notification.type);
    }
  }

  handleNewPost(notification) {
    // 显示新帖子通知
    this.showNotification(`💕 ${notification.message}`, "success");

    // 可以刷新帖子列表
    if (typeof refreshPostList === "function") {
      refreshPostList();
    }
  }

  handleNewComment(notification) {
    // 显示新评论通知
    this.showNotification(`💬 ${notification.message}`, "info");

    // 可以刷新评论
    if (typeof refreshComments === "function") {
      refreshComments();
    }
  }

  handlePairRequest(notification) {
    // 显示配对请求通知
    this.showNotification(`💌 ${notification.message}`, "warning");

    // 可以刷新配对请求列表
    if (typeof refreshPairRequests === "function") {
      refreshPairRequests();
    }
  }

  handlePairAccepted(notification) {
    // 显示配对成功通知
    this.showNotification(`🎉 ${notification.message}`, "success");

    // 可以刷新用户状态
    if (typeof refreshUserStatus === "function") {
      refreshUserStatus();
    }
  }

  showNotification(message, type = "info") {
    // 可以使用任何通知库，这里是一个简单示例
    const notification = document.createElement("div");
    notification.className = `notification ${type}`;
    notification.textContent = message;

    document.body.appendChild(notification);

    // 3秒后自动消失
    setTimeout(() => {
      if (notification.parentElement) {
        notification.remove();
      }
    }, 3000);
  }

  sendHeartbeat() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      // 发送心跳消息（可选）
      this.ws.send(JSON.stringify({ type: "HEARTBEAT" }));
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}
```

### 使用示例

```javascript
// 创建WebSocket连接实例
const wsConnection = new WebSocketConnection();

// 在用户登录后连接
function onUserLogin() {
  wsConnection.connect();
}

// 在用户登出时断开连接
function onUserLogout() {
  wsConnection.disconnect();
}

// 页面加载时检查登录状态
window.addEventListener("load", () => {
  const token = localStorage.getItem("jwtToken");
  if (token) {
    wsConnection.connect();
  }
});
```

## 📨 消息格式

### 接收的通知消息格式

```json
{
  "type": "NEW_POST",
  "fromUserId": 1,
  "toUserId": 2,
  "message": "testbot01 published a new post",
  "timestamp": "2025-01-07T00:12:07.825Z",
  "data": {
    "postId": 95,
    "postTitle": "今天的心情",
    "authorName": "testbot01"
  }
}
```

### 通知类型说明

| 类型            | 说明       | 触发条件             |
| --------------- | ---------- | -------------------- |
| `NEW_POST`      | 新帖子通知 | 对方发布新帖子       |
| `NEW_COMMENT`   | 新评论通知 | 有人评论了你的帖子   |
| `PAIR_REQUEST`  | 配对请求   | 有人向你发送配对请求 |
| `PAIR_ACCEPTED` | 配对成功   | 你的配对请求被接受   |

## 🚨 错误处理

### 常见错误码

| 错误码 | 说明         | 解决方法                |
| ------ | ------------ | ----------------------- |
| 1006   | 连接异常关闭 | 检查网络连接，自动重连  |
| 1011   | 服务器错误   | 检查服务器状态          |
| 4401   | 认证失败     | 检查 JWT Token 是否有效 |

### 调试技巧

1. **检查浏览器控制台** - 查看连接日志
2. **检查 Network 面板** - 确认 WebSocket 连接状态
3. **检查服务器日志** - 查看认证和消息发送状态

## 🔧 测试工具

### 浏览器测试

```javascript
// 在浏览器控制台中测试连接
const token = "your-jwt-token-here";
const testWs = new WebSocket(
  `ws://localhost:8080/ws/notifications?token=${token}`
);

testWs.onopen = () => console.log("✅ 连接成功");
testWs.onmessage = (event) => console.log("📬 收到消息:", event.data);
testWs.onclose = () => console.log("❌ 连接关闭");
testWs.onerror = (error) => console.error("❌ 连接错误:", error);
```

### curl 测试（仅用于验证服务器状态）

```bash
curl -i -N -H "Connection: Upgrade" \
     -H "Upgrade: websocket" \
     -H "Sec-WebSocket-Key: test" \
     -H "Sec-WebSocket-Version: 13" \
     "http://localhost:8080/ws/notifications?token=YOUR_JWT_TOKEN"
```

## 📋 检查清单

在实施 WebSocket 连接时，请确保：

- [ ] 用户已登录并有有效的 JWT Token
- [ ] WebSocket URL 正确
- [ ] 认证参数正确传递
- [ ] 实现了错误处理和重连机制
- [ ] 处理了各种通知类型
- [ ] 在用户登出时断开连接

## 🎯 最佳实践

1. **及时连接** - 用户登录后立即建立 WebSocket 连接
2. **优雅断开** - 用户登出时主动断开连接
3. **错误重连** - 实现自动重连机制
4. **消息处理** - 根据通知类型执行相应的 UI 更新
5. **性能优化** - 避免频繁的连接断开
6. **用户体验** - 提供连接状态指示器

现在你可以按照这个指南在前端建立 WebSocket 连接，就能收到实时通知了！🎉
