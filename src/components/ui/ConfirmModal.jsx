import Button from "./Button";
import Modal from "./Modal";
export default function ConfirmModal( {
    open,title="Xác nhận",message,confirmText="Xác nhận",cancelText="Hủy",danger=false,onConfirm,onClose
}
) {
    return <Modal open= {
        open
    }
    title= {
        title
    }
    onClose= {
        onClose
    }
    footer= {
        <>
        <Button variant="ghost" onClick= {
            onClose
        }
        > {
            cancelText
        }
    </Button>
    <Button variant= {
        danger?"danger":"primary"
    }
    onClick= {
        onConfirm
    }
    > {
        confirmText
    }
</Button>
</>
}
>
<p className="text-sm leading-6 text-slate-600"> {
    message
}
</p>
</Modal>
}
