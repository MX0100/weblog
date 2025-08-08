import React from "react";
import { Link, useLocation } from "react-router-dom";
import "./PublicNavbar.css";

const PublicNavbar: React.FC = () => {
  const { hash, pathname } = useLocation();
  const isLoginPage = pathname === "/login";

  return (
    <nav className="landing-navbar">
      <div className="container">
        <div className="nav-content">
          <div className="brand">
            <Link to="/" className="brand-link">
              WeBlog
            </Link>
          </div>
          <ul className="nav-links">
            <li>
              <a
                href="/#home"
                className={hash === "#home" ? "active" : undefined}
              >
                Home
              </a>
            </li>
            <li>
              <a
                href="/#how"
                className={hash === "#how" ? "active" : undefined}
              >
                How it works
              </a>
            </li>
            <li>
              <a
                href="/#contact"
                className={hash === "#contact" ? "active" : undefined}
              >
                Contact
              </a>
            </li>
          </ul>
          {!isLoginPage && (
            <div className="nav-actions">
              <Link to="/login" className="btn btn-dark">
                Sign In
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};

export default PublicNavbar;
