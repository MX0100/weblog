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
    console.log(
      "🔍 NotificationManager created with token:",
      token ? "TOKEN_PRESENT" : "TOKEN_MISSING"
    );
  }

  // Connect to WebSocket
  connect(): void {
    if (
      this.isDestroyed ||
      this.isConnecting ||
      this.ws?.readyState === WebSocket.OPEN
    ) {
      console.log("🔍 Connect skipped:", {
        isDestroyed: this.isDestroyed,
        isConnecting: this.isConnecting,
        currentState: this.ws?.readyState,
      });
      return;
    }

    this.isConnecting = true;
    console.log("🔍 Starting WebSocket connection...");

    const wsUrl = this.getWebSocketUrl();
    console.log("🔍 WebSocket URL:", wsUrl);

    if (!this.token) {
      console.error("❌ No token available for WebSocket connection");
      this.isConnecting = false;
      this.notifyListeners("connection", { type: "CONNECTION_ERROR" } as any);
      return;
    }

    try {
      console.log("🔍 Creating WebSocket instance...");
      this.ws = new WebSocket(wsUrl);
    } catch (error) {
      console.error("❌ Failed to create WebSocket:", error);
      this.isConnecting = false;
      this.notifyListeners("connection", { type: "CONNECTION_ERROR" } as any);
      return;
    }

    this.ws.onopen = (event) => {
      console.log("✅ WebSocket connection opened:", event);
      this.isConnecting = false;
      this.reconnectAttempts = 0;
      this.notifyListeners("connection", { type: "CONNECTION_OPEN" } as any);
    };

    this.ws.onmessage = (event) => {
      console.log("📨 WebSocket message received:", event.data);
      try {
        const notification: WebSocketNotification = JSON.parse(event.data);
        this.handleNotification(notification);
      } catch (error) {
        console.error(
          "❌ Failed to parse WebSocket message:",
          error,
          event.data
        );
      }
    };

    this.ws.onclose = (event) => {
      console.log("🔌 WebSocket connection closed:", {
        code: event.code,
        reason: event.reason,
        wasClean: event.wasClean,
      });
      this.isConnecting = false;
      this.ws = null;
      this.notifyListeners("connection", { type: "CONNECTION_CLOSED" } as any);

      if (!this.isDestroyed) {
        this.attemptReconnect();
      }
    };

    this.ws.onerror = (error) => {
      console.error("❌ WebSocket error:", error);
      this.isConnecting = false;
      this.notifyListeners("connection", { type: "CONNECTION_ERROR" } as any);
    };

    // Connection timeout check
    setTimeout(() => {
      if (this.ws?.readyState === WebSocket.CONNECTING) {
        console.warn("⏰ WebSocket connection timeout, closing...");
        this.ws.close();
      }
    }, 10000);
  }

  // Get WebSocket URL with token
  private getWebSocketUrl(): string {
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    let host = window.location.host;

    // Handle different environments
    if (
      window.location.hostname === "localhost" ||
      window.location.hostname === "127.0.0.1"
    ) {
      host = "localhost:8080";
    } else {
      host = `${window.location.hostname}:8080`;
    }

    const wsUrl = `${protocol}//${host}/ws/notifications?token=${this.token}`;

    console.log("🔍 WebSocket URL details:", {
      protocol,
      host,
      originalHost: window.location.host,
      hostname: window.location.hostname,
      fullUrl: wsUrl,
      tokenLength: this.token ? this.token.length : 0,
    });

    return wsUrl;
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
