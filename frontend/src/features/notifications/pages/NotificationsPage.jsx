import { useEffect, useState } from "react";
import { motion } from "motion/react";
import { Bell, CircleAlert, RefreshCw } from "lucide-react";
import Button from "../../../components/Button.jsx";
import EmptyState from "../../../components/EmptyState.jsx";
import Panel from "../../../components/Panel.jsx";
import PageHeading from "../../../components/PageHeading.jsx";
import StatCard from "../../../components/StatCard.jsx";
import { listNotifications } from "../../../services/notificationService.js";

function formatDate(value) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function typeTone(type) {
  return type === "EXCEEDED"
    ? "bg-rose-50 text-rose-800 ring-1 ring-rose-200"
    : "bg-amber-50 text-amber-800 ring-1 ring-amber-200";
}

function NotificationsPage() {
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
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

    const loadNotifications = async () => {
      setLoading(true);
      setLoadError("");

      try {
        const payload = await listNotifications({ page, size: 10 });
        if (!active) return;
        setItems(payload.content || []);
        setPageInfo({
          totalPages: payload.totalPages || 0,
          totalElements: payload.totalElements || 0,
          number: payload.number || 0,
        });
      } catch (error) {
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
      } finally {
        if (active) setLoading(false);
      }
    };

    void loadNotifications();

    return () => {
      active = false;
    };
  }, [page, reloadKey]);

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
            disabled={loading}
            icon={RefreshCw}
            onClick={() => setReloadKey((current) => current + 1)}
            variant="secondary"
          >
            {loading ? "Refreshing…" : "Refresh"}
          </Button>
        }
        description="Recent budget alerts for your saved spending thresholds."
        eyebrow="Alerts"
        title="Notifications"
      />

      <div>
        <StatCard
          helper="Budget threshold alerts"
          icon={Bell}
          label="Total alerts"
          tone="emerald"
          value={`${pageInfo.totalElements}`}
        />
      </div>

      <Panel
        description="Budget warnings and exceedance notices for your category limits."
        title="Budget alerts"
      >
        <div className="flex justify-end border-b border-[#edf1ed] px-4 py-3">
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
              disabled={loading}
              icon={RefreshCw}
              onClick={() => setReloadKey((current) => current + 1)}
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
                className="flex flex-col gap-3 px-4 py-4 sm:flex-row sm:items-start sm:justify-between"
                key={notification.id}
              >
                <div>
                  <div className="mb-2 flex items-center gap-2">
                    <span
                      className={`inline-flex items-center rounded-full px-2 py-1 text-[10px] font-semibold tracking-[0.04em] ${typeTone(notification.type)}`}
                    >
                      {notification.type === "EXCEEDED" ? "Exceeded" : "Warning"}
                    </span>
                  </div>
                  <h3 className="text-sm font-semibold text-[#2b3a30]">
                    {notification.category} · {notification.year}-
                    {String(notification.month).padStart(2, "0")}
                  </h3>
                  <p className="mt-1 text-sm text-[#48564d]">
                    {notification.type === "EXCEEDED"
                      ? `${notification.category} exceeded your budget`
                      : `${notification.category} reached ${notification.thresholdPercent}% of your budget`}
                  </p>
                  <p className="mt-1 text-xs text-[#77847c]">
                    Spending ₹
                    {Number(notification.actualSpending).toLocaleString(
                      "en-IN",
                      { maximumFractionDigits: 2 },
                    )}{" "}
                    of ₹
                    {Number(notification.budgetLimit).toLocaleString("en-IN", {
                      maximumFractionDigits: 2,
                    })}{" "}
                    limit
                  </p>
                  {notification.status === "FAILED" && (
                    <p className="mt-2 text-xs text-rose-700">
                      Email could not be delivered
                    </p>
                  )}
                </div>

                <div className="text-xs text-[#6f7d72] sm:text-right">
                  {formatDate(notification.createdAt)}
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
