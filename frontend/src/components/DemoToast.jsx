import { useEffect } from "react";
import { AnimatePresence, motion } from "motion/react";
import { Check, CircleAlert, X } from "lucide-react";
import { useFinance } from "../hooks/useFinance.js";

function DemoToast() {
  const { setToast, toast } = useFinance();

  useEffect(() => {
    if (!toast) return undefined;
    const timeout = window.setTimeout(() => setToast(null), 3600);
    return () => window.clearTimeout(timeout);
  }, [setToast, toast]);

  return (
    <AnimatePresence>
      {toast && (
        <motion.div
          animate={{ opacity: 1, y: 0 }}
          aria-live={toast.tone === "error" ? "assertive" : "polite"}
          className="fixed bottom-24 right-4 z-80 flex max-w-[calc(100vw-2rem)] items-start gap-3 rounded-lg border border-[#dce5dd] bg-white p-4 shadow-lg sm:bottom-6 sm:right-6 sm:max-w-sm"
          exit={{ opacity: 0, y: 8 }}
          initial={{ opacity: 0, y: 8 }}
          role={toast.tone === "error" ? "alert" : "status"}
        >
          {toast.tone === "error" ? (
            <CircleAlert
              aria-hidden="true"
              className="mt-0.5 shrink-0 text-rose-700"
              size={18}
            />
          ) : (
            <Check
              aria-hidden="true"
              className="mt-0.5 shrink-0 text-emerald-700"
              size={18}
            />
          )}
          <p className="flex-1 text-sm leading-5 text-[#34453a]">
            {toast.message}
          </p>
          <button
            aria-label="Dismiss message"
            className="grid size-6 shrink-0 place-items-center rounded text-[#748078] hover:bg-[#edf2ed] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700"
            onClick={() => setToast(null)}
            type="button"
          >
            <X aria-hidden="true" size={15} />
          </button>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

export default DemoToast;
