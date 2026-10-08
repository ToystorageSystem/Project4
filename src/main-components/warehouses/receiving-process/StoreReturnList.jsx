import {
    useMemo,
} from "react";

import Button
    from "../../../components/ui/Button";

import ReceivingStatusBadge
    from "./ReceivingStatusBadge";


export default function StoreReturnList({
                                            items = [],
                                            onView,
                                            keyword,
                                            setKeyword,
                                        }) {

    const filteredItems =
        useMemo(
            () => {

                const search =
                    keyword
                        .trim()
                        .toLowerCase();


                if (!search) {
                    return items;
                }


                return items.filter(
                    (
                        item
                    ) => {

                        const text = [
                            item?.returnCode,
                            item?.storeName,
                            item?.warehouseName,
                            item?.returnType,
                            item?.status,
                        ]
                            .filter(
                                Boolean
                            )
                            .join(
                                " "
                            )
                            .toLowerCase();


                        return text.includes(
                            search
                        );
                    }
                );
            },
            [
                items,
                keyword,
            ]
        );


    return (
        <div className="space-y-5">

            <div className="relative">

                <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400"
                >
                    <circle
                        cx="11"
                        cy="11"
                        r="7"
                        strokeWidth="2"
                    />

                    <path
                        strokeLinecap="round"
                        strokeWidth="2"
                        d="m20 20-4-4"
                    />
                </svg>


                <input
                    type="text"
                    value={keyword}
                    onChange={
                        (
                            event
                        ) =>
                            setKeyword(
                                event.target.value
                            )
                    }
                    placeholder="Search return code, store, status..."
                    className="w-full rounded-xl border border-slate-200 bg-white py-3 pl-12 pr-4 text-sm outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                />

            </div>


            {
                !filteredItems.length
                    ? (

                        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">

                            <p className="font-semibold text-slate-700">
                                No store returns
                            </p>

                            <p className="mt-2 text-sm text-slate-500">
                                There are no Store → Warehouse return receipts matching this view.
                            </p>

                        </div>

                    )
                    : (

                        <div className="grid gap-4 xl:grid-cols-2">

                            {
                                filteredItems.map(
                                    (
                                        item
                                    ) => {

                                        const received =
                                            item?.totalReceivedQuantity ?? 0;

                                        const approved =
                                            item?.totalApprovedQuantity ?? 0;

                                        const rejected =
                                            item?.totalRejectedQuantity ?? 0;


                                        return (
                                            <div
                                                key={
                                                    item?.returnId
                                                }
                                                className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
                                            >

                                                <div className="h-1 bg-[#f25d19]" />


                                                <div className="space-y-4 p-5">

                                                    <div className="flex items-start justify-between gap-4">

                                                        <div>

                                                            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                                                                Store Return
                                                            </p>

                                                            <h3 className="mt-1 text-lg font-semibold text-slate-900">
                                                                {
                                                                    item?.returnCode ??
                                                                    `#${item?.returnId ?? "-"}`
                                                                }
                                                            </h3>

                                                        </div>


                                                        <ReceivingStatusBadge
                                                            status={
                                                                item?.status
                                                            }
                                                        />

                                                    </div>


                                                    <div className="grid grid-cols-2 gap-3 rounded-xl bg-slate-50 p-4 text-sm">

                                                        <div>

                                                            <p className="text-xs text-slate-400">
                                                                From Store
                                                            </p>

                                                            <p className="mt-1 font-semibold text-slate-800">
                                                                {
                                                                    item?.storeName ??
                                                                    `Store #${item?.storeId ?? "-"}`
                                                                }
                                                            </p>

                                                        </div>


                                                        <div>

                                                            <p className="text-xs text-slate-400">
                                                                Return Type
                                                            </p>

                                                            <p className="mt-1 font-semibold text-slate-800">
                                                                {
                                                                    item?.returnType ??
                                                                    "-"
                                                                }
                                                            </p>

                                                        </div>

                                                    </div>


                                                    <div className="grid grid-cols-3 gap-3">

                                                        <div className="rounded-xl border border-slate-100 p-3">

                                                            <p className="text-xs text-slate-400">
                                                                Received
                                                            </p>

                                                            <p className="mt-1 text-lg font-bold text-slate-900">
                                                                {received}
                                                            </p>

                                                        </div>


                                                        <div className="rounded-xl border border-green-100 bg-green-50/50 p-3">

                                                            <p className="text-xs text-green-600">
                                                                Approved
                                                            </p>

                                                            <p className="mt-1 text-lg font-bold text-green-700">
                                                                {approved}
                                                            </p>

                                                        </div>


                                                        <div className="rounded-xl border border-red-100 bg-red-50/50 p-3">

                                                            <p className="text-xs text-red-600">
                                                                Rejected
                                                            </p>

                                                            <p className="mt-1 text-lg font-bold text-red-700">
                                                                {rejected}
                                                            </p>

                                                        </div>

                                                    </div>


                                                    <div className="flex justify-end border-t border-slate-100 pt-4">

                                                        <Button
                                                            onClick={() =>
                                                                onView(
                                                                    item
                                                                )
                                                            }
                                                        >
                                                            View return
                                                        </Button>

                                                    </div>

                                                </div>

                                            </div>
                                        );
                                    }
                                )
                            }

                        </div>

                    )
            }

        </div>
    );
}