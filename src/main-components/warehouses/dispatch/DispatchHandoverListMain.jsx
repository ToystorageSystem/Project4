import {
    useEffect,
    useState,
} from "react";

import {
    useNavigate,
} from "react-router-dom";

import DispatchHandoverList
    from "../../../components/warehouse/dispatch/DispatchHandoverList.jsx";

import {
    getDispatchHandoverList,
} from "../../../api/packages/packing/managerPacking.js";


export default function DispatchHandoverListMain() {

    const navigate =
        useNavigate();


    const [items, setItems] =
        useState([]);


    const [loading, setLoading] =
        useState(true);


    const [error, setError] =
        useState("");


    const [keyword, setKeyword] =
        useState("");


    const loadData =
        async () => {

            try {

                setLoading(true);
                setError("");


                const response =
                    await getDispatchHandoverList(
                        keyword
                    );


                const content =
                    Array.isArray(response)
                        ? response
                        : response?.content ?? [];


                setItems(
                    content
                );

            } catch (err) {

                setError(
                    getErrorMessage(
                        err,
                        "Failed to load dispatch shipments."
                    )
                );


                setItems(
                    []
                );

            } finally {

                setLoading(false);

            }
        };


    useEffect(() => {

        loadData();

    }, []);


    const handleView =
        (transferId) => {

            navigate(
                `/warehouse/dispatch/${transferId}`
            );
        };


    return (
        <DispatchHandoverList
            items={
                items
            }

            loading={
                loading
            }

            error={
                error
            }

            keyword={
                keyword
            }

            onKeywordChange={
                setKeyword
            }

            onSearch={
                loadData
            }

            onView={
                handleView
            }
        />
    );
}


function getErrorMessage(
    error,
    fallback
) {

    const response =
        error?.response?.data;


    if (
        typeof response ===
        "string"
    ) {
        return response;
    }


    return (
        response?.message
        || response?.error
        || fallback
    );
}