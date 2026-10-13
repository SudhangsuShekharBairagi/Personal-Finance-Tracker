import { useEffect, useState } from "react";
import { motion } from "motion/react";
import { Bell, CircleAlert, RefreshCw } from "lucide-react";
import Button from "../../../components/Button.jsx";
import EmptyState from "../../../components/EmptyState.jsx";
import Panel from "../../../components/Panel.jsx";
import PageHeading from "../../../components/PageHeading.jsx";
import StatCard from "../../../components/StatCard.jsx";
import { useFinance } from "../../../hooks/useFinance.js";
import { listNotifications } from "../../../services/notificationService.js";

const statusOptions = [
  { label: "All statuses", value: "" },
  { label: "Pending", value: "PENDING" },
  { label: "Retrying", value: "RETRYING" },
  { label: "Sent", value: "SENT" },
  { label: "Failed", value: "FAILED" },
];

const typeOptions = [
  { label: "All types", value: "" },
  { label: "Warning", value: "WARNING" },
  { label: "Exceeded", value: "EXCEEDED" },
];

function formatDate(value) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function statusTone(status) {
  switch (status) {
    case "SENT":
      return "bg-emerald-50 text-emerald-800 ring-1 ring-emerald-200";
    case "PENDING":
      return "bg-sky-50 text-sky-800 ring-1 ring-sky-200";
    case "RETRYING":
      return "bg-amber-50 text-amber-800 ring-1 ring-amber-200";
    case "FAILED":
      return "bg-rose-50 text-rose-800 ring-1 ring-rose-200";
    default:
      return "bg-slate-100 text-slate-700 ring-1 ring-slate-200";
  }
}

function typeTone(type) {
  return type === "EXCEEDED"
    ? "bg-rose-50 text-rose-800 ring-1 ring-rose-200"
    : "bg-amber-50 text-amber-800 ring-1 ring-amber-200";
}

