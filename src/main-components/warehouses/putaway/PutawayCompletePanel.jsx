import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";

export default function PutawayCompletePanel({
    allCompleted,
    completing = false,
    onComplete,
}) {
    return (
        <Card>
            <h2 className="font-semibold text-slate-900">
                Hoàn thành Putaway
            </h2>

            <p className="mt-2 text-sm leading-6 text-slate-500">
                Chỉ có thể hoàn thành khi toàn bộ
                sản phẩm đã được cất đủ số lượng.
            </p>

            <Button
                className="mt-4 w-full"
                variant="success"
                disabled={
                    !allCompleted ||
                    completing
                }
                onClick={onComplete}
            >
                {completing
                    ? "Đang hoàn thành..."
                    : "Complete Putaway"}
            </Button>
        </Card>
    );
}
