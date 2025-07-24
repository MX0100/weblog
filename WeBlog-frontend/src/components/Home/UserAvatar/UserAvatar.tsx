import React, { useState } from "react";
import type { User } from "../../../types/api";
import "./UserAvatar.css";

interface UserAvatarProps {
  currentUser: User;
  onAddPartnerClick: () => void;
  onProfileClick?: () => void;
}

const UserAvatar: React.FC<UserAvatarProps> = ({
  currentUser,
  onAddPartnerClick,
  onProfileClick,
}) => {
  // Only show add partner button for single users
  const canAddPartner = currentUser.relationshipStatus === "SINGLE";

  const handleProfileClick = () => {
    if (onProfileClick) {
      onProfileClick();
    } else {
      // Default behavior: show profile info (to be implemented)
      // Profile functionality to be implemented
    }
  };

  return (
    <div className="user-avatar-container">
      <div className="user-info">
        <div className="user-avatar-wrapper">
          <div
            className="user-avatar"
            onClick={handleProfileClick}
            title="View profile"
          >
            {currentUser.profileimg ? (
              <img
                src={currentUser.profileimg}
                alt={currentUser.nickname}
                className="avatar-image"
              />
            ) : (
              <span className="avatar-letter">
                {currentUser.nickname.charAt(0).toUpperCase()}
              </span>
            )}
          </div>

          {/* Add Partner Button - only show for single users */}
          {canAddPartner && (
            <div className="add-partner-btn">
              <button
                className="add-partner-circle"
                onClick={onAddPartnerClick}
                title="Add partner"
              >
                <span className="plus-icon">+</span>
              </button>
            </div>
          )}

          {/* Partner indicator - show for coupled users */}
          {!canAddPartner && (
            <div className="partner-indicator">
              <div className="heart-icon">💖</div>
            </div>
          )}
        </div>

        <div className="user-name-status">
          <span className="user-name">{currentUser.nickname}</span>
          <span className="relationship-status">
            {currentUser.relationshipStatus === "SINGLE"
              ? "Single"
              : `In a relationship${
                  currentUser.nickname ? ` (with: ${currentUser.nickname})` : ""
                }`}
          </span>
        </div>
      </div>
    </div>
  );
};

export default UserAvatar;
