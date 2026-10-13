import { useEffect, useState } from "react";
import { motion } from "motion/react";
import {
  CalendarClock,
  CircleAlert,
  Pencil,
  Plus,
  Repeat,
  RotateCcw,
  Trash2,
} from "lucide-react";
import Button from "../../../components/Button.jsx";
import CategoryBadge from "../../../components/CategoryBadge.jsx";
import EmptyState from "../../../components/EmptyState.jsx";
import Modal from "../../../components/Modal.jsx";
import PageHeading from "../../../components/PageHeading.jsx";
import Panel from "../../../components/Panel.jsx";
import StatCard from "../../../components/StatCard.jsx";
import { useFinance } from "../../../hooks/useFinance.js";
import {
  createRecurringTransaction,
  deleteRecurringTransaction,
  listRecurringTransactions,
  pauseRecurringTransaction,
  resumeRecurringTransaction,
  updateRecurringTransaction,
} from "../../../services/recurringTransactionService.js";
import { formatCurrency, formatDate } from "../../../utils/finance.js";
import RecurringForm from "../components/RecurringForm.jsx";

function getRequestError(error, fallback) {
  const fieldErrors = Object.values(error.response?.data?.fieldErrors || {});
  return fieldErrors.join(" ") || error.response?.data?.message || fallback;
}

const statusColors = {
  ACTIVE: "text-emerald-800",
  PAUSED: "text-amber-800",
  ENDED: "text-[#78847b]",
};

