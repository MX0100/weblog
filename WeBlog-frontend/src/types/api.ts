// Basic API response format
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

// Rich Content type
export interface RichContent {
  type: "plain_text" | "rich_text";
  version: string;
  delta?: any;
  plainText: string;
}

// User related types
export interface User {
  userId: number;
  username: string;
  nickname: string;
  gender: Gender;
  profileimg?: string;
  hobby?: string[];
  relationshipStatus: RelationshipStatus;
  partnerId?: number;
  createdAt: string;
  updatedAt: string;
}

export type Gender =
  | "MALE"
  | "FEMALE"
  | "NON_BINARY"
  | "TRANSGENDER_MALE"
  | "TRANSGENDER_FEMALE"
  | "GENDERFLUID"
  | "AGENDER"
  | "OTHER"
  | "PREFER_NOT_TO_SAY";

export type RelationshipStatus = "SINGLE" | "COUPLED";

// Authentication related types
export interface RegisterRequest {
  username: string;
  password: string;
  nickname: string;
  gender: Gender;
  profileimg?: string;
  hobby?: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

// Post related types
export interface Author {
  userId: number;
  nickname: string;
}

export interface Post {
  postId: number;
  author: Author;
  richContent: RichContent;
  comments: number[];
  commentsCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePostRequest {
  richContent: RichContent;
}

export interface UpdatePostRequest {
  richContent: RichContent;
}

// Comment related types
export interface Comment {
  commentId: number;
  postId: number;
  author: Author;
  richContent: RichContent;
  createdAt: string;
}

export interface CreateCommentRequest {
  richContent: RichContent;
}

// Pagination related types
export interface PageRequest {
  page?: number;
  size?: number;
  sort?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// User info update
export interface UpdateUserRequest {
  nickname?: string;
  gender?: Gender;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

// Relationship/Pairing related types
export interface UserRelationship {
  id: number;
  user1Id: number;
  user2Id: number;
  relationshipType: string;
  status: RelationshipStatus;
  createdAt: string;
  endedAt?: string;
}

export interface PairRequest {
  partnerUsername: string;
}

export interface AcceptPairRequest {
  requestUserId: number;
}

export interface RejectPairRequestData {
  requestUserId: number;
}

export interface PendingRequestItem {
  id: number;
  partnerId: number;
  partnerUsername: string;
  partnerNickname: string;
  relationshipType: string;
  status: string;
  createdAt: string;
  endedAt?: string;
}

export interface RelationshipHistoryItem {
  id: number;
  partnerId: number;
  partnerUsername: string;
  partnerNickname: string;
  relationshipType: string;
  status: string;
  createdAt: string;
  endedAt?: string;
}

// WebSocket notification types
export interface WebSocketNotification {
  type: NotificationType;
  fromUserId: number;
  toUserId: number;
  message: string;
  data: any;
  timestamp: string;
}

export type NotificationType =
  | "PAIR_REQUEST"
  | "PAIR_REQUEST_ACCEPTED"
  | "PAIR_REQUEST_REJECTED"
  | "NEW_POST"
  | "NEW_COMMENT"
  | "POST_DELETED"
  | "COMMENT_DELETED"
  | "RELATIONSHIP_ENDED";

// Message Center types - for the new unified message interface
export interface MessageItem {
  id: number;
  type: NotificationType;
  title: string;
  content: string;
  senderNickname: string;
  senderUsername: string;
  senderId: number;
  createdAt: string;
  isRead: boolean;
  metadata?: {
    postId?: number;
    commentId?: number;
    requestId?: number;
    partnerId?: number;
  };
}

export interface MessageGroup {
  pairRequests: PendingRequestItem[];
  allMessages: MessageItem[];
  totalUnreadCount: number;
}
