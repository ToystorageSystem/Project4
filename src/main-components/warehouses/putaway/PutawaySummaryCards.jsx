function SummaryCard({
                         title,
                         value,
                         description,
                         icon,
                     }) {
    return (
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">

            <div className="flex items-start justify-between gap-4">

                <div>
                    <p className="text-sm font-medium text-slate-500">
                        {title}
                    </p>

                    <p className="mt-2 text-3xl font-bold text-slate-900">
                        {value}
                    </p>

                    <p className="mt-1 text-xs text-slate-400">
                        {description}
                    </p>
                </div>


                <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff1e9] text-[#f25d19]">

                    {icon}

                </div>

            </div>

        </div>
    );
}


function BoxIcon() {
    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-5 w-5"
        >
            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M4 7 12 3l8 4-8 4-8-4Zm0 0v10l8 4 8-4V7M12 11v10"
            />
        </svg>
    );
}


function ClockIcon() {
    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-5 w-5"
        >
            <circle
                cx="12"
                cy="12"
                r="9"
                strokeWidth="2"
            />

            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M12 7v5l3 2"
            />
        </svg>
    );
}


function ProgressIcon() {
    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-5 w-5"
        >
            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M4 19V9m5 10V5m5 14v-7m5 7V3"
            />
        </svg>
    );
}


function CheckIcon() {
    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-5 w-5"
        >
            <circle
                cx="12"
                cy="12"
                r="9"
                strokeWidth="2"
            />

            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="m8 12 3 3 5-6"
            />
        </svg>
    );
}


export default function PutawaySummaryCards({
                                                tasks = [],
                                            }) {
    const total =
        tasks.length;


    const available =
        tasks.filter(
            (task) =>
                task?.status === "AVAILABLE" ||
                task?.status === "PENDING"
        ).length;


    const inProgress =
        tasks.filter(
            (task) =>
                task?.status === "IN_PROGRESS"
        ).length;


    const completed =
        tasks.filter(
            (task) =>
                task?.status === "COMPLETED"
        ).length;


    return (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">

            <SummaryCard
                title="Total Tasks"
                value={total}
                description="All putaway tasks"
                icon={<BoxIcon />}
            />

            <SummaryCard
                title="Waiting"
                value={available}
                description="Waiting for staff"
                icon={<ClockIcon />}
            />

            <SummaryCard
                title="In Progress"
                value={inProgress}
                description="Currently being put away"
                icon={<ProgressIcon />}
            />

            <SummaryCard
                title="Completed"
                value={completed}
                description="Finished putaway tasks"
                icon={<CheckIcon />}
            />

        </div>
    );
}