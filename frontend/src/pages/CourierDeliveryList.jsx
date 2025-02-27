import React, { useEffect, useState, useMemo } from "react";
import {
    getAllDeliveries,
    acceptDelivery,
    updateDeliveryStatus,
    getDeliveriesForCourier,
} from "../services/deliveryService";
import { toast } from "react-toastify";

const CourierDeliveryList = () => {
    const [availableDeliveries, setAvailableDeliveries] = useState([]);
    const [myDeliveries, setMyDeliveries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedStatus, setSelectedStatus] = useState({});
    const [activeTab, setActiveTab] = useState("available");

    /**
     * IMPORTANT: Decide whether your backend wants the "username" or the DB "userId".
     * Your DeliveryController has:
     *   @GetMapping("/courier/{courierId}")
     *     -> hasPermissionForCourier(userId, courierId)
     *     -> checks if userId.equals(courierId) or isAdmin
     * If your JWT sets sub = "newuser10" (i.e. the username),
     * then "userId" in your localStorage must be that same string for the check to pass.
     *
     * If your localStorage actually has userData.username = "newuser10"
     * and userData.userId = "67bf7d323e998b6d7fa4924b" (the DB ID),
     * you need to pass the username, not the DB ID.
     */

    const userData = useMemo(() => JSON.parse(localStorage.getItem("userData") || "{}"), []);
    // If your backend wants the courier's "username":
    const courierId = userData.username;  // recommended if the backend checks userId == "newuser10"
    // If your backend truly uses the DB id, then:
    // const courierId = userData.userId; // only do this if the backend also uses the DB id in the token

    // Helper to format date
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
        const fetchDeliveries = async () => {
            try {
                setLoading(true);
                if (!courierId) {
                    throw new Error("User data not found. Please log in again.");
                }

                // 1) Fetch all deliveries (courier or admin).
                const allDeliveries = await getAllDeliveries();

                // 2) Fetch deliveries specifically for this courier
                //    If your backend wants the "username", pass that:
                const courierDeliveries = await getDeliveriesForCourier(courierId);

                // 3) Filter out the unassigned (status == "CREATED") as "available"
                const availableOnes = allDeliveries.filter(
                    (delivery) => delivery.status === "CREATED"
                );

                setAvailableDeliveries(availableOnes || []);
                setMyDeliveries(courierDeliveries || []);
                setError(null);
            } catch (err) {
                console.error("Failed to fetch deliveries:", err);
                setError(
                    err.response?.data?.message ||
                    err.message ||
                    "Failed to load deliveries"
                );
                toast.error("Failed to load deliveries");
            } finally {
                setLoading(false);
            }
        };

        fetchDeliveries();
    }, [courierId]);

    const handleAcceptDelivery = async (deliveryId) => {
        try {
            if (!courierId) throw new Error("Courier ID not found. Please log in again.");

            // The backend picks up the courier from the JWT token, so no param needed
            await acceptDelivery(deliveryId);
            toast.success("Delivery accepted successfully!");

            // Re-fetch after accept
            const allDeliveries = await getAllDeliveries();
            const courierDeliveries = await getDeliveriesForCourier(courierId);

            const availableOnes = allDeliveries.filter(
                (delivery) => delivery.status === "CREATED"
            );

            setAvailableDeliveries(availableOnes || []);
            setMyDeliveries(courierDeliveries || []);

            // Switch to "My Deliveries"
            setActiveTab("my");
        } catch (err) {
            console.error("Failed to accept delivery:", err);
            toast.error(
                err.response?.data?.message || err.message || "Failed to accept delivery"
            );
        }
    };

    const handleStatusChange = (deliveryId, newStatus) => {
        setSelectedStatus((prev) => ({ ...prev, [deliveryId]: newStatus }));
    };

    const handleUpdateStatus = async (deliveryId) => {
        try {
            if (!courierId) throw new Error("Courier ID not found. Please log in again.");

            const newStatus = selectedStatus[deliveryId];
            if (!newStatus) return;

            await updateDeliveryStatus(deliveryId, newStatus);
            toast.success("Delivery status updated successfully!");

            // Refresh the courier’s deliveries
            const courierDeliveries = await getDeliveriesForCourier(courierId);
            setMyDeliveries(courierDeliveries || []);
        } catch (err) {
            console.error("Failed to update delivery status:", err);
            toast.error(
                err.response?.data?.message || err.message || "Failed to update status"
            );
        }
    };

    const handleCancelAssignment = async (deliveryId) => {
        try {
            if (!courierId) throw new Error("Courier ID not found. Please log in again.");

            // Set status back to CREATED => unassigns it
            await updateDeliveryStatus(deliveryId, "CREATED");
            toast.success("Delivery unassigned successfully!");

            // Refresh both lists
            const allDeliveries = await getAllDeliveries();
            const courierDeliveries = await getDeliveriesForCourier(courierId);
            const availableOnes = allDeliveries.filter(
                (delivery) => delivery.status === "CREATED"
            );

            setAvailableDeliveries(availableOnes || []);
            setMyDeliveries(courierDeliveries || []);
        } catch (err) {
            console.error("Failed to unassign delivery:", err);
            toast.error(
                err.response?.data?.message ||
                err.message ||
                "Failed to unassign delivery"
            );
        }
    };

    // Possible statuses a courier might set
    const statusOptions = ["ASSIGNED", "IN_PROGRESS", "COMPLETED"];

    if (loading) return <div className="loading">Loading deliveries...</div>;
    if (error) return <div className="error-message">{error}</div>;

    return (
        <div className="courier-delivery-list-container fade-in">
            <h2>Courier Dashboard</h2>

            <div className="tabs">
                <button
                    className={`tab-button ${activeTab === "available" ? "active" : ""}`}
                    onClick={() => setActiveTab("available")}
                >
                    Available Deliveries ({availableDeliveries.length})
                </button>
                <button
                    className={`tab-button ${activeTab === "my" ? "active" : ""}`}
                    onClick={() => setActiveTab("my")}
                >
                    My Deliveries ({myDeliveries.length})
                </button>
            </div>

            {activeTab === "available" && (
                <>
                    {availableDeliveries.length === 0 ? (
                        <p className="no-deliveries">
                            No deliveries available to accept right now.
                        </p>
                    ) : (
                        <div className="deliveries-grid">
                            {availableDeliveries.map((delivery) => (
                                <div key={delivery.id} className="delivery-card">
                                    <div className="delivery-header">
                                        <h3>Delivery #{delivery.id.substring(0, 8)}</h3>
                                        <span className="status-badge status-created">
                      {delivery.status}
                    </span>
                                    </div>
                                    <div className="delivery-details">
                                        <p>
                                            <strong>Details:</strong> {delivery.deliveryDetails || "N/A"}
                                        </p>
                                        <p>
                                            <strong>Pickup Address:</strong>{" "}
                                            {delivery.pickupAddress || "N/A"}
                                        </p>
                                        <p>
                                            <strong>Delivery Address:</strong>{" "}
                                            {delivery.deliveryAddress || "N/A"}
                                        </p>
                                        <p>
                                            <strong>Created:</strong>{" "}
                                            {delivery.createdDate ? formatDate(delivery.createdDate) : "Unknown"}
                                        </p>
                                        {delivery.deliveryDate && (
                                            <p>
                                                <strong>Scheduled:</strong> {formatDate(delivery.deliveryDate)}
                                            </p>
                                        )}
                                        <p>
                                            <strong>Customer:</strong> {delivery.customerName || "Unknown"}
                                        </p>
                                    </div>
                                    <div className="delivery-actions">
                                        <button
                                            className="button button-primary"
                                            onClick={() => handleAcceptDelivery(delivery.id)}
                                        >
                                            Accept Delivery
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </>
            )}

            {activeTab === "my" && (
                <>
                    {myDeliveries.length === 0 ? (
                        <div className="no-deliveries">
                            <p>You haven't accepted any deliveries yet.</p>
                            <button
                                className="button button-secondary"
                                onClick={() => setActiveTab("available")}
                            >
                                View Available Deliveries
                            </button>
                        </div>
                    ) : (
                        <div className="deliveries-grid">
                            {myDeliveries.map((delivery) => (
                                <div key={delivery.id} className="delivery-card">
                                    <div className="delivery-header">
                                        <h3>Delivery #{delivery.id.substring(0, 8)}</h3>
                                        <span
                                            className={`status-badge status-${delivery.status.toLowerCase()}`}
                                        >
                      {delivery.status}
                    </span>
                                    </div>
                                    <div className="delivery-details">
                                        <p>
                                            <strong>Details:</strong>{" "}
                                            {delivery.deliveryDetails || "N/A"}
                                        </p>
                                        <p>
                                            <strong>Pickup Address:</strong>{" "}
                                            {delivery.pickupAddress || "N/A"}
                                        </p>
                                        <p>
                                            <strong>Delivery Address:</strong>{" "}
                                            {delivery.deliveryAddress || "N/A"}
                                        </p>
                                        <p>
                                            <strong>Created:</strong>{" "}
                                            {delivery.createdDate ? formatDate(delivery.createdDate) : "Unknown"}
                                        </p>
                                        {delivery.deliveryDate && (
                                            <p>
                                                <strong>Scheduled:</strong>{" "}
                                                {formatDate(delivery.deliveryDate)}
                                            </p>
                                        )}
                                        <p>
                                            <strong>Customer:</strong> {delivery.customerName || "Unknown"}
                                        </p>
                                    </div>
                                    <div className="delivery-actions">
                                        {delivery.status !== "COMPLETED" && (
                                            <>
                                                <div className="status-update">
                                                    <select
                                                        value={selectedStatus[delivery.id] || delivery.status}
                                                        onChange={(e) =>
                                                            handleStatusChange(delivery.id, e.target.value)
                                                        }
                                                    >
                                                        {statusOptions.map((status) => (
                                                            <option key={status} value={status}>
                                                                {status.replace("_", " ")}
                                                            </option>
                                                        ))}
                                                    </select>
                                                    <button
                                                        className="button update-button"
                                                        onClick={() => handleUpdateStatus(delivery.id)}
                                                    >
                                                        Update Status
                                                    </button>
                                                </div>
                                                {delivery.status !== "COMPLETED" && (
                                                    <button
                                                        className="button button-secondary"
                                                        onClick={() => handleCancelAssignment(delivery.id)}
                                                    >
                                                        Unassign Delivery
                                                    </button>
                                                )}
                                            </>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default CourierDeliveryList;
