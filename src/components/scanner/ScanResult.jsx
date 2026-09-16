import Alert from "../ui/Alert";
export default function ScanResult( {
    success,message
}
) {
    if(!message)return null;
    return <Alert type= {
        success?"success":"danger"
    }
    > {
        message
    }
</Alert>
}
