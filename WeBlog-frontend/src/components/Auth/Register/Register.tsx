import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { userAPI } from "../../../services/api";
import type { RegisterRequest, Gender } from "../../../types/api";
import "./Register.css";

// Gender options configuration
const GENDER_OPTIONS: { value: Gender; label: string }[] = [
  { value: "MALE", label: "Male" },
  { value: "FEMALE", label: "Female" },
  { value: "NON_BINARY", label: "Non-Binary" },
  { value: "TRANSGENDER_MALE", label: "Trans Male" },
  { value: "TRANSGENDER_FEMALE", label: "Trans Female" },
  { value: "GENDERFLUID", label: "Genderfluid" },
  { value: "AGENDER", label: "Agender" },
  { value: "OTHER", label: "Other" },
  { value: "PREFER_NOT_TO_SAY", label: "Prefer not to say" },
];

const Register: React.FC = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState<RegisterRequest>({
    username: "",
    password: "",
    nickname: "",
    gender: "PREFER_NOT_TO_SAY",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>("");
  const [success, setSuccess] = useState<string>("");

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    setError(""); // Clear error message
    setSuccess(""); // Clear success message
  };

  const handleGenderChange = (gender: Gender) => {
    setFormData((prev) => ({
      ...prev,
      gender,
    }));
    setError("");
    setSuccess("");
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    setSuccess("");

    try {
      const response = await userAPI.register(formData);
      if (response.code === 200) {
        // Registration successful, show success message and delay redirect to login page
        setSuccess(
          "Registration successful! Redirecting to login page in 3 seconds..."
        );
        setTimeout(() => {
          navigate("/login");
        }, 3000);
      } else {
        setError(response.message || "Registration failed");
      }
    } catch (error: any) {
      setError(
        error.response?.data?.message ||
          "Registration failed, please check your network connection"
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2 className="auth-title">Join WeBlog</h2>
        <p className="auth-subtitle">Create your account</p>

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="username" className="form-label">
              Username
            </label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              className="form-input"
              placeholder="3-20 characters, letters, numbers, underscore only"
              pattern="^[a-zA-Z0-9_]{3,20}$"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="nickname" className="form-label">
              Nickname
            </label>
            <input
              type="text"
              id="nickname"
              name="nickname"
              value={formData.nickname}
              onChange={handleChange}
              className="form-input"
              placeholder="Enter your nickname"
              maxLength={50}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password" className="form-label">
              Password
            </label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              className="form-input"
              placeholder="6-50 characters"
              minLength={6}
              maxLength={50}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Gender</label>
            <div className="gender-options">
              {GENDER_OPTIONS.map((option) => (
                <div
                  key={option.value}
                  className={`gender-option ${
                    formData.gender === option.value ? "selected" : ""
                  }`}
                  onClick={() => handleGenderChange(option.value)}
                >
                  <input
                    type="radio"
                    name="gender"
                    value={option.value}
                    checked={formData.gender === option.value}
                    onChange={() => handleGenderChange(option.value)}
                  />
                  <label>{option.label}</label>
                </div>
              ))}
            </div>
          </div>

          {error && <div className="error-message">{error}</div>}
          {success && (
            <div className="success-message">
              {success}{" "}
              <Link to="/login" className="auth-link">
                Go now
              </Link>
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary auth-submit-btn"
            disabled={loading}
          >
            {loading ? (
              <span className="loading-text">
                <span className="spinner"></span>
                Registering...
              </span>
            ) : (
              "Register"
            )}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Already have an account?{" "}
            <Link to="/login" className="auth-link">
              Sign in now
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Register;
