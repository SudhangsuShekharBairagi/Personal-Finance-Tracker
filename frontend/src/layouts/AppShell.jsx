import { NavLink, Outlet, Link } from "react-router-dom";
import {
  ArrowLeftRight,
  Bell,
  LayoutDashboard,
  LogOut,
  PiggyBank,
  Repeat,
  Settings,
  WalletCards,
} from "lucide-react";
import { useFinance } from "../hooks/useFinance.js";
import { useAuth } from "../hooks/useAuth.js";
import { getApiErrorMessage } from "../services/api.js";

const navigation = [
  { label: "Dashboard", to: "/dashboard", icon: LayoutDashboard },
  { label: "Transactions", to: "/transactions", icon: ArrowLeftRight },
  { label: "Budgets", to: "/budgets", icon: PiggyBank },
  { label: "Notifications", to: "/notifications", icon: Bell },
  { label: "Recurring", to: "/recurring", icon: Repeat },
  { label: "Settings", to: "/settings", icon: Settings },
];

function NavigationLink({ item, mobile = false }) {
  const Icon = item.icon;
  return (
    <NavLink
      aria-label={mobile ? item.label : undefined}
      className={({ isActive }) =>
        mobile
          ? `flex min-w-0 flex-1 flex-col items-center gap-1 rounded-md px-1 py-2 text-[10px] font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700 ${isActive ? "text-emerald-800" : "text-[#758178]"}`
          : `flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700 ${isActive ? "bg-emerald-50 text-emerald-900" : "text-[#5b6a60] hover:bg-[#f1f5f1] hover:text-[#1d2b22]"}`
      }
      to={item.to}
    >
      <Icon aria-hidden="true" size={mobile ? 19 : 18} />
      <span className={mobile ? "truncate" : ""}>{item.label}</span>
    </NavLink>
  );
}

function AppShell() {
  const { notify } = useFinance();
  const { logout, user } = useAuth();
  const initial = user.name.trim().charAt(0).toUpperCase() || "A";

  async function handleLogout() {
    try {
      await logout();
      notify("You have signed out.");
    } catch (error) {
      notify(
        getApiErrorMessage(error, "Sign out could not be completed."),
        "error",
      );
    }
  }

  return (
    <div className="min-h-screen bg-[#f5f7f5] text-[#1d2b22]">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-[#e1e8e2] bg-white px-4 py-5 lg:flex">
        <Link className="flex items-center gap-3 px-2" to="/dashboard">
          <span className="grid size-10 place-items-center rounded-md bg-emerald-800 text-white">
            <WalletCards aria-hidden="true" size={21} />
          </span>
          <span>
            <span className="block text-sm font-bold text-[#1b2a20]">
              Pocketwise
            </span>
            <span className="block text-xs text-[#718076]">
              Personal finance
            </span>
          </span>
        </Link>

        <p className="mb-2 mt-10 px-3 text-[10px] font-semibold uppercase tracking-[0.12em] text-[#8a958d]">
          Workspace
        </p>
        <nav aria-label="Main navigation" className="grid gap-1">
          {navigation.map((item) => (
            <NavigationLink item={item} key={item.to} />
          ))}
        </nav>

        <div className="mt-auto border-t border-[#e8ede8] pt-4">
          <div className="mb-4 flex items-center gap-3 px-2">
            <span className="grid size-9 place-items-center rounded-full bg-[#dcebe0] text-sm font-semibold text-emerald-900">
              {initial}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-medium">
                {user.name}
              </span>
              <span className="block truncate text-xs text-[#78847b]">
                {user.email}
              </span>
            </span>
          </div>
          <button
            className="flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-sm text-[#5b6a60] hover:bg-[#f1f5f1] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700"
            onClick={handleLogout}
            type="button"
          >
            <LogOut aria-hidden="true" size={18} />
            Sign out
          </button>
        </div>
      </aside>

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex min-h-16 items-center justify-between gap-3 border-b border-[#e1e8e2] bg-white/95 px-4 backdrop-blur sm:px-7">
          <Link
            aria-label="Pocketwise dashboard"
            className="flex items-center gap-2 lg:hidden"
            to="/dashboard"
          >
            <span className="grid size-8 place-items-center rounded-md bg-emerald-800 text-white">
              <WalletCards aria-hidden="true" size={17} />
            </span>
            <span className="text-sm font-bold">Pocketwise</span>
          </Link>
          <p className="hidden text-sm text-[#657269] lg:block">
            Your money, organized with clarity.
          </p>
          <div className="ml-auto flex items-center gap-3">
            <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-[11px] font-semibold text-amber-900">
              <span
                aria-hidden="true"
                className="size-1.5 rounded-full bg-amber-600"
              />
              DEMO DATA
            </span>
            <Link
              aria-label={`Settings for ${user.name}`}
              className="grid size-9 place-items-center rounded-full bg-[#dcebe0] text-sm font-semibold text-emerald-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700"
              to="/settings"
            >
              {initial}
            </Link>
          </div>
        </header>

        <main className="mx-auto min-h-[calc(100vh-4rem)] max-w-screen-2xl px-4 pb-28 pt-6 sm:px-7 sm:pt-8 lg:px-9">
          <Outlet />
        </main>
      </div>

      <nav
        aria-label="Mobile navigation"
        className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-5 border-t border-[#dfe7e0] bg-white/95 px-1 pb-[env(safe-area-inset-bottom)] pt-1 backdrop-blur lg:hidden"
      >
        {navigation.map((item) => (
          <NavigationLink item={item} key={item.to} mobile />
        ))}
      </nav>
    </div>
  );
}

export default AppShell;
