import React, { useEffect, useState, useMemo } from "react";
import {
    acceptDelivery,
    getAllDeliveries,
    getDeliveriesForCourier,
    updateDeliveryStatus
} from "../services/deliveryService";
import { getUserById } from "../services/userService";
import { toast } from "react-toastify";

const CourierPage = () => {
    const [courierData, setCourierData] = useState(null);
    const [deliveries, setDeliveries] = useState([]);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);
    const [selectedStatus, setSelectedStatus] = useState({});

    // Decide if you want DB ID or username. If your backend
    // identifies the user by username, you might do:
    //   const { username, userId } from userData
    const userData = JSON.parse(localStorage.getItem("userData") || "{}");
    const dbUserId = userData.userId;     // e.g. "67bf7d323e998b6d7fa4924b"
    const courierUsername = userData.username; // e.g. "newuser10"

    // Or if your backend is expecting the username in the token for a "courierId":
    // We'll assume you actually want to show all deliveries for this user (courier).
    // We also might fetch the courier's profile from `getUserById(dbUserId)` if the DB ID is needed.

    const formatDate = (dateData) => {
        if (!dateData) return "N/A";
        if (Array.isArray(dateData) && dateData.length >= 3) {
            const [year, month, day, hour = 0, minute = 0, second = 0, nanos = 0] = dateData;
            const ms = Math.floor(nanos / 1_000_000);
            const date = new Date(year, month - 1, day, hour, minute, second, ms);
            return date.toLocaleString();
        }
        return dateData;
    };

    useEffect(() => {
        const fetchCourierDataAndDeliveries = async () => {
            try {
                if (!dbUserId) {
                    throw new Error("Courier DB userId not found in localStorage.");
                }

                // 1) Optionally fetch the courier's user profile by DB ID
                const userDataResp = await getUserById(dbUserId);
                setCourierData(userDataResp);

                // 2) Now decide which deliveries to show:
                //    a) If you want *all* deliveries visible to a courier:
                const all = await getAllDeliveries();
                setDeliveries(all);

                //    b) Or if you only want deliveries specifically assigned to them:
                // const assigned = await getDeliveriesForCourier(courierUsername);
                // setDeliveries(assigned);

            } catch (err) {
                console.error("Failed to fetch courier data:", err);
                setError(
                    err.response?.data?.message ||
                    err.message ||
                    "Failed to fetch courier data."
                );
            } finally {
                setLoading(false);
            }
        };

        fetchCourierDataAndDeliveries();
    }, [dbUserId]);

    // Accept a delivery
    const handleAcceptDelivery = async (deliveryId) => {
        try {
            await acceptDelivery(deliveryId);
            toast.success("Delivery accepted!");

            // refresh
            const refreshed = await getAllDeliveries();
            setDeliveries(refreshed);
        } catch (err) {
            setError(err.response?.data?.message || err.message || "Failed to accept delivery.");
        }
    };

    // For changing status
    const handleStatusChange = (deliveryId, newStatus) => {
        setSelectedStatus((prev) => ({ ...prev, [deliveryId]: newStatus }));
    };

    const handleUpdateStatus = async (deliveryId) => {
        try {
            const newStatus = selectedStatus[deliveryId];
            if (!newStatus) return;

            await updateDeliveryStatus(deliveryId, newStatus);
            toast.success("Status updated!");

            // refresh
            const refreshed = await getAllDeliveries();
            setDeliveries(refreshed);
        } catch (err) {
            setError(err.response?.data?.message || err.message || "Failed to update status.");
        }
    };

    const statusOptions = ["CREATED", "ASSIGNED", "IN_PROGRESS", "COMPLETED"];

    if (loading) return <p>Loading courier data...</p>;
    if (error) return <p style={{ color: "red" }}>{error}</p>;

    return (
        <div>
            <h2>Courier Dashboard</h2>
            {courierData && (
                <div>
                    <h3>Your Info</h3>
                    <p><strong>Full Name:</strong> {courierData.fullName}</p>
                    <p><strong>Username:</strong> {courierData.username}</p>
                    <p><strong>Email:</strong> {courierData.email}</p>
                    <p><strong>Phone:</strong> {courierData.phoneNumber}</p>
                </div>
            )}

            <div>
                <h3>All Deliveries</h3>
                {deliveries.length === 0 ? (
                    <p>No deliveries available.</p>
                ) : (
                    <ul>
                        {deliveries.map((delivery) => (
                            <li key={delivery.id}>
                                <strong>Details:</strong> {delivery.deliveryDetails || "N/A"} <br />
                                <strong>Pickup Address:</strong> {delivery.pickupAddress || "N/A"} <br />
                                <strong>Delivery Address:</strong> {delivery.deliveryAddress || "N/A"} <br />
                                <strong>Status:</strong> {delivery.status || "Unknown"} <br />
                                <strong>Created:</strong> {formatDate(delivery.createdDate)} <br />

                                {/* Accept button if it's "CREATED" */}
                                {delivery.status === "CREATED" && (
                                    <button onClick={() => handleAcceptDelivery(delivery.id)}>
                                        Accept Delivery
                                    </button>
                                )}

                                {/* Let the courier update status if not COMPLETED */}
                                {delivery.status !== "COMPLETED" && (
                                    <div style={{ marginTop: "10px" }}>
                                        <label>
                                            Update Status:
                                            <select
                                                value={selectedStatus[delivery.id] || delivery.status}
                                                onChange={(e) => handleStatusChange(delivery.id, e.target.value)}
                                            >
                                                {statusOptions.map((status) => (
                                                    <option key={status} value={status}>
                                                        {status.replace("_", " ")}
                                                    </option>
                                                ))}
                                            </select>
                                        </label>
                                        <button onClick={() => handleUpdateStatus(delivery.id)}>
                                            Update
                                        </button>
                                    </div>
                                )}
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
};

export default CourierPage;