function RecurringPage() {
  const { notify } = useFinance();
  const [recurringTransactions, setRecurringTransactions] = useState([]);
  const [loadedRequestKey, setLoadedRequestKey] = useState("");
  const [loadFailure, setLoadFailure] = useState({ key: "", message: "" });
  const [refreshKey, setRefreshKey] = useState(0);
  const [updatingId, setUpdatingId] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [deletingItem, setDeletingItem] = useState(null);
  const requestKey = String(refreshKey);
  const loading = loadedRequestKey !== requestKey;
  const loadError = loadFailure.key === requestKey ? loadFailure.message : "";

  useEffect(() => {
    let active = true;

    listRecurringTransactions()
      .then((data) => {
        if (!active) return;
        setRecurringTransactions(data);
        setLoadFailure({ key: "", message: "" });
      })
      .catch((error) => {
        if (!active) return;
        setRecurringTransactions([]);
        setLoadFailure({
          key: requestKey,
          message: getRequestError(
            error,
            "Could not load recurring templates.",
          ),
        });
      })
      .finally(() => {
        if (active) setLoadedRequestKey(requestKey);
      });

    return () => {
      active = false;
    };
  }, [requestKey]);

  function openAddForm() {
    setEditingItem(null);
    setFormOpen(true);
  }

  async function saveItem(item) {
    if (editingItem) {
      await updateRecurringTransaction(editingItem.id, item);
      notify(
        "Recurring template updated. Existing transactions are unchanged.",
      );
    } else {
      await createRecurringTransaction(item);
      notify("Recurring template added.");
    }
    setFormOpen(false);
    setEditingItem(null);
    setRefreshKey((current) => current + 1);
  }

  async function toggleStatus(item) {
    setUpdatingId(item.id);
    try {
      const updated =
        item.status === "ACTIVE"
          ? await pauseRecurringTransaction(item.id)
          : await resumeRecurringTransaction(item.id);
      notify(
        updated.status === "PAUSED"
          ? "Schedule paused. Its next date is unchanged."
          : "Schedule resumed. Missed dates will be processed gradually.",
      );
      setRefreshKey((current) => current + 1);
    } catch (error) {
      notify(
        getRequestError(error, "Could not change this schedule."),
        "error",
      );
    } finally {
      setUpdatingId(null);
    }
  }

  async function confirmDelete() {
    setDeleting(true);
    try {
      await deleteRecurringTransaction(deletingItem.id);
      notify(
        "Template removed. Generated transactions remain in your history.",
      );
      setDeletingItem(null);
      setRefreshKey((current) => current + 1);
    } catch (error) {
      notify(
        getRequestError(error, "Could not remove this template."),
        "error",
      );
    } finally {
      setDeleting(false);
    }
  }

  const activeCount = recurringTransactions.filter(
    ({ status }) => status === "ACTIVE",
  ).length;
  const incomeCount = recurringTransactions.filter(
    ({ type }) => type === "income",
  ).length;
  const expenseCount = recurringTransactions.filter(
    ({ type }) => type === "expense",
  ).length;

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
            Add recurring item
          </Button>
        }
        description="Templates create income or expense records in this app on their scheduled dates."
        eyebrow="Plan ahead"
        title="Recurring transactions"
      />

      <div className="grid gap-3 sm:grid-cols-3">
        <StatCard
          helper="Schedules currently marked active"
          icon={Repeat}
          label="Active schedules"
          tone="emerald"
          value={`${activeCount}`}
        />
        <StatCard
          helper="Income templates, including paused ones"
          icon={CalendarClock}
          label="Income templates"
          tone="cyan"
          value={`${incomeCount}`}
        />
        <StatCard
          helper="Expense templates, including paused ones"
          icon={CalendarClock}
          label="Expense templates"
          tone="amber"
          value={`${expenseCount}`}
        />
      </div>

      <div className="flex items-start gap-3 rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-6 text-amber-950">
        <CalendarClock
          aria-hidden="true"
          className="mt-0.5 shrink-0"
          size={18}
        />
        <p>
          <span className="font-semibold">App records only.</span> Schedules
          create transaction history entries; they never move money, pay bills,
          or contact a bank.
        </p>
      </div>

      <Panel
        className="overflow-hidden"
        action={
          <Button
            aria-label="Refresh recurring templates"
            disabled={loading}
            icon={RotateCcw}
            onClick={() => setRefreshKey((current) => current + 1)}
            variant="secondary"
          />
        }
        description="Edit future entries, pause a schedule, or remove its template."
        title="Your schedules"
      >
        {loading ? (
          <p
            aria-live="polite"
            className="px-5 py-8 text-sm text-[#68766c]"
            role="status"
          >
            Loading recurring templates…
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
        ) : !recurringTransactions.length ? (
          <EmptyState
            action={
              <Button icon={Plus} onClick={openAddForm}>
                Add recurring item
              </Button>
            }
            description="Add a template to create income or expense records on future dates."
            title="No recurring items"
          />
        ) : (
          <div className="divide-y divide-[#edf1ed]">
            {recurringTransactions.map((item) => (
              <article
                className="grid gap-3 px-4 py-4 sm:grid-cols-[minmax(170px,1.3fr)_minmax(120px,1fr)_minmax(90px,.7fr)_minmax(90px,.7fr)_minmax(120px,.8fr)_auto] sm:items-center sm:px-5"
                key={item.id}
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span
                      className={`size-2 shrink-0 rounded-full ${item.status === "ACTIVE" ? "bg-emerald-600" : item.status === "PAUSED" ? "bg-amber-500" : "bg-[#a2ada4]"}`}
                    />
                    <h3 className="truncate text-sm font-semibold text-[#2a392f]">
                      {item.description}
                    </h3>
                  </div>
                  <div className="mt-2 flex flex-wrap items-center gap-2">
                    <CategoryBadge category={item.category} type={item.type} />
                    <span className="text-xs text-[#78847b]">
                      {item.frequency.toLowerCase()}
                    </span>
                  </div>
                  <p className="mt-2 text-xs text-[#78847b]">
                    Starts {formatDate(item.startDate)} · Ends{" "}
                    {item.endDate ? formatDate(item.endDate) : "never"}
                  </p>
                </div>
                <div>
                  <p className="text-[11px] uppercase tracking-wide text-[#849087]">
                    Next due date
                  </p>
                  <p className="mt-1 text-sm text-[#46554b]">
                    {formatDate(item.nextScheduledDate)}
                  </p>
                </div>
                <div>
                  <p className="text-[11px] uppercase tracking-wide text-[#849087]">
                    Status
                  </p>
                  <p
                    className={`mt-1 text-sm font-medium ${statusColors[item.status] || "text-[#78847b]"}`}
                  >
                    {item.status.toLowerCase()}
                  </p>
                </div>
                <div>
                  <p className="text-[11px] uppercase tracking-wide text-[#849087]">
                    Type
                  </p>
                  <p className="mt-1 text-sm capitalize text-[#46554b]">
                    {item.type}
                  </p>
                </div>
                <p
                  className={`text-sm font-semibold tabular-nums sm:text-right ${item.type === "income" ? "text-emerald-800" : "text-[#35443a]"}`}
                >
                  {item.type === "income" ? "+" : "−"}
                  {formatCurrency(item.amount)}
                </p>
                <div className="flex flex-wrap items-center justify-end gap-1">
                  {item.status !== "ENDED" && (
                    <Button
                      className="min-h-9 px-3 text-xs"
                      disabled={updatingId === item.id}
                      onClick={() => toggleStatus(item)}
                      variant="secondary"
                    >
                      {updatingId === item.id
                        ? "Updating…"
                        : item.status === "ACTIVE"
                          ? "Pause"
                          : "Resume"}
                    </Button>
                  )}
                  <button
                    aria-label={`Edit ${item.description}`}
                    className="grid size-9 place-items-center rounded-md text-[#68766c] hover:bg-[#edf2ed] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700"
                    onClick={() => {
                      setEditingItem(item);
                      setFormOpen(true);
                    }}
                    type="button"
                  >
                    <Pencil aria-hidden="true" size={16} />
                  </button>
                  <button
                    aria-label={`Delete ${item.description}`}
                    className="grid size-9 place-items-center rounded-md text-[#68766c] hover:bg-rose-50 hover:text-rose-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-700"
                    onClick={() => setDeletingItem(item)}
                    type="button"
                  >
                    <Trash2 aria-hidden="true" size={16} />
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </Panel>

      <Modal
        description="The app creates a transaction record when an occurrence is due; this does not initiate a payment."
        onClose={() => setFormOpen(false)}
        open={formOpen}
        title={editingItem ? "Edit recurring item" : "Add recurring item"}
      >
        <RecurringForm
          initialItem={editingItem}
          key={editingItem?.id || "new-recurring"}
          onCancel={() => setFormOpen(false)}
          onSave={saveItem}
        />
      </Modal>

      <Modal
        description="The template is removed; transactions it already created are kept."
        onClose={() => setDeletingItem(null)}
        open={Boolean(deletingItem)}
        title="Remove recurring item?"
      >
        {deletingItem && (
          <div className="grid gap-5">
            <p className="text-sm leading-6 text-[#5f6d63]">
              Remove{" "}
              <span className="font-semibold">{deletingItem.description}</span>{" "}
              from recurring templates?
            </p>
            <div className="flex justify-end gap-2">
              <Button
                disabled={deleting}
                onClick={() => setDeletingItem(null)}
                variant="secondary"
              >
                Keep item
              </Button>
              <Button
                disabled={deleting}
                onClick={confirmDelete}
                variant="danger"
              >
                {deleting ? "Removing…" : "Remove template"}
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </motion.div>
  );
}

export default RecurringPage;
