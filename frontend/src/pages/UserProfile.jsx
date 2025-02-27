import React, { useEffect, useState } from "react";
import { getDeliveriesForCustomer } from "../services/deliveryService";
import { getUserDetailsByUsernameOrEmail } from "../services/userService";

const UserProfile = () => {
    const [user, setUser] = useState(null);
    const [deliveries, setDeliveries] = useState([]);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);

    // Retrieve user data from localStorage
    const userData = JSON.parse(localStorage.getItem("userData") || "{}");
    const username = userData.username; // used as the customerId + for user details

    // Helper to format date arrays:
    const formatDate = (dateData) => {
        if (!dateData) return "N/A";
        // If the backend returns [year, month, day, hour, minute, second, nanos]
        if (Array.isArray(dateData) && dateData.length >= 3) {
            const [year, month, day, hour = 0, minute = 0, second = 0, nanosecond = 0] = dateData;
            const ms = Math.floor(nanosecond / 1000000);
            const date = new Date(year, month - 1, day, hour, minute, second, ms);
            return date.toLocaleString();
        }
        // fallback if it's just a string
        return dateData;
    };

    useEffect(() => {
        const fetchUserAndDeliveries = async () => {
            try {
                if (!username) {
                    throw new Error("Username not found. Please log in again.");
                }

                // 1) Fetch user details
                const userResponse = await getUserDetailsByUsernameOrEmail(username);
                setUser(userResponse.data);

                // 2) Fetch deliveries for that customer
                const deliveriesData = await getDeliveriesForCustomer(username);
                setDeliveries(deliveriesData || []);
            } catch (err) {
                setError(
                    err.response?.data?.message ||
                    err.message ||
                    "Failed to fetch your profile data."
                );
            } finally {
                setLoading(false);
            }
        };

        fetchUserAndDeliveries();
    }, [username]);

    if (loading) return <p>Loading your profile...</p>;
    if (error) return <p style={{ color: "red" }}>{error}</p>;

    return (
        <div>
            <h2>User Profile</h2>

            {user ? (
                <div style={{ marginBottom: "20px" }}>
                    <p>
                        <strong>Full Name:</strong> {user.fullName || "N/A"}
                    </p>
                    <p>
                        <strong>Email:</strong> {user.email || "N/A"}
                    </p>
                    <p>
                        <strong>Phone Number:</strong> {user.phoneNumber || "N/A"}
                    </p>
                    <p>
                        <strong>Active:</strong> {user.active ? "Yes" : "No"}
                    </p>
                    <p>
                        <strong>Date of Birth:</strong>{" "}
                        {user.dateOfBirth ? formatDate(user.dateOfBirth) : "N/A"}
                    </p>
                    <p>
                        <strong>Roles:</strong> {user.roles ? user.roles.join(", ") : "None"}
                    </p>
                </div>
            ) : (
                <p>User details not found.</p>
            )}

            <h3>Your Deliveries</h3>
            {deliveries.length === 0 ? (
                <p>No deliveries found.</p>
            ) : (
                <ul>
                    {deliveries.map((delivery) => (
                        <li key={delivery.id} style={{ marginBottom: "15px" }}>
                            <strong>Details:</strong> {delivery.deliveryDetails || "N/A"} <br />
                            <strong>Pickup Address:</strong> {delivery.pickupAddress || "N/A"} <br />
                            <strong>Delivery Address:</strong> {delivery.deliveryAddress || "N/A"} <br />
                            <strong>Status:</strong> {delivery.status || "Unknown"} <br />
                            <strong>Created:</strong>{" "}
                            {delivery.createdDate
                                ? formatDate(delivery.createdDate)
                                : "Unknown"}
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

export default UserProfile;
