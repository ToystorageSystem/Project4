export default function AlertMessage({
                                         type = "error",
                                         message,
                                     }) {

    if (!message) {
        return null;
    }


    const styles = {

        success:
            "border-emerald-200 bg-emerald-50 text-emerald-700",

        error:
            "border-red-200 bg-red-50 text-red-700",

        warning:
            "border-amber-200 bg-amber-50 text-amber-700",

        info:
            "border-blue-200 bg-blue-50 text-blue-700",
    };


    return (
        <div
            className={`
                mb-5
                rounded-xl
                border
                px-4
                py-3
                text-sm
                ${
                styles[type]
                || styles.error
            }
            `}
        >
            {message}
        </div>
    );
}