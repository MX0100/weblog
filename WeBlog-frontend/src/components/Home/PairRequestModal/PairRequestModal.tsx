import React, { useState } from "react";
import { relationshipAPI } from "../../../services/api";
import type { User } from "../../../types/api";
import "./PairRequestModal.css";

interface PairRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentUser: User;
  onRequestSent: () => void;
}

const PairRequestModal: React.FC<PairRequestModalProps> = ({
  isOpen,
  onClose,
  currentUser,
  onRequestSent,
}) => {
  const [partnerUsername, setPartnerUsername] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const username = partnerUsername.trim();
    if (!username) {
      setError("Please enter a username.");
      return;
    }

    if (username.toLowerCase() === currentUser.username.toLowerCase()) {
      setError("You cannot send a pair request to yourself.");
      return;
    }

    setIsSubmitting(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await relationshipAPI.sendPairRequest({
        partnerUsername: username,
      });

      if (response.code === 200) {
        setSuccess("Pair request sent successfully!");
        setPartnerUsername("");
        onRequestSent();

        setTimeout(() => {
          onClose();
          setSuccess(null);
        }, 1500);
      } else {
        // Handle cases where API returns a non-200 code but doesn't throw
        setError(response.message || "Failed to send request.");
      }
    } catch (error: any) {
      // Handle thrown errors (e.g., from GlobalExceptionHandler)
      if (error?.response?.data?.message) {
        setError(error.response.data.message);
      } else {
        setError(
          "Failed to send request. The user may not exist or another error occurred."
        );
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    if (!isSubmitting) {
      setPartnerUsername("");
      setError(null);
      setSuccess(null);
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="pair-modal-overlay" onClick={handleClose}>
      <div className="pair-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="pair-modal-header">
          <h3>Send Pair Request</h3>
          <button
            className="pair-modal-close"
            onClick={handleClose}
            disabled={isSubmitting}
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="pair-modal-body">
          <div className="form-group">
            <label htmlFor="partnerUsername" className="form-label">
              Partner's Username
            </label>
            <input
              type="text"
              id="partnerUsername"
              value={partnerUsername}
              onChange={(e) => setPartnerUsername(e.target.value)}
              className="form-input"
              placeholder="Enter your partner's username"
              required
              disabled={isSubmitting}
            />
            <div className="form-hint">
              Enter the username of the user you want to pair with.
            </div>
          </div>

          {error && (
            <div className="error-message">
              <span className="error-icon">⚠️</span>
              {error}
            </div>
          )}

          {success && (
            <div className="success-message">
              <span className="success-icon">✅</span>
              {success}
            </div>
          )}

          <div className="pair-modal-footer">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleClose}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={isSubmitting || !partnerUsername.trim()}
            >
              {isSubmitting ? "Sending..." : "Send Request"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default PairRequestModal;
