import EmptyState from "../../../components/ui/EmptyState";
import PutawayItemCard from "./PutawayItemCard.jsx";

export default function PutawayItemList({
    items = [],
    taskStatus,
    selectedItemId,
    onSelectItem,
}) {
    if (items.length === 0) {
        return (
            <EmptyState
                title="Không có sản phẩm"
                description="Putaway task này chưa có sản phẩm để thực hiện."
            />
        );
    }

    return (
        <div className="space-y-4">
            {items.map((item) => (
                <PutawayItemCard
                    key={item.itemId}
                    item={item}
                    taskStatus={taskStatus}
                    selected={
                        String(selectedItemId) ===
                        String(item.itemId)
                    }
                    onSelect={onSelectItem}
                />
            ))}
        </div>
    );
}
