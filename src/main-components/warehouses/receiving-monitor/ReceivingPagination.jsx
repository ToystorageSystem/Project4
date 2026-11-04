import Button from "../../../components/ui/Button";


export default function ReceivingPagination({
                                                page = 0,
                                                totalPages = 0,
                                                totalElements = 0,
                                                onPrevious,
                                                onNext,
                                            }) {
    return (
        <div className="flex w-full flex-col gap-3 rounded-xl border border-slate-200 bg-white px-5 py-4 shadow-sm sm:flex-row sm:items-center sm:justify-between">

            <p className="text-sm text-slate-500">

                Total{" "}

                <span className="font-semibold text-slate-800">
                    {totalElements}
                </span>

                {" "}receipts

            </p>


            <div className="flex items-center gap-3">

                <Button
                    variant="outline"
                    disabled={
                        page <= 0
                    }
                    onClick={
                        onPrevious
                    }
                >
                    Previous
                </Button>


                <span className="min-w-24 text-center text-sm font-medium text-slate-600">

                    Page{" "}
                    {page + 1}
                    {" / "}
                    {Math.max(
                        totalPages,
                        1
                    )}

                </span>


                <Button
                    variant="outline"
                    disabled={
                        totalPages <= 0 ||
                        page + 1 >= totalPages
                    }
                    onClick={
                        onNext
                    }
                >
                    Next
                </Button>

            </div>

        </div>
    );
}