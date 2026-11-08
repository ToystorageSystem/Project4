import Select from "../ui/Select";
const defaultOptions=[ {
    value:"NORMAL",label:"Normal"
}
, {
    value:"DAMAGED",label:"Damaged"
}
, {
    value:"EXPIRED",label:"Expired"
}
, {
    value:"QUARANTINE",label:"Quarantine"
}
];
export default function ConditionSelector( {
    value,onChange,options=defaultOptions
}
) {
    return <Select label="Condition" value= {
        value
    }
    options= {
        options
    }
    onChange= {
        e=>onChange?.(e.target.value)
    }
    />
}
