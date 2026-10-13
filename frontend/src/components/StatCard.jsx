function StatCard({ helper, icon: Icon, label, tone = "emerald", value }) {
  const tones = {
    emerald: "bg-emerald-50 text-emerald-800",
    cyan: "bg-cyan-50 text-cyan-800",
    rose: "bg-rose-50 text-rose-800",
    amber: "bg-amber-50 text-amber-800",
  };

  return (
    <article className="rounded-lg border border-[#e0e7e1] bg-white p-4 shadow-[0_1px_2px_rgba(20,45,28,0.04)] sm:p-5">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-medium text-[#68766c]">{label}</p>
        <span
          className={`grid size-9 place-items-center rounded-md ${tones[tone]}`}
        >
          <Icon aria-hidden="true" size={18} />
        </span>
      </div>
      <p className="mt-4 break-words text-2xl font-semibold tabular-nums text-[#1b2a20]">
        {value}
      </p>
      <p className="mt-1 text-xs leading-5 text-[#758178]">{helper}</p>
    </article>
  );
}

export default StatCard;
