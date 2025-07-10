import React, { useState } from "react";
import type { User } from "../../../types/api";
import UserAvatar from "../UserAvatar/UserAvatar";
import NotificationButton from "../NotificationButton/NotificationButton";
import "./Navbar.css";

interface NavbarProps {
  user: User;
  wsConnected: boolean;
  notificationCount: number;
  onLogout: () => void;
  onAddPartnerClick: () => void;
  onNotificationClick: () => void;
  onManualRefresh: () => void;
  onProfileClick: () => void;
}

const Navbar: React.FC<NavbarProps> = ({
  user,
  wsConnected,
  notificationCount,
  onLogout,
  onAddPartnerClick,
  onNotificationClick,
  onManualRefresh,
  onProfileClick,
}) => {
  // Add debug state
  const [debugInfo, setDebugInfo] = useState<any>(null);
  const [showDebug, setShowDebug] = useState(false);

  // Check WebSocket debug info
  const checkWebSocketDebug = async () => {
    try {
      const response = await fetch("/api/users/debug/online", {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
          "Content-Type": "application/json",
        },
      });

      if (response.ok) {
        const result = await response.json();

        // Add some additional debug info from local state
        const enhancedDebugInfo = {
          ...result.data,
          frontendWebSocketUrl: localStorage.getItem("token")
            ? `ws://localhost:8080/ws/notifications?token=${localStorage
                .getItem("token")
                ?.substring(0, 20)}...`
            : "No token available",
          lastWebSocketActivity: "Check browser console for WebSocket logs",
          optimizedForRealTime: "🚀 Pure WebSocket system - No API polling!",
        };

        setDebugInfo(enhancedDebugInfo);
        setShowDebug(true);
      }
    } catch (error) {
      console.error("Failed to get debug info:", error);
    }
  };

  return (
    <header className="navbar">
      <div className="container">
        <div className="navbar-content">
          <div className="navbar-left">
            <h1 className="logo">WeBlog</h1>
          </div>

          <div className="navbar-center">
            <h2 className="welcome-text">
              Welcome back, {user.nickname || user.username}!
            </h2>
          </div>

          <div className="navbar-right">
            <UserAvatar
              currentUser={user}
              onAddPartnerClick={onAddPartnerClick}
              onProfileClick={onProfileClick}
            />

            {/* WebSocket connection status indicator */}
            <div
              className={`connection-status ${
                wsConnected ? "connected" : "disconnected"
              }`}
              title={
                wsConnected
                  ? "WebSocket connected - Real-time notifications active"
                  : "WebSocket disconnected - Click to reconnect (no polling needed)"
              }
              onClick={wsConnected ? undefined : onManualRefresh}
            >
              <span className="status-icon">{wsConnected ? "🟢" : "🔴"}</span>
              <span className="status-text">
                {wsConnected ? "Live" : "Offline"}
              </span>
            </div>

            {/* Debug button */}
            <button
              className="debug-btn"
              onClick={checkWebSocketDebug}
              title="Check WebSocket Debug Info"
            >
              🔍
            </button>

            <NotificationButton
              notificationCount={notificationCount}
              onClick={onNotificationClick}
            />

            <button className="btn btn-secondary logout-btn" onClick={onLogout}>
              Logout
            </button>
          </div>
        </div>
      </div>

      {/* Debug info modal */}
      {showDebug && debugInfo && (
        <div
          className="debug-modal-overlay"
          onClick={() => setShowDebug(false)}
        >
          <div className="debug-modal" onClick={(e) => e.stopPropagation()}>
            <h3>WebSocket Debug Info</h3>
            <div className="debug-content">
              <p>
                <strong>Current User ID:</strong> {debugInfo.currentUserId}
              </p>
              <p>
                <strong>Current User Online:</strong>{" "}
                {debugInfo.currentUserOnline ? "Yes" : "No"}
              </p>
              <p>
                <strong>Total Online Users:</strong>{" "}
                {debugInfo.totalOnlineUsers}
              </p>
              <p>
                <strong>Frontend Connection:</strong>{" "}
                {wsConnected ? "Connected" : "Disconnected"}
              </p>
              <p>
                <strong>System Status:</strong> {debugInfo.optimizedForRealTime}
              </p>
              <p>
                <strong>Connection URL:</strong>{" "}
                {debugInfo.frontendWebSocketUrl}
              </p>
              <p>
                <strong>Debug Tip:</strong> {debugInfo.lastWebSocketActivity}
              </p>
            </div>
            <button onClick={() => setShowDebug(false)}>Close</button>
          </div>
        </div>
      )}
    </header>
  );
};

export default Navbar;
