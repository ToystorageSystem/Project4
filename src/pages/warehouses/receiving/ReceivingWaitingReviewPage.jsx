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

import ReceivingReviewDetail
    from "../../../main-components/warehouses/receiving-monitor/ReceivingReviewDetail";

import RequestReinspectionModal
    from "../../../main-components/warehouses/receiving-confirmation/RequestReinspectionModal";

import {
    getWaitingReviewList,
} from "../../../api/receipts/receiving/receivingMonitor";

import {
    confirmReceivingInspection,
    getReceivingInspectionResult,
    requestReceivingReinspection,
} from "../../../api/receipts/receiving/receivingConfirmation";


export default function ReceivingWaitingReviewPage() {
    const [items, setItems] =
        useState([]);

    const [selected, setSelected] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [
        detailLoading,
        setDetailLoading,
    ] = useState(false);

    const [
        confirming,
        setConfirming,
    ] = useState(false);

    const [
        requesting,
        setRequesting,
    ] = useState(false);

    const [
        modalOpen,
        setModalOpen,
    ] = useState(false);

    const [error, setError] =
        useState("");

    const [success, setSuccess] =
        useState("");

    const [page, setPage] =
        useState(0);

    const [
        totalPages,
        setTotalPages,
    ] = useState(0);

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

    const pageSize = 10;

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
                        await getWaitingReviewList({
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
                        "Unable to load receipts waiting for review."
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


    const clearSearch = () => {
        setSearchInput("");
        setKeyword("");
        setPage(0);
    };


    const openReceipt =
        async (item) => {

            const id =
                item?.receiptId ??
                item?.id;

            if (!id) {
                return;
            }

            try {

                setDetailLoading(
                    true
                );

                setError("");
                setSuccess("");

                selectedReceiptIdRef.current =
                    id;

                setSelected(
                    await getReceivingInspectionResult(
                        id
                    )
                );

            } catch (e) {

                setError(
                    e.response?.data?.message ??
                    "Unable to load inspection result."
                );

            } finally {

                setDetailLoading(
                    false
                );
            }
        };


    const backToList =
        async () => {

            selectedReceiptIdRef.current =
                null;

            setSelected(null);

            setModalOpen(false);

            await loadList({
                showLoading: false,
            });
        };


    const handleConfirm =
        async () => {

            const id =
                selected?.receiptId ??
                selected?.id ??
                selectedReceiptIdRef.current;

            if (!id) {
                return;
            }

            if (
                !window.confirm(
                    "Confirm this receiving result? Inventory will be updated and Putaway will be created."
                )
            ) {
                return;
            }

            try {

                setConfirming(true);

                setError("");

                await confirmReceivingInspection(
                    id
                );

                setSuccess(
                    "Receiving confirmed successfully."
                );

                await backToList();

            } catch (e) {

                setError(
                    e.response?.data?.message ??
                    "Unable to confirm receiving result."
                );

            } finally {

                setConfirming(false);
            }
        };


    const handleRequestReinspection =
        async (payload) => {

            const id =
                selected?.receiptId ??
                selected?.id ??
                selectedReceiptIdRef.current;

            if (!id) {
                return;
            }

            try {

                setRequesting(true);

                setError("");

                await requestReceivingReinspection(
                    id,
                    payload
                );

                setModalOpen(false);

                setSuccess(
                    "Re-inspection requested. The receipt returned to RECEIVING."
                );

                await backToList();

            } catch (e) {

                setError(
                    e.response?.data?.message ??
                    "Unable to request re-inspection."
                );

            } finally {

                setRequesting(false);
            }
        };


    useEffect(() => {
        loadList();
    }, [loadList]);


    return (
        <MainLayout>

            <div className="flex min-h-0 flex-1 flex-col gap-6">

                <PageHeader
                    title="Receiving - Waiting for Review"
                    description="Staff has finished inspection. Resolve discrepancies or confirm the receiving result."
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


                {success && (
                    <Alert type="success">
                        {success}
                    </Alert>
                )}


                {detailLoading && (
                    <Alert type="info">
                        Loading inspection result...
                    </Alert>
                )}


                {selected ? (

                    <ReceivingReviewDetail
                        data={selected}
                        confirming={
                            confirming
                        }
                        onBack={
                            backToList
                        }
                        onConfirm={
                            handleConfirm
                        }
                        onRequestReinspection={
                            () =>
                                setModalOpen(
                                    true
                                )
                        }
                    />

                ) : loading ? (

                    <LoadingPage />

                ) : (

                    <div className="flex min-h-0 flex-1 flex-col">

                        <ReceivingMonitorList
                            items={items}
                            onView={
                                openReceipt
                            }
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
                                    : "No receipts waiting for review"
                            }
                            emptyDescription={
                                keyword
                                    ? `No receiving receipt matched "${keyword}".`
                                    : "There are no INSPECTED receipts waiting for Warehouse Manager action."
                            }
                        />

                    </div>

                )}

            </div>


            <RequestReinspectionModal
                open={modalOpen}
                loading={requesting}
                onClose={() =>
                    setModalOpen(false)
                }
                onSubmit={
                    handleRequestReinspection
                }
            />

        </MainLayout>
    );
}