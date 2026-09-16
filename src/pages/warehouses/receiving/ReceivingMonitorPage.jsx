import {
    useCallback,
    useEffect,
    useRef,
    useState,
} from "react";

import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Button from "../../../components/ui/Button";
import Alert from "../../../components/ui/Alert";
import LoadingPage from "../../../components/feedback/LoadingPage";

import ReceivingMonitorList
    from "../../../main-components/warehouses/receiving-monitor/ReceivingMonitorList";

import ReceivingProgressDetail
    from "../../../main-components/warehouses/receiving-monitor/ReceivingProgressDetail";

import {
    getReceivingMonitoringList,
    getReceivingProgress,
} from "../../../api/receipts/receiving/receivingMonitor";

import {
    createReceivingSocket,
} from "../../../services/receivingSocket";


export default function ReceivingMonitorPage() {
    const [items, setItems] =
        useState([]);

    const [selected, setSelected] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [detailLoading, setDetailLoading] =
        useState(false);

    const [error, setError] =
        useState("");

    const [
        realtimeConnected,
        setRealtimeConnected,
    ] = useState(false);

    const [page, setPage] =
        useState(0);

    const [totalPages, setTotalPages] =
        useState(0);

    const [
        totalElements,
        setTotalElements,
    ] = useState(0);

    const [
        searchInput,
        setSearchInput,
    ] = useState("");

    const [keyword, setKeyword] =
        useState("");

    const pageSize = 12;

    const selectedReceiptIdRef =
        useRef(null);


    useEffect(() => {
        const timer =
            setTimeout(
                () => {
                    setPage(0);

                    setKeyword(
                        searchInput.trim()
                    );
                },
                400
            );

        return () =>
            clearTimeout(timer);
    }, [searchInput]);


    const loadList =
        useCallback(
            async ({
                       showLoading = true,
                   } = {}) => {
                try {
                    if (showLoading) {
                        setLoading(true);
                    }

                    setError("");

                    const data =
                        await getReceivingMonitoringList({
                            keyword,
                            page,
                            size: pageSize,
                        });

                    setItems(
                        data?.content ?? []
                    );

                    setTotalPages(
                        data?.totalPages ?? 0
                    );

                    setTotalElements(
                        data?.totalElements ?? 0
                    );

                } catch (e) {

                    setError(
                        e.response?.data?.message ??
                        "Unable to load receiving receipts."
                    );

                } finally {

                    if (showLoading) {
                        setLoading(false);
                    }
                }
            },
            [
                page,
                keyword,
            ]
        );


    const openReceipt =
        async (item) => {
            const id =
                item?.receiptId ??
                item?.id;

            if (!id) {
                return;
            }

            try {
                setDetailLoading(true);

                setError("");

                selectedReceiptIdRef.current =
                    id;

                const data =
                    await getReceivingProgress(
                        id
                    );

                setSelected(data);

            } catch (e) {

                setError(
                    e.response?.data?.message ??
                    "Unable to load receiving progress."
                );

            } finally {

                setDetailLoading(false);
            }
        };


    const backToList =
        async () => {
            selectedReceiptIdRef.current =
                null;

            setSelected(null);

            await loadList({
                showLoading: false,
            });
        };


    const clearSearch = () => {
        setSearchInput("");
        setKeyword("");
        setPage(0);
    };


    useEffect(() => {
        loadList();
    }, [loadList]);


    useEffect(() => {
        const client =
            createReceivingSocket({
                onConnected:
                    (
                        connectedClient
                    ) => {

                        setRealtimeConnected(
                            true
                        );

                        connectedClient.subscribe(
                            "/topic/receiving",
                            async () => {

                                await loadList({
                                    showLoading:
                                        false,
                                });

                                const id =
                                    selectedReceiptIdRef
                                        .current;

                                if (id) {
                                    try {
                                        setSelected(
                                            await getReceivingProgress(
                                                id
                                            )
                                        );
                                    } catch {
                                        // ignore
                                    }
                                }
                            }
                        );
                    },

                onDisconnected: () => {
                    setRealtimeConnected(
                        false
                    );
                },

                onError: () => {
                    setRealtimeConnected(
                        false
                    );
                },
            });

        client.activate();

        return () =>
            client.deactivate();

    }, [loadList]);


    return (
        <MainLayout>

            <div className="flex min-h-0 flex-1 flex-col gap-6">

                <PageHeader
                    title="Receiving - In Progress"
                    description="Warehouse Staff inspections currently in progress, including re-inspection attempts."
                    actions={
                        <Button
                            variant="outline"
                            onClick={() =>
                                loadList()
                            }
                        >
                            Refresh
                        </Button>
                    }
                />


                {!selected && (

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
                            value={searchInput}
                            onChange={(e) =>
                                setSearchInput(
                                    e.target.value
                                )
                            }
                            placeholder="Search receipt code or warehouse staff..."
                            className="w-full rounded-xl border border-slate-200 bg-white py-2.5 pl-12 pr-24 text-sm outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                        />


                        {searchInput && (

                            <button
                                type="button"
                                onClick={
                                    clearSearch
                                }
                                className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-semibold text-[#f25d19] hover:text-[#d94f12]"
                            >
                                Clear
                            </button>

                        )}

                    </div>

                )}


                {error && (
                    <Alert type="danger">
                        {error}
                    </Alert>
                )}


                {detailLoading && (
                    <Alert type="info">
                        Loading receiving receipt...
                    </Alert>
                )}


                {selected ? (

                    <ReceivingProgressDetail
                        data={selected}
                        onBack={backToList}
                    />

                ) : loading ? (

                    <LoadingPage />

                ) : (

                    <div className="flex min-h-0 flex-1 flex-col">

                        <ReceivingMonitorList
                            items={items}
                            onView={openReceipt}
                            page={page}
                            totalPages={
                                totalPages
                            }
                            totalElements={
                                totalElements
                            }
                            onPrevious={() =>
                                setPage(
                                    (value) =>
                                        Math.max(
                                            value - 1,
                                            0
                                        )
                                )
                            }
                            onNext={() =>
                                setPage(
                                    (value) =>
                                        Math.min(
                                            value + 1,
                                            Math.max(
                                                totalPages - 1,
                                                0
                                            )
                                        )
                                )
                            }
                            emptyTitle={
                                keyword
                                    ? "No matching receipts"
                                    : "No inspections in progress"
                            }
                            emptyDescription={
                                keyword
                                    ? `No receiving receipt matched "${keyword}".`
                                    : "There are no RECEIVING receipts currently being inspected."
                            }
                        />

                    </div>

                )}

            </div>

        </MainLayout>
    );
}