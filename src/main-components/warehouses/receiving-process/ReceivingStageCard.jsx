export default function ReceivingStageCard({
                                               title,
                                               description,
                                               count = 0,
                                               active = false,
                                               onClick,
                                               icon,
                                           }) {

    return (
        <button
            type="button"
            onClick={onClick}
            className={
                "w-full rounded-2xl border bg-white p-4 text-left transition " +

                (
                    active
                        ? "border-[#f25d19] bg-[#fff8f4] ring-2 ring-[#f25d19]/10"
                        : "border-slate-200 hover:-translate-y-0.5 hover:border-orange-200 hover:shadow-md"
                )
            }
        >

            <div className="flex items-start justify-between gap-3">

                <div className="flex min-w-0 items-start gap-3">

                    <div
                        className={
                            "flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl " +

                            (
                                active
                                    ? "bg-[#f25d19] text-white"
                                    : "bg-[#fff1e9] text-[#f25d19]"
                            )
                        }
                    >
                        {icon}
                    </div>


                    <div className="min-w-0">

                        <p className="font-semibold text-slate-900">
                            {title}
                        </p>

                        <p className="mt-1 text-xs leading-5 text-slate-500">
                            {description}
                        </p>

                    </div>

                </div>


                <div className="flex h-8 min-w-8 flex-shrink-0 items-center justify-center rounded-full bg-slate-100 px-2 text-sm font-bold text-slate-700">
                    {count}
                </div>

            </div>

        </button>
    );
}