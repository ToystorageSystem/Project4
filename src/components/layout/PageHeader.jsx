export default function PageHeader({
    title,
    description,
    actions,
}) {
    return (
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                    <div className="mb-2 h-1 w-12 rounded-full bg-[#f25d19]" />

                    <h1 className="text-2xl font-bold tracking-tight text-slate-900 md:text-3xl">
                        {title}
                    </h1>

                    {description && (
                        <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-500">
                            {
                                description
                            }
                        </p>
                    )}
                </div>

                {actions && (
                    <div className="flex flex-wrap items-center gap-2">
                        {actions}
                    </div>
                )}
            </div>
        </div>
    );
}
