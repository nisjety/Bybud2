// Login.jsx
import React, { useState } from "react";
import PropTypes from "prop-types";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";
import { login } from "../services/authService";

const Login = ({ setAuthenticated, setRoles }) => {
    const [identifier, setIdentifier] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setError(null);

        try {
            // login() => { status, message, data: { accessToken, refreshToken, userId, username, roles } }
            const result = await login(identifier, password);
            const loginData = result.data;

            if (loginData) {
                // Store in localStorage
                localStorage.setItem("userData", JSON.stringify(loginData));
                // (Optionally) also store "roles" and "userId" separately if needed
                localStorage.setItem("roles", JSON.stringify(loginData.roles));
                localStorage.setItem("userId", loginData.userId);

                // Immediately update App-level state so the navbar sees the new roles
                setAuthenticated(true);
                setRoles(loginData.roles);

                toast.success("Login successful!");

                // Redirect based on roles
                if (loginData.roles.includes("COURIER")) {
                    navigate("/deliveries");
                } else if (loginData.roles.includes("CUSTOMER")) {
                    navigate("/profile");
                } else {
                    navigate("/home");
                }
            }
        } catch (err) {
            const errorMessage =
                err.response?.data?.message ||
                "Login failed. Please check your credentials.";
            setError(errorMessage);
            toast.error(errorMessage);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="login-container fade-in">
            <div className="card login-card">
                <h2>Login</h2>
                {error && <div className="error-message">{error}</div>}

                <form onSubmit={handleSubmit} className="login-form">
                    <div className="form-group">
                        <label htmlFor="identifier">Username or Email</label>
                        <input
                            id="identifier"
                            type="text"
                            value={identifier}
                            onChange={(e) => setIdentifier(e.target.value)}
                            required
                            className="form-control"
                            placeholder="Enter your username or email"
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="password">Password</label>
                        <input
                            id="password"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            className="form-control"
                            placeholder="Enter your password"
                        />
                    </div>
                    <button
                        type="submit"
                        className="button button-primary"
                        disabled={isLoading}
                    >
                        {isLoading ? "Logging in..." : "Login"}
                    </button>
                </form>

                <div className="login-footer">
                    <p>
                        Don't have an account? <Link to="/register">Register here</Link>
                    </p>
                </div>
            </div>
        </div>
    );
};

Login.propTypes = {
    setAuthenticated: PropTypes.func.isRequired,
    setRoles: PropTypes.func.isRequired,
};

export default Login;
