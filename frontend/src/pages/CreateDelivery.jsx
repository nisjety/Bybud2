import React, { useState } from "react";
import { createDelivery } from "../services/deliveryService";

const CreateDelivery = () => {
    const [formData, setFormData] = useState({
        deliveryDetails: "",
        pickupAddress: "",
        deliveryAddress: "",
        deliveryDate: "",
    });
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(false);

    const handleChange = (e) => {
        setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
        setError(null);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            // Retrieve user data
            const userDataStr = localStorage.getItem("userData");
            if (!userDataStr) throw new Error("User data not found. Please log in again.");
            const userData = JSON.parse(userDataStr);
            const username = userData.username;
            if (!username) throw new Error("Username not found in user data.");

            const payload = {
                // The backend enforces that only a logged-in CUSTOMER or ADMIN can create
                customerId: username,
                deliveryDetails: formData.deliveryDetails,
                pickupAddress: formData.pickupAddress,
                deliveryAddress: formData.deliveryAddress,
                // The backend may store or ignore this date
                deliveryDate: formData.deliveryDate,
            };

            await createDelivery(payload);

            // If successful:
            setSuccess(true);
            setFormData({
                deliveryDetails: "",
                pickupAddress: "",
                deliveryAddress: "",
                deliveryDate: "",
            });
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.message ||
                "Failed to create delivery."
            );
            setSuccess(false);
        }
    };

    return (
        <div>
            <h2>Create Delivery</h2>
            {success && <p style={{ color: "green" }}>Delivery created successfully!</p>}
            {error && <p style={{ color: "red" }}>{error}</p>}

            <form onSubmit={handleSubmit}>
                <label>
                    Delivery Details:
                    <textarea
                        name="deliveryDetails"
                        value={formData.deliveryDetails}
                        onChange={handleChange}
                        placeholder="Enter delivery details"
                        required
                    />
                </label>
                <br />
                <label>
                    Pickup Address:
                    <input
                        type="text"
                        name="pickupAddress"
                        value={formData.pickupAddress}
                        onChange={handleChange}
                        placeholder="Enter pickup address"
                        required
                    />
                </label>
                <br />
                <label>
                    Delivery Address:
                    <input
                        type="text"
                        name="deliveryAddress"
                        value={formData.deliveryAddress}
                        onChange={handleChange}
                        placeholder="Enter delivery address"
                        required
                    />
                </label>
                <br />
                <label>
                    Delivery Date:
                    <input
                        type="date"
                        name="deliveryDate"
                        value={formData.deliveryDate}
                        onChange={handleChange}
                        required
                    />
                </label>
                <br />
                <button type="submit">Create</button>
            </form>
        </div>
    );
};

export default CreateDelivery;
