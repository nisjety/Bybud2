import React, { useState } from "react";
import PropTypes from "prop-types";
import { Link, useNavigate } from "react-router-dom";

const Navbar = ({ authenticated, onLogout, roles = [] }) => {
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const navigate = useNavigate();

    // Role checks
    const isCourier = roles.includes("COURIER");
    const isCustomer = roles.includes("CUSTOMER");

    const handleLogout = () => {
        onLogout();
        // After clearing localStorage, we navigate to the login screen
        navigate("/login", { replace: true });
    };

    const toggleMobileMenu = () => {
        setMobileMenuOpen(!mobileMenuOpen);
    };

    const closeMenu = () => {
        setMobileMenuOpen(false);
    };

    return (
        <nav className="navbar">
            <div className="navbar-container">
                {/* Logo or brand link */}
                <Link to="/home" className="navbar-logo" aria-label="Go to Home">
                    ByBud
                </Link>

                {/* Mobile toggle button */}
                <button
                    className="mobile-menu-toggle"
                    onClick={toggleMobileMenu}
                    aria-label={mobileMenuOpen ? "Close menu" : "Open menu"}
                    aria-expanded={mobileMenuOpen}
                >
                    <span></span>
                    <span></span>
                    <span></span>
                </button>

                {/* Navbar links, shown or hidden based on mobileMenuOpen */}
                <div className={`navbar-links ${mobileMenuOpen ? "active" : ""}`}>
                    {authenticated ? (
                        <>
                            {/* Always show Home link if authenticated */}
                            <Link to="/home" onClick={closeMenu} aria-label="Home">
                                Home
                            </Link>

                            {/* Common link for all logged-in users: Profile */}
                            <Link to="/profile" onClick={closeMenu} aria-label="Profile">
                                Profile
                            </Link>

                            {/* Courier-specific link */}
                            {isCourier && (
                                <Link
                                    to="/deliveries"
                                    onClick={closeMenu}
                                    aria-label="Manage Deliveries"
                                >
                                    Manage Deliveries
                                </Link>
                            )}

                            {/* Customer-specific links */}
                            {isCustomer && (
                                <>
                                    <Link to="/delivery" onClick={closeMenu} aria-label="My Deliveries">
                                        My Deliveries
                                    </Link>
                                    <Link
                                        to="/delivery/create"
                                        onClick={closeMenu}
                                        aria-label="Create Delivery"
                                    >
                                        Create Delivery
                                    </Link>
                                </>
                            )}

                            {/* Logout button */}
                            <button className="btn-logout" onClick={handleLogout} aria-label="Logout">
                                Logout
                            </button>
                        </>
                    ) : (
                        <>
                            {/* If not authenticated, show Login/Register */}
                            <Link to="/login" onClick={closeMenu} aria-label="Login">
                                Login
                            </Link>
                            <Link to="/register" onClick={closeMenu} aria-label="Register">
                                Register
                            </Link>
                        </>
                    )}
                </div>
            </div>
        </nav>
    );
};

Navbar.propTypes = {
    authenticated: PropTypes.bool.isRequired,
    onLogout: PropTypes.func.isRequired,
    roles: PropTypes.array,
};

export default Navbar;
