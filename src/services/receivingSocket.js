import { Client } from "@stomp/stompjs";

function resolveWebSocketUrl() {
    const configured =
        import.meta.env.VITE_WS_URL;

    if (configured) {
        return configured;
    }

    const protocol =
        window.location.protocol === "https:"
            ? "wss:"
            : "ws:";

    return `${protocol}//${window.location.host}/ws`;
}

export function createReceivingSocket({
    onConnected,
    onDisconnected,
    onError,
}) {
    const client = new Client({
        brokerURL: resolveWebSocketUrl(),

        reconnectDelay: 3000,

        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,

        debug: () => {},
    });

    client.onConnect = () => {
        onConnected?.(client);
    };

    client.onWebSocketClose = () => {
        onDisconnected?.();
    };

    client.onStompError = (frame) => {
        onError?.(
            frame?.headers?.message ??
            "Receiving realtime connection failed."
        );
    };

    client.onWebSocketError = () => {
        onError?.(
            "Unable to connect to receiving realtime service."
        );
    };

    return client;
}
