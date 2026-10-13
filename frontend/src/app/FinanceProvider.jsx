import { useEffect, useState } from "react";
import api, { requestCsrfToken } from "../services/api.js";
import { useAuth } from "../hooks/useAuth.js";
import { FinanceContext } from "./FinanceContext.js";
import { DEFAULT_MONTH, mockBudgets } from "../data/mockFinanceData.js";

function normalizeTransaction(transaction) {
  return {
    id: transaction.id,
    description: transaction.description ?? "",
    amount: Number(transaction.amount ?? 0),
    type: transaction.type ?? "expense",
    category: transaction.category ?? "Other",
    date:
      typeof transaction.date === "string"
        ? transaction.date.slice(0, 10)
        : transaction.date,
  };
}

function createId(prefix) {
  const randomId = globalThis.crypto?.randomUUID?.();
  return randomId
    ? `${prefix}-${randomId}`
    : `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function FinanceProvider({ children }) {
  const { status, user } = useAuth();
  const userId = user?.id;
  const [transactionSnapshot, setTransactionSnapshot] = useState({
    userId: null,
    rows: [],
  });
  const transactions =
    status === "authenticated" && transactionSnapshot.userId === user?.id
      ? transactionSnapshot.rows
      : [];
  const [budgets, setBudgets] = useState(mockBudgets);
  const [selectedMonth, setSelectedMonth] = useState(DEFAULT_MONTH);
  const [preferences, setPreferences] = useState({
    dashboardMonth: DEFAULT_MONTH,
  });
  const [toast, setToast] = useState(null);

  useEffect(() => {
    if (status !== "authenticated" || userId == null) return undefined;
    let active = true;

    async function refreshTransactions() {
      try {
        const rows = [];
        let page = 0;
        let totalPages = 1;

        while (page < totalPages) {
          const { data } = await api.get("/transactions", {
            params: { page, size: 200 },
          });
          const pageRows = Array.isArray(data) ? data : (data?.content ?? []);
          rows.push(...pageRows);
          totalPages = Array.isArray(data) ? 1 : (data?.totalPages ?? page + 1);
          page += 1;
        }
        if (!active) return;
        setTransactionSnapshot({
          userId,
          rows: rows.map(normalizeTransaction),
        });
      } catch {
        if (active) setTransactionSnapshot({ userId, rows: [] });
      }
    }

    function refreshWhenVisible() {
      if (document.visibilityState === "visible") refreshTransactions();
    }

    refreshTransactions();
    const refreshInterval = window.setInterval(refreshTransactions, 60_000);
    window.addEventListener("focus", refreshTransactions);
    document.addEventListener("visibilitychange", refreshWhenVisible);

    return () => {
      active = false;
      window.clearInterval(refreshInterval);
      window.removeEventListener("focus", refreshTransactions);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [status, userId]);

  const addTransaction = async (transaction) => {
    await requestCsrfToken();
    const { data } = await api.post("/transactions", {
      description: transaction.description,
      amount: Number(transaction.amount),
      type: transaction.type,
      category: transaction.category,
      date: transaction.date,
    });

    setTransactionSnapshot((current) => ({
      userId: user.id,
      rows: [
        normalizeTransaction(data),
        ...(current.userId === user.id ? current.rows : []),
      ],
    }));
    return data;
  };

  const updateTransaction = async (updatedTransaction) => {
    await requestCsrfToken();
    const { data } = await api.put(`/transactions/${updatedTransaction.id}`, {
      description: updatedTransaction.description,
      amount: Number(updatedTransaction.amount),
      type: updatedTransaction.type,
      category: updatedTransaction.category,
      date: updatedTransaction.date,
    });

    setTransactionSnapshot((current) => ({
      userId: user.id,
      rows: (current.userId === user.id ? current.rows : []).map(
        (transaction) =>
          transaction.id === updatedTransaction.id
            ? normalizeTransaction(data)
            : transaction,
      ),
    }));
    return data;
  };

  const deleteTransaction = async (transactionId) => {
    await requestCsrfToken();
    await api.delete(`/transactions/${transactionId}`);
    setTransactionSnapshot((current) => ({
      userId: user.id,
      rows: (current.userId === user.id ? current.rows : []).filter(
        ({ id }) => id !== transactionId,
      ),
    }));
  };

  const addBudget = (budget) => {
    setBudgets((current) => [
      { ...budget, id: createId("budget") },
      ...current,
    ]);
  };

  const updateBudget = (updatedBudget) => {
    setBudgets((current) =>
      current.map((budget) =>
        budget.id === updatedBudget.id ? updatedBudget : budget,
      ),
    );
  };

  const deleteBudget = (budgetId) => {
    setBudgets((current) => current.filter(({ id }) => id !== budgetId));
  };

  const notify = (message, tone = "success") => {
    setToast({ id: Date.now(), message, tone });
  };

  const value = {
    transactions,
    budgets,
    selectedMonth,
    setSelectedMonth,
    preferences,
    setPreferences,
    toast,
    setToast,
    addTransaction,
    updateTransaction,
    deleteTransaction,
    addBudget,
    updateBudget,
    deleteBudget,
    notify,
  };

  return (
    <FinanceContext.Provider value={value}>{children}</FinanceContext.Provider>
  );
}
