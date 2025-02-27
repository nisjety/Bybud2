import { DeliveryAPI } from "./APIUtility";

/**
 * Create a new delivery (Customer or Admin).
 */
export const createDelivery = async (deliveryData) => {
    const response = await DeliveryAPI.post("/", deliveryData, {
        headers: { "Content-Type": "application/json" },
    });
    return response.data.data;
};

/**
 * Get deliveries for a specific customer (customer or admin).
 */
export const getDeliveriesForCustomer = async (customerId) => {
    const response = await DeliveryAPI.get(`/customer/${customerId}`);
    return response.data.data;
};

/**
 * Get deliveries for a specific courier (courier or admin).
 */
export const getDeliveriesForCourier = async (courierId) => {
    const response = await DeliveryAPI.get(`/courier/${courierId}`);
    return response.data.data;
};

/**
 * Get all deliveries (courier or admin).
 */
export const getAllDeliveries = async () => {
    const response = await DeliveryAPI.get("/");
    return response.data.data;
};

/**
 * Accept a delivery (courier only).
 */
export const acceptDelivery = async (deliveryId) => {
    const response = await DeliveryAPI.put(`/${deliveryId}/accept`);
    return response.data.data;
};

/**
 * Update a delivery's status (courier, customer, or admin).
 */
export const updateDeliveryStatus = async (deliveryId, status) => {
    const response = await DeliveryAPI.put(`/${deliveryId}/status`, null, {
        params: { status },
    });
    return response.data.data;
};

/**
 * Cancel a delivery (customer or admin).
 */
export const cancelDelivery = async (deliveryId) => {
    return await updateDeliveryStatus(deliveryId, "CANCELED");
};
