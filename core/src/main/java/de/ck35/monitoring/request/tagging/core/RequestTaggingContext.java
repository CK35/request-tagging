package de.ck35.monitoring.request.tagging.core;

import java.io.Closeable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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

    private final DefaultRequestTaggingStatusConsumer consumer;
    private final ScheduledThreadPoolExecutor executor;
    private final AtomicReference<Instant> nextResetReference;
    private final Supplier<Function<Instant, RequestTaggingStatusReporter>> requestTaggingStatusReporterFactory;
    private final Function<String, String> hashAlgorithm;

    private volatile Consumer<String> loggerInfo;
    private volatile BiConsumer<String, Throwable> loggerWarn;

    private volatile Clock clock;
    private volatile Duration collectorSendDelayDuration;
    private volatile Duration collectorResetDelayDuration;
    private volatile Function<Instant, RequestTaggingStatusReporter> requestTaggingStatusReporterReference;

    public RequestTaggingContext() {
        this(new RequestTaggingStatusReporterFactory()::build, new HashAlgorithm()::hash);
    }

    public RequestTaggingContext(Supplier<Function<Instant, RequestTaggingStatusReporter>> requestTaggingStatusReporterFactory,
                                 Function<String, String> hashAlgorithm) {
        nextResetReference = new AtomicReference<>();
        consumer = new DefaultRequestTaggingStatusConsumer();
        executor = new ScheduledThreadPoolExecutor(1);
        this.requestTaggingStatusReporterFactory = Objects.requireNonNull(requestTaggingStatusReporterFactory);
        this.hashAlgorithm = hashAlgorithm;

        loggerInfo = System.out::println;
        loggerWarn = (message, throwable) -> {
            System.out.println(message);
            throwable.printStackTrace();
        };

        setCollectorSendDelayDuration("PT1M");
        setCollectorResetDelayDuration("P1D");
    }

    public void initialize() {
        loggerInfo.accept("Initializing request tagging context.");

        requestTaggingStatusReporterReference = requestTaggingStatusReporterFactory.get();

        loggerInfo.accept("Scheduling send process for request tagging data with delay of '" + collectorSendDelayDuration + "'.");
        long startDelay = Math.max(0, clock.instant()
                                           .plus(collectorSendDelayDuration)
                                           .toEpochMilli()
                - Instant.now()
                         .toEpochMilli());
        executor.scheduleWithFixedDelay(this::send, startDelay, collectorSendDelayDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        loggerInfo.accept("Shutting down request tagging context.");
        executor.shutdown();
    }

    protected void send() {
        try {
            Instant now = clock.instant();
            RequestTaggingStatusReporter reporter = requestTaggingStatusReporterReference.apply(now);
            Instant nextReset = nextResetReference.getAndUpdate(reset -> now.isAfter(reset) ? now.plus(collectorResetDelayDuration) : reset);
            if (now.isAfter(nextReset)) {
                consumer.reportAndReset(reporter);
            } else {
                consumer.report(reporter);
            }
            reporter.commit();
        } catch (RuntimeException e) {
            loggerWarn.accept("Error while sending request tagging data!", e);
        }
    }

    public RequestTaggingRunnable taggingRunnable(Runnable runnable) {
        DefaultRequestTaggingStatus status = new DefaultRequestTaggingStatus(consumer, hashAlgorithm);
        return new RequestTaggingRunnable(runnable, status);
    }

    public void setLoggerInfo(Consumer<String> loggerInfo) {
        this.loggerInfo = Objects.requireNonNull(loggerInfo, "Can not set loggerInfo to null!");
    }

    public void setLoggerWarn(BiConsumer<String, Throwable> loggerWarn) {
        this.loggerWarn = Objects.requireNonNull(loggerWarn, "Can not set loggerWarn to null!");
    }

    public Clock getClock() {
        return clock;
    }

    public void setCollectorSendDelayDuration(String collectorSendDelayDuration) {
        this.collectorSendDelayDuration = Duration.parse(collectorSendDelayDuration);
        this.clock = Clock.tick(Clock.systemUTC(), this.collectorSendDelayDuration);
    }

    public Duration getCollectorSendDelayDuration() {
        return collectorSendDelayDuration;
    }

    public void setCollectorResetDelayDuration(String collectorResetDelayDuration) {
        this.collectorResetDelayDuration = Duration.parse(collectorResetDelayDuration);
        this.nextResetReference.set(clock.instant()
                                         .plus(this.collectorResetDelayDuration));
    }

    public Duration getCollectorResetDelayDuration() {
        return collectorResetDelayDuration;
    }
}