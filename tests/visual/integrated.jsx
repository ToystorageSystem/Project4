// Isolated browser fixture: never imported by the production entry point.
import React from "react";
import { createRoot } from "react-dom/client";
import App from "../../src/App";
import api from "../../src/api/api";
import "../../src/index.css";
import "../../src/workspace.css";
import "../../src/personal-workspace.css";
import "../../src/integration-theme.css";
const options = new URLSearchParams(location.search);
const role = options.get("role") || "admin";
const home = role === "admin" ? "/admin/dashboard" : role === "business" ? "/business/dashboard" : "/warehouse/dashboard";
const path = options.get("path") || home;
if (options.has("mobile")) {
  const inner = new URLSearchParams({role,path});
  createRoot(document.getElementById("root")).render(<iframe title="390px integrated workspace" src={`integrated.html?${inner}`} style={{width:390,height:1200,border:0,display:"block",margin:"20px auto"}}/>);
} else {
  const user = {id:99003,name:"Đinh Tùng Lâm",email:"design@example.invalid",stage:"AUTHENTICATED",authorities:[role === "admin" ? "ROLE_ADMIN" : role === "business" ? "ROLE_ROLE-BIZ-OPS" : "ROLE_WH_MANAGER","WAREHOUSE_MANAGE","USER_READ"]};
  const product = {id:1,productCode:"TOY-0001",sku:"TOY-0001",name:"Wooden building blocks",status:"ACTIVE",categoryName:"Learning toys",barcode:"893000000001",unit:"SET",price:250000};
  const page = content => ({content,totalElements:content.length,totalPages:content.length?1:0,number:0,size:10});
  function response(url) {
    const p=url.split("?")[0].replace(/^\/api/,"");
    if (p === "/auth/status" || p === "/auth/me") return user;
    if (p === "/auth/csrf") return {token:"design-qa",headerName:"X-CSRF-TOKEN"};
    if (p === "/admin/users") return page([{id:1,userCode:"USR-0001",name:"Ngọc Anh",email:"sample@example.invalid",roles:[{roleCode:"WH_STAFF",roleName:"Warehouse Staff"}],status:"ACTIVE",phone:""}]);
    if (p === "/admin/roles") return [];
    if (p === "/admin/permissions") return [];
    if (p === "/admin/warehouses") return page([{id:1,warehouseCode:"WH-001",name:"Main warehouse",type:"MAIN_WAREHOUSE",address:"Sample address",status:"ACTIVE"}]);
    if (p.includes("activity-logs") || p.includes("suspicious-alerts")) return page([]);
    if (p === "/products") return page([product]);
    if (p === "/categories") return page([{id:1,name:"Learning toys",status:"ACTIVE"}]);
    if (p.endsWith("/options")) return [];
    if (p.includes("suppliers")) return page([]);
    if (p.includes("purchase-orders")) return [];
    if (p.includes("goods-receipts")) return page([]);
    if (p.includes("packing-confirmations")) return page([]);
    return [];
  }
  api.defaults.adapter=async config=>({data:response(config.url),status:200,headers:{},config});
  globalThis.fetch=async url=>new Response(JSON.stringify(response(String(url))),{status:200,headers:{"Content-Type":"application/json"}});
  history.replaceState({},"",path);
  createRoot(document.getElementById("root")).render(<><div style={{background:"#fbe8ce",color:"#734215",padding:"5px 15px",fontSize:11,textAlign:"center"}}>Design QA · sample records only · {role} workspace</div><App/></>);
}
