import React, { useState, useEffect } from "react";
import ReactQuill from "react-quill";
import "react-quill/dist/quill.bubble.css"; // Theme for display
import { formatDateTime } from "../../../utils/date";
import { getUser } from "../../../utils/auth";
import { commentAPI, postAPI } from "../../../services/api";
import RichTextEditor, {
  createPlainTextContent,
  getPlainText,
  isContentEmpty,
  type RichContent,
} from "../../RichTextEditor";
import type {
  Post,
  Comment,
  CreateCommentRequest,
  UpdatePostRequest,
} from "../../../types/api";
import "./PostModal.css";

interface PostModalProps {
  post: Post;
  isOpen: boolean;
  onClose: () => void;
  onPostUpdate?: (post: Post) => void;
}

const PostModal: React.FC<PostModalProps> = ({
  post,
  isOpen,
  onClose,
  onPostUpdate,
}) => {
  const [comments, setComments] = useState<Comment[]>([]);
  const [newComment, setNewComment] = useState<RichContent | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [editContent, setEditContent] = useState<RichContent | null>(null);
  const [updating, setUpdating] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showDeleteCommentConfirm, setShowDeleteCommentConfirm] =
    useState(false);
  const [commentToDelete, setCommentToDelete] = useState<number | null>(null);
  const currentUser = getUser();

  useEffect(() => {
    if (post && isOpen) {
      loadComments();
    }
  }, [post, isOpen]);

  useEffect(() => {
    if (!isOpen) {
      setError(null);
    }
  }, [isOpen]);

  const loadComments = async () => {
    try {
      setLoading(true);
      setError(null);

      // First try to use the comments array from post data
      if (post.comments && post.comments.length > 0) {
        const response = await commentAPI.getCommentsBatch(post.comments);
        if (response.code === 200) {
          setComments(response.data);
          return;
        }
      }

      // Fallback: use getPostComments API if comments array is empty or failed
      const response = await commentAPI.getPostComments(post.postId, {
        page: 0,
        size: 100,
        sort: "createdAt,asc",
      });

      if (response.code === 200) {
        setComments(response.data.content);

        // Update post data with correct comment info if it was missing
        if (
          (!post.comments || post.comments.length === 0) &&
          response.data.content.length > 0
        ) {
          const commentIds = response.data.content.map((c) => c.commentId);
          const updatedPost = {
            ...post,
            comments: commentIds,
            commentsCount: response.data.content.length,
          };
          onPostUpdate?.(updatedPost);
        }
      } else {
        setComments([]);
      }
    } catch (error) {
      // Silent error handling
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment || isContentEmpty(newComment)) return;

    try {
      setSubmitting(true);
      setError(null);

      const commentData: CreateCommentRequest = {
        richContent: newComment,
      };

      const response = await commentAPI.createComment(post.postId, commentData);
      if (response.code === 200) {
        const newCommentObj = response.data;
        const newComments = [...comments, newCommentObj];
        setComments(newComments);
        setNewComment(null);

        // Update the post object with new comment count
        const updatedPost = {
          ...post,
          commentsCount: newComments.length,
          comments: [...(post.comments || []), newCommentObj.commentId],
        };
        onPostUpdate?.(updatedPost);
      } else {
        setError("Failed to submit comment");
      }
    } catch (error) {
      setError("Failed to submit comment");
    } finally {
      setSubmitting(false);
    }
  };

  const showDeleteCommentConfirmation = (commentId: number) => {
    setCommentToDelete(commentId);
    setShowDeleteCommentConfirm(true);
  };

  const hideDeleteCommentConfirmation = () => {
    setShowDeleteCommentConfirm(false);
    setCommentToDelete(null);
  };

  const handleDeleteComment = async () => {
    if (!currentUser || !commentToDelete) return;

    try {
      setError(null);
      const response = await commentAPI.deleteComment(commentToDelete);
      if (response.code === 200) {
        const newComments = comments.filter(
          (comment) => comment.commentId !== commentToDelete
        );
        setComments(newComments);

        // Update the post object with new comment count
        const updatedPost = {
          ...post,
          commentsCount: newComments.length,
          comments: (post.comments || []).filter(
            (id) => id !== commentToDelete
          ),
        };
        onPostUpdate?.(updatedPost);
      } else {
        setError("Failed to delete comment");
      }
    } catch (error) {
      setError("Failed to delete comment");
    } finally {
      hideDeleteCommentConfirmation();
    }
  };

  const canEditPost = () => {
    if (!currentUser) return false;
    return post.author.userId === currentUser.userId;
  };

  const handleEditPost = () => {
    setEditing(true);
    setEditContent(post.richContent || createPlainTextContent(""));
  };

  const handleUpdatePost = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editContent || isContentEmpty(editContent)) return;

    setUpdating(true);
    try {
      const updateData: UpdatePostRequest = {
        richContent: editContent,
      };

      const response = await postAPI.updatePost(post.postId, updateData);

      if (response.code === 200) {
        const updatedPost = response.data;
        onPostUpdate?.(updatedPost);
        setEditing(false);
        setEditContent(null);
      } else {
        setError("Failed to update post");
      }
    } catch (error) {
      setError("Failed to update post");
    } finally {
      setUpdating(false);
    }
  };

  const handleDeletePost = async () => {
    // The confirmation is now handled by the custom modal
    try {
      setError(null);
      const response = await postAPI.deletePost(post.postId);
      if (response.code === 200) {
        const deletedPost = { ...post, postId: -1 };
        onPostUpdate?.(deletedPost);
        onClose();
      } else {
        setError("Failed to delete post");
      }
    } catch (error) {
      setError("Failed to delete post");
    }
    // The modal is hidden by confirmDelete
  };

  const showDeleteConfirmation = () => {
    setShowDeleteConfirm(true);
  };

  const hideDeleteConfirmation = () => {
    setShowDeleteConfirm(false);
  };

  const confirmDelete = () => {
    setShowDeleteConfirm(false);
    handleDeletePost();
  };

  const getPlainTextFromRichContent = (content: RichContent): string => {
    if (!content || !content.delta || content.delta.length === 0) {
      return "";
    }

    return content.delta.reduce((text: string, op: any) => {
      if (typeof op.insert === "string") {
        return text + op.insert;
      } else if (typeof op.insert === "object" && "text" in op.insert) {
        return text + (op.insert as { text: string }).text;
      }
      return text;
    }, "");
  };

  if (!isOpen) {
    return null;
  }

  if (!post) {
    return (
      <div className="modal-overlay">
        <div className="modal-content">
          <p>Error: Post data is invalid</p>
          <button onClick={onClose}>Close</button>
        </div>
      </div>
    );
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-content post-modal"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h3>Post Details</h3>
          <button className="modal-close" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="modal-body">
          {error && <div className="error-message">{error}</div>}

          {/* Post Content */}
          <div className="post-detail">
            <div className="post-header">
              <div className="post-author">
                <div className="author-avatar">
                  {post.author?.nickname?.charAt(0).toUpperCase() || "?"}
                </div>
                <div className="author-info">
                  <span className="author-name">
                    {post.author?.nickname || "Anonymous"}
                  </span>
                  <span className="post-time">
                    {formatDateTime(post.createdAt)}
                  </span>
                  {post.createdAt !== post.updatedAt && (
                    <span className="updated-time">
                      (Edited on {formatDateTime(post.updatedAt)})
                    </span>
                  )}
                </div>
              </div>

              {/* Post actions - only show for post author */}
              {canEditPost() && (
                <div className="post-actions">
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={handleEditPost}
                  >
                    Edit
                  </button>
                  <button
                    className="btn btn-danger btn-sm"
                    onClick={showDeleteConfirmation}
                  >
                    Delete
                  </button>
                </div>
              )}
            </div>

            <div className="post-content-full">
              {editing ? (
                <form onSubmit={handleUpdatePost} className="edit-post-form">
                  <RichTextEditor
                    value={editContent}
                    onChange={setEditContent}
                    placeholder="Edit your post..."
                    maxLength={10000}
                    minHeight={150}
                    disabled={updating}
                  />
                  <div className="edit-form-footer">
                    <div className="edit-form-buttons">
                      <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() => {
                          setEditing(false);
                          setEditContent(null);
                        }}
                      >
                        Cancel
                      </button>
                      <button
                        type="submit"
                        className="btn btn-primary"
                        disabled={
                          updating ||
                          !editContent ||
                          isContentEmpty(editContent)
                        }
                      >
                        {updating ? "Updating..." : "Update Post"}
                      </button>
                    </div>
                  </div>
                </form>
              ) : (
                <ReactQuill
                  value={post.richContent?.delta || ""}
                  readOnly={true}
                  theme="bubble"
                  className="rich-content-display"
                />
              )}
            </div>
          </div>

          {/* Comments Section */}
          <div className="comments-section">
            <h4>Comments ({comments.length})</h4>

            {/* Add Comment Form */}
            {currentUser && (
              <form onSubmit={handleSubmitComment} className="comment-form">
                <RichTextEditor
                  value={newComment}
                  onChange={setNewComment}
                  placeholder="Write a comment..."
                  maxLength={500}
                  minHeight={100}
                  disabled={submitting}
                />
                <div className="comment-form-footer">
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={
                      submitting || !newComment || isContentEmpty(newComment)
                    }
                  >
                    {submitting ? "Submitting..." : "Add Comment"}
                  </button>
                </div>
              </form>
            )}

            {/* Comments list */}
            <div className="comments-list">
              {loading ? (
                <div className="loading">Loading comments...</div>
              ) : comments.length > 0 ? (
                comments.map((comment) => (
                  <div key={comment.commentId} className="comment-item">
                    <div className="comment-header">
                      <div className="comment-author">
                        <div className="author-avatar">
                          {comment.author?.nickname?.charAt(0).toUpperCase() ||
                            "?"}
                        </div>
                        <div className="author-info">
                          <span className="author-name">
                            {comment.author?.nickname || "Anonymous"}
                          </span>
                          <span className="comment-time">
                            {formatDateTime(comment.createdAt)}
                          </span>
                        </div>
                      </div>
                      {canEditPost() && (
                        <button
                          className="delete-btn"
                          onClick={() =>
                            showDeleteCommentConfirmation(comment.commentId)
                          }
                          title="Delete comment"
                        >
                          Delete
                        </button>
                      )}
                    </div>
                    <div className="comment-content">
                      <ReactQuill
                        value={comment.richContent?.delta || ""}
                        readOnly={true}
                        theme="bubble"
                        className="rich-content-display"
                      />
                    </div>
                  </div>
                ))
              ) : (
                <div className="no-comments">
                  No comments yet. Be the first to share your thoughts!
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {showDeleteConfirm && (
        <div className="confirm-overlay" onClick={hideDeleteConfirmation}>
          <div className="confirm-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="confirm-header">
              <h3>Delete Post</h3>
            </div>
            <div className="confirm-body">
              <p>
                Are you sure you want to delete this post? This action cannot be
                undone.
              </p>
            </div>
            <div className="confirm-footer">
              <button
                className="btn btn-secondary"
                onClick={hideDeleteConfirmation}
              >
                Cancel
              </button>
              <button className="btn btn-danger" onClick={confirmDelete}>
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {showDeleteCommentConfirm && (
        <div
          className="confirm-overlay"
          onClick={hideDeleteCommentConfirmation}
        >
          <div className="confirm-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="confirm-header">
              <h3>Delete Comment</h3>
            </div>
            <div className="confirm-body">
              <p>
                Are you sure you want to delete this comment? This action cannot
                be undone.
              </p>
            </div>
            <div className="confirm-footer">
              <button
                className="btn btn-secondary"
                onClick={hideDeleteCommentConfirmation}
              >
                Cancel
              </button>
              <button className="btn btn-danger" onClick={handleDeleteComment}>
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PostModal;
