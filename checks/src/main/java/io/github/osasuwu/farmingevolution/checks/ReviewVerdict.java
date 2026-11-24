package io.github.osasuwu.farmingevolution.checks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reads the verdict file the CI reviewer leaves and reports why the review does not pass.
 * Uses only the JDK so the review job can run it as a single-file program from the base commit.
 */
public final class ReviewVerdict {
    /** The finding classes of REVIEW.md; a test keeps the two lists equal. */
    public static final Set<String> CLASSES = Set.of(
            "intent-vs-logic",
            "requirement-semantics",
            "regression",
            "breaking-contract",
            "exception-handling",
            "concurrency",
            "sidedness",
            "performance",
            "check-weakening",
            "hollow-test",
            "asset-source",
            "design-modularity");

    private static final String CLEAN = "verdict: clean";
    private static final String BLOCKING = "verdict: blocking";
    private static final String FINDING = "finding:";

    private ReviewVerdict() {
    }

    /**
     * Empty only for a file that says "verdict: clean" and nothing else. A blocking verdict
     * returns its findings; anything the reviewer was not asked to write is a problem too,
     * so a reviewer that never answered cannot pass.
     */
    public static List<String> problems(List<String> lines) {
        List<String> content = lines.stream().map(String::strip).filter(line -> !line.isEmpty()).toList();
        if (content.isEmpty()) {
            return List.of("no verdict: the file is empty");
        }
        String verdict = content.get(0);
        if (!verdict.equals(CLEAN) && !verdict.equals(BLOCKING)) {
            return List.of("no verdict: the first line is \"" + verdict + "\", expected \"" + CLEAN + "\" or \""
                    + BLOCKING + "\"");
        }
        boolean blocking = verdict.equals(BLOCKING);
        List<String> problems = new ArrayList<>();
        int findings = 0;
        for (String line : content.subList(1, content.size())) {
            if (!line.startsWith(FINDING)) {
                problems.add("unexpected line: " + line);
                continue;
            }
            findings++;
            String[] parts = line.substring(FINDING.length()).split("\\|", 3);
            if (parts.length < 3 || parts[1].isBlank() || parts[2].isBlank()) {
                problems.add("malformed finding, expected \"finding: <class> | <file> | <text>\": " + line);
            } else if (!CLASSES.contains(parts[0].strip())) {
                problems.add("unknown finding class \"" + parts[0].strip() + "\": " + line);
            } else if (!blocking) {
                problems.add("verdict is clean but a finding is listed: " + line);
            } else {
                problems.add(line);
            }
        }
        if (blocking && findings == 0) {
            problems.add("verdict is blocking but no finding is listed");
        }
        return problems;
    }

    public static void main(String[] args) throws IOException {
        Path verdict = Path.of(args[0]);
        if (!Files.isRegularFile(verdict)) {
            System.err.println("Review verdict not found: " + verdict);
            System.exit(1);
        }
        List<String> problems = problems(Files.readAllLines(verdict, StandardCharsets.UTF_8));
        if (!problems.isEmpty()) {
            System.err.println("Review does not pass (" + verdict + "):");
            problems.forEach(problem -> System.err.println("  " + problem));
            System.exit(1);
        }
        System.out.println("Review is clean: " + verdict);
    }
}
