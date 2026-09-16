import Badge from "./Badge";
const map= {
    CREATED:"orange",ASSIGNED:"orange",ACCEPTED:"blue",READY_TO_SHIP:"yellow",IN_TRANSIT:"blue",ARRIVED:"yellow",DELIVERED:"green",COMPLETED:"green",FAILED:"red",CANCELLED:"red",REJECTED:"red",PACKING:"orange",PACKED:"yellow",RECEIVED:"green",ACTIVE:"green",INACTIVE:"default"
}
;
export default function StatusBadge( {
    status
}
) {
    return <Badge variant= {
        map[status]||"default"
    }
    > {
        status||"-"
    }
</Badge>
}
