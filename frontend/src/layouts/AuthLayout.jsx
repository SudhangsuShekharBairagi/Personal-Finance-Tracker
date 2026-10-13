import { Link, Outlet } from "react-router-dom";
import { motion } from "motion/react";
import { CircleDollarSign, ShieldCheck } from "lucide-react";

function AuthLayout() {
  return (
    <motion.div
      animate={{ opacity: 1 }}
      className="min-h-screen bg-white text-[#1d2b22]"
      initial={{ opacity: 0 }}
      transition={{ duration: 0.25 }}
    >
      <header className="flex h-16 items-center justify-between border-b border-[#e6ece6] px-5 sm:px-8">
        <Link className="flex items-center gap-2.5" to="/dashboard">
          <span className="grid size-9 place-items-center rounded-md bg-emerald-800 text-white">
            <CircleDollarSign aria-hidden="true" size={20} />
          </span>
          <span className="text-sm font-bold">Pocketwise</span>
        </Link>
        <Link
          className="text-sm font-medium text-emerald-800 hover:text-emerald-950"
          to="/dashboard"
        >
          Back to dashboard
        </Link>
      </header>

      <div className="grid min-h-[calc(100vh-4rem)] lg:grid-cols-[minmax(0,1fr)_minmax(420px,.9fr)]">
        <aside className="hidden flex-col justify-between bg-[#163c2b] px-12 py-14 text-white lg:flex xl:px-20">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[#b4d8c2]">
              Personal Finance Tracker
            </p>
            <h1 className="mt-8 max-w-xl text-5xl font-semibold leading-tight">
              Build a clearer picture of your money.
            </h1>
            <p className="mt-5 max-w-lg text-base leading-7 text-[#d2e3d7]">
              A small learning project for exploring everyday finance workflows
              in a calm, organized space.
            </p>
          </div>
          <div className="max-w-lg border-t border-white/20 pt-5">
            <div className="flex items-start gap-3">
              <ShieldCheck
                aria-hidden="true"
                className="mt-0.5 shrink-0 text-[#b4d8c2]"
                size={19}
              />
              <p className="text-sm leading-6 text-[#d2e3d7]">
                Account verification and password recovery require a configured
                email service. Session cookies are HTTP-only; state-changing
                requests include CSRF protection.
              </p>
            </div>
          </div>
        </aside>

        <main className="flex items-center justify-center px-5 py-10 sm:px-10 lg:px-12">
          <Outlet />
        </main>
      </div>
    </motion.div>
  );
}

export default AuthLayout;
