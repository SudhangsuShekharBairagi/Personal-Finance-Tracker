import { useEffect, useState } from "react";
import { motion } from "motion/react";
import {
  CircleAlert,
  Pencil,
  PiggyBank,
  Plus,
  RotateCcw,
  Trash2,
} from "lucide-react";
import Button from "../../../components/Button.jsx";
import EmptyState from "../../../components/EmptyState.jsx";
import Field, { controlClassName } from "../../../components/Field.jsx";
import Modal from "../../../components/Modal.jsx";
import PageHeading from "../../../components/PageHeading.jsx";
import Panel from "../../../components/Panel.jsx";
import StatCard from "../../../components/StatCard.jsx";
import { useFinance } from "../../../hooks/useFinance.js";
import {
  createBudget,
  deleteBudget,
  listBudgets,
  updateBudget,
} from "../../../services/budgetService.js";
import { formatCurrency, formatMonth } from "../../../utils/finance.js";
import BudgetForm from "../components/BudgetForm.jsx";

function currentPeriod() {
  const today = new Date();
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}`;
}

function getRequestError(error, fallback) {
  const fieldErrors = Object.values(error.response?.data?.fieldErrors || {});
  return fieldErrors.join(" ") || error.response?.data?.message || fallback;
}

function BudgetsPage() {
  const { notify } = useFinance();
  const [selectedPeriod, setSelectedPeriod] = useState(currentPeriod);
  const [budgets, setBudgets] = useState([]);
  const [loadedRequestKey, setLoadedRequestKey] = useState("");
  const [loadFailure, setLoadFailure] = useState({ key: "", message: "" });
  const [refreshKey, setRefreshKey] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [editingBudget, setEditingBudget] = useState(null);
  const [deletingBudget, setDeletingBudget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const requestKey = `${selectedPeriod}:${refreshKey}`;
  const loading = loadedRequestKey !== requestKey;
  const loadError = loadFailure.key === requestKey ? loadFailure.message : "";
  const visibleBudgets = loading ? [] : budgets;

  useEffect(() => {
    let active = true;
    const [year, month] = selectedPeriod.split("-").map(Number);

    listBudgets(year, month)
      .then((data) => {
        if (!active) return;
        setBudgets(data);
        setLoadFailure({ key: "", message: "" });
      })
      .catch((error) => {
        if (!active) return;
        setBudgets([]);
        setLoadFailure({
          key: requestKey,
          message: getRequestError(error, "Could not load budgets. Try again."),
        });
      })
      .finally(() => {
        if (active) setLoadedRequestKey(requestKey);
      });

    return () => {
      active = false;
    };
  }, [requestKey, selectedPeriod]);

  const warningCount = visibleBudgets.filter(
    ({ status }) => status === "WARNING",
  ).length;
  const overBudgetCount = visibleBudgets.filter(
    ({ status }) => status === "EXCEEDED",
  ).length;

  function openAddForm() {
    setEditingBudget(null);
    setFormOpen(true);
  }

  async function saveBudget(budget) {
    if (editingBudget) {
      await updateBudget(editingBudget.id, budget);
      notify("Budget updated.");
    } else {
      await createBudget(budget);
      notify("Budget added.");
    }
    setFormOpen(false);
    setEditingBudget(null);
    setRefreshKey((current) => current + 1);
  }

  async function confirmDelete() {
    setDeleting(true);
    try {
      await deleteBudget(deletingBudget.id);
      notify("Budget removed.");
      setDeletingBudget(null);
      setRefreshKey((current) => current + 1);
    } catch (error) {
      notify(getRequestError(error, "Could not remove this budget."), "error");
    } finally {
      setDeleting(false);
    }
  }

  return (
    <motion.div
      animate={{ opacity: 1, y: 0 }}
      className="space-y-6 sm:space-y-8"
      initial={{ opacity: 0, y: 8 }}
      transition={{ duration: 0.3 }}
    >
      <PageHeading
        action={
          <Button icon={Plus} onClick={openAddForm}>
            Add budget
          </Button>
        }
        description={`Category limits compared with saved expenses for ${formatMonth(selectedPeriod)}.`}
        eyebrow="Plan ahead"
        title="Budgets"
      />

      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <Field htmlFor="budget-month-filter" label="Budget month">
          <input
            className={controlClassName}
            id="budget-month-filter"
            onChange={(event) => {
              if (event.target.value) setSelectedPeriod(event.target.value);
            }}
            type="month"
            value={selectedPeriod}
          />
        </Field>
        <p className="text-xs text-[#68766c]">
          Expense-only spending is calculated from saved transactions.
        </p>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <StatCard
          helper="For the selected month"
          icon={PiggyBank}
          label="Category budgets"
          tone="emerald"
          value={`${visibleBudgets.length}`}
        />
        <StatCard
          helper="At least 80% used"
          icon={PiggyBank}
          label="Warning"
          tone="amber"
          value={`${warningCount}`}
        />
        <StatCard
          helper="Categories at or above their limit"
          icon={PiggyBank}
          label="Exceeded"
          tone={overBudgetCount ? "rose" : "amber"}
          value={`${overBudgetCount}`}
        />
      </div>

      <Panel
        className="overflow-hidden"
        description="Actual spending is calculated from matching saved expense transactions."
        title={`${formatMonth(selectedPeriod)} category limits`}
      >
        {loading ? (
          <p
            aria-live="polite"
            className="px-5 py-8 text-sm text-[#68766c]"
            role="status"
          >
            Loading budgets…
          </p>
        ) : loadError ? (
          <div
            className="grid justify-items-start gap-3 px-5 py-6"
            role="alert"
          >
            <p className="flex items-center gap-2 text-sm text-rose-700">
              <CircleAlert aria-hidden="true" size={17} />
              {loadError}
            </p>
            <Button
              icon={RotateCcw}
              onClick={() => setRefreshKey((current) => current + 1)}
              variant="secondary"
            >
              Try again
            </Button>
          </div>
        ) : !visibleBudgets.length ? (
          <EmptyState
            action={
              <Button icon={Plus} onClick={openAddForm}>
                Create a budget
              </Button>
            }
            description="Add a category limit to compare it with saved expenses."
            title="No budgets set"
          />
        ) : (
          <div className="divide-y divide-[#edf1ed]">
            {visibleBudgets.map((budget) => {
              const percentUsed = Number(budget.percentageUsed);
              const remaining = Number(budget.remainingAmount);
              const exceeded = budget.status === "EXCEEDED";
              const warning = budget.status === "WARNING";
              const meterColor = exceeded
                ? "bg-rose-600"
                : warning
                  ? "bg-amber-500"
                  : "bg-emerald-700";

              return (
                <article
                  className="grid gap-3 px-4 py-4 sm:grid-cols-[minmax(150px,1fr)_minmax(180px,1.5fr)_minmax(180px,1.2fr)_auto] sm:items-center sm:px-5"
                  key={budget.id}
                >
                  <div>
                    <h3 className="text-sm font-semibold text-[#2b3a30]">
                      {budget.category}
                    </h3>
                    <p className="mt-1 text-xs text-[#78847b]">
                      {budget.status === "EXCEEDED"
                        ? "Exceeded"
                        : budget.status === "WARNING"
                          ? "Warning"
                          : "Normal"}
                    </p>
                  </div>
                  <div>
                    <div className="mb-2 flex justify-between gap-3 text-xs">
                      <span className="text-[#77837a]">Spent / limit</span>
                      <span className="font-medium tabular-nums text-[#3c4a40]">
                        {formatCurrency(budget.actualSpending)} /{" "}
                        {formatCurrency(budget.budgetLimit)}
                      </span>
                    </div>
                    <div
                      aria-label={`${budget.category} budget ${percentUsed.toFixed(2)} percent used`}
                      aria-valuemax={100}
                      aria-valuemin={0}
                      aria-valuenow={Math.min(100, percentUsed)}
                      aria-valuetext={`${percentUsed.toFixed(2)}% used`}
                      className="h-2 overflow-hidden rounded-full bg-[#edf1ed]"
                      role="meter"
                    >
                      <div
                        className={`h-full rounded-full transition-[width] duration-300 ${meterColor}`}
                        style={{
                          width: `${Math.min(100, Math.max(0, percentUsed))}%`,
                        }}
                      />
                    </div>
                  </div>
                  <div className="flex items-center justify-between gap-4 text-xs sm:block sm:text-right">
                    <span className="text-[#78847b]">
                      {percentUsed.toFixed(2)}% used
                    </span>
                    <span
                      className={`font-semibold tabular-nums ${remaining < 0 ? "text-rose-700" : "text-[#405047]"}`}
                    >
                      {formatCurrency(remaining)} remaining
                    </span>
                  </div>
                  <div className="flex justify-end gap-1">
                    <button
                      aria-label={`Edit ${budget.category} budget`}
                      className="grid size-9 place-items-center rounded-md text-[#68766c] hover:bg-[#edf2ed] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700"
                      onClick={() => {
                        setEditingBudget(budget);
                        setFormOpen(true);
                      }}
                      type="button"
                    >
                      <Pencil aria-hidden="true" size={16} />
                    </button>
                    <button
                      aria-label={`Delete ${budget.category} budget`}
                      className="grid size-9 place-items-center rounded-md text-[#68766c] hover:bg-rose-50 hover:text-rose-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-700"
                      onClick={() => setDeletingBudget(budget)}
                      type="button"
                    >
                      <Trash2 aria-hidden="true" size={16} />
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </Panel>

      <Modal
        description="Set a category limit for a calendar month."
        onClose={() => setFormOpen(false)}
        open={formOpen}
        title={editingBudget ? "Edit budget" : "Add monthly budget"}
      >
        <BudgetForm
          initialBudget={editingBudget}
          key={editingBudget?.id || "new-budget"}
          onCancel={() => setFormOpen(false)}
          onSave={saveBudget}
          selectedPeriod={selectedPeriod}
        />
      </Modal>

      <Modal
        description="This removes the saved category limit from your account."
        onClose={() => setDeletingBudget(null)}
        open={Boolean(deletingBudget)}
        title="Remove budget?"
      >
        {deletingBudget && (
          <div className="grid gap-5">
            <p className="text-sm leading-6 text-[#5f6d63]">
              Remove the{" "}
              <span className="font-semibold">{deletingBudget.category}</span>{" "}
              budget?
            </p>
            <div className="flex justify-end gap-2">
              <Button
                disabled={deleting}
                onClick={() => setDeletingBudget(null)}
                variant="secondary"
              >
                Keep budget
              </Button>
              <Button
                disabled={deleting}
                onClick={confirmDelete}
                variant="danger"
              >
                {deleting ? "Removing…" : "Remove budget"}
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </motion.div>
  );
}

export default BudgetsPage;
