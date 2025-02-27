import { Navigate } from "react-router-dom";
import PropTypes from "prop-types";
import { useEffect, useState } from "react";

/**
 * Enhanced ProtectedRoute that verifies:
 * 1. The user is authenticated
 * 2. The user has the required roles
 */
const ProtectedRoute = ({ children, authenticated, allowedRoles }) => {
    const [hasAccess, setHasAccess] = useState(false);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let userRoles = [];
        try {
            const userData = JSON.parse(localStorage.getItem("userData") || "{}");
            userRoles = userData.roles || [];
        } catch (err) {
            console.error("Error parsing user roles:", err);
        }

        // Check if any of the userRoles match the allowedRoles
        const hasAllowedRole = userRoles.some((role) => allowedRoles.includes(role));
        setHasAccess(authenticated && hasAllowedRole);
        setLoading(false);
    }, [authenticated, allowedRoles]);

    if (loading) {
        return <div className="loading">Checking access...</div>;
    }

    // If not authenticated, go to /login
    if (!authenticated) {
        return <Navigate to="/login" replace />;
    }

    // If user is authenticated but does not have the required roles, go home
    if (!hasAccess) {
        return <Navigate to="/" replace />;
    }

    // Otherwise, render the child component
    return children;
};

ProtectedRoute.propTypes = {
    children: PropTypes.node.isRequired,
    authenticated: PropTypes.bool.isRequired,
    allowedRoles: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default ProtectedRoute;
