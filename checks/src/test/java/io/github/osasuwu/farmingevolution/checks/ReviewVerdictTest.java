package io.github.osasuwu.farmingevolution.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ReviewVerdictTest {
    @Test
    void cleanVerdictPasses() {
        assertEquals(List.of(), ReviewVerdict.problems(List.of("verdict: clean")));
    }

    @Test
    void placeholderLeftByAReviewerThatNeverAnsweredFails() {
        assertEquals(List.of("no verdict: the first line is \"unreviewed\", expected \"verdict: clean\" or \"verdict: blocking\""),
                ReviewVerdict.problems(List.of("unreviewed")));
    }

    @Test
    void emptyFileFails() {
        assertEquals(List.of("no verdict: the file is empty"), ReviewVerdict.problems(List.of("", "  ")));
    }

    @Test
    void blockingVerdictReportsEachFinding() {
        assertEquals(
                List.of("finding: sidedness | src/main/java/Foo.java:12 | client class loaded on the server",
                        "finding: hollow-test | checks/src/test/java/FooTest.java | asserts nothing about the result"),
                ReviewVerdict.problems(List.of(
                        "verdict: blocking",
                        "finding: sidedness | src/main/java/Foo.java:12 | client class loaded on the server",
                        "",
                        "finding: hollow-test | checks/src/test/java/FooTest.java | asserts nothing about the result")));
    }

    @Test
    void blockingVerdictWithoutFindingsFails() {
        assertEquals(List.of("verdict is blocking but no finding is listed"),
                ReviewVerdict.problems(List.of("verdict: blocking")));
    }

    @Test
    void cleanVerdictWithFindingsFails() {
        assertEquals(
                List.of("verdict is clean but a finding is listed: finding: regression | a.java | broke it again"),
                ReviewVerdict.problems(List.of("verdict: clean", "finding: regression | a.java | broke it again")));
    }

    @Test
    void unknownVerdictValueFails() {
        assertEquals(List.of("no verdict: the first line is \"verdict: fine\", expected \"verdict: clean\" or \"verdict: blocking\""),
                ReviewVerdict.problems(List.of("verdict: fine")));
    }

    @Test
    void findingWithUnknownClassFails() {
        assertEquals(List.of("unknown finding class \"nitpick\": finding: nitpick | a.java | rename it"),
                ReviewVerdict.problems(List.of("verdict: blocking", "finding: nitpick | a.java | rename it")));
    }

    @Test
    void findingWithoutThreePartsFails() {
        assertEquals(List.of("malformed finding, expected \"finding: <class> | <file> | <text>\": finding: regression | a.java"),
                ReviewVerdict.problems(List.of("verdict: blocking", "finding: regression | a.java")));
    }

    @Test
    void proseBesideTheVerdictFails() {
        assertEquals(List.of("unexpected line: Looks good to me."),
                ReviewVerdict.problems(List.of("verdict: clean", "Looks good to me.")));
    }

    /** REVIEW.md tells the reviewer which classes exist; the validator must accept exactly those. */
    @Test
    void classesMatchReviewRules() throws IOException {
        Path rules = Path.of("..", "REVIEW.md");
        Set<String> documented = new TreeSet<>();
        boolean inSection = false;
        Pattern entry = Pattern.compile("^- `([a-z-]+)`");
        for (String line : Files.readAllLines(rules, StandardCharsets.UTF_8)) {
            if (line.startsWith("## ")) {
                inSection = line.equals("## Классы находок");
                continue;
            }
            Matcher matcher = entry.matcher(line);
            if (inSection && matcher.find()) {
                documented.add(matcher.group(1));
            }
        }
        assertEquals(documented, new TreeSet<>(ReviewVerdict.CLASSES));
    }
}
