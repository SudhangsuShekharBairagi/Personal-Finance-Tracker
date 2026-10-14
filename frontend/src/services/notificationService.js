import api from "./api.js";

export async function listNotifications({
  page = 0,
  size = 25,
} = {}) {
  const { data } = await api.get("/notifications", { params: { page, size } });
  return data;
}

export async function getNotification(id) {
  const { data } = await api.get(`/notifications/${id}`);
  return data;
}
