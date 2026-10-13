const moneyToPaise = (amount) => Math.round(Number(amount) * 100);
const paiseToMoney = (amount) => amount / 100;

export function formatCurrency(amount, currency = "INR") {
  const value =
    typeof amount === "string" && amount.trim() ? amount : Number(amount) || 0;
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(value);
}

export function formatDate(date) {
  if (!date) return "—";
  return new Intl.DateTimeFormat("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(`${date}T00:00:00`));
}

export function formatMonth(month) {
  const [year, monthNumber] = month.split("-").map(Number);
  return new Intl.DateTimeFormat("en-IN", {
    month: "short",
    year: "numeric",
  }).format(new Date(year, monthNumber - 1, 1));
}

export function getMonthOptions(transactions) {
  return [...new Set(transactions.map(({ date }) => date.slice(0, 7)))]
    .sort((first, second) => second.localeCompare(first))
    .map((value) => ({ value, label: formatMonth(value) }));
}

export function sumAmounts(transactions) {
  return paiseToMoney(
    transactions.reduce(
      (total, transaction) => total + moneyToPaise(transaction.amount),
      0,
    ),
  );
}

export function getMonthTransactions(transactions, month) {
  return transactions.filter(({ date }) => date.startsWith(month));
}

export function getDashboardSummary(transactions, month) {
  const selectedTransactions = getMonthTransactions(transactions, month);
  const income = sumAmounts(
    selectedTransactions.filter(({ type }) => type === "income"),
  );
  const expenses = sumAmounts(
    selectedTransactions.filter(({ type }) => type === "expense"),
  );
  const allIncome = sumAmounts(
    transactions.filter(({ type }) => type === "income"),
  );
  const allExpenses = sumAmounts(
    transactions.filter(({ type }) => type === "expense"),
  );

  return {
    income,
    expenses,
    balance: allIncome - allExpenses,
    savingsRate: income > 0 ? ((income - expenses) / income) * 100 : 0,
  };
}

export function getMonthlyTrend(transactions) {
  const months = getMonthOptions(transactions).slice(0, 7).reverse();

  return months.map(({ value, label }) => {
    const monthTransactions = getMonthTransactions(transactions, value);
    return {
      month: label,
      income: sumAmounts(
        monthTransactions.filter(({ type }) => type === "income"),
      ),
      expenses: sumAmounts(
        monthTransactions.filter(({ type }) => type === "expense"),
      ),
    };
  });
}

export function getExpenseBreakdown(transactions, month) {
  const totals = getMonthTransactions(transactions, month)
    .filter(({ type }) => type === "expense")
    .reduce((categories, transaction) => {
      categories[transaction.category] =
        (categories[transaction.category] || 0) +
        moneyToPaise(transaction.amount);
      return categories;
    }, {});

  return Object.entries(totals)
    .map(([name, amount]) => ({ name, value: paiseToMoney(amount) }))
    .sort((first, second) => second.value - first.value);
}

export function getBudgetUsage(budget, transactions, month) {
  const spent = sumAmounts(
    getMonthTransactions(transactions, month).filter(
      ({ type, category }) =>
        type === "expense" && category === budget.category,
    ),
  );
  const remaining = budget.limit - spent;

  return {
    spent,
    remaining,
    percentUsed: budget.limit > 0 ? (spent / budget.limit) * 100 : 0,
    exceeded: remaining < 0,
  };
}
