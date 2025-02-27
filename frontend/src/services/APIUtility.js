import axios from "axios";

// Points to your API Gateway
const GATEWAY_BASE_URL = "http://localhost:8080";

// Derive full endpoints
const AUTH_BASE_URL = `${GATEWAY_BASE_URL}/api/auth`;
const DELIVERY_BASE_URL = `${GATEWAY_BASE_URL}/api/delivery`;
const USER_BASE_URL = `${GATEWAY_BASE_URL}/api/users`;

// Create an Axios instance that automatically attaches tokens
const createAPI = (baseURL) => {
    const apiInstance = axios.create({ baseURL });

    // Request interceptor: attach JWT if present in localStorage
    apiInstance.interceptors.request.use(
        (config) => {
            const userData = localStorage.getItem("userData");
            if (userData) {
                const token = JSON.parse(userData).accessToken;
                if (token) {
                    config.headers.Authorization = `Bearer ${token}`;
                }
            }
            return config;
        },
        (error) => Promise.reject(error)
    );

    // Optional: Global error handler
    apiInstance.interceptors.response.use(
        (response) => response,
        (error) => {
            console.error("API Error:", error?.response?.data || error.message);
            return Promise.reject(error);
        }
    );

    return apiInstance;
};

export const AuthAPI = createAPI(AUTH_BASE_URL);
export const DeliveryAPI = createAPI(DELIVERY_BASE_URL);
export const UserAPI = createAPI(USER_BASE_URL);
