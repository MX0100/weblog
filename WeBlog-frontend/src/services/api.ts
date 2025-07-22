import type {
  ApiResponse,
  User,
  LoginRequest,
  RegisterRequest,
  LoginResponse,
  UpdateUserRequest,
  ChangePasswordRequest,
  PairRequest,
  AcceptPairRequest,
  UserRelationship,
  Post,
  PageResponse,
  CreatePostRequest,
  UpdatePostRequest,
  Comment,
  CreateCommentRequest,
} from "../types/api";

// ======================================
// Environment Configuration
// ======================================
const ENV_CONFIG = {
  // API Configuration - 修复：通过CloudFront HTTPS终止访问EC2
  API_BASE_URL:
    import.meta.env.VITE_API_BASE_URL ||
    (import.meta.env.DEV
      ? "http://localhost:8080"
      : "https://dcyz06osekbqs.cloudfront.net"), // 修改：通过CloudFront代理

  // WebSocket Configuration - 修复：通过CloudFront WebSocket代理
  WS_URL:
    import.meta.env.VITE_WS_URL ||
    (import.meta.env.DEV
      ? "ws://localhost:8080/ws"
      : "wss://dcyz06osekbqs.cloudfront.net/ws"), // 修改：通过CloudFront WSS

  // Application Configuration
  APP_TITLE: import.meta.env.VITE_APP_TITLE || "WeBlog",
  APP_VERSION: import.meta.env.VITE_APP_VERSION || "1.0.0",
  ENV_NAME:
    import.meta.env.VITE_ENV_NAME ||
    (import.meta.env.DEV ? "development" : "production"),

  // Feature Flags
  ENABLE_DEVTOOLS:
    import.meta.env.VITE_ENABLE_DEVTOOLS === "true" || import.meta.env.DEV,
  ENABLE_DEBUG:
    import.meta.env.VITE_ENABLE_DEBUG === "true" || import.meta.env.DEV,
  ENABLE_ANALYTICS: import.meta.env.VITE_ENABLE_ANALYTICS === "true",

  // AWS Configuration
  CLOUDFRONT_DOMAIN: import.meta.env.VITE_CLOUDFRONT_DOMAIN || "",
  S3_BUCKET: import.meta.env.VITE_S3_BUCKET || "",
  AWS_REGION: import.meta.env.VITE_AWS_REGION || "us-east-1",
};

const API_BASE_URL = ENV_CONFIG.API_BASE_URL;

// ======================================
// API路径配置
// ======================================
// 开发环境：直接访问 localhost:8080/api
// 生产环境：直接访问 EC2:8080/api (不需要额外前缀)
const getApiUrl = (path: string) => {
  // 统一：直接使用API_BASE_URL + path，后端路径已包含/api
  return `${API_BASE_URL}${path}`;
};

// 调试信息
if (ENV_CONFIG.ENABLE_DEBUG) {
  console.log("🔧 环境信息:", {
    envName: ENV_CONFIG.ENV_NAME,
    isDev: import.meta.env.DEV,
    isProd: import.meta.env.PROD,
    apiBaseUrl: ENV_CONFIG.API_BASE_URL,
    wsUrl: ENV_CONFIG.WS_URL,
    mode: import.meta.env.MODE,
    features: {
      devtools: ENV_CONFIG.ENABLE_DEVTOOLS,
      debug: ENV_CONFIG.ENABLE_DEBUG,
      analytics: ENV_CONFIG.ENABLE_ANALYTICS,
    },
    aws: {
      cloudfront: ENV_CONFIG.CLOUDFRONT_DOMAIN,
      s3Bucket: ENV_CONFIG.S3_BUCKET,
      region: ENV_CONFIG.AWS_REGION,
    },
  });
}

const apiClient = {
  async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const token = localStorage.getItem("token");
    const headers = new Headers({
      "Content-Type": "application/json",
      ...options.headers,
    });

    if (token) {
      headers.append("Authorization", `Bearer ${token}`);
    }

    const config: RequestInit = {
      ...options,
      headers,
    };

    // 构建完整的API URL
    const fullUrl = getApiUrl(endpoint);
    const response = await fetch(fullUrl, config);

    const text = await response.text();
    const data = text ? JSON.parse(text) : {};

    if (!response.ok) {
      const error: any = new Error(
        data.message || `HTTP error! status: ${response.status}`
      );
      error.response = {
        status: response.status,
        data: data,
      };
      throw error;
    }

    return data;
  },
};

export const userAPI = {
  login: (data: LoginRequest) =>
    apiClient.request<LoginResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  register: (data: RegisterRequest) =>
    apiClient.request<User>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  getCurrentUser: () => apiClient.request<User>("/api/users/me"),
  getUserById: (userId: number) =>
    apiClient.request<User>(`/api/users/${userId}`),
  updateUser: (userId: number, data: UpdateUserRequest) =>
    apiClient.request<User>(`/api/users/${userId}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  changePassword: (userId: number, data: ChangePasswordRequest) =>
    apiClient.request<void>(`/api/users/${userId}/password`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  searchUser: (username: string) =>
    apiClient.request<User>(
      `/api/users/search?username=${encodeURIComponent(username)}`
    ),
};

export const relationshipAPI = {
  sendPairRequest: (data: PairRequest) =>
    apiClient.request<void>("/api/relationships/pair-request", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  acceptPairRequest: (data: { partnerUsername: string }) =>
    apiClient.request<UserRelationship>("/api/relationships/accept-request", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  rejectPairRequest: (data: { partnerUsername: string }) =>
    apiClient.request<void>("/api/relationships/reject-request", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  unpair: () =>
    apiClient.request<void>("/api/relationships/unpair", {
      method: "POST",
    }),
  getRelationshipStatus: () =>
    apiClient.request<any>("/api/relationships/status"),
  getPendingRequests: () =>
    apiClient.request<UserRelationship[]>("/api/relationships/pending"),
  getSentRequests: () =>
    apiClient.request<UserRelationship[]>("/api/relationships/sent"),
  getHistory: () =>
    apiClient.request<UserRelationship[]>("/api/relationships/history"),
};

export const postAPI = {
  getPosts: (page: number, size: number) =>
    apiClient.request<PageResponse<Post>>(
      `/api/posts?page=${page}&size=${size}`
    ),
  getPost: (postId: number) => apiClient.request<Post>(`/api/posts/${postId}`),
  createPost: (data: CreatePostRequest) =>
    apiClient.request<Post>("/api/posts", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  updatePost: (postId: number, data: UpdatePostRequest) =>
    apiClient.request<Post>(`/api/posts/${postId}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  deletePost: (postId: number) =>
    apiClient.request<null>(`/api/posts/${postId}`, {
      method: "DELETE",
    }),
};

export const commentAPI = {
  getPostComments: (
    postId: number,
    params: { page: number; size: number; sort: string }
  ) => {
    const { page, size, sort } = params;
    return apiClient.request<PageResponse<Comment>>(
      `/api/posts/${postId}/comments?page=${page}&size=${size}&sort=${sort}`
    );
  },
  createComment: (postId: number, data: CreateCommentRequest) =>
    apiClient.request<Comment>(`/api/posts/${postId}/comments`, {
      method: "POST",
      body: JSON.stringify(data),
    }),
  getCommentsBatch: (commentIds: number[]) =>
    apiClient.request<Comment[]>(`/api/comments/batch`, {
      method: "POST",
      body: JSON.stringify({ commentIds }),
    }),
  deleteComment: (commentId: number) =>
    apiClient.request<null>(`/api/comments/${commentId}`, {
      method: "DELETE",
    }),
};
