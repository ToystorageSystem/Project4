import {
    createContext,
    useContext,
    useState,
} from "react";

const AuthContext =
    createContext(null);

export function AuthProvider({
                                 children,
                             }) {
    const [user, setUserState] =
        useState(() => {
            const savedUser =
                sessionStorage.getItem(
                    "currentUser"
                );

            if (!savedUser) {
                return null;
            }

            try {
                return JSON.parse(
                    savedUser
                );
            } catch {
                sessionStorage.removeItem(
                    "currentUser"
                );

                return null;
            }
        });


    const setUser = (
        userData
    ) => {
        setUserState(
            userData
        );


        if (userData) {
            sessionStorage.setItem(
                "currentUser",
                JSON.stringify(
                    userData
                )
            );
        }
        else {
            sessionStorage.removeItem(
                "currentUser"
            );
        }
    };


    const clearUser = () => {
        setUserState(
            null
        );

        sessionStorage.removeItem(
            "currentUser"
        );
    };


    return (
        <AuthContext.Provider
            value={{
                user,
                setUser,
                clearUser,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}


export function useAuth() {

    const context =
        useContext(
            AuthContext
        );


    if (!context) {

        throw new Error(
            "useAuth must be used inside AuthProvider"
        );
    }


    return context;
}
