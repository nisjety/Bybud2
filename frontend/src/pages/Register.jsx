import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";
// Ensure this import matches your actual file name (e.g., "userService.js")
import { createUser } from "../services/userService";

const Register = () => {
    const [formData, setFormData] = useState({
        username: "",
        email: "",
        password: "",
        confirmPassword: "",
        fullName: "",
        dateOfBirth: "",
        phoneNumber: "",
        role: "CUSTOMER", // Default role
    });
    const [error, setError] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
        setError(null); // Clear errors if any
    };

    const validateForm = () => {
        // 1) Password match
        if (formData.password !== formData.confirmPassword) {
            setError("Passwords do not match");
            return false;
        }

        if (formData.password.length < 6) {
            setError("Password must be at least 6 characters long");
            return false;
        }

        // 2) Basic email validation
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(formData.email)) {
            setError("Please enter a valid email address");
            return false;
        }

        // 3) Phone validation (basic)
        const phoneDigits = formData.phoneNumber.replace(/[^0-9]/g, "");
        if (!/^\d{8,15}$/.test(phoneDigits)) {
            setError("Please enter a valid phone number");
            return false;
        }

        return true;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        // Validate
        if (!validateForm()) {
            return;
        }

        setIsLoading(true);

        try {
            // Convert dateOfBirth to [YYYY, MM, DD]
            const dob = new Date(formData.dateOfBirth);
            const dateArray = [dob.getFullYear(), dob.getMonth() + 1, dob.getDate()];

            // Build payload
            const payload = {
                username: formData.username,
                email: formData.email,
                password: formData.password,
                fullName: formData.fullName,
                dateOfBirth: dateArray,
                phoneNumber: formData.phoneNumber,
                roles: [formData.role], // ["CUSTOMER"] or ["COURIER"]
            };

            // Call the user service
            await createUser(payload);

            toast.success("Registration successful! Please log in.");
            navigate("/login");
        } catch (err) {
            const errorMessage =
                err.response?.data?.message ||
                "Registration failed. Please try again.";
            setError(errorMessage);
            toast.error(errorMessage);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="register-container fade-in">
            <div className="card register-card">
                <h2>Create an Account</h2>
                {error && <div className="error-message">{error}</div>}

                <form onSubmit={handleSubmit} className="register-form">
                    <div className="form-group">
                        <label htmlFor="username">Username</label>
                        <input
                            id="username"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            placeholder="Choose a username"
                            required
                            className="form-control"
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="email">Email</label>
                        <input
                            id="email"
                            name="email"
                            type="email"
                            value={formData.email}
                            onChange={handleChange}
                            placeholder="Your email address"
                            required
                            className="form-control"
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="password">Password</label>
                        <input
                            id="password"
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            placeholder="Create a password"
                            required
                            className="form-control"
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="confirmPassword">Confirm Password</label>
                        <input
                            id="confirmPassword"
                            type="password"
                            name="confirmPassword"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            placeholder="Confirm your password"
                            required
                            className="form-control"
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="fullName">Full Name</label>
                        <input
                            id="fullName"
                            name="fullName"
                            value={formData.fullName}
                            onChange={handleChange}
                            placeholder="Your full name"
                            required
                            className="form-control"
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="dateOfBirth">Date of Birth</label>
                        <input
                            id="dateOfBirth"
                            name="dateOfBirth"
                            type="date"
                            value={formData.dateOfBirth}
                            onChange={handleChange}
                            required
                            className="form-control"
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="phoneNumber">Phone Number</label>
                        <input
                            id="phoneNumber"
                            name="phoneNumber"
                            value={formData.phoneNumber}
                            onChange={handleChange}
                            placeholder="Your phone number"
                            required
                            className="form-control"
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="role">Account Type</label>
                        <select
                            id="role"
                            name="role"
                            value={formData.role}
                            onChange={handleChange}
                            className="form-control"
                        >
                            <option value="CUSTOMER">Customer</option>
                            <option value="COURIER">Courier</option>
                        </select>
                    </div>

                    <button
                        type="submit"
                        className="button button-primary"
                        disabled={isLoading}
                    >
                        {isLoading ? "Creating Account..." : "Register"}
                    </button>
                </form>

                <div className="register-footer">
                    <p>
                        Already have an account? <Link to="/login">Login here</Link>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default Register;
