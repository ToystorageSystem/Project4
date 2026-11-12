import { useMemo, useState } from "react";
import Input from "./Input";
import Select from "./Select";

export default function SearchableSelect({ label, searchPlaceholder = "Search by name or code", options = [], value, disabled = false, onChange, ...rest }) {
    const [keyword, setKeyword] = useState("");
    const filtered = useMemo(() => {
        const query = keyword.trim().toLowerCase();
        return options.filter((option) =>
            !query || option.value === "" || String(option.value) === String(value)
            || String(option.label || "").toLowerCase().includes(query)
            || String(option.value ?? "").toLowerCase().includes(query)
        );
    }, [options, keyword, value]);

    return (
        <div className="space-y-2">
            <Input
                label={`Search ${String(label).replaceAll("*", "").trim()}`}
                placeholder={searchPlaceholder}
                value={keyword}
                disabled={disabled}
                onChange={(event) => setKeyword(event.target.value)}
            />
            <Select label={label} options={filtered} value={value} disabled={disabled} onChange={onChange} {...rest} />
        </div>
    );
}
