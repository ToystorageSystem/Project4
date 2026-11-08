// Isolated design fixture. Not imported by the app and not included in Vite's production entry.
import React from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../../src/auth/AuthContext";
import Dashboard from "../../src/pages/warehouses/dashboard/WarehouseDashboardPage";
import Login from "../../src/pages/auth/Login";
import api from "../../src/api/api";
import "../../src/index.css";
import "../../src/workspace.css";
import "../../src/personal-workspace.css";
const params = new URLSearchParams(location.search);
if (params.has("mobile")) {
  document.body.style.background = "#ddd";
  createRoot(document.getElementById("root")).render(<iframe title="Mobile viewport — 390px" src="?screen=dashboard" style={{ width:390, height:1100, display:"block", margin:"20px auto", border:0 }} />);
} else {
  const login = params.get("screen") === "login";
  api.defaults.adapter = async config => {
    let data = [];
    if (config.url === "/auth/status") {
      if (login) throw { response:{ status:401 }, config };
      data = { id:99001, name:"Đinh Tùng Lâm", email:"design@example.invalid", stage:"AUTHENTICATED", authorities:["ROLE_WAREHOUSE_MANAGER", "STORE_VIEW"] };
    } else if (config.url.includes("monitoring")) data = { totalElements:8, content:[
      { receiptId:1, receiptCode:"GR-2026-0042", staffName:"Ngọc Anh", progressPercent:75, issueProducts:0 },
      { receiptId:2, receiptCode:"GR-2026-0041", staffName:"Minh Khang", progressPercent:40, issueProducts:2 },
      { receiptId:3, receiptCode:"GR-2026-0039", staffName:"Hoài Nam", progressPercent:90, issueProducts:1 },
    ] };
    else if (config.url.includes("waiting-review")) data = { totalElements:3, content:[] };
    else if (config.url === "/putaway-plans") data = [{status:"AVAILABLE"},{status:"IN_PROGRESS"}];
    else if (config.url.includes("packing-confirmations")) data = { totalElements:5, content:[] };
    return {data,status:200,headers:{},config};
  };
  createRoot(document.getElementById("root")).render(<><div style={{background:"#fbe8ce",color:"#734215",padding:"5px 15px",fontSize:11,textAlign:"center"}}>Design QA · sample records only</div><MemoryRouter initialEntries={[login ? "/login" : "/warehouse/dashboard"]}><AuthProvider>{login ? <Login /> : <Dashboard />}</AuthProvider></MemoryRouter></>);
}
