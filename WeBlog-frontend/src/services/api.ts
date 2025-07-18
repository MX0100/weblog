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

    const response = await fetch(endpoint, config);

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
