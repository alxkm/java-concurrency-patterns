package org.alxkm;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks on the test code itself.
 * <p>
 * Three times in this repository's history a commented-out test hid a real bug in the class it was
 * meant to test (ReactorTest, SemaphorePrintQueueExampleTest, LeaderFollowerPatternTest), and a fourth
 * hid a test that asserted the outcome of a race. A commented-out test is invisible: it does not fail,
 * it is not reported as skipped, and the test count drops by one without saying why.
 * <p>
 * The honest options are to fix the test or to disable it with the reason written down, where the
 * report shows it. These checks keep it to those two, for unit tests, benchmarks and stress tests alike.
 */
class RepositoryHygieneTest {

    private static final List<Path> SOURCE_ROOTS =
            List.of(Path.of("src/test/java"), Path.of("src/jcstress/java"), Path.of("src/jmh/java"));

    /** The annotations that make a method run. Commenting one out silently removes a check. */
    private static final Pattern RUNNABLE_ANNOTATION = Pattern.compile(
            "^@(Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate|Benchmark|JCStressTest|Actor)\\b");

    /** A disabling annotation with no reason, or an empty one. */
    private static final Pattern DISABLED_WITHOUT_REASON =
            Pattern.compile("@Disabled(\\s*\\(\\s*(\"\\s*\")?\\s*\\))?\\s*$");

    @Test
    void noTestIsCommentedOut() throws IOException {
        List<String> found = new ArrayList<>();
        for (Path file : javaSources()) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int line : commentedOutTests(lines)) {
                found.add(file + ":" + line + ": " + lines.get(line - 1).trim());
            }
        }
        assertTrue(found.isEmpty(), () -> "Commented-out tests hide failures. Fix them, or disable them with "
                + "@Disabled(\"reason\") so the report shows they are off:\n  " + String.join("\n  ", found));
    }

    @Test
    void everyDisabledTestSaysWhy() throws IOException {
        List<String> found = new ArrayList<>();
        for (Path file : javaSources()) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int line : disabledWithoutReason(lines)) {
                found.add(file + ":" + line + ": " + lines.get(line - 1).trim());
            }
        }
        assertTrue(found.isEmpty(), () -> "A disabled test needs a reason, @Disabled(\"why\"), so the "
                + "report says why it is off:\n  " + String.join("\n  ", found));
    }

    /**
     * A check that never fires looks exactly like a codebase with nothing to find, so the checks are run
     * against code that must trip them, and code that must not.
     */
    @Test
    void theChecksCatchWhatTheyAreFor() {
        List<String> sample = List.of(
                "class Sample {",                                   // 1
                "//    @Test",                                      // 2  caught: line comment
                "    void one() { }",                               // 3
                "    /* @RepeatedTest(10) */",                      // 4  caught: block comment on one line
                "    /*",                                           // 5
                "    @Test",                                        // 6  caught: inside a block comment
                "    void two() { }",                               // 7
                "    */",                                           // 8
                "    /**",                                          // 9
                "     * Explains why {@code @Test} is not enough.", // 10 fine: prose about an annotation
                "     */",                                          // 11
                "    @Test",                                        // 12 fine: a live test
                "    void three() { }",                             // 13
                "    @Disabled",                                    // 14 caught: no reason
                "    @Disabled(\"\")",                              // 15 caught: empty reason
                "    @Disabled(\"waits for JDK-8000000\")",         // 16 fine: has a reason
                "}");

        assertEquals(List.of(2, 4, 6), commentedOutTests(sample), "commented-out annotations found");
        assertEquals(List.of(14, 15), disabledWithoutReason(sample), "reasonless @Disabled found");
    }

    /** One-based numbers of the lines where a runnable annotation sits behind a comment marker. */
    static List<Integer> commentedOutTests(List<String> lines) {
        List<Integer> found = new ArrayList<>();
        boolean inBlockComment = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            String afterMarker = null;
            if (inBlockComment) {
                afterMarker = line.startsWith("*") ? line.substring(1).trim() : line;
            } else if (line.startsWith("//")) {
                afterMarker = line.substring(2).trim();
            } else if (line.startsWith("/*")) {
                afterMarker = line.replaceFirst("^/\\*+", "").trim();
            }
            if (afterMarker != null && RUNNABLE_ANNOTATION.matcher(afterMarker).find()) {
                found.add(i + 1);
            }
            inBlockComment = nextLineInBlockComment(line, inBlockComment);
        }
        return found;
    }

    /** One-based numbers of the lines with a reasonless {@code @Disabled}. */
    static List<Integer> disabledWithoutReason(List<String> lines) {
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (DISABLED_WITHOUT_REASON.matcher(lines.get(i)).find()) {
                found.add(i + 1);
            }
        }
        return found;
    }

    /** Whether the line after this one starts inside a block comment. Good enough for test sources. */
    private static boolean nextLineInBlockComment(String line, boolean inside) {
        int open = line.lastIndexOf("/*");
        int close = line.lastIndexOf("*/");
        if (open >= 0 && open > close) {
            return true;
        }
        return inside && close < 0;
    }

    private static List<Path> javaSources() throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path root : SOURCE_ROOTS) {
            if (Files.isDirectory(root)) {
                try (Stream<Path> walk = Files.walk(root)) {
                    walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
                }
            }
        }
        // An empty scan would pass both checks. Finding this very file proves the roots resolved.
        assertTrue(files.contains(Path.of("src/test/java/org/alxkm/RepositoryHygieneTest.java")),
                "did not find the test sources; is the working directory the project?");
        return files;
    }
}
