import Input from "../ui/Input";
export default function QuantityInput( {
    value,onChange,label="Số lượng",min=0
}
) {
    return <Input label= {
        label
    }
    type="number" min= {
        min
    }
    value= {
        value
    }
    onChange= {
        e=>onChange?.(Number(e.target.value))
    }
    />
}
