import React, { useEffect, useState, useMemo } from "react";
import { getDeliveriesForCustomer, updateDeliveryStatus } from "../services/deliveryService";
import { toast } from "react-toastify";

const CustomerDeliveryList = () => {
    const [deliveries, setDeliveries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // userData from localStorage
    const userData = useMemo(
        () => JSON.parse(localStorage.getItem("userData") || "{}"),
        []
    );
    const username = userData.username;

    // Format date array or string
    const formatDate = (dateData) => {
        if (!dateData) return "N/A";
        if (Array.isArray(dateData) && dateData.length >= 3) {
            const [year, month, day, hour = 0, minute = 0, second = 0, nanosecond = 0] = dateData;
            const ms = Math.floor(nanosecond / 1000000);
            const date = new Date(year, month - 1, day, hour, minute, second, ms);
            return date.toLocaleString();
        }
        return dateData;
    };

    useEffect(() => {
        const fetchDeliveries = async () => {
            try {
                setLoading(true);
                if (!username) {
                    throw new Error("User data not found. Please log in again.");
                }

                const data = await getDeliveriesForCustomer(username);
                setDeliveries(data || []);
                setError(null);
            } catch (err) {
                console.error("Failed to fetch deliveries:", err);
                setError(err.response?.data?.message || err.message || "Failed to load deliveries");
                toast.error("Failed to load deliveries");
            } finally {
                setLoading(false);
            }
        };

        fetchDeliveries();
    }, [username]);

    const handleCancelDelivery = async (deliveryId) => {
        try {
            // Update status to CANCELED
            await updateDeliveryStatus(deliveryId, "CANCELED");
            toast.success("Delivery canceled successfully!");

            // Refresh the list
            const updatedDeliveries = await getDeliveriesForCustomer(username);
            setDeliveries(updatedDeliveries || []);
        } catch (err) {
            console.error("Failed to cancel delivery:", err);
            toast.error(
                err.response?.data?.message ||
                err.message ||
                "Failed to cancel delivery"
            );
        }
    };

    if (loading) return <div className="loading">Loading your deliveries...</div>;
    if (error) return <div className="error-message">{error}</div>;

    return (
        <div className="delivery-list-container fade-in">
            <h2>Your Deliveries</h2>
            {deliveries.length === 0 ? (
                <div className="no-deliveries">
                    <p>You haven't created any deliveries yet.</p>
                    <button
                        className="button button-primary"
                        onClick={() => (window.location.href = "/delivery/create")}
                    >
                        Create a Delivery
                    </button>
                </div>
            ) : (
                <div className="deliveries-grid">
                    {deliveries.map((delivery) => (
                        <div key={delivery.id} className="delivery-card">
                            <div className="delivery-header">
                                <h3>Delivery #{delivery.id.substring(0, 8)}</h3>
                                <span className={`status-badge status-${delivery.status?.toLowerCase()}`}>
                  {delivery.status}
                </span>
                            </div>
                            <div className="delivery-details">
                                <p><strong>Details:</strong> {delivery.deliveryDetails || "N/A"}</p>
                                <p><strong>Pickup Address:</strong> {delivery.pickupAddress || "N/A"}</p>
                                <p><strong>Delivery Address:</strong> {delivery.deliveryAddress || "N/A"}</p>
                                <p>
                                    <strong>Created:</strong>{" "}
                                    {delivery.createdDate ? formatDate(delivery.createdDate) : "Unknown"}
                                </p>
                                {delivery.deliveryDate && (
                                    <p>
                                        <strong>Scheduled:</strong> {formatDate(delivery.deliveryDate)}
                                    </p>
                                )}
                                {delivery.courierId && (
                                    <p><strong>Courier:</strong> {delivery.courierUsername || "Unknown"}</p>
                                )}
                            </div>
                            <div className="delivery-actions">
                                {delivery.status !== "COMPLETED" && delivery.status !== "CANCELED" && (
                                    <button
                                        className="button button-danger"
                                        onClick={() => handleCancelDelivery(delivery.id)}
                                    >
                                        Cancel Delivery
                                    </button>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default CustomerDeliveryList;