function NotificationsPage() {
  const { notify } = useFinance();
  const [status, setStatus] = useState("");
  const [type, setType] = useState("");
  const [page, setPage] = useState(0);
  const [items, setItems] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    totalPages: 0,
    totalElements: 0,
    number: 0,
  });
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  useEffect(() => {
    let active = true;

    setLoading(true);
    setLoadError("");

    listNotifications({ page, size: 10, status, type })
      .then((payload) => {
        if (!active) return;
        setItems(payload.content || []);
        setPageInfo({
          totalPages: payload.totalPages || 0,
          totalElements: payload.totalElements || 0,
          number: payload.number || 0,
        });
      })
      .catch((error) => {
        if (!active) return;
        setItems([]);
        setPageInfo({ totalPages: 0, totalElements: 0, number: 0 });
        const fieldErrors = Object.values(
          error.response?.data?.fieldErrors || {},
        );
        setLoadError(
          fieldErrors.join(" ") ||
            error.response?.data?.message ||
            "Could not load notifications.",
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [status, type, page]);

  const sentCount = items.filter((item) => item.status === "SENT").length;
  const pendingCount = items.filter((item) => item.status === "PENDING").length;

  return (
    <motion.div
      animate={{ opacity: 1, y: 0 }}
      className="space-y-6"
      initial={{ opacity: 0, y: 8 }}
      transition={{ duration: 0.3 }}
    >
      <PageHeading
        action={
          <Button
            icon={RefreshCw}
            onClick={() => {
              notify("Notifications refreshed.");
              setPage(0);
            }}
            variant="secondary"
          >
            Refresh
          </Button>
        }
        description="Recent budget alerts for your saved spending thresholds."
        eyebrow="Alerts"
        title="Notifications"
      />

      <div className="grid gap-3 md:grid-cols-3">
        <StatCard
          helper="Total records in this page"
          icon={Bell}
          label="Notifications"
          tone="emerald"
          value={`${pageInfo.totalElements}`}
        />
        <StatCard
          helper="Successfully sent"
          icon={Bell}
          label="Sent"
          tone="emerald"
          value={`${sentCount}`}
        />
        <StatCard
          helper="Awaiting retry or delivery"
          icon={Bell}
          label="Pending"
          tone="amber"
          value={`${pendingCount}`}
        />
      </div>

      <Panel
        description="Budget warnings and exceedance notices for your category limits."
        title="Budget alerts"
      >
        <div className="flex flex-col gap-3 border-b border-[#edf1ed] px-4 py-3 md:flex-row md:items-center md:justify-between">
          <div className="flex flex-col gap-3 md:flex-row md:items-center">
            <label className="flex items-center gap-2 text-sm text-[#5d6d62]">
              <span>Status</span>
              <select
                className="rounded-md border border-[#dfe7e0] bg-white px-2.5 py-2 text-sm text-[#27352d] outline-none focus:ring-2 focus:ring-emerald-700"
                onChange={(event) => {
                  setPage(0);
                  setStatus(event.target.value);
                }}
                value={status}
              >
                {statusOptions.map((option) => (
                  <option
                    key={option.value || "all-status"}
                    value={option.value}
                  >
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="flex items-center gap-2 text-sm text-[#5d6d62]">
              <span>Type</span>
              <select
                className="rounded-md border border-[#dfe7e0] bg-white px-2.5 py-2 text-sm text-[#27352d] outline-none focus:ring-2 focus:ring-emerald-700"
                onChange={(event) => {
                  setPage(0);
                  setType(event.target.value);
                }}
                value={type}
              >
                {typeOptions.map((option) => (
                  <option key={option.value || "all-type"} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <span className="text-xs text-[#68766c]">
            {pageInfo.totalElements} total alert
            {pageInfo.totalElements === 1 ? "" : "s"}
          </span>
        </div>

        {loading ? (
          <p className="px-5 py-8 text-sm text-[#68766c]" role="status">
            Loading notifications…
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
              icon={RefreshCw}
              onClick={() => setPage((current) => current + 0)}
              variant="secondary"
            >
              Retry
            </Button>
          </div>
        ) : !items.length ? (
          <EmptyState
            description="There are no budget alerts yet. When your spending reaches a threshold, a message will appear here."
            title="No notifications"
          />
        ) : (
          <div className="divide-y divide-[#edf1ed]">
            {items.map((notification) => (
              <article
                className="grid gap-3 px-4 py-4 md:grid-cols-[1.3fr_1fr_1fr_auto] md:items-center"
                key={notification.id}
              >
                <div>
                  <div className="mb-2 flex items-center gap-2">
                    <span
                      className={`inline-flex items-center rounded-full px-2 py-1 text-[10px] font-semibold uppercase tracking-[0.08em] ${typeTone(notification.type)}`}
                    >
                      {notification.type}
                    </span>
                    <span
                      className={`inline-flex items-center rounded-full px-2 py-1 text-[10px] font-semibold uppercase tracking-[0.08em] ${statusTone(notification.status)}`}
                    >
                      {notification.status}
                    </span>
                  </div>
                  <h3 className="text-sm font-semibold text-[#2b3a30]">
                    {notification.category} • {notification.year}-
                    {String(notification.month).padStart(2, "0")}
                  </h3>
                  <p className="mt-1 text-xs text-[#77847c]">
                    Actual spending: ₹
                    {Number(notification.actualSpending).toLocaleString(
                      "en-IN",
                      { maximumFractionDigits: 2 },
                    )}
                  </p>
                </div>

                <div className="text-sm text-[#48564d]">
                  <p>
                    Limit: ₹
                    {Number(notification.budgetLimit).toLocaleString("en-IN", {
                      maximumFractionDigits: 2,
                    })}
                  </p>
                  <p className="mt-1">
                    Threshold: {notification.thresholdPercent}%
                  </p>
                </div>

                <div className="text-sm text-[#48564d]">
                  <p>{notification.recipientEmail}</p>
                  <p className="mt-1 text-xs text-[#6f7d72]">
                    {formatDate(notification.createdAt)}
                  </p>
                </div>

                <div className="text-xs text-[#68766c] md:text-right">
                  {notification.failureReason || "No failure details"}
                </div>
              </article>
            ))}
          </div>
        )}

        {pageInfo.totalPages > 1 && (
          <div className="flex items-center justify-end gap-2 border-t border-[#edf1ed] px-4 py-3">
            <Button
              disabled={page <= 0}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              variant="secondary"
            >
              Previous
            </Button>
            <span className="text-sm text-[#5d6d62]">
              Page {page + 1} of {pageInfo.totalPages}
            </span>
            <Button
              disabled={page >= pageInfo.totalPages - 1}
              onClick={() => setPage((current) => current + 1)}
              variant="secondary"
            >
              Next
            </Button>
          </div>
        )}
      </Panel>
    </motion.div>
  );
}

export default NotificationsPage;
