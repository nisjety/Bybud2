import React, { useEffect, useState } from "react";
import {
    acceptDelivery,
    getAllDeliveries,
    getDeliveriesForCourier,
    updateDeliveryStatus,
} from "../services/deliveryService";
import { getUserById } from "../services/userService";
import { toast } from "react-toastify";

const CourierPage = () => {
    const [courierData, setCourierData] = useState(null);
    const [deliveries, setDeliveries] = useState([]);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);
    const [selectedStatus, setSelectedStatus] = useState({});

    // Grab user info from localStorage
    const userData = JSON.parse(localStorage.getItem("userData") || "{}");
    const dbUserId = userData.userId;        // DB ID of the courier (e.g. "67bf7d...")
    const courierUsername = userData.username; // Username of the courier (e.g. "newuser10")

    // Helper to format the date from [year,month,day,hour,minute,second,nanos] arrays
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

                // 1) Fetch the courier's user profile (via DB ID):
                const userDataResp = await getUserById(dbUserId);
                setCourierData(userDataResp);

                // 2) Decide which deliveries to show. For a Courier, `getAllDeliveries` is allowed,
                //    but you may prefer only the deliveries assigned to them:
                const all = (await getAllDeliveries()) ?? [];
                setDeliveries(all);

                // Alternatively, if you only want the courier's assigned deliveries:
                // const assigned = (await getDeliveriesForCourier(courierUsername)) ?? [];
                // setDeliveries(assigned);

                setError(null);
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

    // Accept a delivery (status goes from CREATED -> ASSIGNED)
    const handleAcceptDelivery = async (deliveryId) => {
        try {
            await acceptDelivery(deliveryId);
            toast.success("Delivery accepted!");

            // Refresh deliveries
            const refreshed = (await getAllDeliveries()) ?? [];
            setDeliveries(refreshed);
        } catch (err) {
            console.error("Failed to accept delivery:", err);
            setError(
                err.response?.data?.message || err.message || "Failed to accept delivery."
            );
        }
    };

    // Set a new status in local state
    const handleStatusChange = (deliveryId, newStatus) => {
        setSelectedStatus((prev) => ({ ...prev, [deliveryId]: newStatus }));
    };

    // Actually update status on the server
    const handleUpdateStatus = async (deliveryId) => {
        try {
            const newStatus = selectedStatus[deliveryId];
            if (!newStatus) return;

            await updateDeliveryStatus(deliveryId, newStatus);
            toast.success("Status updated!");

            // Refresh deliveries
            const refreshed = (await getAllDeliveries()) ?? [];
            setDeliveries(refreshed);
        } catch (err) {
            console.error("Failed to update status:", err);
            setError(
                err.response?.data?.message || err.message || "Failed to update status."
            );
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
                <h3>All Deliveries (or assigned deliveries)</h3>
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
                                <strong>Created:</strong>{" "}
                                {delivery.createdDate ? formatDate(delivery.createdDate) : "Unknown"} <br />
                                {delivery.deliveryDate && (
                                    <>
                                        <strong>Scheduled:</strong> {formatDate(delivery.deliveryDate)} <br />
                                    </>
                                )}
                                {delivery.customerName && (
                                    <>
                                        <strong>Customer:</strong> {delivery.customerName} <br />
                                    </>
                                )}

                                {/* Accept button if it's "CREATED" */}
                                {delivery.status === "CREATED" && (
                                    <button onClick={() => handleAcceptDelivery(delivery.id)}>
                                        Accept Delivery
                                    </button>
                                )}

                                {/* Courier can update status if it's not COMPLETED */}
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
