import api from "../../api";
const BASE_URL = "/putaway-plans";
// Plans are created and completed by backend receipt/task workflows.
export const getPutawayPlans = async () => (await api.get(BASE_URL)).data;
export const getPutawayPlan = async (taskId) =>
  (await api.get(`${BASE_URL}/${taskId}`)).data;
