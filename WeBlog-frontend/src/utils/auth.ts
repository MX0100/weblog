import type { User } from "../types/api";

// Local storage key names
export const STORAGE_KEYS = {
  TOKEN: "token",
  USER: "user",
} as const;

// Save token to local storage
export const saveToken = (token: string): void => {
  localStorage.setItem(STORAGE_KEYS.TOKEN, token);
};

// Get token
export const getToken = (): string | null => {
  return localStorage.getItem(STORAGE_KEYS.TOKEN);
};

// Remove token
export const removeToken = (): void => {
  localStorage.removeItem(STORAGE_KEYS.TOKEN);
};

// Save user info to local storage
export const saveUser = (user: User): void => {
  localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
};

// Get user info
export const getUser = (): User | null => {
  const userStr = localStorage.getItem(STORAGE_KEYS.USER);
  if (userStr) {
    try {
      return JSON.parse(userStr);
    } catch (error) {
      // Silently handle parse errors
      return null;
    }
  }
  return null;
};

// Remove user info
export const removeUser = (): void => {
  localStorage.removeItem(STORAGE_KEYS.USER);
};

// Check if user is authenticated
export const isAuthenticated = (): boolean => {
  return !!(getToken() && getUser());
};

// Clear all authentication info
export const clearAuth = (): void => {
  removeToken();
  removeUser();
};

// Login
export const login = (token: string, user: User): void => {
  saveToken(token);
  saveUser(user);
};

// Logout
export const logout = (): void => {
  clearAuth();
  // Add other logout logic here, such as clearing other caches
};
