import {
    useCallback,
    useEffect,
    useState,
} from "react";

import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import LoadingPage from "../../../components/feedback/LoadingPage";

import ReceivingIncidentReportList
    from "../../../main-components/warehouses/receiving-incidents/ReceivingIncidentReportList";

import ReceivingIncidentReportDetail
    from "../../../main-components/warehouses/receiving-incidents/ReceivingIncidentReportDetail";

import {
    getReceivingIncidentReportDetail,
    getReceivingIncidentReports,
    updateReceivingIncidentReport,
    updateReceivingIncidentReportItem,
} from "../../../api/receipts/incidents/receivingIncidentReport";


export default function ReceivingIncidentReportsPage() {
    const [items, setItems] =
        useState([]);

    const [selected, setSelected] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [busy, setBusy] =
        useState(false);

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

    const pageSize = 20;


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
                        await getReceivingIncidentReports({
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
                        "Unable to load receiving incident reports."
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


    useEffect(() => {
        loadList();
    }, [loadList]);


    const openReport =
        async (item) => {

            try {

                setBusy(true);

                setError("");
                setSuccess("");

                setSelected(
                    await getReceivingIncidentReportDetail(
                        item.id
                    )
                );

            } catch (e) {

                setError(
                    e.response?.data?.message ??
                    "Unable to load incident report."
                );

            } finally {

                setBusy(false);
            }
        };


    const reloadSelected =
        async () => {

            if (!selected?.id) {
                return;
            }

            setSelected(
                await getReceivingIncidentReportDetail(
                    selected.id
                )
            );
        };


    const saveGeneral =
        async (payload) => {

            if (!selected?.id) {
                return;
            }

            try {

                setBusy(true);

                setError("");
                setSuccess("");

                await updateReceivingIncidentReport(
                    selected.id,
                    payload
                );

                await reloadSelected();

                setSuccess(
                    "Report updated successfully."
                );

            } catch (e) {

                setError(
                    e.response?.data?.message ??
                    "Unable to save report."
                );

            } finally {

                setBusy(false);
            }
        };


    const saveItem =
        async (
            itemId,
            payload
        ) => {

            if (!selected?.id) {
                return;
            }

            try {

                setBusy(true);

                setError("");
                setSuccess("");

                await updateReceivingIncidentReportItem(
                    selected.id,
                    itemId,
                    payload
                );

                await reloadSelected();

                setSuccess(
                    "Reason saved."
                );

            } catch (e) {

                setError(
                    e.response?.data?.message ??
                    "Unable to save report item."
                );

            } finally {

                setBusy(false);
            }
        };


    const backToList =
        async () => {

            setSelected(null);

            setSuccess("");

            await loadList({
                showLoading: false,
            });
        };


    const printReport = () => {

        if (!selected?.id) {
            return;
        }

        window.open(
            `/warehouse/receiving/incident-reports/${selected.id}/print`,
            "_blank"
        );
    };


    return (
        <MainLayout>

            <div className="flex min-h-0 flex-1 flex-col gap-6">

                <PageHeader
                    title="Receiving Discrepancy Report"
                    description="One report per receipt for discrepancies accepted by Warehouse Manager."
                    actions={
                        !selected ? (

                            <Button
                                variant="outline"
                                onClick={() =>
                                    loadList()
                                }
                            >
                                Refresh
                            </Button>

                        ) : null
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
                            value={
                                searchInput
                            }
                            onChange={(e) =>
                                setSearchInput(
                                    e.target.value
                                )
                            }
                            placeholder="Search report code, receipt code, staff or manager..."
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


                {selected ? (

                    <ReceivingIncidentReportDetail
                        data={selected}
                        busy={busy}
                        onBack={
                            backToList
                        }
                        onPrint={
                            printReport
                        }
                        onSaveGeneral={
                            saveGeneral
                        }
                        onSaveItem={
                            saveItem
                        }
                    />

                ) : loading ? (

                    <LoadingPage />

                ) : (

                    <div className="flex min-h-0 flex-1 flex-col">

                        <ReceivingIncidentReportList
                            items={items}
                            page={page}
                            totalPages={
                                totalPages
                            }
                            totalElements={
                                totalElements
                            }
                            onView={
                                openReport
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
                        />

                    </div>

                )}

            </div>

        </MainLayout>
    );
}