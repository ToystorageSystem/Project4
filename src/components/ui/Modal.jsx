import { useEffect, useId, useRef } from "react";
export default function Modal({ open, title, children, footer, onClose }) {
  const ref = useRef(null);
  const id = useId();
  useEffect(() => {
    const dialog = ref.current;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);
  return (
    <dialog
      ref={ref}
      className="ops-dialog"
      aria-labelledby={id}
      onCancel={(e) => {
        e.preventDefault();
        onClose?.();
      }}
    >
      <div className="dialog-heading">
        <h2 id={id}>{title}</h2>
        <button type="button" onClick={onClose} aria-label="Close">
          ×
        </button>
      </div>
      <div className="dialog-body">{children}</div>
      {footer && <div className="dialog-footer">{footer}</div>}
    </dialog>
  );
}
