import i18n from "i18next";
import { initReactI18next } from "react-i18next";
i18n.use(initReactI18next).init({
  resources: {
    en: {
      translation: {
        returnsManagement: "Returns Management",
        supplierReturn: "Supplier Return",
        storeReturn: "Store Return",
        pending: "Pending",
        inProgress: "In Progress",
        completed: "Completed",
        viewDetails: "View Details",
      },
    },
  },
  lng: "en",
  fallbackLng: "en",
  interpolation: { escapeValue: false },
});
export default i18n;
