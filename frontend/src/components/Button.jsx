const variants = {
  primary:
    "bg-emerald-800 text-white hover:bg-emerald-900 focus-visible:ring-emerald-700",
  secondary:
    "border border-[#dbe3dc] bg-white text-[#26372c] hover:bg-[#f5f8f5] focus-visible:ring-emerald-700",
  ghost: "text-[#526258] hover:bg-[#edf2ed] focus-visible:ring-emerald-700",
  danger:
    "bg-rose-700 text-white hover:bg-rose-800 focus-visible:ring-rose-700",
};

function Button({
  children,
  className = "",
  icon: Icon,
  type = "button",
  variant = "primary",
  ...props
}) {
  return (
    <button
      className={`inline-flex min-h-10 items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 ${variants[variant]} ${className}`}
      type={type}
      {...props}
    >
      {Icon && <Icon aria-hidden="true" size={17} />}
      {children}
    </button>
  );
}

export default Button;
