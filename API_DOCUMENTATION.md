# WeBlog API Documentation

This document provides a detailed overview of the WeBlog application's backend API endpoints.

**Base URL**: `/api`

---

## Table of Contents

1.  [Authentication](#authentication)
2.  [Users](#users)
3.  [Posts](#posts)
4.  [Comments](#comments)
5.  [Relationships](#relationships)

---

## 1. Authentication

Endpoints for user registration and login.

### Register User

- **Description**: Registers a new user.
- **Method**: `POST`
- **Endpoint**: `/api/auth/register`
- **Permissions**: Public
- **Request Body**:
  ```json
  {
    "username": "string",
    "password": "string",
    "nickname": "string",
    "gender": "string (e.g., MALE, FEMALE, OTHER)",
    "profileimg": "string (URL, optional)",
    "hobby": ["string", "(optional)"]
  }
  ```
- **Response**: `UserResponse` object.

### Login User

- **Description**: Authenticates a user and returns a JWT token.
- **Method**: `POST`
- **Endpoint**: `/api/auth/login`
- **Permissions**: Public
- **Request Body**:
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **Response**: `LoginResponse` object containing the token and user data.

---

## 2. Users

Endpoints for managing user profiles.

### Get User by ID

- **Description**: Retrieves a user's public profile.
- **Method**: `GET`
- **Endpoint**: `/api/users/{userId}`
- **Permissions**: Authenticated
- **Response**: `UserResponse` object.

### Search User by Username

- **Description**: Searches for a user by their exact username.
- **Method**: `GET`
- **Endpoint**: `/api/users/search?username={username}`
- **Permissions**: Authenticated
- **Response**: `UserResponse` object.

### Update User Profile

- **Description**: Updates the current authenticated user's profile information.
- **Method**: `PUT`
- **Endpoint**: `/api/users/{userId}`
- **Permissions**: Owner
- **Request Body**:
  ```json
  {
    "nickname": "string (optional)",
    "gender": "string (optional)",
    "profileimg": "string (URL, optional)",
    "hobby": ["string", "(optional)"]
  }
  ```
- **Response**: Updated `UserResponse` object.

### Change Password

- **Description**: Changes the current authenticated user's password.
- **Method**: `PUT`
- **Endpoint**: `/api/users/{userId}/password`
- **Permissions**: Owner
- **Request Body**:
  ```json
  {
    "oldPassword": "string",
    "newPassword": "string"
  }
  ```
- **Response**: Success message.

---

## 3. Posts

Endpoints for creating and managing posts. All endpoints require authentication.

### Get Posts

- **Description**: Retrieves a paginated list of posts visible to the current user (self and partner).
- **Method**: `GET`
- **Endpoint**: `/api/posts?page={page_number}&size={page_size}`
- **Permissions**: Authenticated
- **Response**: `PageResponse<PostResponse>` object.

### Create Post

- **Description**: Creates a new post.
- **Method**: `POST`
- **Endpoint**: `/api/posts`
- **Permissions**: Authenticated
- **Request Body**:
  ```json
  {
    "richContent": {
      "type": "rich_text",
      "version": "1.0",
      "delta": {},
      "plainText": "string"
    }
  }
  ```
- **Response**: `PostResponse` object of the newly created post.

### Get Post by ID

- **Description**: Retrieves a single post by its ID.
- **Method**: `GET`
- **Endpoint**: `/api/posts/{postId}`
- **Permissions**: Authenticated (must be self or partner)
- **Response**: `PostResponse` object.

### Update Post

- **Description**: Updates an existing post.
- **Method**: `PUT`
- **Endpoint**: `/api/posts/{postId}`
- **Permissions**: Owner
- **Request Body**:
  ```json
  {
    "richContent": {
      "type": "rich_text",
      "version": "1.0",
      "delta": {},
      "plainText": "string"
    }
  }
  ```
- **Response**: Updated `PostResponse` object.

### Delete Post

- **Description**: Deletes a post.
- **Method**: `DELETE`
- **Endpoint**: `/api/posts/{postId}`
- **Permissions**: Owner
- **Response**: Success message.

---

## 4. Comments

Endpoints for managing comments on posts. All endpoints require authentication.

### Get Comments for a Post

- **Description**: Retrieves a paginated list of comments for a specific post.
- **Method**: `GET`
- **Endpoint**: `/api/posts/{postId}/comments?page={page_number}&size={page_size}`
- **Permissions**: Authenticated (must have access to the post)
- **Response**: `PageResponse<CommentResponse>` object.

### Create Comment

- **Description**: Adds a new comment to a post.
- **Method**: `POST`
- **Endpoint**: `/api/posts/{postId}/comments`
- **Permissions**: Authenticated (must have access to the post)
- **Request Body**:
  ```json
  {
    "richContent": {
      "type": "rich_text",
      "version": "1.0",
      "delta": {},
      "plainText": "string"
    }
  }
  ```
- **Response**: `CommentResponse` object of the newly created comment.

### Get Comments by Batch

- **Description**: Retrieves a list of comments by their IDs.
- **Method**: `POST`
- **Endpoint**: `/api/comments/batch`
- **Permissions**: Authenticated
- **Request Body**:
  ```json
  {
    "commentIds": [1, 2, 3]
  }
  ```
- **Response**: `List<CommentResponse>`.

### Delete Comment

- **Description**: Deletes a comment.
- **Method**: `DELETE`
- **Endpoint**: `/api/comments/{commentId}`
- **Permissions**: Owner
- **Response**: Success message.

---

## 5. Relationships

Endpoints for managing user pairing and relationships. All endpoints require authentication.

### Send Pair Request

- **Description**: Sends a pair request to another user by their username.
- **Method**: `POST`
- **Endpoint**: `/api/relationships/pair-request`
- **Permissions**: Authenticated
- **Request Body**:
  ```json
  {
    "partnerUsername": "string"
  }
  ```
- **Response**: Success message.

### Accept Pair Request

- **Description**: Accepts a pending pair request.
- **Method**: `POST`
- **Endpoint**: `/api/relationships/accept-request`
- **Permissions**: Authenticated (user who received the request)
- **Request Body**:
  ```json
  {
    "requestUserId": "number"
  }
  ```
- **Response**: `UserRelationship` object.

### Reject Pair Request

- **Description**: Rejects a pending pair request.
- **Method**: `POST`
- **Endpoint**: `/api/relationships/{userId}/reject-pair`
- **Permissions**: Authenticated (user who received the request)
- **Request Body**:
  ```json
  {
    "requestUserId": "number"
  }
  ```
- **Response**: Success message.

### Unpair (End Relationship)

- **Description**: Ends the current active relationship.
- **Method**: `DELETE`
- **Endpoint**: `/api/relationships/{userId}/unpair`
- **Permissions**: Owner
- **Response**: Success message.

### Get Pending Requests

- **Description**: Gets a list of pair requests received by the user.
- **Method**: `GET`
- **Endpoint**: `/api/relationships/{userId}/pending-requests`
- **Permissions**: Owner
- **Response**: `List<RelationshipHistoryResponse>`.

### Get Sent Requests

- **Description**: Gets a list of pair requests sent by the user.
- **Method**: `GET`
- **Endpoint**: `/api/relationships/{userId}/sent-requests`
- **Permissions**: Owner
- **Response**: `List<RelationshipHistoryResponse>`.

### Get Relationship History

- **Description**: Gets the user's relationship history.
- **Method**: `GET`
- **Endpoint**: `/api/relationships/{userId}/relationship-history`
- **Permissions**: Owner
- **Response**: `List<RelationshipHistoryResponse>`.
