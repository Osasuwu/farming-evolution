package io.github.osasuwu.farmingevolution.checks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reads the log of a dedicated server boot and reports what makes the boot a failure:
 * any ERROR or FATAL line, or no completed startup.
 */
public final class ServerLogScan {
    /** Log level as the game writes it: "[Server thread/ERROR]". */
    private static final Pattern FAILURE_LEVEL = Pattern.compile("\\[[^\\]]*/(ERROR|FATAL)\\]");
    /** The line the dedicated server writes once startup is complete. */
    private static final Pattern STARTED = Pattern.compile("Done \\(.*\\)! For help");

    /**
     * Vanilla logs this at ERROR when the game directory has no server.properties yet,
     * then writes the file with defaults. It says nothing about the mod.
     */
    private static final String FIRST_BOOT_NOISE = "Failed to load properties from file: server.properties";

    private ServerLogScan() {
    }

    public static List<String> problems(List<String> lines) {
        List<String> problems = new ArrayList<>();
        boolean started = false;
        for (String line : lines) {
            if (FAILURE_LEVEL.matcher(line).find() && !line.contains(FIRST_BOOT_NOISE)) {
                problems.add("error in log: " + line);
            }
            if (STARTED.matcher(line).find()) {
                started = true;
            }
        }
        if (!started) {
            problems.add("server never finished starting: no \"Done (...)!\" line");
        }
        return problems;
    }

    public static void main(String[] args) throws IOException {
        Path log = Path.of(args[0]);
        if (!Files.isRegularFile(log)) {
            System.err.println("Server log not found: " + log);
            System.exit(1);
        }
        List<String> problems = problems(Files.readAllLines(log, StandardCharsets.UTF_8));
        if (!problems.isEmpty()) {
            System.err.println("Dedicated server boot failed (" + log + "):");
            problems.forEach(problem -> System.err.println("  " + problem));
            System.exit(1);
        }
        System.out.println("Dedicated server booted cleanly: " + log);
    }
}
