// Sends actions to the client started by gradlew runControl (issue #74) and prints its replies.
// Plain Java, no dependencies; run with a JDK 21: java tools/ClientControl.java ...
// How to use it and the list of actions: README.md, "Управление клиентом".
//
//   java tools/ClientControl.java <action words...>      one action
//   java tools/ClientControl.java --scenario <file>       every line of the file, in order
//   java tools/ClientControl.java --wait-ready [seconds]  waits until the client takes actions
//   option --port <n> (default 25599, the same as -PcontrolPort)
//
// Exit code: 0 ok, 1 an action failed (reply starts with "error"), 2 no client to talk to.

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ClientControl {
    /** The game ends an action that takes longer itself; this only guards against a stuck game. */
    private static final int REPLY_TIMEOUT_MILLIS = 11 * 60 * 1000;

    public static void main(String[] args) throws Exception {
        int port = 25599;
        List<String> words = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port")) {
                port = number(i + 1 < args.length ? args[++i] : null, "--port");
            } else {
                words.add(args[i]);
            }
        }
        if (words.isEmpty()) {
            System.err.println("usage: java tools/ClientControl.java [--port N] <action> | --scenario <file> | --wait-ready [seconds]");
            System.exit(2);
        }
        int code;
        if (words.get(0).equals("--wait-ready")) {
            int seconds = words.size() > 1 ? number(words.get(1), "--wait-ready") : 300;
            code = waitReady(port, seconds);
        } else if (words.get(0).equals("--scenario")) {
            if (words.size() != 2) {
                System.err.println("--scenario takes one file");
                System.exit(2);
            }
            code = scenario(port, Path.of(words.get(1)));
        } else {
            code = send(port, String.join(" ", words));
        }
        System.exit(code);
    }

    /** A positive whole number for an option, or exit 2 (a usage error, like a missing client). */
    private static int number(String value, String option) {
        try {
            int number = value == null ? -1 : Integer.parseInt(value);
            if (number > 0) {
                return number;
            }
        } catch (NumberFormatException e) {
            // reported below
        }
        System.err.println(option + " takes a positive whole number, not " + value);
        System.exit(2);
        return -1;
    }

    /** Runs the lines of the file one by one; blank lines and lines starting with # are skipped. Stops at the first error. */
    private static int scenario(int port, Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            System.out.println("> " + line);
            int code = send(port, line);
            if (code != 0) {
                return code;
            }
        }
        return 0;
    }

    private static int waitReady(int port, int seconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                String reply = exchange(port, "state");
                System.out.print(reply);
                return reply.startsWith("ok") ? 0 : 1;
            } catch (IOException e) {
                Thread.sleep(1000);
            }
        }
        System.err.println("no client on 127.0.0.1:" + port + " after " + seconds + " s; is gradlew runControl running?");
        return 2;
    }

    private static int send(int port, String line) {
        try {
            String reply = exchange(port, line);
            System.out.print(reply);
            return reply.startsWith("ok") ? 0 : 1;
        } catch (ConnectException e) {
            System.err.println("no client on 127.0.0.1:" + port + "; start it with gradlew runControl and wait with --wait-ready");
            return 2;
        } catch (IOException e) {
            System.err.println("connection failed: " + e);
            return 2;
        }
    }

    private static String exchange(int port, String line) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), port), 2000);
            socket.setSoTimeout(REPLY_TIMEOUT_MILLIS);
            OutputStream out = socket.getOutputStream();
            out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder reply = new StringBuilder();
            String got;
            while ((got = reader.readLine()) != null) {
                reply.append(got).append(System.lineSeparator());
            }
            if (reply.isEmpty()) {
                throw new IOException("the client closed the connection without a reply");
            }
            return reply.toString();
        }
    }
}
