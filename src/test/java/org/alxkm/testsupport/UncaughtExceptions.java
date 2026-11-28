package org.alxkm.testsupport;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fails a test when a thread dies of an uncaught exception while the test runs.
 * <p>
 * An exception thrown on a thread the test did not join goes to the thread's uncaught exception handler,
 * which prints it to stderr, and the test passes anyway. That is how a broken example stays green:
 * its main() starts two threads, one of them fails, and mainRuns() sees nothing. With this extension the
 * test fails and names the thread.
 * <p>
 * It is registered for every test through JUnit's extension autodetection
 * (src/test/resources/META-INF/services and junit-platform.properties), so a new test cannot forget it.
 * <p>
 * What it sees and what it does not:
 * <ul>
 *   <li>It replaces the JVM-wide default handler for the duration of each test, so it sees threads that
 *   have no handler of their own: plain threads and tasks passed to an executor's execute().</li>
 *   <li>A task passed to submit() does not die of its exception; the Future keeps it, and only get()
 *   reports it. That stays the test's job.</li>
 *   <li>A thread a test leaves behind that fails after the test has finished is charged to whichever
 *   test is running then. The failure names the thread, which says where it came from.</li>
 *   <li>It relies on tests running one at a time, as they do here: with parallel execution, one test's
 *   handler would catch another test's threads.</li>
 * </ul>
 */
public final class UncaughtExceptions implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UncaughtExceptions.class);

    /**
     * A thread that died, and what it died of.
     *
     * @param thread the thread's name.
     * @param error  the uncaught exception.
     */
    public record Death(String thread, Throwable error) {
    }

    /**
     * Records every uncaught exception between {@link #install()} and {@link #uninstall()}, and prints it
     * as the JVM would. It does not pass exceptions on to the handler it replaced, so a recorder
     * installed inside a test keeps what it sees to itself.
     */
    public static final class Recorder implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler previous;
        private final List<Death> deaths = new CopyOnWriteArrayList<>();

        private Recorder(Thread.UncaughtExceptionHandler previous) {
            this.previous = previous;
        }

        /**
         * Makes a new recorder the default handler.
         *
         * @return the recorder; call {@link #uninstall()} on it to put the previous handler back.
         */
        public static Recorder install() {
            Recorder recorder = new Recorder(Thread.getDefaultUncaughtExceptionHandler());
            Thread.setDefaultUncaughtExceptionHandler(recorder);
            return recorder;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable error) {
            deaths.add(new Death(thread.getName(), error));
            System.err.print("Exception in thread \"" + thread.getName() + "\" ");
            error.printStackTrace(System.err);
        }

        /**
         * @return the threads that have died so far, in the order they died.
         */
        public List<Death> deaths() {
            return List.copyOf(deaths);
        }

        /**
         * Puts back the handler that was the default before {@link #install()}.
         *
         * @return the threads that died meanwhile, in the order they died.
         */
        public List<Death> uninstall() {
            Thread.setDefaultUncaughtExceptionHandler(previous);
            return List.copyOf(deaths);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getStore(NAMESPACE).put(Recorder.class, Recorder.install());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Recorder recorder = context.getStore(NAMESPACE).remove(Recorder.class, Recorder.class);
        if (recorder == null) {
            return;
        }
        List<Death> deaths = recorder.uninstall();
        if (deaths.isEmpty()) {
            return;
        }
        Death first = deaths.get(0);
        AssertionError failure = new AssertionError(deaths.size() + " thread(s) died of an uncaught exception"
                + " during the test, first \"" + first.thread() + "\": " + first.error(), first.error());
        for (Death other : deaths.subList(1, deaths.size())) {
            failure.addSuppressed(other.error());
        }
        throw failure;
    }
}
