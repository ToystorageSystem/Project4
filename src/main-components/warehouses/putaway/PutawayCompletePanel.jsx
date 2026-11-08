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
                Complete Putaway
            </h2>

            <p className="mt-2 text-sm leading-6 text-slate-500">
                Putaway can be completed only when all items have been stored in full.
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
                    ? "Completing..."
                    : "Complete Putaway"}
            </Button>
        </Card>
    );
}
