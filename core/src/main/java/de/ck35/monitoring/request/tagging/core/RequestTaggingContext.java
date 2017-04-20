package de.ck35.monitoring.request.tagging.core;

import java.io.Closeable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporterFactory;

/**
 * A reusable context for Request Tagging.
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class RequestTaggingContext implements Closeable {

    private final DefaultRequestTaggingStatusConsumer statusConsumer;
    private final ScheduledThreadPoolExecutor executor;
    private final Supplier<Function<Instant, RequestTaggingStatusReporter>> requestTaggingStatusReporterFactory;
    private final DefaultRequestTaggingStatus defaultStatus;

    private volatile Consumer<String> loggerInfo;
    private volatile BiConsumer<String, Throwable> loggerWarn;

    private volatile Clock sendIntervalClock;
    private volatile Duration collectorSendDelayDuration;
    private volatile Function<Instant, RequestTaggingStatusReporter> requestTaggingStatusReporterReference;

    public RequestTaggingContext() {
        this(new RequestTaggingStatusReporterFactory()::build);
    }

    public RequestTaggingContext(Supplier<Function<Instant, RequestTaggingStatusReporter>> requestTaggingStatusReporterFactory) {
        this(requestTaggingStatusReporterFactory, new HashAlgorithm()::hash, Clock.systemUTC());
    }

    public RequestTaggingContext(Supplier<Function<Instant, RequestTaggingStatusReporter>> requestTaggingStatusReporterFactory, Function<String, String> hashAlgorithm, Clock measurementClock) {
        this.requestTaggingStatusReporterFactory = Objects.requireNonNull(requestTaggingStatusReporterFactory);
        this.statusConsumer = new DefaultRequestTaggingStatusConsumer();
        defaultStatus = new DefaultRequestTaggingStatus(statusConsumer, hashAlgorithm, measurementClock);
        executor = new ScheduledThreadPoolExecutor(1);

        loggerInfo = System.out::println;
        loggerWarn = (message, throwable) -> {
            System.out.println(message);
            throwable.printStackTrace();
        };

        setCollectorSendDelayDuration("PT1M");
    }

    public void initialize() {
        loggerInfo.accept("Initializing request tagging context.");

        requestTaggingStatusReporterReference = requestTaggingStatusReporterFactory.get();

        loggerInfo.accept("Scheduling send process for request tagging data with delay of '" + collectorSendDelayDuration + "'.");
        long startDelay = Math.max(0, sendIntervalClock.instant()
                                               .plus(collectorSendDelayDuration)
                                               .toEpochMilli()
                - Instant.now()
                         .toEpochMilli());
        executor.scheduleWithFixedDelay(this::send, startDelay, collectorSendDelayDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void runWithinContext(Runnable runnable) {
        taggingRunnable(runnable).run();
    }

    public RequestTaggingRunnable taggingRunnable(Runnable runnable) {
        return new RequestTaggingRunnable(runnable, new DefaultRequestTaggingStatus(defaultStatus));
    }

    @Override
    public void close() {
        loggerInfo.accept("Shutting down request tagging context.");
        executor.shutdown();
    }

    protected void send() {
        try {
            Instant now = sendIntervalClock.instant();
            RequestTaggingStatusReporter reporter = requestTaggingStatusReporterReference.apply(now);
            try {
                statusConsumer.report(reporter);
            } finally {                
                reporter.close();
            }
        } catch (RuntimeException e) {
            loggerWarn.accept("Error while sending request tagging data!", e);
        }
    }

    public void setLoggerInfo(Consumer<String> loggerInfo) {
        this.loggerInfo = Objects.requireNonNull(loggerInfo, "Can not set loggerInfo to null!");
    }

    public void setLoggerWarn(BiConsumer<String, Throwable> loggerWarn) {
        this.loggerWarn = Objects.requireNonNull(loggerWarn, "Can not set loggerWarn to null!");
    }

    public Clock getSendIntervalClock() {
        return sendIntervalClock;
    }

    public void setCollectorSendDelayDuration(String collectorSendDelayDuration) {
        this.collectorSendDelayDuration = Duration.parse(collectorSendDelayDuration);
        this.sendIntervalClock = Clock.tick(Clock.systemUTC(), this.collectorSendDelayDuration);
    }

    public Duration getCollectorSendDelayDuration() {
        return collectorSendDelayDuration;
    }

    public DefaultRequestTaggingStatusConsumer getStatusConsumer() {
        return statusConsumer;
    }

    public DefaultRequestTaggingStatus getDefaultRequestTaggingStatus() {
        return defaultStatus;
    }
}