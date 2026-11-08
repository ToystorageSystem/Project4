import { afterEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { authPath, canAccess, homePath } from "../src/auth/access";
import { menuFor } from "../src/components/layout/navigation";
import { AuthProvider } from "../src/auth/AuthContext";
import ProtectedRoute from "../src/auth/ProtectedRoute";
import api from "../src/api/api";
import { createAdminUser } from "../src/features/admin/api/admin/userAccountsApi";
import { createProduct } from "../src/api/business/products";
import { sessionFetch } from "../src/api/sessionFetch";
import App from "../src/App";
import UserAccountsPage from "../src/features/admin/UserAccountsPage";

const original = api.defaults.adapter;
afterEach(() => { cleanup(); api.defaults.adapter=original; sessionStorage.clear(); vi.restoreAllMocks(); });
const account = role => ({ id:1, name:"Lâm", stage:"AUTHENTICATED", authorities:[role] });
it.each([
 ["ROLE_ADMIN","/admin/dashboard"], ["ROLE_ROLE-ADMIN","/admin/dashboard"],
 ["ROLE_ROLE-BIZ-OPS","/business/dashboard"], ["ROLE_BUSINESS_OPERATIONS","/business/dashboard"],
 ["ROLE_BIZ_MANAGER","/business/dashboard"], ["ROLE_BIZ_STAFF","/business/dashboard"],
 ["ROLE_ROLE-WH-MANAGER","/warehouse/dashboard"], ["ROLE_ROLE-WH-STAFF","/warehouse/dashboard"], ["ROLE_DELIVERY_STAFF","/warehouse/dashboard"],
 ["ROLE_STORE_MANAGER","/workspace"],
])("routes %s to its own workspace", (role, expected) => {
 expect(authPath(account(role))).toBe(expected);
 expect(homePath(account(role))).toBe(expected);
 expect(authPath({...account(role),stage:"SETUP_2FA"})).toBe("/2fa/setup");
});
it("shows only role-scoped modules and denies unknown access keys", () => {
 const admin=account("ROLE_ADMIN"), business=account("ROLE_ROLE-BIZ-OPS"), staff=account("ROLE_WH_STAFF");
 expect(menuFor(admin).map(i=>i.path)).toEqual(["/admin/dashboard","/admin/users","/admin/roles","/admin/warehouses","/admin/activity-logs","/admin/security-alerts"]);
 expect(menuFor(business).every(i=>i.path.startsWith("/business/"))).toBe(true);
 expect(menuFor(staff).some(i=>i.path.startsWith("/admin"))).toBe(false);
 expect(canAccess(admin,"business-operations")).toBe(false);
 expect(canAccess(business,"admin-users")).toBe(false);
 expect(canAccess(account("ROLE_WH_MANAGER"),"unknown-privilege")).toBe(false);
});
it.each([["ROLE_WH_STAFF","admin-users"],["ROLE_ROLE-BIZ-OPS","admin-users"],["ROLE_ADMIN","business-operations"]])("blocks %s from %s even with forged saved state",async(role,access)=>{
 sessionStorage.setItem("currentUser",JSON.stringify(account("ROLE_ADMIN")));
 api.defaults.adapter=async config=>({data:account(role),status:200,headers:{},config});
 render(<MemoryRouter initialEntries={["/protected"]}><AuthProvider><Routes><Route element={<ProtectedRoute access={access}/>}><Route path="/protected" element={<p>Restricted form</p>}/></Route></Routes></AuthProvider></MemoryRouter>);
 await screen.findByRole("heading",{name:"Access denied"});
 expect(screen.queryByText("Restricted form")).toBeNull();
});
it("uses fresh CSRF and credentials for imported Admin mutations without changing payloads",async()=>{
 let version=0;
 api.defaults.adapter=async config=>({data:{token:`t${++version}`,headerName:"X-CSRF-TOKEN"},status:200,headers:{},config});
 const sent=[];
 vi.spyOn(globalThis,"fetch").mockImplementation(async(url,options)=>{sent.push({url,options});return new Response(JSON.stringify({id:7}),{status:200,headers:{"Content-Type":"application/json"}});});
 const payload={name:"Example",email:"example@example.invalid",roleIds:[1]};
 expect(await createAdminUser(payload)).toEqual({id:7});
 await createAdminUser(payload);
 expect(sent[0].url).toBe("/api/admin/users");
 expect(JSON.parse(sent[0].options.body)).toEqual({...payload,warehouseId:null});
 expect(sent[0].options.credentials).toBe("include");
 expect(sent[0].options.headers.get("X-CSRF-TOKEN")).toBe("t1");
 expect(sent[1].options.headers.get("X-CSRF-TOKEN")).toBe("t2");
});
it("uses the shared Axios client for Business mutations",async()=>{
 const sent=[];
 api.defaults.adapter=async config=>{sent.push(config);return {data:config.url==="/auth/csrf"?{token:"business-token",headerName:"X-CSRF-TOKEN"}:{id:3},status:200,headers:{},config};};
 await createProduct({name:"Test product",categoryId:2});
 expect(sent.map(c=>c.url)).toEqual(["/auth/csrf","/products"]);
 expect(sent[1].headers.get("X-CSRF-TOKEN")).toBe("business-token");
 expect(JSON.parse(sent[1].data)).toEqual({name:"Test product",categoryId:2});
});
it("expires the shared session after an Admin API 401 and never retries writes",async()=>{
 api.defaults.adapter=async config=>({data:{token:"t"},status:200,headers:{},config});
 const expired=vi.fn(); window.addEventListener("session-expired",expired);
 const fetch=vi.spyOn(globalThis,"fetch").mockResolvedValue(new Response(null,{status:401}));
 await sessionFetch("/api/admin/users/1",{method:"PATCH"});
 expect(expired).toHaveBeenCalledTimes(1); expect(fetch).toHaveBeenCalledTimes(1);
 window.removeEventListener("session-expired",expired);
});
it.each([["ROLE_ADMIN","/admin/dashboard","Operations Dashboard"],["ROLE_ROLE-BIZ-OPS","/business/dashboard","Operations Overview"],["ROLE_WH_MANAGER","/warehouse/dashboard",null]])("opens the complete app at the correct destination for %s",async(role,path,title)=>{
 window.history.replaceState({},"","/");
 api.defaults.adapter=async config=>({data:config.url==="/auth/status"?account(role):config.url.includes("monitoring")||config.url.includes("waiting-review")||config.url.includes("packing-confirmations")||config.url==="/products"?{content:[],totalElements:0,totalPages:0}:[],status:200,headers:{},config});
 vi.spyOn(globalThis,"fetch").mockImplementation(async()=>new Response(JSON.stringify({content:[],totalElements:0,totalPages:0}),{status:200}));
 render(<App/>);
 if(title) await screen.findByRole("heading",{name:title});
 else await screen.findByRole("heading",{name:/Good (morning|afternoon|evening),/});
 expect(window.location.pathname).toBe(path);
});
it("retains Admin management permissions for normalized roles and the new email session shape",async()=>{
 const admin={...account("ROLE_ROLE-ADMIN"),email:"owner@example.invalid",authorities:["ROLE_ROLE-ADMIN","USER_MANAGE","ROLE_MANAGE","WAREHOUSE_READ"]};
 const fetch=vi.spyOn(globalThis,"fetch").mockImplementation(async url=>{
  const data=String(url).includes("/auth/me")?admin:String(url).includes("/admin/users")?{content:[{...admin,status:"ACTIVE",roles:[]}],totalElements:1,totalPages:1}:String(url).includes("/admin/roles")?[{id:1,roleCode:"ADMIN",roleName:"Administrator",status:"ACTIVE"}]:[];
  return new Response(JSON.stringify(data),{status:200});
 });
 render(<UserAccountsPage/>);
 const create=await screen.findByRole("button",{name:"Create User"});
 await vi.waitFor(()=>expect(create.disabled).toBe(false));
 fireEvent.click(screen.getByRole("button",{name:"Lock Lâm"}));
 fireEvent.click(screen.getByRole("button",{name:"Confirm",exact:true}));
 await screen.findByText("You cannot disable your own signed-in account.");
 expect(fetch.mock.calls.some(([,options])=>options.method === "PATCH")).toBe(false);
});
