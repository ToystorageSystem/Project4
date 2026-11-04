import MainLayout
    from "../../../components/layout/MainLayout";

import ShipmentManifestMain
    from "../../../main-components/warehouses/manifests/ShipmentManifestMain";

export default function ShipmentManifestPage() {
    return (
        <MainLayout>
            <ShipmentManifestMain />
        </MainLayout>
    );
}