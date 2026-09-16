import MainLayout
    from "../../../components/layout/MainLayout";

import PageHeader
    from "../../../components/layout/PageHeader";


function DashboardCard({
                           title,
                           description,
                           icon,
                       }) {
    return (
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">

            <div className="flex items-start gap-4">

                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-[#fff1e9] text-[#f25d19]">
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

        </div>
    );
}


const Icon = ({
                  children,
              }) => (
    <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        className="h-6 w-6"
    >
        {children}
    </svg>
);


export default function WarehouseDashboardPage() {
    return (
        <MainLayout>
            <div className="space-y-6">

                <PageHeader
                    title="Warehouse Dashboard"
                    description="Overview of warehouse receiving operations."
                />

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">

                    <DashboardCard
                        title="Receiving"
                        description="Monitor goods currently being received."
                        icon={
                            <Icon>
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth="2"
                                    d="M3 7h13v10H3V7m13-7 5 5v12h-5"
                                />
                            </Icon>
                        }
                    />

                    <DashboardCard
                        title="Waiting Review"
                        description="Review completed warehouse inspections."
                        icon={
                            <Icon>
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth="2"
                                    d="m5 12 4 4L19 6"
                                />
                            </Icon>
                        }
                    />

                    <DashboardCard
                        title="Completed"
                        description="View completed receiving history."
                        icon={
                            <Icon>
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
                            </Icon>
                        }
                    />

                    <DashboardCard
                        title="Discrepancies"
                        description="View receiving discrepancy reports."
                        icon={
                            <Icon>
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth="2"
                                    d="M12 3 2 21h20L12 3Z"
                                />

                                <path
                                    strokeLinecap="round"
                                    strokeWidth="2"
                                    d="M12 9v5"
                                />
                            </Icon>
                        }
                    />

                </div>

            </div>
        </MainLayout>
    );
}