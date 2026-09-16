import  {
    useState
}
from "react";
import Button from "../ui/Button";
import Input from "../ui/Input";
export default function BarcodeInput( {
    label="Mã barcode",placeholder="Quét hoặc nhập barcode",onScan
}
) {
    const[value,setValue]=useState("");
    const submit=()=> {
        const code=value.trim();
        if(!code)return;
        onScan?.(code);
        setValue("")
    }
    ;
    return <div className="flex items-end gap-2">
    <div className="flex-1">
        <Input label= {
            label
        }
        value= {
            value
        }
        placeholder= {
            placeholder
        }
        autoFocus onChange= {
            e=>setValue(e.target.value)
        }
        onKeyDown= {
            e=> {
                if(e.key==="Enter")submit()
            }
        }
        />
    </div>
    <Button onClick= {
        submit
    }
    >Xác nhận</Button>
</div>
}
