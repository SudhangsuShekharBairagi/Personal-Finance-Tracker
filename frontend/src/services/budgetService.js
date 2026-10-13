import api, { requestCsrfToken } from "./api.js";

export async function listBudgets(year, month) {
  const { data } = await api.get("/budgets", { params: { year, month } });
  return data;
}

export async function createBudget(budget) {
  await requestCsrfToken();
  const { data } = await api.post("/budgets", budget);
  return data;
}

export async function updateBudget(id, budget) {
  await requestCsrfToken();
  const { data } = await api.put(`/budgets/${id}`, budget);
  return data;
}

export async function deleteBudget(id) {
  await requestCsrfToken();
  await api.delete(`/budgets/${id}`);
}
