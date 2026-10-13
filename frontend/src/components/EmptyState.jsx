import { Inbox } from "lucide-react";

function EmptyState({ action, description, title }) {
  return (
    <div className="flex min-h-52 flex-col items-center justify-center px-6 py-10 text-center">
      <span className="grid size-11 place-items-center rounded-full bg-[#edf3ee] text-emerald-800">
        <Inbox aria-hidden="true" size={21} />
      </span>
      <h3 className="mt-4 text-sm font-semibold text-[#2a392f]">{title}</h3>
      <p className="mt-1 max-w-sm text-sm leading-6 text-[#6c7970]">
        {description}
      </p>
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export default EmptyState;
