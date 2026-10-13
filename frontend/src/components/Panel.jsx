function Panel({ action, children, className = "", description, title }) {
  return (
    <section
      className={`rounded-lg border border-[#e0e7e1] bg-white shadow-[0_1px_2px_rgba(20,45,28,0.04)] ${className}`}
    >
      {(title || description || action) && (
        <div className="flex items-start justify-between gap-4 border-b border-[#edf1ed] px-5 py-4 sm:px-6">
          <div>
            {title && (
              <h2 className="text-sm font-semibold text-[#26372c]">{title}</h2>
            )}
            {description && (
              <p className="mt-1 text-xs leading-5 text-[#748078]">
                {description}
              </p>
            )}
          </div>
          {action}
        </div>
      )}
      {children}
    </section>
  );
}

export default Panel;
