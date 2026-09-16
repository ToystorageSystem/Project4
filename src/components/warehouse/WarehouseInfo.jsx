import LocationInfo from "./LocationInfo";
export default function WarehouseInfo( {
    warehouse,title="Kho"
}
) {
    if(!warehouse)return null;
    return <LocationInfo title= {
        title
    }
    code= {
        warehouse.code||warehouse.warehousesCode
    }
    name= {
        warehouse.name
    }
    address= {
        warehouse.address
    }
    />
}
