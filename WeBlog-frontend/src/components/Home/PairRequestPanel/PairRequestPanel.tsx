import React, { useState, useEffect } from "react";
import { relationshipAPI } from "../../../services/api";
import type {
  User,
  MessageItem,
  PendingRequestItem,
  WebSocketNotification,
} from "../../../types/api";
import "./PairRequestPanel.css";

interface PairRequestPanelProps {
  currentUser: User;
  isOpen: boolean;
  onClose: () => void;
  notificationCount: number;
  onNotificationCountChange: (count: number) => void;
  messages: MessageItem[];
  onMessageRead: (messageId: number) => void;
  onUserUpdated: (updatedUser: User) => void;
  pairStatusNotifications: WebSocketNotification[];
  onPairStatusNotificationRead: (notificationId: string) => void;
}

const PairRequestPanel: React.FC<PairRequestPanelProps> = ({
  currentUser,
  isOpen,
  onClose,
  notificationCount,
  onNotificationCountChange,
  messages,
  onMessageRead,
  onUserUpdated,
  pairStatusNotifications,
  onPairStatusNotificationRead,
}) => {
  const [activeTab, setActiveTab] = useState<"pending" | "messages">("pending");
  const [pendingRequests, setPendingRequests] = useState<PendingRequestItem[]>(
    []
  );
  const [sentRequests, setSentRequests] = useState<PendingRequestItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>("");

  // Load messages and requests when panel opens
  useEffect(() => {
    if (isOpen) {
      loadMessages();
    }
  }, [isOpen, currentUser.userId]);

  // Load messages and requests
  const loadMessages = async () => {
    try {
      setLoading(true);
      setError("");

      // Load pending requests
      const pendingResponse = await relationshipAPI.getPendingRequests();

      // Load sent requests
      const sentResponse = await relationshipAPI.getSentRequests();

      if (pendingResponse.code === 200 && sentResponse.code === 200) {
        // The backend now returns RelationshipHistoryResponse[], so we map it to PendingRequestItem[]
        const mapToPendingItem = (
          item: any,
          currentUserId: number
        ): PendingRequestItem => ({
          ...item,
          partnerId: item.partnerId,
          partnerUsername: item.partnerUsername,
          partnerNickname: item.partnerNickname,
        });

        setPendingRequests(
          pendingResponse.data.map((item: any) =>
            mapToPendingItem(item, currentUser.userId)
          )
        );
        setSentRequests(
          sentResponse.data.map((item: any) =>
            mapToPendingItem(item, currentUser.userId)
          )
        );
        onNotificationCountChange(pendingResponse.data.length);
      } else {
        if (pendingResponse.code !== 200) {
          setError("Failed to load pending requests");
        }
        if (sentResponse.code !== 200) {
          setError("Failed to load sent requests");
        }
      }
    } catch (error) {
      setError("Failed to load messages");
    } finally {
      setLoading(false);
    }
  };

  // Accept pair request
  const acceptRequest = async (request: PendingRequestItem) => {
    try {
      setLoading(true);
      setError("");

      const response = await relationshipAPI.acceptPairRequest({
        partnerUsername: request.partnerUsername,
      });

      if (response.code === 200) {
        // Remove from pending requests
        setPendingRequests((prev) =>
          prev.filter((req) => req.id !== request.id)
        );

        // Update notification count
        onNotificationCountChange(
          pendingRequests.filter((req) => req.id !== request.id).length
        );

        // Refresh data
        loadMessages();

        // Update user status to COUPLED when accepting a pair request
        const updatedUser: User = {
          ...currentUser,
          relationshipStatus: "COUPLED",
          partnerId: request.partnerId,
        };

        onUserUpdated(updatedUser);
      } else {
        setError("Failed to accept request");
      }
    } catch (error) {
      setError("Failed to accept request");
    } finally {
      setLoading(false);
    }
  };

  // Reject pair request
  const rejectRequest = async (request: PendingRequestItem) => {
    try {
      setLoading(true);
      setError("");

      const response = await relationshipAPI.rejectPairRequest({
        partnerUsername: request.partnerUsername,
      });

      if (response.code === 200) {
        // Remove from pending requests
        setPendingRequests((prev) =>
          prev.filter((req) => req.id !== request.id)
        );

        // Update notification count
        onNotificationCountChange(
          pendingRequests.filter((req) => req.id !== request.id).length
        );

        // Refresh data
        loadMessages();
      } else {
        setError("Failed to reject request");
      }
    } catch (error) {
      setError("Failed to reject request");
    } finally {
      setLoading(false);
    }
  };

  // Format date
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString() + " " + date.toLocaleTimeString();
  };

  // Get user avatar
  const getUserAvatar = (user: any) => {
    if (user.profileimg) {
      return (
        <img
          src={user.profileimg}
          alt={user.nickname}
          className="user-avatar-img"
        />
      );
    }
    return (
      <div className="user-avatar-letter">
        {user.nickname?.charAt(0).toUpperCase() || "?"}
      </div>
    );
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div className="message-panel-overlay">
      <div className="message-panel">
        <div className="message-panel-header">
          <h3>Notifications</h3>
          <button className="close-button" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="message-panel-tabs">
          <button
            className={`tab-button ${activeTab === "pending" ? "active" : ""}`}
            onClick={() => setActiveTab("pending")}
          >
            Pair Activity (
            {pendingRequests.length + pairStatusNotifications.length})
          </button>
          <button
            className={`tab-button ${activeTab === "messages" ? "active" : ""}`}
            onClick={() => setActiveTab("messages")}
          >
            Messages ({messages.filter((m) => !m.isRead).length})
          </button>
        </div>

        <div className="message-panel-content">
          {loading && <div className="loading">Loading...</div>}
          {error && <div className="error">{error}</div>}

          {activeTab === "pending" && (
            <div className="pair-activity-container">
              {/* Combined Pair Activity List */}
              {pendingRequests.length === 0 &&
              pairStatusNotifications.length === 0 ? (
                <div className="no-activity">No pair activity</div>
              ) : (
                <div className="activity-list">
                  {/* Pending Requests */}
                  {pendingRequests.map((request) => (
                    <div
                      key={`request-${request.id}`}
                      className="activity-item request-item"
                    >
                      <div className="activity-header">
                        <div className="activity-avatar">
                          {getUserAvatar({ nickname: request.partnerNickname })}
                        </div>
                        <div className="activity-info">
                          <span className="activity-sender">
                            {request.partnerNickname}
                          </span>
                          <span className="activity-message">
                            wants to pair with you
                          </span>
                          <span className="activity-time">
                            {formatDate(request.createdAt)}
                          </span>
                        </div>
                        <div className="activity-type-badge incoming">
                          New Request
                        </div>
                      </div>
                      <div className="activity-actions">
                        <button
                          className="accept-btn"
                          onClick={() => acceptRequest(request)}
                          disabled={loading}
                        >
                          Accept
                        </button>
                        <button
                          className="reject-btn"
                          onClick={() => rejectRequest(request)}
                          disabled={loading}
                        >
                          Reject
                        </button>
                      </div>
                    </div>
                  ))}

                  {/* Pair Status Notifications */}
                  {pairStatusNotifications.map((notification) => (
                    <div
                      key={`notification-${notification.type}-${notification.timestamp}`}
                      className="activity-item status-item"
                      onClick={() =>
                        onPairStatusNotificationRead(
                          `${notification.type}-${notification.timestamp}`
                        )
                      }
                    >
                      <div className="activity-header">
                        <div className="activity-avatar">
                          <div className="status-avatar">
                            {notification.type === "PAIR_REQUEST_ACCEPTED"
                              ? "✅"
                              : "❌"}
                          </div>
                        </div>
                        <div className="activity-info">
                          <span className="activity-message">
                            {notification.message}
                          </span>
                          <span className="activity-time">
                            {formatDate(notification.timestamp)}
                          </span>
                        </div>
                        <div
                          className={`activity-type-badge ${
                            notification.type === "PAIR_REQUEST_ACCEPTED"
                              ? "accepted"
                              : "rejected"
                          }`}
                        >
                          {notification.type === "PAIR_REQUEST_ACCEPTED"
                            ? "Accepted"
                            : "Rejected"}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === "messages" && (
            <div className="messages-list">
              {messages.length === 0 ? (
                <div className="no-messages">No messages</div>
              ) : (
                messages.map((message) => (
                  <div
                    key={message.id}
                    className={`message-item ${
                      message.isRead ? "read" : "unread"
                    }`}
                    onClick={() => onMessageRead(message.id)}
                  >
                    <div className="message-header">
                      <div className="message-avatar">
                        {message.senderNickname?.charAt(0).toUpperCase() || "?"}
                      </div>
                      <div className="message-info">
                        <span className="message-sender">
                          {message.senderNickname}
                        </span>
                        <span className="message-title">{message.title}</span>
                        <span className="message-time">
                          {formatDate(message.createdAt)}
                        </span>
                      </div>
                    </div>
                    <div className="message-content">{message.content}</div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default PairRequestPanel;
