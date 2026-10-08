export default function ReceivingSourceCard({
                                                title,
                                                description,
                                                active,
                                                onClick,
                                                icon,
                                            }) {

    return (
        <button
            type="button"
            onClick={onClick}
            className={
                "rounded-2xl border p-5 text-left transition " +

                (
                    active
                        ? "border-[#f25d19] bg-[#fff8f4] shadow-sm"
                        : "border-slate-200 bg-white hover:border-orange-200 hover:bg-orange-50/30"
                )
            }
        >

            <div className="flex items-start gap-4">

                <div
                    className={
                        "flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-2xl " +

                        (
                            active
                                ? "bg-[#f25d19] text-white"
                                : "bg-slate-100 text-slate-500"
                        )
                    }
                >
                    {icon}
                </div>


                <div>

                    <h3 className="font-semibold text-slate-900">
                        {title}
                    </h3>

                    <p className="mt-1 text-sm leading-6 text-slate-500">
                        {description}
                    </p>

                </div>

            </div>

        </button>
    );
}