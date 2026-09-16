export default function InfoField({
                                      label,
                                      value,
                                  }) {

    return (
        <div>

            <div
                className="
                    text-xs
                    font-semibold
                    uppercase
                    tracking-wide
                    text-slate-400
                "
            >
                {label}
            </div>


            <div
                className="
                    mt-1
                    text-sm
                    font-medium
                    text-slate-800
                "
            >
                {value ?? "-"}
            </div>

        </div>
    );
}