import { useEffect, useId, useMemo, useRef, useState } from "react";

// O chon co goi y: go de loc danh sach, nhung chi nhan gia tri co trong options.
// options: [{ value, label }], value: string ("" = chua chon)
export default function ComboboxSelect({
    label, options = [], value = "", onChange, error, disabled = false,
    placeholder = "Type to search...", emptyLabel = "", noMatchText = "No matching results.",
}) {
    const listId = useId();
    const inputRef = useRef(null);
    const selected = options.find((option) => String(option.value) === String(value));
    const [text, setText] = useState(selected?.label || "");
    const [open, setOpen] = useState(false);
    const [active, setActive] = useState(0);

    // Dong bo text khi gia tri duoc chon tu ben ngoai (vd: vua them brand moi)
    useEffect(() => {
        if (!open) setText(selected?.label || "");
    }, [selected?.label, open]);

    const suggestions = useMemo(() => {
        const term = text.trim().toLocaleLowerCase();
        // Dang hien dung label cua muc da chon thi show het danh sach
        const showAll = !term || (selected && text === selected.label);
        const rows = showAll ? options : options.filter((option) => option.label.toLocaleLowerCase().includes(term));
        return emptyLabel ? [{ value: "", label: emptyLabel }, ...rows] : rows;
    }, [options, text, selected, emptyLabel]);

    const choose = (option) => {
        onChange?.(String(option.value));
        setText(option.value === "" ? "" : option.label);
        setOpen(false);
    };

    const openList = () => {
        if (disabled) return;
        setOpen(true);
        setActive(Math.max(0, suggestions.findIndex((option) => String(option.value) === String(value))));
    };

    // Roi khoi o ma khong chon: tra ve muc dang chon (khong cho nhap tu do)
    const handleBlur = () => setTimeout(() => {
        setOpen(false);
        setText(selected?.label || "");
    }, 120);

    const handleKeyDown = (event) => {
        if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();
            if (!open) { openList(); return; }
            const step = event.key === "ArrowDown" ? 1 : -1;
            setActive((current) => (current + step + suggestions.length) % Math.max(suggestions.length, 1));
        } else if (event.key === "Enter") {
            if (open && suggestions[active]) {
                event.preventDefault();
                choose(suggestions[active]);
            }
        } else if (event.key === "Escape") {
            setOpen(false);
            setText(selected?.label || "");
        }
    };

    return (
        <div className="relative space-y-1.5">
            {label && <label htmlFor={`${listId}-input`} className="block text-sm font-medium text-slate-700">{label}</label>}
            <div className="relative">
                <input
                    id={`${listId}-input`}
                    ref={inputRef}
                    type="text"
                    role="combobox"
                    aria-expanded={open}
                    aria-controls={`${listId}-list`}
                    aria-autocomplete="list"
                    aria-invalid={Boolean(error)}
                    autoComplete="off"
                    disabled={disabled}
                    placeholder={placeholder}
                    value={text}
                    onFocus={(event) => { event.target.select(); openList(); }}
                    onChange={(event) => { setText(event.target.value); setOpen(true); setActive(0); }}
                    onKeyDown={handleKeyDown}
                    onBlur={handleBlur}
                    className={`w-full rounded-xl border bg-white px-3 py-2.5 pr-9 text-sm text-slate-900 outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/20 disabled:bg-slate-50 ${error ? "border-red-500" : "border-slate-300"}`}
                />
                <button type="button" tabIndex={-1} aria-label="Show options" disabled={disabled}
                    className="absolute inset-y-0 right-0 flex w-9 items-center justify-center text-slate-400"
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => { if (open) setOpen(false); else inputRef.current?.focus(); }}>
                    ▾
                </button>
            </div>
            {open && !disabled && (
                <ul id={`${listId}-list`} role="listbox"
                    className="absolute z-[1100] mt-1 max-h-60 w-full overflow-y-auto rounded-xl border border-slate-200 bg-white p-1 shadow-lg">
                    {suggestions.map((option, index) => (
                        <li key={option.value || "__empty"} role="option"
                            aria-selected={String(option.value) === String(value)}
                            className={`cursor-pointer rounded-lg px-3 py-2 text-sm ${index === active ? "bg-orange-50 text-[#c94810]" : "text-slate-700"} ${String(option.value) === String(value) ? "font-semibold" : ""} ${option.value === "" ? "italic text-slate-500" : ""}`}
                            onMouseDown={(event) => event.preventDefault()}
                            onMouseEnter={() => setActive(index)}
                            onClick={() => choose(option)}>
                            {option.label}
                        </li>
                    ))}
                    {!suggestions.some((option) => option.value !== "") && (
                        <li className="px-3 py-2 text-sm text-slate-500">{noMatchText}</li>
                    )}
                </ul>
            )}
            {error && <span className="text-xs text-red-600">{error}</span>}
        </div>
    );
}
