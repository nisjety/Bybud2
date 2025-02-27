import { AuthAPI } from "./APIUtility";

/**
 * Logs in the user by calling the gateway /login endpoint.
 * @param {string} usernameOrEmail
 * @param {string} password
 * @returns {object} { status, message, data: { accessToken, refreshToken, userId, username, roles } }
 */
export const login = async (usernameOrEmail, password) => {
    // The gateway expects a POST /api/auth/login with a LoginRequest body
    const response = await AuthAPI.post("/login", {
        usernameOrEmail,
        password,
    });
    // The server returns BaseResponse<JwtResponse>
    // e.g. { status: "SUCCESS", message: "Login successful.", data: { ...JwtResponse } }
    return response.data;
};

/**
 * Refresh the user's tokens by calling /refresh
 * @param {string} refreshToken
 * @returns {object} { status, message, data: { accessToken, refreshToken, ... } }
 */
export const refreshToken = async (refreshToken) => {
    // POST /api/auth/refresh?refreshToken=<...>
    const response = await AuthAPI.post(`/refresh?refreshToken=${refreshToken}`);
    return response.data;
};

/**
 * Fetch user details by usernameOrEmail via the gateway
 * @param {string} usernameOrEmail
 * @returns {object} { status, message, data: UserDTO }
 */
export const getUserDetails = async (usernameOrEmail) => {
    // GET /api/auth/user?usernameOrEmail=<...>
    const response = await AuthAPI.get("/user", {
        params: { usernameOrEmail },
    });
    return response.data;
};

/**
 * Log out the user by invalidating the tokens in the gateway
 * The gateway needs:
 *  - Header: Authorization: Bearer <accessToken>
 *  - Query param: ?refreshToken=<refreshToken>
 */
export const logout = async () => {
    // We need both the accessToken in the header
    // and the refreshToken in query param
    const localData = JSON.parse(localStorage.getItem("userData") || "{}");
    const accessToken = localData.accessToken;
    const refreshToken = localData.refreshToken;

    if (!accessToken || !refreshToken) {
        // You can still remove localStorage and treat it as a "logical" logout
        throw new Error("No tokens found; user might already be logged out.");
    }

    const config = {
        headers: { Authorization: `Bearer ${accessToken}` },
        params: { refreshToken },
    };

    // POST /api/auth/logout
    const response = await AuthAPI.post("/logout", null, config);
    return response.data;
};

/**
 * Optionally, invalidate a token in the gateway (for admin or security usage).
 * @param {string} reason
 */
export const invalidateToken = async (reason = "Security measure") => {
    const localData = JSON.parse(localStorage.getItem("userData") || "{}");
    const accessToken = localData.accessToken;
    if (!accessToken) {
        throw new Error("No token found.");
    }

    // e.g. POST /api/auth/invalidate?reason=<...>
    // with Bearer <accessToken>
    const config = {
        headers: { Authorization: `Bearer ${accessToken}` },
        params: { reason },
    };
    const response = await AuthAPI.post("/invalidate", null, config);
    return response.data;
};
