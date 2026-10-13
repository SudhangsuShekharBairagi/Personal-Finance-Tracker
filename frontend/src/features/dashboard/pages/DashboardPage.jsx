import { useMemo } from "react";
import { Link } from "react-router-dom";
import { motion } from "motion/react";
import {
  ArrowDownLeft,
  ArrowUpRight,
  BriefcaseBusiness,
  CircleDollarSign,
  PiggyBank,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import Button from "../../../components/Button.jsx";
import CategoryBadge from "../../../components/CategoryBadge.jsx";
import EmptyState from "../../../components/EmptyState.jsx";
import PageHeading from "../../../components/PageHeading.jsx";
import Panel from "../../../components/Panel.jsx";
import StatCard from "../../../components/StatCard.jsx";
import TransactionTable from "../../../components/TransactionTable.jsx";
import { CATEGORY_COLORS } from "../../../data/mockFinanceData.js";
import { useFinance } from "../../../hooks/useFinance.js";
import {
  formatCurrency,
  formatMonth,
  getBudgetUsage,
  getDashboardSummary,
  getExpenseBreakdown,
  getMonthOptions,
  getMonthTransactions,
  getMonthlyTrend,
} from "../../../utils/finance.js";

const tooltipStyle = {
  border: "1px solid #dfe7e0",
  borderRadius: 8,
  boxShadow: "0 8px 24px rgba(20,45,28,.08)",
  fontSize: 12,
};

function DashboardPage() {
  const { budgets, selectedMonth, setSelectedMonth, transactions } =
    useFinance();
  const monthOptions = getMonthOptions(transactions);
  const summary = getDashboardSummary(transactions, selectedMonth);
  const trend = getMonthlyTrend(transactions);
  const categoryBreakdown = getExpenseBreakdown(transactions, selectedMonth);
  const monthTransactions = getMonthTransactions(transactions, selectedMonth);
  const recentTransactions = useMemo(
    () =>
      [...monthTransactions]
        .sort((first, second) => second.date.localeCompare(first.date))
        .slice(0, 5),
    [monthTransactions],
  );
  const totalCategoryExpenses = categoryBreakdown.reduce(
    (total, category) => total + category.value,
    0,
  );

  return (
    <motion.div
      animate={{ opacity: 1, y: 0 }}
      className="space-y-6 sm:space-y-8"
      initial={{ opacity: 0, y: 8 }}
      transition={{ duration: 0.3, ease: "easeOut" }}
    >
      <PageHeading
        action={
          <label
            className="grid gap-1.5 text-xs font-medium text-[#647168]"
            htmlFor="dashboard-month"
          >
            Reporting month
            <select
              className="min-h-10 min-w-36 rounded-md border border-[#d8e1d9] bg-white px-3 text-sm text-[#26372c] outline-none focus:border-emerald-700 focus:ring-2 focus:ring-emerald-700/15"
              id="dashboard-month"
              onChange={(event) => setSelectedMonth(event.target.value)}
              value={selectedMonth}
            >
              {monthOptions.map(({ label, value }) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
        }
        description={`A view of saved income and expenses for ${formatMonth(selectedMonth)}.`}
        eyebrow="Your finances at a glance"
        title="Overview"
      />

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          helper={`${formatMonth(selectedMonth)} · saved income`}
          icon={ArrowDownLeft}
          label="Total income"
          tone="emerald"
          value={formatCurrency(summary.income)}
        />
        <StatCard
          helper={`${formatMonth(selectedMonth)} · saved expenses`}
          icon={ArrowUpRight}
          label="Total expenses"
          tone="rose"
          value={formatCurrency(summary.expenses)}
        />
        <StatCard
          helper="Saved app records · not a bank balance"
          icon={CircleDollarSign}
          label="Current balance"
          tone="cyan"
          value={formatCurrency(summary.balance)}
        />
        <StatCard
          helper="Income less expenses this month"
          icon={PiggyBank}
          label="Savings rate"
          tone="amber"
          value={`${summary.savingsRate.toFixed(1)}%`}
        />
      </div>

      <div className="grid gap-5 xl:grid-cols-[1.55fr_1fr]">
        <Panel
          description="Income and expenses calculated from saved transaction records."
          title="Monthly cash flow"
        >
          <div
            aria-label="Monthly income and expense chart"
            className="h-72 px-2 pb-4 pt-5 sm:px-5"
            role="img"
          >
            <ResponsiveContainer height="100%" width="100%">
              <BarChart
                data={trend}
                margin={{ top: 8, right: 8, left: -16, bottom: 0 }}
              >
                <CartesianGrid
                  stroke="#edf1ed"
                  strokeDasharray="3 3"
                  vertical={false}
                />
                <XAxis
                  axisLine={false}
                  dataKey="month"
                  tick={{ fill: "#758178", fontSize: 11 }}
                  tickLine={false}
                />
                <YAxis
                  axisLine={false}
                  tick={{ fill: "#758178", fontSize: 11 }}
                  tickFormatter={(value) => `${Math.round(value / 1000)}k`}
                  tickLine={false}
                />
                <Tooltip
                  formatter={(value, name) => [
                    formatCurrency(value),
                    name === "income" ? "Income" : "Expenses",
                  ]}
                  contentStyle={tooltipStyle}
                />
                <Bar
                  dataKey="income"
                  fill="#16835f"
                  name="Income"
                  radius={[4, 4, 0, 0]}
                />
                <Bar
                  dataKey="expenses"
                  fill="#e3a23b"
                  name="Expenses"
                  radius={[4, 4, 0, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="flex justify-center gap-5 pb-4 text-xs text-[#66736a]">
            <span className="inline-flex items-center gap-2">
              <span className="size-2 rounded-sm bg-emerald-700" />
              Income
            </span>
            <span className="inline-flex items-center gap-2">
              <span className="size-2 rounded-sm bg-amber-500" />
              Expenses
            </span>
          </div>
        </Panel>

        <Panel
          description={`Expense categories for ${formatMonth(selectedMonth)}.`}
          title="Where money went"
        >
          {categoryBreakdown.length ? (
            <div className="grid items-center gap-2 px-5 py-4 sm:grid-cols-[minmax(150px,0.9fr)_1.1fr] xl:grid-cols-1 2xl:grid-cols-[minmax(150px,0.9fr)_1.1fr]">
              <div
                aria-label="Expense share by category"
                className="relative mx-auto h-48 w-full max-w-56"
                role="img"
              >
                <ResponsiveContainer height="100%" width="100%">
                  <PieChart>
                    <Pie
                      data={categoryBreakdown}
                      dataKey="value"
                      innerRadius={57}
                      outerRadius={82}
                      paddingAngle={2}
                      stroke="none"
                    >
                      {categoryBreakdown.map(({ name }) => (
                        <Cell
                          fill={CATEGORY_COLORS[name] || "#64748b"}
                          key={name}
                        />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value) => formatCurrency(value)}
                      contentStyle={tooltipStyle}
                    />
                  </PieChart>
                </ResponsiveContainer>
                <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
                  <span className="text-[10px] font-medium uppercase tracking-wide text-[#7a867d]">
                    Spent
                  </span>
                  <span className="mt-1 text-sm font-semibold tabular-nums text-[#26372c]">
                    {formatCurrency(totalCategoryExpenses)}
                  </span>
                </div>
              </div>
              <div className="grid gap-2">
                {categoryBreakdown.map((category) => (
                  <div
                    className="flex items-center justify-between gap-3 text-xs"
                    key={category.name}
                  >
                    <CategoryBadge category={category.name} />
                    <span className="shrink-0 font-medium tabular-nums text-[#526258]">
                      {formatCurrency(category.value)} ·{" "}
                      {totalCategoryExpenses
                        ? Math.round(
                            (category.value / totalCategoryExpenses) * 100,
                          )
                        : 0}
                      %
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <EmptyState
              description="There are no expense records for this month yet."
              title="Nothing to chart"
            />
          )}
        </Panel>
      </div>

      <div className="grid gap-5 xl:grid-cols-[1.45fr_1fr]">
        <Panel
          action={
            <Link
              className="text-xs font-semibold text-emerald-800 hover:text-emerald-950"
              to="/transactions"
            >
              All transactions <span aria-hidden="true">→</span>
            </Link>
          }
          description={`Latest activity in ${formatMonth(selectedMonth)}.`}
          title="Recent transactions"
        >
          <TransactionTable transactions={recentTransactions} />
        </Panel>

        <Panel
          action={
            <Link
              className="text-xs font-semibold text-emerald-800 hover:text-emerald-950"
              to="/budgets"
            >
              Manage budgets <span aria-hidden="true">→</span>
            </Link>
          }
          description={`Saved expense totals compared with example category limits for ${formatMonth(selectedMonth)}.`}
          title="Budget preview"
        >
          <div className="divide-y divide-[#edf1ed] px-5">
            {budgets.slice(0, 5).map((budget) => {
              const usage = getBudgetUsage(budget, transactions, selectedMonth);
              const barColor = usage.exceeded
                ? "bg-rose-600"
                : usage.percentUsed >= 75
                  ? "bg-amber-500"
                  : "bg-emerald-700";

              return (
                <div className="py-4" key={budget.id}>
                  <div className="flex items-center justify-between gap-3">
                    <span className="text-sm font-medium text-[#35453a]">
                      {budget.category}
                    </span>
                    <span className="text-xs tabular-nums text-[#6c7970]">
                      {formatCurrency(usage.spent)} /{" "}
                      {formatCurrency(budget.limit)}
                    </span>
                  </div>
                  <div
                    aria-label={`${budget.category} budget ${Math.round(usage.percentUsed)} percent used`}
                    className="mt-2 h-1.5 overflow-hidden rounded-full bg-[#edf1ed]"
                    role="meter"
                    aria-valuenow={Math.min(100, Math.round(usage.percentUsed))}
                    aria-valuemin={0}
                    aria-valuemax={100}
                  >
                    <div
                      className={`h-full rounded-full ${barColor}`}
                      style={{
                        width: `${Math.min(100, Math.max(0, usage.percentUsed))}%`,
                      }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
          {!budgets.length && (
            <EmptyState
              description="Add a monthly spending limit to see progress here."
              title="No budgets yet"
            />
          )}
        </Panel>
      </div>

      <div className="flex items-center gap-2 rounded-md border border-[#dfe8e0] bg-[#f0f5f0] px-4 py-3 text-xs leading-5 text-[#5e6c62]">
        <BriefcaseBusiness
          aria-hidden="true"
          className="shrink-0 text-emerald-800"
          size={16}
        />
        <p>
          Dashboard income and expenses use saved app transactions. The budget
          preview uses example limits; no figure represents bank activity.
        </p>
        <Button
          className="ml-auto hidden shrink-0 sm:inline-flex"
          icon={ArrowUpRight}
          variant="ghost"
          onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
        >
          Back to top
        </Button>
      </div>
    </motion.div>
  );
}

export default DashboardPage;
