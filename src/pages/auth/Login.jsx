import {
    useState,
} from "react";

import {
    useNavigate,
} from "react-router-dom";

import Button from "../../components/ui/Button";
import Card from "../../components/ui/Card";
import Input from "../../components/ui/Input";
import Alert from "../../components/ui/Alert";

import {
    login,
} from "../../api/auth/auth";

import {
    useAuth,
} from "../../auth/AuthContext";


export default function Login() {

    const navigate =
        useNavigate();


    const {
        setUser,
    } = useAuth();


    const [email, setEmail] =
        useState("");


    const [password, setPassword] =
        useState("");


    const [loading, setLoading] =
        useState(false);


    const [error, setError] =
        useState("");


    const handleSubmit =
        async (
            event
        ) => {

            event.preventDefault();


            try {

                setLoading(
                    true
                );

                setError(
                    ""
                );


                const response =
                    await login({
                        email,
                        password,
                    });


                const status =
                    response?.status ??
                    response?.loginStatus;


                /*
                 * Nếu backend trả các bước trung gian
                 * thì xử lý trước.
                 */
                if (
                    status
                    ===
                    "REQUIRE_CHANGE_PASSWORD"
                ) {

                    navigate(
                        "/change-password"
                    );

                    return;
                }


                if (
                    status
                    ===
                    "REQUIRE_2FA_SETUP"
                ) {

                    navigate(
                        "/2fa/setup"
                    );

                    return;
                }


                if (
                    status
                    ===
                    "REQUIRE_OTP"
                ) {

                    navigate(
                        "/2fa"
                    );

                    return;
                }


                /*
                 * Backend hiện trả trực tiếp user:
                 *
                 * {
                 *   id,
                 *   userCode,
                 *   name,
                 *   email,
                 *   authorities
                 * }
                 */
                if (
                    !response?.id
                    ||
                    !response?.name
                ) {

                    setError(
                        "Login succeeded but user information was not returned."
                    );

                    return;
                }


                /*
                 * Lưu user vào context
                 * + sessionStorage.
                 */
                setUser(
                    response
                );


                navigate(
                    "/warehouse/dashboard",
                    {
                        replace: true,
                    }
                );
            }
            catch (
                requestError
                ) {

                setError(
                    requestError
                        .response
                        ?.data
                        ?.message
                    ??
                    "Invalid email or password."
                );
            }
            finally {

                setLoading(
                    false
                );
            }
        };


    return (
        <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#fff8f4] p-4">

            <div className="absolute inset-0">

                <div className="absolute left-[-10%] top-[-10%] h-80 w-80 rounded-full bg-[#f25d19]/10 blur-3xl" />

                <div className="absolute bottom-[-15%] right-[-10%] h-96 w-96 rounded-full bg-orange-200/40 blur-3xl" />

            </div>


            <div className="relative z-10 w-full max-w-md">

                <div className="mb-6 text-center">

                    <div className="app-logo-frame app-logo-frame--large mx-auto">

                        <img
                            src="/logo.png"
                            alt="Toy Storage System logo"
                            className="app-logo app-logo--large"
                        />

                    </div>


                    <h1 className="mt-5 text-3xl font-bold tracking-tight text-slate-900">
                        Toy Storage System
                    </h1>


                    <p className="mt-2 text-sm text-slate-500">
                        Warehouse Management System
                    </p>

                </div>


                <Card className="border-slate-200 bg-white p-6 shadow-xl shadow-orange-100/60">

                    <form
                        className="space-y-5"
                        onSubmit={
                            handleSubmit
                        }
                    >

                        <div>

                            <h2 className="text-xl font-semibold text-slate-900">
                                Sign in
                            </h2>


                            <p className="mt-1 text-sm text-slate-500">
                                Enter your account credentials to continue.
                            </p>

                        </div>


                        {error && (

                            <Alert type="danger">
                                {error}
                            </Alert>

                        )}


                        <Input
                            label="Email"
                            type="email"
                            value={
                                email
                            }
                            placeholder="name@company.com"
                            onChange={(
                                event
                            ) =>
                                setEmail(
                                    event
                                        .target
                                        .value
                                )
                            }
                            required
                        />


                        <Input
                            label="Password"
                            type="password"
                            value={
                                password
                            }
                            placeholder="Enter your password"
                            onChange={(
                                event
                            ) =>
                                setPassword(
                                    event
                                        .target
                                        .value
                                )
                            }
                            required
                        />


                        <Button
                            type="submit"
                            size="lg"
                            className="w-full"
                            disabled={
                                loading
                            }
                        >

                            {loading
                                ? "Signing in..."
                                : "Sign in"}

                        </Button>

                    </form>

                </Card>

            </div>

        </div>
    );
}
