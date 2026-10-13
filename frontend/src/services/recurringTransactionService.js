import api, { requestCsrfToken } from "./api.js";

export async function listRecurringTransactions() {
  const { data } = await api.get("/recurring-transactions");
  return data;
}

export async function createRecurringTransaction(template) {
  await requestCsrfToken();
  const { data } = await api.post("/recurring-transactions", template);
  return data;
}

export async function updateRecurringTransaction(id, template) {
  await requestCsrfToken();
  const { data } = await api.put(`/recurring-transactions/${id}`, template);
  return data;
}

export async function pauseRecurringTransaction(id) {
  await requestCsrfToken();
  const { data } = await api.patch(`/recurring-transactions/${id}/pause`);
  return data;
}

export async function resumeRecurringTransaction(id) {
  await requestCsrfToken();
  const { data } = await api.patch(`/recurring-transactions/${id}/resume`);
  return data;
}

export async function deleteRecurringTransaction(id) {
  await requestCsrfToken();
  await api.delete(`/recurring-transactions/${id}`);
}
