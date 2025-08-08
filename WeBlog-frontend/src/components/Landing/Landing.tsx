import React from "react";
import { Link } from "react-router-dom";
import "./Landing.css";
import PublicNavbar from "../common/PublicNavbar/PublicNavbar";

// Navbar moved to common/PublicNavbar

const Landing: React.FC = () => {
  return (
    <div className="landing-page">
      <PublicNavbar />

      {/* Hero */}
      <section id="home" className="hero">
        <div className="container hero-inner">
          <h1 className="headline">
            <span className="muted">The</span> #1 Place To{" "}
            <span className="highlight">Share</span> & Discover
          </h1>
          <h2 className="subhead">
            Publish posts, connect with others, and get real-time updates. Most
            users start sharing in minutes.
          </h2>

          <div className="cta-group">
            <Link to="/register" className="btn btn-primary btn-lg">
              Join For Free
            </Link>
            <a href="#how" className="btn btn-ghost">
              Learn more
            </a>
          </div>
        </div>
      </section>

      {/* How it works */}
      <section id="how" className="how">
        <div className="container">
          <h3>How it works</h3>
          <div className="steps">
            <div className="step">
              <div className="step-title">1. Create an account</div>
              <p>Register and set up your profile in seconds.</p>
            </div>
            <div className="step">
              <div className="step-title">2. Write and post</div>
              <p>
                Use the rich text editor to create engaging posts with images
                and formatting.
              </p>
            </div>
            <div className="step">
              <div className="step-title">3. Interact in real-time</div>
              <p>
                Receive notifications, comments, and build relationships
                instantly.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Contact */}
      <section id="contact" className="contact">
        <div className="container">
          <h3>Contact</h3>
          <p>
            For inquiries, reach out via email:{" "}
            <a href="mailto:contact@example.com">lucaswang0402@gmail.com</a>
          </p>
          <p>
            Or connect via GitHub:{" "}
            <a
              href="https://github.com/mx0100"
              target="_blank"
              rel="noreferrer"
            >
              github.com/mx0100
            </a>
          </p>
        </div>
      </section>

      <footer className="footer">
        <div className="container">
          <span>© {new Date().getFullYear()} WeBlog</span>
        </div>
      </footer>
    </div>
  );
};

export default Landing;
