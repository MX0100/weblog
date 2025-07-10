import React from "react";
import { formatRelativeTime } from "../../../utils/date";
import { getPlainText } from "../../RichTextEditor";
import type { Post } from "../../../types/api";
import "./PostCard.css";

interface PostCardProps {
  post: Post;
  onClick?: () => void;
}

const PostCard: React.FC<PostCardProps> = ({ post, onClick }) => {
  // 获取内容预览文本
  const getContentPreview = (maxLength = 150) => {
    const text = getPlainText(post.richContent);

    if (text.length <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + "...";
  };

  // 渲染富文本预览
  const renderContentPreview = () => {
    const preview = getContentPreview();

    // 如果是富文本且包含格式，显示一个小图标表示
    const hasRichFormatting = post.richContent?.type === "rich_text";

    return (
      <p>
        {hasRichFormatting && <span className="rich-text-indicator">🎨 </span>}
        {preview}
      </p>
    );
  };

  return (
    <div className="post-card" onClick={onClick}>
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
              {formatRelativeTime(post.createdAt)}
            </span>
          </div>
        </div>
      </div>

      <div className="post-content">{renderContentPreview()}</div>

      <div className="post-footer">
        <div className="post-stats">
          <span className="comments-count">
            💬 {post.commentsCount || 0} comments
          </span>
          {post.createdAt !== post.updatedAt && (
            <span className="updated-indicator">Edited</span>
          )}
        </div>
        <div className="post-actions">
          <button
            className="post-action-btn"
            onClick={(e) => {
              e.stopPropagation();
              onClick?.();
            }}
          >
            View Details
          </button>
        </div>
      </div>
    </div>
  );
};

export default PostCard;
