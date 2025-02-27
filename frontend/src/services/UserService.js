import { UserAPI } from "./APIUtility";

/**
 * Register a new user (public).
 */
export const createUser = async (createUserDTO) => {
    const response = await UserAPI.post("/register", createUserDTO, {
        headers: { "Content-Type": "application/json" },
    });
    return response.data; // { status, message, data: { ...UserDTO } }
};

/**
 * Get user by ID (admin, or same user).
 */
export const getUserById = async (userId) => {
    const response = await UserAPI.get(`/${userId}`);
    return formatUserDates(response.data.data);
};

/**
 * Get user details by username or email (admin, or same user).
 */
export const getUserDetailsByUsernameOrEmail = async (usernameOrEmail) => {
    const response = await UserAPI.get("/details", {
        params: { usernameOrEmail },
    });
    return response.data;
};

/**
 * Get all users (admin).
 */
export const getAllUsers = async () => {
    const response = await UserAPI.get("/admin/all");
    return response.data;
};

/**
 * Update user profile (admin, or same user).
 */
export const updateUserProfile = async (id, updateData) => {
    const response = await UserAPI.put(`/${id}`, updateData, {
        headers: { "Content-Type": "application/json" },
    });
    return response.data;
};

/** Helper to format date arrays into a user-friendly string */
const formatUserDates = (user) => {
    if (!user) return null;
    return {
        ...user,
        dateOfBirth: convertDateArray(user.dateOfBirth),
    };
};

const convertDateArray = (dateArray) => {
    if (!Array.isArray(dateArray) || dateArray.length < 3) return "Invalid Date";
    const [year, month, day] = dateArray;
    return new Date(year, month - 1, day).toLocaleDateString();
};
