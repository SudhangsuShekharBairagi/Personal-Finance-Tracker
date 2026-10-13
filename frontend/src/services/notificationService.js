import api from "./api.js";

export async function listNotifications({
  page = 0,
  size = 25,
  status,
  type,
} = {}) {
  const params = { page, size };

  if (status) params.status = status;
  if (type) params.type = type;

  const { data } = await api.get("/notifications", { params });
  return data;
}

export async function getNotification(id) {
  const { data } = await api.get(`/notifications/${id}`);
  return data;
}
