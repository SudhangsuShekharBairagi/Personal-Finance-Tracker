import { useEffect, useId, useRef } from "react";
import { X } from "lucide-react";

function Modal({ children, onClose, open, title, description }) {
  const dialogRef = useRef(null);
  const dialogId = useId().replaceAll(":", "");

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      aria-describedby={description ? `${dialogId}-description` : undefined}
      aria-labelledby={`${dialogId}-title`}
      className="m-auto max-h-[90dvh] w-[min(92vw,34rem)] overflow-y-auto rounded-lg border border-[#dce5dd] bg-white p-0 text-[#1d2b22] shadow-2xl backdrop:bg-[#13251a]/45"
      onCancel={onClose}
      onClose={onClose}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
      ref={dialogRef}
    >
      <div className="flex items-start justify-between gap-5 border-b border-[#e7ece7] px-5 py-4 sm:px-6">
        <div>
          <h2 className="text-lg font-semibold" id={`${dialogId}-title`}>
            {title}
          </h2>
          {description && (
            <p
              className="mt-1 text-sm text-[#68766c]"
              id={`${dialogId}-description`}
            >
              {description}
            </p>
          )}
        </div>
        <button
          aria-label="Close dialog"
          className="grid size-9 shrink-0 place-items-center rounded-md text-[#69766d] hover:bg-[#f0f4f0] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700"
          onClick={onClose}
          type="button"
        >
          <X aria-hidden="true" size={18} />
        </button>
      </div>
      <div className="px-5 py-5 sm:px-6">{children}</div>
    </dialog>
  );
}

export default Modal;
