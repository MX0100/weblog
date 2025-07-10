import React from "react";
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Navigate,
} from "react-router-dom";
import { isAuthenticated } from "./utils/auth";
import Login from "./components/Auth/Login/Login";
import Register from "./components/Auth/Register/Register";
import Home from "./components/Home/Home";
import "./styles/global.css";

// Private route component
const PrivateRoute: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  return isAuthenticated() ? <>{children}</> : <Navigate to="/login" replace />;
};

// No need for PublicRoute component - login/register should always be accessible

function App() {
  return (
    <Router>
      <div className="App">
        <Routes>
          {/* Public routes - always accessible */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Default route - always redirect to login */}
          <Route path="/" element={<Navigate to="/login" replace />} />

          {/* Private routes */}
          <Route
            path="/home"
            element={
              <PrivateRoute>
                <Home />
              </PrivateRoute>
            }
          />

          {/* Redirect to home */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
