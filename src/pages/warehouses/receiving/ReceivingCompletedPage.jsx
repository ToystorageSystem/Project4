import {
    useCallback,
    useEffect,
    useState,
} from "react";

import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Button from "../../../components/ui/Button";
import Alert from "../../../components/ui/Alert";
import LoadingPage from "../../../components/feedback/LoadingPage";

import ReceivingMonitorList
    from "../../../main-components/warehouses/receiving-monitor/ReceivingMonitorList";

import ReceivingReviewDetail
    from "../../../main-components/warehouses/receiving-monitor/ReceivingReviewDetail";

import {
    getCompletedReceivingList,
} from "../../../api/receipts/receiving/receivingMonitor";

import {
    getReceivingInspectionResult,
} from "../../../api/receipts/receiving/receivingConfirmation";

export default function ReceivingCompletedPage() {
    const [items, setItems] = useState([]);
    const [selected, setSelected] = useState(null);

    const [loading, setLoading] = useState(true);
    const [detailLoading, setDetailLoading] = useState(false);

    const [error, setError] = useState("");

    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);

    const [searchInput, setSearchInput] = useState("");
    const [keyword, setKeyword] = useState("");

    const pageSize = 20;

    useEffect(() => {
        const timer = setTimeout(() => {
            setPage(0);
            setKeyword(searchInput.trim());
        }, 400);

        return () => clearTimeout(timer);
    }, [searchInput]);

    const loadList = useCallback(
        async ({ showLoading = true } = {}) => {
            try {
                if (showLoading) {
                    setLoading(true);
                }

                setError("");

                const data =
                    await getCompletedReceivingList({
                        keyword,
                        page,
                        size: pageSize,
                    });

                setItems(data?.content ?? []);
                setTotalPages(data?.totalPages ?? 0);
                setTotalElements(data?.totalElements ?? 0);
            } catch (e) {
                setError(
                    e.response?.data?.message ??
                    "Unable to load completed receiving receipts."
                );
            } finally {
                if (showLoading) {
                    setLoading(false);
                }
            }
        },
        [page, keyword]
    );

    const clearSearch = () => {
        setSearchInput("");
        setKeyword("");
        setPage(0);
    };

    const openReceipt = async (item) => {
        const id =
            item?.receiptId ??
            item?.id;

        if (!id) return;

        try {
            setDetailLoading(true);
            setError("");

            setSelected(
                await getReceivingInspectionResult(id)
            );
        } catch (e) {
            setError(
                e.response?.data?.message ??
                "Unable to load completed receiving result."
            );
        } finally {
            setDetailLoading(false);
        }
    };

    const backToList = async () => {
        setSelected(null);

        await loadList({
            showLoading: false,
        });
    };

    useEffect(() => {
        loadList();
    }, [loadList]);

    return (
        <MainLayout>

            <div className="flex min-h-0 flex-1 flex-col gap-6">

                <PageHeader
                    title="Receiving - Completed"
                    description="Read-only history of receiving receipts already confirmed by Warehouse Manager."
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
                    <div className="flex flex-col gap-2 sm:flex-row">

                        <input
                            type="text"
                            value={searchInput}
                            onChange={(e) =>
                                setSearchInput(
                                    e.target.value
                                )
                            }
                            placeholder="Search receipt code or warehouse staff..."
                            className="w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                        />

                        {searchInput && (
                            <Button
                                type="button"
                                variant="outline"
                                onClick={clearSearch}
                            >
                                Clear
                            </Button>
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
                        Loading completed receiving result...
                    </Alert>
                )}


                {selected ? (

                    <ReceivingReviewDetail
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
                            totalPages={totalPages}
                            totalElements={totalElements}

                            onPrevious={() =>
                                setPage(
                                    (v) =>
                                        Math.max(
                                            v - 1,
                                            0
                                        )
                                )
                            }

                            onNext={() =>
                                setPage(
                                    (v) =>
                                        Math.min(
                                            v + 1,
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
                                    : "No completed receipts"
                            }

                            emptyDescription={
                                keyword
                                    ? `No completed receipt matched "${keyword}".`
                                    : "There are no COMPLETED receiving receipts yet."
                            }
                        />

                    </div>

                )}

            </div>

        </MainLayout>
    );
}