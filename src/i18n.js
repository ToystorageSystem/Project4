import i18n from "i18next";
import { initReactI18next } from "react-i18next";

const resources = {
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

    vi: {
        translation: {
            returnsManagement: "Returns Management",
            supplierReturn: "Supplier Returns",
            storeReturn: "Store Returns",
            pending: "Pending",
            inProgress: "In Progress",
            completed: "Completed",
            viewDetails: "View Details",
        },
    },

    hi: {
        translation: {
            returnsManagement: "रिटर्न प्रबंधन",
            supplierReturn: "आपूर्तिकर्ता वापसी",
            storeReturn: "स्टोर वापसी",
            pending: "लंबित",
            inProgress: "प्रगति में",
            completed: "पूर्ण",
            viewDetails: "विवरण देखें",
        },
    },
};

i18n
    .use(initReactI18next)
    .init({
        resources,
        lng: "en",
        fallbackLng: "en",
        interpolation: {
            escapeValue: false,
        },
    });

export default i18n;