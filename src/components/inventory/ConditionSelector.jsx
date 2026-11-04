import Select from "../ui/Select";
const defaultOptions=[ {
    value:"NORMAL",label:"Bình thường"
}
, {
    value:"DAMAGED",label:"Hư hỏng"
}
, {
    value:"EXPIRED",label:"Hết hạn"
}
, {
    value:"QUARANTINE",label:"Cách ly"
}
];
export default function ConditionSelector( {
    value,onChange,options=defaultOptions
}
) {
    return <Select label="Tình trạng" value= {
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
