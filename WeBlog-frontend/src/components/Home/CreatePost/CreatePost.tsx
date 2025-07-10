import React, { useState } from "react";
import RichTextEditor, {
  type RichContent,
  isContentEmpty,
} from "../../RichTextEditor";
import "./CreatePost.css";

interface CreatePostProps {
  onCancel: () => void;
  onSubmit: (content: RichContent) => void;
  disabled?: boolean;
}

const CreatePost: React.FC<CreatePostProps> = ({
  onCancel,
  onSubmit,
  disabled = false,
}) => {
  const [content, setContent] = useState<RichContent | null>(null);

  const handleSubmit = () => {
    if (content && !isContentEmpty(content)) {
      onSubmit(content);
    }
  };

  return (
    <div className="create-post-form">
      <RichTextEditor
        value={content}
        onChange={setContent}
        placeholder="What's on your mind?"
        maxLength={10000}
        minHeight={120}
        disabled={disabled}
      />
      <div className="create-post-footer">
        <div className="create-post-actions">
          <button
            className="btn btn-secondary"
            onClick={onCancel}
            disabled={disabled}
          >
            Cancel
          </button>
          <button
            className="btn btn-primary"
            onClick={handleSubmit}
            disabled={disabled || !content || isContentEmpty(content)}
          >
            {disabled ? "Publishing..." : "Publish"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default CreatePost;
