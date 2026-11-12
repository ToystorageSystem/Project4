import { useState } from "react";
import { createRoot } from "react-dom/client";
import Modal from "../components/ui/Modal";

/**
 * In-app confirmation popup that replaces the browser's window.confirm ("localhost says …").
 * Usage:  if (!(await confirmDialog("Submit this task?"))) return;
 * Options: { title, confirmLabel, cancelLabel, danger }
 * Resolves true when the user confirms, false when they cancel / close.
 */
export function confirmDialog(message, options = {}) {
    const host = document.createElement("div");
    document.body.appendChild(host);
    const root = createRoot(host);
    return new Promise((resolve) => {
        const finish = (value) => {
            resolve(value);
            // let the <dialog> close before unmounting
            setTimeout(() => {
                root.unmount();
                host.remove();
            }, 0);
        };
        root.render(<ConfirmPopup message={message} options={options} onDone={finish} />);
    });
}

function ConfirmPopup({ message, options, onDone }) {
    const [open, setOpen] = useState(true);
    const close = (value) => {
        setOpen(false);
        onDone(value);
    };
    return (
        <Modal
            open={open}
            title={options.title || "Please confirm"}
            onClose={() => close(false)}
            footer={
                <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
                    <button type="button" className="ops-button secondary" onClick={() => close(false)}>
                        {options.cancelLabel || "Cancel"}
                    </button>
                    <button
                        type="button"
                        className="ops-button"
                        style={options.danger ? { background: "#b91c1c", borderColor: "#b91c1c" } : undefined}
                        autoFocus
                        onClick={() => close(true)}
                    >
                        {options.confirmLabel || "Confirm"}
                    </button>
                </div>
            }
        >
            <p style={{ margin: 0, whiteSpace: "pre-line", lineHeight: 1.5 }}>{message}</p>
        </Modal>
    );
}
