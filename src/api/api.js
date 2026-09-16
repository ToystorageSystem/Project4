import axios from "axios";

const api = axios.create({
    baseURL: "/api",
    withCredentials: true,
});

api.interceptors.response.use(
    (response) => response,

    (error) => {
        const status = error.response?.status;
        const data = error.response?.data;

        const message =
            typeof data === "string"
                ? data
                : data?.message;

        // Chưa đăng nhập / session hết hạn
        if (
            status === 401 ||
            message === "NOT_LOGIN" ||
            message === "NOT_AUTHENTICATED"
        ) {
            // Tránh redirect liên tục nếu đã ở login
            if (window.location.pathname !== "/login") {
                window.location.href = "/login";
            }
        }

        return Promise.reject(error);
    }
);

export default api;