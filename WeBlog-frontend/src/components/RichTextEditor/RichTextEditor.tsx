import React, { useCallback, useMemo, useRef } from "react";
import ReactQuill, { Quill } from "react-quill";
import imageCompression from "browser-image-compression";
import "react-quill/dist/quill.snow.css";
import "./RichTextEditor.css";

export interface RichContent {
  type: "plain_text" | "rich_text";
  version: string;
  delta?: any;
  plainText: string;
}

interface RichTextEditorProps {
  value?: RichContent | null;
  onChange?: (content: RichContent) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  maxLength?: number;
  minHeight?: number;
  enableImageUpload?: boolean;
  onImageUpload?: (file: File) => Promise<string>;
}

const RichTextEditor: React.FC<RichTextEditorProps> = ({
  value,
  onChange,
  placeholder = "Start typing...",
  disabled = false,
  className = "",
  maxLength = 10000,
  minHeight = 200,
  enableImageUpload = true,
  onImageUpload,
}) => {
  const quillRef = useRef<ReactQuill>(null);

  // 图片上传处理
  const imageHandler = useCallback(() => {
    const input = document.createElement("input");
    input.setAttribute("type", "file");
    input.setAttribute("accept", "image/*");

    input.onchange = async () => {
      const file = input.files?.[0];
      if (!file) return;

      // 验证文件大小 (最大5MB)
      if (file.size > 5 * 1024 * 1024) {
        alert("Image size cannot exceed 5MB");
        return;
      }

      // 验证文件类型
      if (!file.type.startsWith("image/")) {
        alert("Please select an image file");
        return;
      }

      // 压缩图片
      const options = {
        maxSizeMB: 0.5, // Further reduce max file size
        maxWidthOrHeight: 800, // Stricter max dimensions
        useWebWorker: true,
      };

      try {
        console.log(
          `Original image size: ${(file.size / 1024 / 1024).toFixed(2)} MB`
        );
        const compressedFile = await imageCompression(file, options);
        console.log(
          `Compressed image size: ${(compressedFile.size / 1024 / 1024).toFixed(
            2
          )} MB`
        );

        let imageUrl: string;

        if (onImageUpload) {
          // 使用自定义上传函数
          imageUrl = await onImageUpload(compressedFile);
        } else {
          // 默认转换为base64
          imageUrl = await fileToBase64(compressedFile);
        }

        const quill = quillRef.current?.getEditor();
        if (quill) {
          const range = quill.getSelection(true); // Ensure we have the latest range
          quill.insertEmbed(range.index, "image", imageUrl);

          // Manually trigger the onChange handler because insertEmbed doesn't
          setTimeout(() => {
            const newContents = quill.getContents();
            const newPlainText = quill.getText();
            const newRichContent: RichContent = {
              type: "rich_text",
              version: "1.0",
              plainText: newPlainText.trim(),
              delta: newContents,
            };
            onChange?.(newRichContent);
          }, 0);
        }
      } catch (error) {
        console.error("Image processing failed:", error);
        alert("Image processing failed, please try again.");
      }
    };

    input.click();
  }, [onImageUpload, onChange]);

  // 文件转base64
  const fileToBase64 = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  };

  // 配置工具栏
  const modules = useMemo(
    () => ({
      toolbar: {
        container: [
          [{ header: [1, 2, 3, false] }],
          ["bold", "italic", "underline", "strike"],
          [{ color: [] }, { background: [] }],
          [{ list: "ordered" }, { list: "bullet" }],
          [{ align: [] }],
          ["blockquote", "code-block"],
          ["link"],
          ...(enableImageUpload ? [["image"]] : []),
          ["clean"],
        ],
        handlers: {
          ...(enableImageUpload && { image: imageHandler }),
        },
      },
      clipboard: {
        matchVisual: false,
      },
    }),
    [enableImageUpload, imageHandler]
  );

  const formats = [
    "header",
    "bold",
    "italic",
    "underline",
    "strike",
    "color",
    "background",
    "list",
    "bullet",
    "align",
    "blockquote",
    "code-block",
    "link",
    "image",
  ];

  // 处理内容变化
  const handleChange = useCallback(
    (content: string, delta: any, source: string, editor: any) => {
      // Only process changes from the user to prevent infinite loops
      if (source !== "user" || !onChange) {
        return;
      }

      const plainText = editor.getText();

      // 检查长度限制
      if (plainText.length > maxLength) {
        return;
      }

      const deltaOps = editor.getContents();

      // 判断是否为富文本内容
      const isRichText =
        deltaOps.ops &&
        deltaOps.ops.some(
          (op: any) =>
            op.attributes ||
            (op.insert && typeof op.insert !== "string") ||
            (op.insert === "\n" && deltaOps.ops.length > 1)
        );

      const richContent: RichContent = {
        type: isRichText ? "rich_text" : "plain_text",
        version: "1.0",
        plainText: plainText.trim(),
        delta: deltaOps, // Always include the delta
      };

      onChange(richContent);
    },
    [onChange, maxLength]
  );

  // 从RichContent获取Quill的内容
  const getQuillValue = useCallback(() => {
    if (!value) return "";

    // Always prioritize delta if it exists
    if (value.delta) {
      return value.delta;
    }

    // Fallback for old data or plain text without a delta
    if (value.type === "plain_text" && value.plainText) {
      return value.plainText;
    }

    return "";
  }, [value]);

  // 获取字符计数
  const getCharCount = useCallback(() => {
    return value?.plainText?.length || 0;
  }, [value]);

  return (
    <div className={`rich-text-editor ${className}`}>
      <ReactQuill
        ref={quillRef}
        theme="snow"
        value={getQuillValue()}
        onChange={handleChange}
        modules={modules}
        formats={formats}
        placeholder={placeholder}
        readOnly={disabled}
        style={{ minHeight: `${minHeight}px` }}
      />

      {/* 字符计数器 */}
      <div className="rich-text-editor__footer">
        <span
          className={`char-count ${
            getCharCount() > maxLength * 0.9 ? "warning" : ""
          }`}
        >
          {getCharCount()} / {maxLength}
        </span>
        {enableImageUpload && (
          <span className="image-upload-hint">
            💡 Click the image icon or drag & drop to upload
          </span>
        )}
      </div>
    </div>
  );
};

// 创建纯文本内容的辅助函数
export const createPlainTextContent = (text: string): RichContent => ({
  type: "plain_text",
  version: "1.0",
  plainText: text,
});

// 获取内容的纯文本表示
export const getPlainText = (content: RichContent | null): string => {
  return content?.plainText || "";
};

// 检查内容是否为空
export const isContentEmpty = (content: RichContent | null): boolean => {
  return (
    !content || !content.plainText || content.plainText.trim().length === 0
  );
};

export default RichTextEditor;
