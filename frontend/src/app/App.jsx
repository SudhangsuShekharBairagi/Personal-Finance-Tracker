import { lazy, Suspense } from "react";
import { MotionConfig } from "motion/react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import DemoToast from "../components/DemoToast.jsx";
import RequireAuth from "./RequireAuth.jsx";
import AuthLayout from "../layouts/AuthLayout.jsx";
import AppShell from "../layouts/AppShell.jsx";
import { FinanceProvider } from "./FinanceProvider.jsx";
import { AuthProvider } from "./AuthProvider.jsx";

const AuthPage = lazy(
  () => import("../features/auth/pages/ConnectedAuthPage.jsx"),
);
const BudgetsPage = lazy(
  () => import("../features/budgets/pages/BudgetsPage.jsx"),
);
const DashboardPage = lazy(
  () => import("../features/dashboard/pages/DashboardPage.jsx"),
);
const LandingPage = lazy(
  () => import("../features/dashboard/pages/LandingPage.jsx"),
);
const RecurringPage = lazy(
  () => import("../features/recurring/pages/RecurringPage.jsx"),
);
const NotFoundPage = lazy(
  () => import("../features/settings/pages/NotFoundPage.jsx"),
);
const NotificationsPage = lazy(
  () => import("../features/notifications/pages/NotificationsPage.jsx"),
);
const SettingsPage = lazy(
  () => import("../features/settings/pages/SettingsPage.jsx"),
);
const TransactionsPage = lazy(
  () => import("../features/transactions/pages/TransactionsPage.jsx"),
);

function RouteLoading() {
  return (
    <div
      aria-live="polite"
      aria-busy="true"
      className="grid min-h-64 place-items-center text-sm text-[#68766c]"
      role="status"
    >
      Loading page…
    </div>
  );
}

function App() {
  return (
    <MotionConfig reducedMotion="user">
      <AuthProvider>
        <FinanceProvider>
          <BrowserRouter>
            <Suspense fallback={<RouteLoading />}>
              <Routes>
                <Route
                  element={<Navigate replace to="/dashboard" />}
                  path="/"
                />
                <Route element={<LandingPage />} path="/welcome" />
                <Route element={<AuthLayout />}>
                  <Route element={<AuthPage mode="login" />} path="/login" />
                  <Route
                    element={<AuthPage mode="register" />}
                    path="/register"
                  />
                  <Route
                    element={<AuthPage mode="verify" />}
                    path="/verify-email"
                  />
                  <Route
                    element={<AuthPage mode="forgot" />}
                    path="/forgot-password"
                  />
                  <Route
                    element={<AuthPage mode="reset" />}
                    path="/reset-password"
                  />
                </Route>
                <Route element={<RequireAuth />}>
                  <Route element={<AppShell />}>
                    <Route element={<DashboardPage />} path="/dashboard" />
                    <Route
                      element={<TransactionsPage />}
                      path="/transactions"
                    />
                    <Route element={<BudgetsPage />} path="/budgets" />
                    <Route
                      element={<NotificationsPage />}
                      path="/notifications"
                    />
                    <Route element={<RecurringPage />} path="/recurring" />
                    <Route element={<SettingsPage />} path="/settings" />
                  </Route>
                </Route>
                <Route element={<NotFoundPage />} path="*" />
              </Routes>
            </Suspense>
            <DemoToast />
          </BrowserRouter>
        </FinanceProvider>
      </AuthProvider>
    </MotionConfig>
  );
}

export default App;
