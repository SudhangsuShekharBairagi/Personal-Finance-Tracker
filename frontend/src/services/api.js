import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
  headers: {
    "Content-Type": "application/json",
  },
});

export async function requestCsrfToken() {
  return api.get("/auth/csrf");
}

export function getApiErrorMessage(error, fallback) {
  const response = error.response?.data;
  const fieldErrors = Object.values(response?.fieldErrors || {});
  if (fieldErrors.length) return fieldErrors.join(" ");
  if (response?.message) return response.message;
  if (!error.response) {
    return "The authentication service could not be reached. Check that the backend is running.";
  }
  return fallback;
}

export default api;
