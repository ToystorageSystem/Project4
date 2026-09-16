import EmptyState from "../../../components/ui/EmptyState";
import ReceivingMonitorCard from "./ReceivingMonitorCard";
import ReceivingPagination from "./ReceivingPagination";


export default function ReceivingMonitorList({
                                                 items = [],
                                                 onView,
                                                 page = 0,
                                                 totalPages = 0,
                                                 totalElements = 0,
                                                 onPrevious,
                                                 onNext,
                                                 emptyTitle = "No receiving receipts",
                                                 emptyDescription = "There are no receiving receipts in this section.",
                                             }) {
    return (
        <div className="flex min-h-0 flex-1 flex-col">

            <div className="flex-1">

                {!items.length ? (

                    <EmptyState
                        title={emptyTitle}
                        description={emptyDescription}
                    />

                ) : (

                    <div className="grid gap-4 xl:grid-cols-2">

                        {items.map(
                            (
                                item,
                                index
                            ) => (

                                <ReceivingMonitorCard
                                    key={
                                        item?.receiptId ??
                                        item?.id ??
                                        index
                                    }
                                    item={item}
                                    onView={onView}
                                />

                            )
                        )}

                    </div>

                )}

            </div>


            <div className="mt-auto pt-6">

                <ReceivingPagination
                    page={page}
                    totalPages={totalPages}
                    totalElements={totalElements}
                    onPrevious={onPrevious}
                    onNext={onNext}
                />

            </div>

        </div>
    );
}