import { afterEach, expect, it } from "vitest";
import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import Chart, { groupStatuses } from "../src/components/dashboard/DashboardCharts";
afterEach(cleanup);
it("keeps unavailable queues distinct from zero and scales bars from actual counts", () => {
  render(<Chart title="Workload" data={[{label:"Receiving",value:24},{label:"Picking",value:0},{label:"Packing",value:null}]} />);
  expect(screen.getByRole("meter", {name:"Receiving"}).getAttribute("aria-valuenow")).toBe("24");
  expect(screen.getByRole("meter", {name:"Picking"}).getAttribute("aria-valuenow")).toBe("0");
  expect(screen.queryByRole("meter", {name:"Packing"})).toBeNull();
  expect(screen.getByText("Unavailable")).toBeTruthy();
});
it("does not draw a zero-total donut or fabricate percentages for missing data", () => {
  const view = render(<Chart title="Statuses" type="donut" data={[{label:"Completed",value:0}]} />);
  expect(screen.queryByRole("img")).toBeNull();
  expect(screen.getByText("No data to display")).toBeTruthy();
  view.rerender(<Chart title="Statuses" type="donut" data={[{label:"Completed",value:2},{label:"Pending",value:null}]} />);
  expect(screen.queryByRole("img")).toBeNull();
  expect(screen.getByText("Data unavailable")).toBeTruthy();
});
it("groups loaded records and supports keyboard selection of donut segments", () => {
  const rows = [{status:"IN_PROGRESS"},{status:"IN_PROGRESS"},{status:"COMPLETED"}];
  render(<Chart title="Loaded statuses" type="donut" data={groupStatuses(rows)} />);
  const button = screen.getByRole("button", {name:"completed: 1 (33%)"});
  fireEvent.focus(button);
  expect(button.getAttribute("aria-pressed")).toBe("true");
  const image = screen.getByRole("img");
  expect(within(image).getByText("1")).toBeTruthy();
  fireEvent.blur(button);
  expect(within(image).getByText("3")).toBeTruthy();
});
