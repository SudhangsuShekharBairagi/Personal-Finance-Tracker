export const controlClassName =
  "min-h-10 w-full rounded-md border border-[#d8e1d9] bg-white px-3 py-2 text-sm text-[#1d2b22] outline-none transition focus:border-emerald-700 focus:ring-2 focus:ring-emerald-700/15 disabled:bg-[#f3f6f3]";

function Field({ children, error, htmlFor, label, hint }) {
  return (
    <div className="grid gap-1.5">
      <label className="text-sm font-medium text-[#34453a]" htmlFor={htmlFor}>
        {label}
      </label>
      {children}
      {error ? (
        <p className="text-xs text-rose-700" id={`${htmlFor}-error`}>
          {error}
        </p>
      ) : hint ? (
        <p className="text-xs text-[#6a786e]">{hint}</p>
      ) : null}
    </div>
  );
}

export default Field;
