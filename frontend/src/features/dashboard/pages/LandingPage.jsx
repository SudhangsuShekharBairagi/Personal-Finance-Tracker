import { motion } from "motion/react";
import { ArrowDownRight, ArrowUpRight, CircleDollarSign } from "lucide-react";
import { Link } from "react-router-dom";

const foundations = [
  {
    name: "Transactions",
    detail: "Track income, expenses, and recent activity securely.",
    icon: ArrowUpRight,
  },
  {
    name: "Budgets",
    detail: "Monitor category limits and warning thresholds.",
    icon: ArrowDownRight,
  },
  {
    name: "Recurring",
    detail: "Create due schedules that generate real saved records.",
    icon: ArrowUpRight,
  },
];

function LandingPage() {
  return (
    <main className="min-h-screen bg-[#f6f8f5] text-[#17251d]">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-6 py-6 md:px-10">
        <a
          href="/"
          className="flex items-center gap-3"
          aria-label="Personal Finance Tracker home"
        >
          <span className="grid size-10 place-items-center rounded-sm bg-emerald-800 text-white">
            <CircleDollarSign aria-hidden="true" size={22} />
          </span>
          <span className="text-sm font-semibold tracking-wide">
            PERSONAL FINANCE TRACKER
          </span>
        </a>
        <div className="flex items-center gap-3">
          <Link
            className="rounded-md border border-[#d7dfd7] px-3 py-1.5 text-xs font-medium text-[#526258] transition-colors hover:bg-white"
            to="/login"
          >
            Log in
          </Link>
          <Link
            className="rounded-md bg-emerald-800 px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-emerald-700"
            to="/register"
          >
            Create account
          </Link>
        </div>
      </header>

      <motion.section
        className="mx-auto grid max-w-6xl gap-14 px-6 pb-20 pt-16 md:px-10 md:pb-28 md:pt-24 lg:grid-cols-[1.2fr_0.8fr] lg:items-end"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, ease: "easeOut" }}
      >
        <div>
          <p className="mb-5 text-xs font-semibold tracking-[0.12em] text-emerald-800">
            PERSONAL FINANCE MANAGEMENT
          </p>
          <h1 className="max-w-2xl text-5xl font-semibold leading-[1.05] md:text-6xl">
            Stay on top of what you earn, spend, and save.
          </h1>
          <p className="mt-6 max-w-xl text-lg leading-8 text-[#526258]">
            Track monthly budgets, review recent spending, manage recurring
            payments, and keep your account data organized in one place.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              className="rounded-md bg-emerald-800 px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-emerald-700"
              to="/register"
            >
              Get started
            </Link>
            <Link
              className="rounded-md border border-[#d7dfd7] bg-white px-5 py-3 text-sm font-semibold text-[#2d3d35] transition-colors hover:bg-[#f5f8f5]"
              to="/login"
            >
              Sign in
            </Link>
          </div>
        </div>

        <div className="border-t border-[#cfd8cf]">
          {foundations.map(({ name, detail, icon: Icon }) => (
            <div
              key={name}
              className="flex items-center justify-between gap-5 border-b border-[#cfd8cf] py-5"
            >
              <div>
                <h2 className="text-sm font-semibold">{name}</h2>
                <p className="mt-1 text-sm text-[#657269]">{detail}</p>
              </div>
              <Icon
                aria-hidden="true"
                className="shrink-0 text-emerald-800"
                size={18}
              />
            </div>
          ))}
        </div>
      </motion.section>
    </main>
  );
}

export default LandingPage;
