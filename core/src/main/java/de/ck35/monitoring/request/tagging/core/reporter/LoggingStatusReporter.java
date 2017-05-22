package de.ck35.monitoring.request.tagging.core.reporter;

import java.time.Instant;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A status reporter which can be used for writing request-data directly into a
 * log file.
 * 
 * @author Christian Kaspari
 * @since 2.0.0
 */
public class LoggingStatusReporter implements StatusReporter {

    private final Instant instant;
    private final Consumer<String> logger;
    private final StringBuilder report;
    private final BiFunction<Instant, Consumer<String>, StatusReporter> reporters;

    private StatusReporter reporter;

    public LoggingStatusReporter(Instant instant, Consumer<String> logger, BiFunction<Instant, Consumer<String>, StatusReporter> reporters) {
        this.logger = logger;
        this.instant = instant;
        this.reporters = reporters;
        this.report = new StringBuilder();
    }

    public static Function<Instant, StatusReporter> statusReporter(Consumer<String> logger, BiFunction<Instant, Consumer<String>, StatusReporter> reporters) {
        return instant -> new LoggingStatusReporter(instant, logger, reporters);
    }

    @Override
    public void accept(Resource resource) {
        if (reporter == null) {
            reporter = reporters.apply(instant, report::append);
        }
        reporter.accept(resource);
    }

    @Override
    public void close() {
        if (reporter == null) {
            return;
        }
        reporter.close();
        logger.accept(report.toString());
    }
}