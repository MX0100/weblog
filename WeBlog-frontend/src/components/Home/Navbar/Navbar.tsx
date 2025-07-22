import React, { useState } from "react";
import type { User } from "../../../types/api";
import { userAPI, relationshipAPI } from "../../../services/api";
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
  // User search state
  const [showSearchModal, setShowSearchModal] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResult, setSearchResult] = useState<User | null>(null);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  // Pair request state
  const [sendingRequest, setSendingRequest] = useState(false);
  const [showResultModal, setShowResultModal] = useState(false);
  const [requestResult, setRequestResult] = useState<{
    success: boolean;
    message: string;
  } | null>(null);

  // 添加部署测试标记
  const deploymentVersion =
    "v2.0-auto-deploy-test-" + new Date().toISOString().slice(0, 10);

  // Handle search user
  const handleSearchUser = async () => {
    if (!searchQuery.trim()) {
      setSearchError("Please enter username");
      return;
    }

    setSearchLoading(true);
    setSearchError(null);
    setSearchResult(null);

    try {
      const response = await userAPI.searchUser(searchQuery.trim());
      if (response.code === 200) {
        setSearchResult(response.data);
      } else {
        setSearchError(response.message || "Search failed");
      }
    } catch (error: any) {
      setSearchError(error.response?.data?.message || "User not found");
    } finally {
      setSearchLoading(false);
    }
  };

  // Handle send pair request
  const handleSendPairRequest = async () => {
    if (!searchResult) return;

    // Check if target user is already coupled
    if (searchResult.relationshipStatus !== "SINGLE") {
      setRequestResult({
        success: false,
        message: "Target user is already in a relationship",
      });
      setShowResultModal(true);
      return;
    }

    // Check if current user is already coupled
    if (user.relationshipStatus !== "SINGLE") {
      setRequestResult({
        success: false,
        message: "You are already in a relationship",
      });
      setShowResultModal(true);
      return;
    }

    setSendingRequest(true);

    try {
      const response = await relationshipAPI.sendPairRequest({
        partnerUsername: searchResult.username,
      });

      if (response.code === 200) {
        setRequestResult({
          success: true,
          message: "Request sent successfully",
        });
      } else {
        setRequestResult({
          success: false,
          message: response.message || "Failed to send request",
        });
      }
    } catch (error: any) {
      setRequestResult({
        success: false,
        message: error.response?.data?.message || "Failed to send request",
      });
    } finally {
      setSendingRequest(false);
      setShowResultModal(true);
    }
  };

  // Open search modal
  const openSearchModal = () => {
    setShowSearchModal(true);
    setSearchQuery("");
    setSearchResult(null);
    setSearchError(null);
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

            {/* Search user button */}
            <button
              className="search-btn"
              onClick={openSearchModal}
              title="Search Users"
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

      {/* Search user modal */}
      {showSearchModal && (
        <div
          className="search-modal-overlay"
          onClick={() => setShowSearchModal(false)}
        >
          <div className="search-modal" onClick={(e) => e.stopPropagation()}>
            <h3>Search User</h3>
            <div className="search-content">
              <div className="search-input-group">
                <input
                  type="text"
                  placeholder="Enter username"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onKeyPress={(e) => e.key === "Enter" && handleSearchUser()}
                  disabled={searchLoading}
                />
                <button
                  onClick={handleSearchUser}
                  disabled={searchLoading || !searchQuery.trim()}
                  className="search-submit-btn"
                >
                  {searchLoading ? "Searching..." : "Search"}
                </button>
              </div>

              {searchError && (
                <div className="search-error">
                  <p>{searchError}</p>
                </div>
              )}

              {searchResult && (
                <div className="search-result">
                  <div className="user-info">
                    <div className="user-avatar">
                      {searchResult.profileimg ? (
                        <img
                          src={searchResult.profileimg}
                          alt={searchResult.nickname}
                        />
                      ) : (
                        <span>
                          {searchResult.nickname.charAt(0).toUpperCase()}
                        </span>
                      )}
                    </div>
                    <div className="user-details">
                      <h4>{searchResult.nickname}</h4>
                      <p>@{searchResult.username}</p>
                      <p>Gender: {searchResult.gender}</p>
                      <p>
                        Relationship Status:{" "}
                        {searchResult.relationshipStatus === "SINGLE"
                          ? "Single"
                          : "In a relationship"}
                        {searchResult.username === user.username &&
                          " (This is you)"}
                      </p>
                      {searchResult.hobby && searchResult.hobby.length > 0 && (
                        <p>Hobbies: {searchResult.hobby.join(", ")}</p>
                      )}

                      {/* Send pair request button - only show if not searching self */}
                      {searchResult.username !== user.username && (
                        <div className="search-actions">
                          <button
                            className="send-request-btn"
                            onClick={handleSendPairRequest}
                            disabled={sendingRequest}
                          >
                            {sendingRequest ? "Sending..." : "Send Request"}
                          </button>
                        </div>
                      )}

                      {/* Self search message */}
                      {searchResult.username === user.username && (
                        <div className="self-search-message">
                          <p>👤 This is your profile</p>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>
            <button
              className="search-close-btn"
              onClick={() => setShowSearchModal(false)}
            >
              Close
            </button>
          </div>
        </div>
      )}

      {/* Result notification modal */}
      {showResultModal && requestResult && (
        <div
          className="result-modal-overlay"
          onClick={() => setShowResultModal(false)}
        >
          <div className="result-modal" onClick={(e) => e.stopPropagation()}>
            <div
              className={`result-icon ${
                requestResult.success ? "success" : "error"
              }`}
            >
              {requestResult.success ? "✓" : "✗"}
            </div>
            <h3>{requestResult.success ? "Request Sent" : "Request Failed"}</h3>
            <p>{requestResult.message}</p>
            <button
              className="result-close-btn"
              onClick={() => setShowResultModal(false)}
            >
              OK
            </button>
          </div>
        </div>
      )}
    </header>
  );
};

export default Navbar;
