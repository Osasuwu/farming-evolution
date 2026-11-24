package io.github.osasuwu.farmingevolution.screenshot;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;

/**
 * The listening end of the client control ({@link ControlRun}): one connection is one action.
 * The client sends one line, the game thread performs it, and the reply text goes back before
 * the connection closes. Bound to the loopback address only, so nothing outside the machine
 * reaches it.
 */
final class ControlServer {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** A client that connects and sends nothing is dropped after this long. */
    private static final int READ_TIMEOUT_MILLIS = 30_000;

    /** The longest an action may take; the game ends a slower one itself ({@link ControlRun}). */
    private static final long REPLY_TIMEOUT_MINUTES = 10;

    /** One line from a client and the reply the game thread completes. */
    record Request(String line, CompletableFuture<String> reply) {
    }

    private final Queue<Request> requests = new ConcurrentLinkedQueue<>();
    private final ServerSocket socket;

    private ControlServer(ServerSocket socket) {
        this.socket = socket;
    }

    /** Binds 127.0.0.1:port and serves on a daemon thread. */
    static ControlServer start(int port) throws IOException {
        ServerSocket socket = new ServerSocket();
        // 127.0.0.1 by number: the game prefers IPv6, so the "loopback address" would be ::1 here.
        socket.bind(new InetSocketAddress(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), port));
        ControlServer server = new ControlServer(socket);
        Thread thread = new Thread(server::serve, "Farming Evolution client control");
        thread.setDaemon(true);
        thread.start();
        return server;
    }

    /** The next action a client sent, or null. Read on the game thread. */
    Request poll() {
        return requests.poll();
    }

    void close() {
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.warn("Client control: closing the socket failed", e);
        }
    }

    private void serve() {
        while (!socket.isClosed()) {
            try (Socket client = socket.accept()) {
                client.setSoTimeout(READ_TIMEOUT_MILLIS);
                answer(client);
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    LOGGER.warn("Client control: a connection failed", e);
                }
            }
        }
    }

    private void answer(Socket client) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
        String line;
        try {
            line = reader.readLine();
        } catch (SocketTimeoutException e) {
            return;
        }
        if (line == null) {
            return;
        }
        var request = new Request(line.strip(), new CompletableFuture<>());
        requests.add(request);
        String reply;
        try {
            reply = request.reply().get(REPLY_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            reply = "error the game did not answer in " + REPLY_TIMEOUT_MINUTES + " minutes\n";
        } catch (Exception e) {
            reply = "error " + e + "\n";
        }
        OutputStream out = client.getOutputStream();
        out.write(reply.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
