import React, { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { postAPI, relationshipAPI, userAPI } from "../../services/api";
import { getUser, logout, saveUser } from "../../utils/auth";
import { isContentEmpty } from "../RichTextEditor";
import PostCard from "../Post/PostCard/PostCard";
import PostModal from "../Post/PostModal/PostModal";
import CreatePost from "./CreatePost/CreatePost";
import Navbar from "./Navbar/Navbar";
import PairRequestModal from "./PairRequestModal/PairRequestModal";
import MessagePanel from "./PairRequestPanel/PairRequestPanel";
import {
  NotificationManager,
  showNotificationToast,
} from "../../utils/websocket";
import type {
  Post,
  PageResponse,
  User,
  WebSocketNotification,
  MessageItem,
  RichContent,
} from "../../types/api";
import "./Home.css";

const Home: React.FC = () => {
  const navigate = useNavigate();

  // Post related states
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedPost, setSelectedPost] = useState<Post | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [showCreatePost, setShowCreatePost] = useState(false);
  const [creatingPost, setCreatingPost] = useState(false);

  // User related states
  const [currentUser, setCurrentUser] = useState<User | null>(null);

  // Modal states
  const [showPairRequestModal, setShowPairRequestModal] = useState(false);
  const [showPairRequestPanel, setShowPairRequestPanel] = useState(false);

  // Notification states
  const [pendingRequestCount, setPendingRequestCount] = useState(0); // Pair request count
  const [wsConnected, setWsConnected] = useState(false);

  // Message management states
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [messageIdCounter, setMessageIdCounter] = useState(1);
  const [pairStatusNotifications, setPairStatusNotifications] = useState<
    WebSocketNotification[]
  >([]);

  // Refs
  const notificationManagerRef = useRef<NotificationManager | null>(null);
  const currentUserRef = useRef<User | null>(null);

  // Calculate total notification count: pair requests + unread messages
  const totalNotificationCount =
    pendingRequestCount + messages.filter((msg) => !msg.isRead).length;

  // Update refs when state changes
  useEffect(() => {
    currentUserRef.current = currentUser;
  }, [currentUser]);

  // Add new message
  const addMessage = useCallback(
    (notification: WebSocketNotification) => {
      const newMessage: MessageItem = {
        id: messageIdCounter,
        type: notification.type,
        title: getMessageTitle(notification.type),
        content: getMessageContent(notification),
        senderNickname: getUserNickname(notification.fromUserId),
        senderUsername: getUserUsername(notification.fromUserId),
        senderId: notification.fromUserId,
        createdAt: notification.timestamp,
        isRead: false,
        metadata: extractMetadata(notification),
      };

      setMessages((prev) => [newMessage, ...prev]);
      setMessageIdCounter((prev) => prev + 1);
    },
    [messageIdCounter]
  );

  // Mark message as read
  const markMessageAsRead = useCallback((messageId: number) => {
    setMessages((prev) =>
      prev.map((msg) => (msg.id === messageId ? { ...msg, isRead: true } : msg))
    );
  }, []);

  // Mark pair status notification as read
  const markPairStatusNotificationAsRead = useCallback(
    (notificationId: string) => {
      setPairStatusNotifications((prev) =>
        prev.filter(
          (notification) =>
            `${notification.type}-${notification.timestamp}` !== notificationId
        )
      );
    },
    []
  );

  // Get message title based on notification type
  const getMessageTitle = (type: string): string => {
    switch (type) {
      case "NEW_POST":
        return "posted a new update";
      case "NEW_COMMENT":
        return "commented on your post";
      case "POST_DELETED":
        return "deleted a post";
      case "COMMENT_DELETED":
        return "deleted a comment";
      case "PAIR_REQUEST_ACCEPTED":
        return "accepted your pair request";
      case "PAIR_REQUEST_REJECTED":
        return "rejected your pair request";
      case "RELATIONSHIP_ENDED":
        return "ended the relationship";
      default:
        return "system message";
    }
  };

  // Get message content from notification
  const getMessageContent = (notification: WebSocketNotification): string => {
    return notification.message || "No details available";
  };

  // Extract metadata from notification
  const extractMetadata = (notification: WebSocketNotification): any => {
    const metadata: any = {};

    if (notification.data) {
      if (notification.data.postId) metadata.postId = notification.data.postId;
      if (notification.data.commentId)
        metadata.commentId = notification.data.commentId;
      if (notification.data.requestId)
        metadata.requestId = notification.data.requestId;
    }

    return metadata;
  };

  // Get user nickname (temporary implementation)
  const getUserNickname = (userId: number): string => {
    return `User${userId}`;
  };

  // Get username (temporary implementation)
  const getUserUsername = (userId: number): string => {
    return `user${userId}`;
  };

  // Load initial pending requests (one-time only, then WebSocket takes over)
  const loadInitialPendingRequests = useCallback(async (reason: string) => {
    try {
      const response = await relationshipAPI.getPendingRequests();
      if (response.code === 200) {
        const pendingCount = response.data.length;
        setPendingRequestCount(pendingCount);
      }
    } catch (error) {
      // Silently handle error
    }
  }, []);

  // Initialize WebSocket connection
  const initializeWebSocket = useCallback(
    (token: string) => {
      if (notificationManagerRef.current) {
        notificationManagerRef.current.disconnect();
      }

      try {
        const notificationManager = new NotificationManager(token);

        notificationManagerRef.current = notificationManager;

        // Connection state listener
        notificationManager.addListener("connection", (notification) => {
          const eventType = (notification as any).type;

          if (eventType === "CONNECTION_OPEN") {
            setWsConnected(true);
          } else {
            setWsConnected(false);
          }
        });

        // Pair request notifications
        notificationManager.addListener("PAIR_REQUEST", (notification) => {
          showNotificationToast(notification);

          // Update pending request count directly from WebSocket notification
          setPendingRequestCount((prev) => prev + 1);

          // Pure WebSocket system - no API polling ever needed!
        });

        // Other notification types - add to message center
        notificationManager.addListener(
          "PAIR_REQUEST_ACCEPTED",
          (notification) => {
            showNotificationToast(notification);

            // Add to pair status notifications instead of messages
            setPairStatusNotifications((prev) => [
              notification,
              ...prev.slice(0, 9),
            ]); // Keep only last 10

            // Update user's relationship status - they are now paired
            if (currentUserRef.current) {
              const metadata = extractMetadata(notification);

              // If current user was the one who sent the request that got accepted
              if (
                metadata &&
                metadata.fromUserId !== currentUserRef.current.userId
              ) {
                // Update to COUPLED status and set partner info
                const updatedUser: User = {
                  ...currentUserRef.current!,
                  relationshipStatus: "COUPLED",
                  partnerId: metadata.fromUserId, // Corrected: use partnerId
                };
                setCurrentUser(updatedUser);
                saveUser(updatedUser); // Persist updated user info
              }
            }
          }
        );

        notificationManager.addListener(
          "PAIR_REQUEST_REJECTED",
          (notification) => {
            showNotificationToast(notification);

            // Add to pair status notifications instead of messages
            setPairStatusNotifications((prev) => [
              notification,
              ...prev.slice(0, 9),
            ]); // Keep only last 10

            // Similar to accepted - update pending count if needed
            if (currentUserRef.current) {
              const metadata = extractMetadata(notification);
              if (
                metadata &&
                metadata.fromUserId !== currentUserRef.current.userId
              ) {
                setPendingRequestCount((prev) => Math.max(0, prev - 1));
              }
            }
          }
        );

        notificationManager.addListener("NEW_POST", (notification) => {
          showNotificationToast(notification);
          addMessage(notification);
        });

        notificationManager.addListener("NEW_COMMENT", (notification) => {
          showNotificationToast(notification);
          addMessage(notification);
        });

        notificationManager.addListener("POST_DELETED", (notification) => {
          showNotificationToast(notification);
          addMessage(notification);
        });

        notificationManager.addListener("COMMENT_DELETED", (notification) => {
          showNotificationToast(notification);
          addMessage(notification);
        });

        notificationManager.addListener(
          "RELATIONSHIP_ENDED",
          (notification) => {
            showNotificationToast(notification);
            addMessage(notification);

            // Update user's relationship status - they are now single
            if (currentUserRef.current) {
              const updatedUser = {
                ...currentUserRef.current,
                relationshipStatus: "SINGLE" as const,
                partnerId: undefined,
              };

              setCurrentUser(updatedUser);
              currentUserRef.current = updatedUser;

              // Save updated user to localStorage
              saveUser(updatedUser);
            }
          }
        );

        notificationManager.connect();
      } catch (error) {
        // Silently handle error
      }
    },
    [addMessage]
  );

  // Fetches posts from the API
  const processPostData = (post: any): Post => {
    // Ensure richContent is parsed if it's a string
    if (typeof post.richContent === "string") {
      try {
        post.richContent = JSON.parse(post.richContent);
      } catch (e) {
        // Fallback to error content on parse failure
        post.richContent = { ops: [{ insert: "Error loading content." }] };
      }
    }
    return post;
  };

  const loadPosts = useCallback(
    async (pageNum = 0, append = false) => {
      // if (loading || !hasMore) return; // This logic prevents initial load if hasMore is false

      setLoading(true);
      try {
        const response = await postAPI.getPosts(pageNum, 10);
        if (response.code === 200 && response.data) {
          const pageResponse = response.data;
          const processedPosts = pageResponse.content.map(processPostData);
          setPosts((prevPosts) =>
            append ? [...prevPosts, ...processedPosts] : processedPosts
          );
          setPage(pageResponse.page + 1); // Corrected: use page directly
          setHasMore(!pageResponse.last);
        } else {
          setHasMore(false);
        }
      } catch (error) {
        setHasMore(false); // Stop trying on error
      } finally {
        setLoading(false);
      }
    },
    [] // Remove dependencies to allow it to be called freely
  );

  // Main initialization logic
  useEffect(() => {
    const user = getUser();
    if (!user) {
      navigate("/login");
      return;
    }
    setCurrentUser(user);

    loadPosts(0, false); // Load initial posts on mount

    const token = localStorage.getItem("token");
    if (token && user) {
      initializeWebSocket(token);
      loadInitialPendingRequests("Initial load on login");
    }

    return () => {
      if (notificationManagerRef.current) {
        notificationManagerRef.current.disconnect();
      }
    };
  }, [navigate, initializeWebSocket, loadInitialPendingRequests]);

  // Manual refresh - only reconnect WebSocket, no API polling needed
  const handleManualRefresh = useCallback(() => {
    // Always try to reconnect WebSocket
    const token = localStorage.getItem("token");
    if (token) {
      // Disconnect current connection and reconnect
      if (notificationManagerRef.current) {
        notificationManagerRef.current.disconnect();
      }
      // Small delay to ensure clean disconnect
      setTimeout(() => {
        initializeWebSocket(token);
      }, 100);
    }
  }, [initializeWebSocket]);

  // Handle post click
  const handlePostClick = (post: Post) => {
    setSelectedPost(post);
    setIsModalOpen(true);
  };

  // Handle post update
  const handlePostUpdate = (updatedPost: Post) => {
    if (updatedPost.postId === -1) {
      setPosts((prev) =>
        prev.filter((post) => post.postId !== selectedPost?.postId)
      );
      setSelectedPost(null);
      return;
    }

    setPosts((prev) =>
      prev.map((post) =>
        post.postId === updatedPost.postId ? updatedPost : post
      )
    );
    setSelectedPost(updatedPost);
  };

  // Load more posts
  const loadMore = () => {
    if (!loading && hasMore) {
      loadPosts(page + 1, true);
    }
  };

  // Handle create post
  const handleCreatePost = async (content: RichContent) => {
    if (!content || isContentEmpty(content)) return;

    setCreatingPost(true);
    try {
      const response = await postAPI.createPost({
        richContent: content,
      });

      if (response.code === 200) {
        const processedPost = processPostData(response.data);
        setPosts((prev) => [processedPost, ...prev]);
        setShowCreatePost(false);
      }
    } catch (error) {
      // Silently handle error
    } finally {
      setCreatingPost(false);
    }
  };

  // Handle logout
  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  // Handle add partner click
  const handleAddPartnerClick = () => {
    setShowPairRequestModal(true);
  };

  // Handle pair request sent
  const handlePairRequestSent = () => {
    // Handle pair request sent
  };

  // Handle notification button click
  const handleNotificationClick = () => {
    setShowPairRequestPanel(true);
  };

  // Handle notification count change from MessagePanel
  const handleNotificationCountChange = (count: number) => {
    setPendingRequestCount(count);
  };

  // Handle profile click
  const handleProfileClick = () => {
    // Navigate to profile page or open profile modal
  };

  if (!currentUser) {
    return <div>Not authenticated</div>;
  }

  return (
    <div className="home-container">
      {/* Navigation bar */}
      <Navbar
        user={currentUser}
        notificationCount={totalNotificationCount}
        wsConnected={wsConnected}
        onAddPartnerClick={handleAddPartnerClick}
        onNotificationClick={handleNotificationClick}
        onManualRefresh={handleManualRefresh}
        onLogout={handleLogout}
        onProfileClick={handleProfileClick}
      />

      {/* Main content */}
      <main className="main-content">
        <div className="container">
          <div className="content-wrapper">
            {/* Posts section */}
            <div className="posts-section">
              <div className="section-header">
                <h3>Latest Updates</h3>
                <div className="section-actions">
                  <span className="posts-count">
                    {posts.length > 0 && `${posts.length} posts`}
                  </span>
                  <button
                    className="btn btn-primary create-post-btn"
                    onClick={() => setShowCreatePost(!showCreatePost)}
                  >
                    {showCreatePost ? "Cancel" : "Create Post"}
                  </button>
                </div>
              </div>

              {/* Create post form */}
              {showCreatePost && (
                <CreatePost
                  onCancel={() => setShowCreatePost(false)}
                  onSubmit={handleCreatePost}
                  disabled={creatingPost}
                />
              )}

              {/* Posts list */}
              <div className="posts-list">
                {loading && posts.length === 0 ? (
                  <div className="loading-message">Loading posts...</div>
                ) : posts.length === 0 ? (
                  <div className="no-posts-message">
                    <p>No posts yet.</p>
                    <p>Create your first post to get started!</p>
                  </div>
                ) : (
                  posts.map((post) => (
                    <PostCard
                      key={post.postId}
                      post={post}
                      onClick={() => handlePostClick(post)}
                    />
                  ))
                )}

                {/* Load more button */}
                {hasMore && posts.length > 0 && (
                  <div className="load-more-container">
                    <button
                      className="btn btn-secondary load-more-btn"
                      onClick={loadMore}
                      disabled={loading}
                    >
                      {loading ? "Loading..." : "Load More"}
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Post detail modal */}
      {isModalOpen && selectedPost && (
        <PostModal
          post={selectedPost}
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onPostUpdate={handlePostUpdate}
        />
      )}

      {/* Pair request modal */}
      {showPairRequestModal && (
        <PairRequestModal
          isOpen={showPairRequestModal}
          currentUser={currentUser}
          onClose={() => setShowPairRequestModal(false)}
          onRequestSent={handlePairRequestSent}
        />
      )}

      {/* Message panel */}
      {showPairRequestPanel && (
        <MessagePanel
          currentUser={currentUser}
          isOpen={showPairRequestPanel}
          onClose={() => setShowPairRequestPanel(false)}
          notificationCount={pendingRequestCount}
          onNotificationCountChange={handleNotificationCountChange}
          messages={messages}
          onMessageRead={markMessageAsRead}
          onUserUpdated={(updatedUser) => {
            setCurrentUser(updatedUser);
            currentUserRef.current = updatedUser;
            saveUser(updatedUser);
          }}
          pairStatusNotifications={pairStatusNotifications}
          onPairStatusNotificationRead={markPairStatusNotificationAsRead}
        />
      )}
    </div>
  );
};

export default Home;
