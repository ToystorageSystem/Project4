import DashboardChart from "../../../components/dashboard/DashboardCharts";
import { useEffect } from "react";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import WorkspaceWelcome from "../../../components/layout/WorkspaceWelcome";
import useResource from "../../../hooks/useResource";
import { ResourceState, Table, number } from "../../../components/operations/WorkspaceUI";
import { STORE_API } from "../../../api/stores/manager/storeApi";
import { dateOnly, dateTime, money } from "../storeFormat";

const REFRESH_MS = 60_000;

function SectionTitle({ children }) {
  return <h2 style={{ fontSize: 17, margin: "24px 0 10px" }}>{children}</h2>;
}

function Metrics({ items }) {
  return (
    <div className="metric-strip">
      {items.map(([label, value, highlight]) => (
        <div key={label} style={highlight ? { borderColor: "#f25d19", background: "#faeadd" } : undefined}>
          <span>{label}</span>
          <strong>{value}</strong>
        </div>
      ))}
    </div>
  );
}

export default function StoreDashboardPage() {
  const resource = useResource(STORE_API.dashboard);
  const { reload } = resource;
  const d = resource.data;

  // Auto refresh; keep showing the last data while reloading (no loading flash).
  useEffect(() => {
    const timer = setInterval(reload, REFRESH_MS);
    return () => clearInterval(timer);
  }, [reload]);

  return (
    <MainLayout>
      <WorkspaceWelcome />
      <PageHeader
        title="Store dashboard"
        description={d ? `Today: ${dateOnly(d.date)} · refreshes every minute` : "Today's sales, shelf alerts and incoming deliveries."}
        actions={
          <button className="ops-button secondary" disabled={resource.loading} onClick={reload}>
            Refresh ↻
          </button>
        }
      />
      {!d ? (
        <ResourceState resource={resource} empty={false} />
      ) : (
        <>
          {resource.error && <div role="alert" className="ops-alert">{resource.error}</div>}
          <SectionTitle>Sales today</SectionTitle>
          <Metrics
            items={[
              ["Revenue", money(d.todayRevenue)],
              ["Sales", number(d.todaySales)],
              ["Items sold", number(d.todayItemsSold)],
            ]}
          />
          <SectionTitle>Shelf alerts</SectionTitle>
          <Metrics
            items={[
              ["Low shelf (restock from backroom)", number(d.lowShelfCount), d.lowShelfCount > 0],
              ["Out of stock", number(d.outOfStockCount), d.outOfStockCount > 0],
            ]}
          />
          <SectionTitle>Incoming deliveries</SectionTitle>
          <Metrics
            items={[
              ["In transit", number(d.manifestsInTransit)],
              ["To receive", number(d.manifestsToReceive)],
              ["To inspect", number(d.manifestsToInspect)],
              ["Awaiting your confirmation", number(d.manifestsToConfirm), d.manifestsToConfirm > 0],
            ]}
          />
          <div className="analytics-grid">
            <DashboardChart title="Incoming delivery stages" data={[
              { label: "In transit", value: d.manifestsInTransit }, { label: "To receive", value: d.manifestsToReceive },
              { label: "To inspect", value: d.manifestsToInspect }, { label: "To confirm", value: d.manifestsToConfirm },
            ]} />
            <DashboardChart title="Recent sale amounts (VND)" data={(d.recentSales || []).slice(0, 6).map(sale => ({ label: sale.code, value: Number.isFinite(Number(sale.totalAmount)) && sale.totalAmount != null ? Number(sale.totalAmount) : null }))} />
          </div>
          <SectionTitle>Recent sales</SectionTitle>
          <ResourceState resource={{ ...resource, loading: false, error: "" }} empty={!d.recentSales?.length}>
            <Table head={["Sale", "Items", "Amount", "Cashier", "Time"]}>
              {(d.recentSales || []).map((s) => (
                <tr key={s.id}>
                  <td className="mono">{s.code}</td>
                  <td>{number(s.totalQuantity)}</td>
                  <td><strong>{money(s.totalAmount)}</strong></td>
                  <td>{s.createdByName || "—"}</td>
                  <td>{dateTime(s.createdAt)}</td>
                </tr>
              ))}
            </Table>
          </ResourceState>
        </>
      )}
    </MainLayout>
  );
}
