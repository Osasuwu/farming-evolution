package io.github.osasuwu.farmingevolution.checks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fails when the git index holds build output or decompiled Minecraft sources.
 * Run by the check pipeline and, as a single-file program, by .githooks/pre-commit,
 * so it must not depend on anything outside the JDK.
 */
public final class TrackedFilesCheck {
    /** Directories of a Gradle project that hold build output; the decompiled game lands in build/. */
    private static final Set<String> OUTPUT_DIRS = Set.of("build", ".gradle", "run");

    private TrackedFilesCheck() {
    }

    public static List<String> forbidden(List<String> paths) {
        List<String> forbidden = new ArrayList<>();
        for (String path : paths) {
            if (isForbidden(path)) {
                forbidden.add(path);
            }
        }
        return forbidden;
    }

    private static boolean isForbidden(String path) {
        String normalized = path.replace('\\', '/');
        if (normalized.endsWith("-sources.jar") || normalized.endsWith("-merged.jar")) {
            return true;
        }
        String[] segments = normalized.split("/");
        // The last segment is the file name, never a directory.
        for (int i = 0; i < segments.length - 1; i++) {
            // Root project or a direct subproject; deeper, the same name is a Java package.
            if (i <= 1 && OUTPUT_DIRS.contains(segments[i])) {
                return true;
            }
            if (i + 1 < segments.length - 1) {
                String pair = segments[i] + "/" + segments[i + 1];
                if (pair.equals("net/minecraft") || pair.equals("com/mojang")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Process git = new ProcessBuilder("git", "ls-files", "-z")
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        String output = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (git.waitFor() != 0) {
            System.err.println("git ls-files failed");
            System.exit(1);
        }
        List<String> paths = new ArrayList<>();
        for (String path : output.split("\0")) {
            if (!path.isEmpty()) {
                paths.add(path);
            }
        }
        List<String> forbidden = forbidden(paths);
        if (!forbidden.isEmpty()) {
            System.err.println("Build output or decompiled Minecraft sources are staged or tracked:");
            forbidden.forEach(path -> System.err.println("  " + path));
            System.err.println("Unstage them: git rm --cached <path>");
            System.exit(1);
        }
    }
}
