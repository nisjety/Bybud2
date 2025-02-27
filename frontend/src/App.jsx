// App.jsx
import React, { lazy, Suspense, useEffect, useState } from "react";
import {
    BrowserRouter as Router,
    Navigate,
    Route,
    Routes
} from "react-router-dom";
import Navbar from "./components/Navbar";
import Loader from "./components/Loader";
import ProtectedRoute from "./components/ProtectedRoute";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import "./App.css";

// Lazy imports
const HomePage = lazy(() => import("./pages/HomePage"));
const Login = lazy(() => import("./pages/Login"));
const Register = lazy(() => import("./pages/Register"));
const CustomerDeliveryList = lazy(() => import("./pages/CustomerDeliveryList"));
const CourierDeliveryList = lazy(() => import("./pages/CourierDeliveryList"));
const CreateDelivery = lazy(() => import("./pages/CreateDelivery"));
const UserProfile = lazy(() => import("./pages/UserProfile"));
const CourierPage = lazy(() => import("./pages/CourierPage"));

const App = () => {
    const [authenticated, setAuthenticated] = useState(false);
    const [roles, setRoles] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const checkAuthState = () => {
            setLoading(true);
            try {
                const userData = JSON.parse(localStorage.getItem("userData") || "{}");
                const accessToken = userData.accessToken;

                if (accessToken && Array.isArray(userData.roles)) {
                    setRoles(userData.roles);
                    setAuthenticated(true);
                } else {
                    setRoles([]);
                    setAuthenticated(false);
                }
            } catch (err) {
                console.error("Error parsing user data:", err);
                setRoles([]);
                setAuthenticated(false);
            } finally {
                setLoading(false);
            }
        };

        // Initial check
        checkAuthState();

        // Listen for localStorage changes from other tabs
        const handleStorageChange = (e) => {
            if (e.key === "userData" || e.key === null) {
                checkAuthState();
            }
        };
        window.addEventListener("storage", handleStorageChange);

        return () => {
            window.removeEventListener("storage", handleStorageChange);
        };
    }, []);

    // Logout logic
    const onLogout = () => {
        localStorage.removeItem("userData");
        localStorage.removeItem("roles");
        localStorage.removeItem("userId");
        setAuthenticated(false);
        setRoles([]);
    };

    const isCourier = roles.includes("COURIER");
    const isCustomer = roles.includes("CUSTOMER");

    if (loading) {
        return <Loader message="Initializing application..." />;
    }

    return (
        <Router>
            <Navbar authenticated={authenticated} onLogout={onLogout} roles={roles} />
            <main className="main-content">
                <Suspense fallback={<Loader />}>
                    <Routes>
                        <Route
                            path="/"
                            element={
                                authenticated ? (
                                    isCourier ? (
                                        <Navigate to="/deliveries" replace />
                                    ) : (
                                        <Navigate to="/profile" replace />
                                    )
                                ) : (
                                    <HomePage />
                                )
                            }
                        />

                        {/* Pass setAuthenticated and setRoles to the Login component! */}
                        <Route
                            path="/login"
                            element={
                                authenticated ? (
                                    <Navigate to="/" replace />
                                ) : (
                                    <Login
                                        setAuthenticated={setAuthenticated}
                                        setRoles={setRoles}
                                    />
                                )
                            }
                        />

                        <Route
                            path="/register"
                            element={
                                authenticated ? <Navigate to="/" replace /> : <Register />
                            }
                        />

                        <Route path="/home" element={<HomePage />} />

                        <Route
                            path="/delivery"
                            element={
                                <ProtectedRoute authenticated={authenticated} allowedRoles={["CUSTOMER"]}>
                                    <CustomerDeliveryList />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/delivery/create"
                            element={
                                <ProtectedRoute authenticated={authenticated} allowedRoles={["CUSTOMER"]}>
                                    <CreateDelivery />
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/deliveries"
                            element={
                                <ProtectedRoute authenticated={authenticated} allowedRoles={["COURIER"]}>
                                    <CourierDeliveryList />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/courier"
                            element={
                                <ProtectedRoute authenticated={authenticated} allowedRoles={["COURIER"]}>
                                    <CourierPage />
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/profile"
                            element={
                                <ProtectedRoute authenticated={authenticated} allowedRoles={["CUSTOMER", "COURIER"]}>
                                    <UserProfile />
                                </ProtectedRoute>
                            }
                        />

                        <Route path="*" element={<Navigate to="/" replace />} />
                    </Routes>
                </Suspense>
            </main>

            <ToastContainer
                position="top-right"
                autoClose={3000}
                hideProgressBar={false}
                newestOnTop
                closeOnClick
                rtl={false}
                pauseOnFocusLoss
                draggable
                pauseOnHover
                theme="colored"
            />
        </Router>
    );
};

export default App;
