import { useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, CircleUserRound, Settings2 } from "lucide-react";
import Button from "../../../components/Button.jsx";
import Field, { controlClassName } from "../../../components/Field.jsx";
import PageHeading from "../../../components/PageHeading.jsx";
import Panel from "../../../components/Panel.jsx";
import { useFinance } from "../../../hooks/useFinance.js";
import { useAuth } from "../../../hooks/useAuth.js";
import { getMonthOptions } from "../../../utils/finance.js";

function SettingsPage() {
  const {
    notify,
    preferences,
    selectedMonth,
    setPreferences,
    setSelectedMonth,
    transactions,
  } = useFinance();
  const { user } = useAuth();
  const [dashboardMonth, setDashboardMonth] = useState(
    preferences.dashboardMonth,
  );
  const monthOptions = getMonthOptions(transactions);

  function handleSubmit(event) {
    event.preventDefault();
    setPreferences({ ...preferences, dashboardMonth });
    setSelectedMonth(dashboardMonth);
    notify(
      "Dashboard month applied in this tab only. It is not saved to your account.",
      "info",
    );
  }

  return (
    <div className="space-y-6 sm:space-y-8">
      <PageHeading
        description="View your authenticated profile and adjust local dashboard preferences."
        eyebrow="Your workspace"
        title="Settings"
      />

      <div className="grid gap-5 xl:grid-cols-[1.2fr_.8fr]">
        <Panel
          description="Account identity comes from the server. Dashboard preferences remain local to this tab."
          title="Profile and display"
        >
          <form className="grid gap-5 p-5 sm:p-6" onSubmit={handleSubmit}>
            <div className="flex items-center gap-3 border-b border-[#edf1ed] pb-5">
              <span className="grid size-12 place-items-center rounded-full bg-[#dcebe0] text-emerald-900">
                <CircleUserRound aria-hidden="true" size={24} />
              </span>
              <div>
                <p className="text-sm font-semibold text-[#2c3b31]">
                  Verified account
                </p>
                <p className="mt-1 text-xs text-[#748078]">
                  This information is read-only in this phase.
                </p>
              </div>
            </div>

            <Field htmlFor="settings-display-name" label="Account name">
              <input
                className={controlClassName}
                id="settings-display-name"
                readOnly
                value={user.name}
              />
            </Field>
            <Field htmlFor="settings-email" label="Verified email">
              <input
                autoComplete="email"
                className={controlClassName}
                id="settings-email"
                readOnly
                value={user.email}
              />
            </Field>
            <Field
              hint="This only changes the dashboard summary month in this tab."
              htmlFor="settings-dashboard-month"
              label="Dashboard starting month"
            >
              <select
                className={controlClassName}
                id="settings-dashboard-month"
                onChange={(event) => setDashboardMonth(event.target.value)}
                value={dashboardMonth}
              >
                {monthOptions.map(({ label, value }) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </Field>
            <Field
              hint="Sample amounts are displayed in Indian rupees throughout this demo."
              htmlFor="settings-currency"
              label="Display currency"
            >
              <input
                className={controlClassName}
                id="settings-currency"
                readOnly
                value="Indian rupee (INR ₹)"
              />
            </Field>
            <div className="flex justify-end border-t border-[#edf1ed] pt-4">
              <Button icon={Settings2} type="submit">
                Apply locally
              </Button>
            </div>
          </form>
        </Panel>

        <div className="space-y-5">
          <Panel
            description="Your API session uses an HTTP-only cookie."
            title="Account access"
          >
            <div className="grid gap-3 p-5">
              <p className="text-sm leading-6 text-[#647168]">
                Password recovery starts with a one-time code sent to your
                verified email address.
              </p>
              <Link
                className="inline-flex items-center justify-between gap-3 rounded-md border border-[#dbe3dc] px-3 py-3 text-sm font-medium text-[#35453a] hover:bg-[#f6f9f6] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700"
                to="/login"
              >
                Password recovery <ArrowRight aria-hidden="true" size={16} />
              </Link>
            </div>
          </Panel>
          <Panel
            description="Your selected month is shared with the dashboard view."
            title="Current dashboard month"
          >
            <div className="p-5">
              <p className="text-2xl font-semibold text-[#26372c]">
                {monthOptions.find(({ value }) => value === selectedMonth)
                  ?.label || "No month selected"}
              </p>
              <p className="mt-2 text-xs leading-5 text-[#748078]">
                This affects only which mock transactions are summarized on the
                dashboard.
              </p>
            </div>
          </Panel>
        </div>
      </div>
    </div>
  );
}

export default SettingsPage;
