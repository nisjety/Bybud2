// DeliveryList.jsx
import React, { useMemo } from "react";
import CustomerDeliveryList from "./CustomerDeliveryList";
import CourierDeliveryList from "./CourierDeliveryList";

/**
 * This component acts as a router to show the appropriate delivery list
 * based on the user's role.
 */
const DeliveryList = () => {
    // Possibly you want to read roles from userData instead of 'roles' key
    const userData = JSON.parse(localStorage.getItem("userData") || "{}");
    const userRoles = userData.roles || []; // roles might be ["CUSTOMER"] or ["COURIER"]
    const isCourier = userRoles.includes("COURIER");

    return isCourier ? <CourierDeliveryList /> : <CustomerDeliveryList />;
};

export default DeliveryList;
