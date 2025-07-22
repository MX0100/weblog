import type { WebSocketNotification, NotificationType } from "../types/api";

export class NotificationManager {
  private ws: WebSocket | null = null;
  private token: string;
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 5;
  private reconnectDelay: number = 3000;
  private isConnecting: boolean = false;
  private isDestroyed: boolean = false;
  private listeners: Map<
    string,
    (notification: WebSocketNotification) => void
  > = new Map();

  constructor(token: string) {
    this.token = token;
  }

  // Connect to WebSocket
  connect(): void {
    if (
      this.isDestroyed ||
      this.isConnecting ||
      this.ws?.readyState === WebSocket.OPEN
    ) {
      return;
    }

    this.isConnecting = true;

    const wsUrl = this.getWebSocketUrl();

    if (!this.token) {
      this.isConnecting = false;
      this.notifyListeners("connection", { type: "CONNECTION_ERROR" } as any);
      return;
    }

    try {
      this.ws = new WebSocket(wsUrl);
    } catch (error) {
      this.isConnecting = false;
      this.notifyListeners("connection", { type: "CONNECTION_ERROR" } as any);
      return;
    }

    this.ws.onopen = (event) => {
      this.isConnecting = false;
      this.reconnectAttempts = 0;
      this.notifyListeners("connection", { type: "CONNECTION_OPEN" } as any);
    };

    this.ws.onmessage = (event) => {
      try {
        const notification: WebSocketNotification = JSON.parse(event.data);
        this.handleNotification(notification);
      } catch (error) {
        // Silently handle parse errors
      }
    };

    this.ws.onclose = (event) => {
      this.isConnecting = false;
      this.ws = null;
      this.notifyListeners("connection", { type: "CONNECTION_CLOSED" } as any);

      if (!this.isDestroyed) {
        this.attemptReconnect();
      }
    };

    this.ws.onerror = (error) => {
      this.isConnecting = false;
      this.notifyListeners("connection", { type: "CONNECTION_ERROR" } as any);
    };

    // Connection timeout check
    setTimeout(() => {
      if (this.ws?.readyState === WebSocket.CONNECTING) {
        this.ws.close();
      }
    }, 10000);
  }

  // Get WebSocket URL with token - 智能环境切换
  private getWebSocketUrl(): string {
    // 优先使用环境变量
    const envWsUrl = import.meta.env.VITE_WS_URL;
    if (envWsUrl) {
      return `${envWsUrl}/notifications?token=${this.token}`;
    }

    // 根据环境自动判断
    if (import.meta.env.DEV) {
      // 开发环境：直接连接本地WebSocket
      return `ws://localhost:8080/ws/notifications?token=${this.token}`;
    } else {
      // 生产环境：通过CloudFront WSS代理访问EC2
      return `wss://dcyz06osekbqs.cloudfront.net/ws/notifications?token=${this.token}`;
    }
  }

  // Handle incoming notifications
  private handleNotification(notification: WebSocketNotification): void {
    // Notify type-specific listeners
    this.notifyListeners(notification.type, notification);

    // Notify general listeners
    this.notifyListeners("all", notification);
  }

  // Notify listeners
  private notifyListeners(
    type: string,
    notification: WebSocketNotification
  ): void {
    const listener = this.listeners.get(type);
    if (listener) {
      try {
        listener(notification);
      } catch (error) {
        // Silently handle listener errors
      }
    }
  }

  // Add notification listener
  addListener(
    type: NotificationType | "all" | "connection",
    callback: (notification: WebSocketNotification) => void
  ): void {
    this.listeners.set(type, callback);
  }

  // Remove notification listener
  removeListener(type: NotificationType | "all" | "connection"): void {
    this.listeners.delete(type);
  }

  // Attempt to reconnect
  private attemptReconnect(): void {
    if (
      this.isDestroyed ||
      this.reconnectAttempts >= this.maxReconnectAttempts
    ) {
      if (this.reconnectAttempts >= this.maxReconnectAttempts) {
        this.notifyListeners("connection", {
          type: "CONNECTION_FAILED",
        } as any);
      }
      return;
    }

    this.reconnectAttempts++;

    setTimeout(() => {
      if (!this.isDestroyed) {
        this.connect();
      }
    }, this.reconnectDelay);
  }

  // Check if connected
  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  // Disconnect WebSocket
  disconnect(): void {
    this.isDestroyed = true;
    this.isConnecting = false;

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.listeners.clear();
  }

  // Update token (for token refresh scenarios)
  updateToken(newToken: string): void {
    const wasConnected = this.isConnected();
    this.token = newToken;

    if (wasConnected) {
      this.disconnect();
      this.isDestroyed = false;
      this.connect();
    }
  }
}

// Toast notification helper
export const showNotificationToast = (
  notification: WebSocketNotification
): void => {
  const options = {
    duration: 4000,
    position: "top-right" as const,
  };

  const icon = getNotificationIcon(notification.type);
  const message = `${icon} ${notification.message}`;

  // Simple toast implementation
  createToast(message, options);
};

// Get notification icon based on type
export const getNotificationIcon = (type: NotificationType): string => {
  switch (type) {
    case "PAIR_REQUEST":
      return "💕";
    case "PAIR_REQUEST_ACCEPTED":
      return "💖";
    case "PAIR_REQUEST_REJECTED":
      return "💔";
    case "NEW_POST":
      return "📝";
    case "NEW_COMMENT":
      return "💬";
    case "POST_DELETED":
      return "🗑️";
    case "COMMENT_DELETED":
      return "🗑️";
    case "RELATIONSHIP_ENDED":
      return "💔";
    default:
      return "🔔";
  }
};

// Simple toast implementation
const createToast = (
  message: string,
  options: { duration: number; position: string }
): void => {
  const toast = document.createElement("div");
  toast.className = "notification-toast";
  toast.textContent = message;
  toast.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    background: #333;
    color: white;
    padding: 12px 20px;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    z-index: 9999;
    font-size: 14px;
    max-width: 300px;
    word-wrap: break-word;
    animation: slideIn 0.3s ease-out;
  `;

  // Add slide-in animation
  const style = document.createElement("style");
  style.textContent = `
    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }
  `;
  document.head.appendChild(style);

  document.body.appendChild(toast);

  // Remove toast after duration
  setTimeout(() => {
    if (toast.parentElement) {
      toast.remove();
    }
  }, options.duration);
};
