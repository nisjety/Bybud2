import { DeliveryAPI } from "./APIUtility";

/**
 * Create a new delivery (Accessible by CUSTOMER or ADMIN).
 * POST /api/delivery
 */
export const createDelivery = async (deliveryData) => {
    // Example deliveryData:
    // {
    //   customerId: "someCustomerId",
    //   pickupAddress: "123 Main St",
    //   deliveryAddress: "456 Destination Ave",
    //   deliveryDate: "2025-03-10",
    //   deliveryDetails: "Package details..."
    // }
    const response = await DeliveryAPI.post("", deliveryData, {
        headers: { "Content-Type": "application/json" },
    });
    return response.data.data;
};

/**
 * Get deliveries for a specific customer (Accessible by CUSTOMER [their own] or ADMIN).
 * GET /api/delivery/customer/{customerId}
 */
export const getDeliveriesForCustomer = async (customerId) => {
    const response = await DeliveryAPI.get(`/customer/${customerId}`);
    return response.data.data;
};

/**
 * Get all deliveries (Accessible by COURIER or ADMIN).
 * GET /api/delivery
 */
export const getAllDeliveries = async () => {
    const response = await DeliveryAPI.get("");
    return response.data.data;
};

/**
 * Get deliveries for a specific courier (Accessible by COURIER [their own] or ADMIN).
 * GET /api/delivery/courier/{courierId}
 */
export const getDeliveriesForCourier = async (courierId) => {
    const response = await DeliveryAPI.get(`/courier/${courierId}`);
    return response.data.data;
};

/**
 * Accept a delivery (Accessible by COURIER only).
 * PUT /api/delivery/{deliveryId}/accept
 */
export const acceptDelivery = async (deliveryId) => {
    const response = await DeliveryAPI.put(`/${deliveryId}/accept`);
    return response.data.data;
};

/**
 * Update a delivery's status (Accessible by COURIER, CUSTOMER, or ADMIN).
 * PUT /api/delivery/{deliveryId}/status?status={status}
 *
 * statuses include:
 *  - "CREATED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CANCELED"
 */
export const updateDeliveryStatus = async (deliveryId, status) => {
    const response = await DeliveryAPI.put(`/${deliveryId}/status`, null, {
        params: { status },
    });
    return response.data.data;
};

/**
 * Cancel a delivery (Accessible by CUSTOMER [their own] or ADMIN).
 * Effectively sets the delivery status to "CANCELED".
 */
export const cancelDelivery = async (deliveryId) => {
    return await updateDeliveryStatus(deliveryId, "CANCELED");
};
