import {
    useState,
} from "react";

import MainLayout
    from "../../../components/layout/MainLayout";

import PageHeader
    from "../../../components/layout/PageHeader";

import ReceivingSourceCard
    from "../../../main-components/warehouses/receiving-process/ReceivingSourceCard";

import SupplierReceiving
    from "../../../main-components/warehouses/receiving-process/SupplierReceiving";

import StoreReceiving
    from "../../../main-components/warehouses/receiving-process/StoreReceiving";


export default function ReceivingProcessPage() {

    const [source, setSource] =
        useState("SUPPLIER");


    return (
        <MainLayout>

            <div className="flex min-h-0 flex-1 flex-col gap-6">

                <PageHeader
                    title="Receiving Process"
                    description="Track receipts waiting for inspection, under inspection, requiring action, being handled, and completed."
                />


                <div className="grid gap-4 md:grid-cols-2">

                    <ReceivingSourceCard
                        title="From Supplier (NCC)"
                        description="Purchase Order / Supplier → Warehouse receiving process."
                        active={
                            source === "SUPPLIER"
                        }
                        onClick={() =>
                            setSource("SUPPLIER")
                        }
                        icon={
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                className="h-6 w-6"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth="2"
                                    d="M3 7h13v10H3V7Zm13 3h3l2 3v4h-5v-7"
                                />
                            </svg>
                        }
                    />


                    <ReceivingSourceCard
                        title="From Store"
                        description="Store → Warehouse return receiving process."
                        active={
                            source === "STORE"
                        }
                        onClick={() =>
                            setSource("STORE")
                        }
                        icon={
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                className="h-6 w-6"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth="2"
                                    d="M3 21h18M5 21V7l7-4 7 4v14M9 12h6M12 9v6"
                                />
                            </svg>
                        }
                    />

                </div>


                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">

                    {
                        source === "SUPPLIER"
                            ? (
                                <SupplierReceiving />
                            )
                            : (
                                <StoreReceiving />
                            )
                    }

                </div>

            </div>

        </MainLayout>
    );
}