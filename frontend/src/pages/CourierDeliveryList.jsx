import React, { useEffect, useState, useMemo } from "react";
import {
    getAllDeliveries,
    acceptDelivery,
    updateDeliveryStatus,
    getDeliveriesForCourier
} from "../services/deliveryService";
import { toast } from "react-toastify";

const CourierDeliveryList = () => {
    const [availableDeliveries, setAvailableDeliveries] = useState([]);
    const [myDeliveries, setMyDeliveries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedStatus, setSelectedStatus] = useState({});
    const [activeTab, setActiveTab] = useState("available");

    // Retrieve user data from localStorage
    const userData = useMemo(
        () => JSON.parse(localStorage.getItem("userData") || "{}"),
        []
    );

    /**
     * Your DeliveryController uses hasPermissionForCourier(userId, courierId)
     * which compares the JWT subject (userId) to courierId OR checks if isAdmin.
     * If your JWT sub = "newuser10", then courierId must be "newuser10" as well.
     */
    const courierId = userData.username; // e.g. "newuser10"

    // Helper: format date from [year, month, day, hour, minute, second, nanos]
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
                    throw new Error("User data not found or missing username. Please log in again.");
                }

                // 1) Fetch every delivery the courier is allowed to see via GET /api/delivery
                //    (since it's annotated with @PreAuthorize("hasAnyRole('COURIER','ADMIN')")).
                const allDeliveries = (await getAllDeliveries()) ?? [];

                // 2) Fetch deliveries specifically assigned to this courier (GET /api/delivery/courier/{courierId})
                const courierDeliveries = (await getDeliveriesForCourier(courierId)) ?? [];

                // 3) Filter out the unassigned or "CREATED" deliveries as "available"
                const availableOnes = allDeliveries.filter(
                    (delivery) => delivery.status === "CREATED"
                );

                setAvailableDeliveries(availableOnes);
                setMyDeliveries(courierDeliveries);
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

    /**
     * Accept a delivery -> calls PUT /api/delivery/{deliveryId}/accept
     * This changes its status from CREATED to ASSIGNED (or ACCEPTED),
     * and sets courierId in the backend from the JWT subject.
     */
    const handleAcceptDelivery = async (deliveryId) => {
        try {
            if (!courierId) {
                throw new Error("Courier ID not found. Please log in again.");
            }

            await acceptDelivery(deliveryId);
            toast.success("Delivery accepted successfully!");

            // After accepting, refetch to update both sets
            const allDeliveries = (await getAllDeliveries()) ?? [];
            const courierDeliveries = (await getDeliveriesForCourier(courierId)) ?? [];

            const availableOnes = allDeliveries.filter((d) => d.status === "CREATED");

            setAvailableDeliveries(availableOnes);
            setMyDeliveries(courierDeliveries);

            // Switch tab to "My Deliveries"
            setActiveTab("my");
        } catch (err) {
            console.error("Failed to accept delivery:", err);
            toast.error(
                err.response?.data?.message || err.message || "Failed to accept delivery"
            );
        }
    };

    // Update local state with the chosen new status
    const handleStatusChange = (deliveryId, newStatus) => {
        setSelectedStatus((prev) => ({ ...prev, [deliveryId]: newStatus }));
    };

    // Actually call the backend to update the status
    const handleUpdateStatus = async (deliveryId) => {
        try {
            const newStatus = selectedStatus[deliveryId];
            if (!newStatus) return;

            await updateDeliveryStatus(deliveryId, newStatus);
            toast.success("Delivery status updated successfully!");

            // Refresh only "my deliveries" after status change
            const courierDeliveries = (await getDeliveriesForCourier(courierId)) ?? [];
            setMyDeliveries(courierDeliveries);
        } catch (err) {
            console.error("Failed to update status:", err);
            toast.error(
                err.response?.data?.message || err.message || "Failed to update status"
            );
        }
    };

    /**
     * "Unassign" a delivery by setting status back to "CREATED"
     * if your system allows that. This effectively frees it up for other couriers.
     */
    const handleCancelAssignment = async (deliveryId) => {
        try {
            await updateDeliveryStatus(deliveryId, "CREATED");
            toast.success("Delivery unassigned successfully!");

            // Refresh both
            const allDeliveries = (await getAllDeliveries()) ?? [];
            const courierDeliveries = (await getDeliveriesForCourier(courierId)) ?? [];

            const availableOnes = allDeliveries.filter((d) => d.status === "CREATED");

            setAvailableDeliveries(availableOnes);
            setMyDeliveries(courierDeliveries);
        } catch (err) {
            console.error("Failed to unassign delivery:", err);
            toast.error(
                err.response?.data?.message || err.message || "Failed to unassign delivery"
            );
        }
    };

    // Possible statuses for a courier
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

            {/* Available (unassigned) deliveries */}
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
                                        <p><strong>Details:</strong> {delivery.deliveryDetails || "N/A"}</p>
                                        <p><strong>Pickup:</strong> {delivery.pickupAddress || "N/A"}</p>
                                        <p><strong>Destination:</strong> {delivery.deliveryAddress || "N/A"}</p>
                                        <p>
                                            <strong>Created:</strong>{" "}
                                            {delivery.createdDate ? formatDate(delivery.createdDate) : "Unknown"}
                                        </p>
                                        {delivery.deliveryDate && (
                                            <p>
                                                <strong>Scheduled:</strong> {formatDate(delivery.deliveryDate)}
                                            </p>
                                        )}
                                        <p><strong>Customer:</strong> {delivery.customerName || "Unknown"}</p>
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

            {/* Deliveries assigned to this courier */}
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
                                        <p><strong>Details:</strong> {delivery.deliveryDetails || "N/A"}</p>
                                        <p><strong>Pickup:</strong> {delivery.pickupAddress || "N/A"}</p>
                                        <p><strong>Destination:</strong> {delivery.deliveryAddress || "N/A"}</p>
                                        <p>
                                            <strong>Created:</strong>{" "}
                                            {delivery.createdDate ? formatDate(delivery.createdDate) : "Unknown"}
                                        </p>
                                        {delivery.deliveryDate && (
                                            <p>
                                                <strong>Scheduled:</strong> {formatDate(delivery.deliveryDate)}
                                            </p>
                                        )}
                                        <p><strong>Customer:</strong> {delivery.customerName || "Unknown"}</p>
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

                                                {/* Optionally let couriers unassign themselves by reverting to CREATED */}
                                                <button
                                                    className="button button-secondary"
                                                    onClick={() => handleCancelAssignment(delivery.id)}
                                                >
                                                    Unassign Delivery
                                                </button>
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
