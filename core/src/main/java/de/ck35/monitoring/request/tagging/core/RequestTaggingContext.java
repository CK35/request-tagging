package de.ck35.monitoring.request.tagging.core;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter;
import de.ck35.monitoring.request.tagging.core.reporter.influxdb.InfluxDB;


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

    private volatile Supplier<Function<Instant, RequestTaggingStatusReporter>> defaultRequestTaggingStatusReporterSupplier;
    
    private volatile Consumer<String> loggerInfo;
    private volatile BiConsumer<String, Throwable> loggerWarn;
    
    private volatile Clock clock;
    private volatile Duration collectorSendDelayDuration;
    private volatile Duration collectorResetDelayDuration;
    private volatile Function<Instant, RequestTaggingStatusReporter> reporterFunctionReference;

    private volatile String localHostName;
    private volatile String localInstanceId;

    private volatile boolean reportToInfluxDB;
    private volatile String influxDBProtocol;
    private volatile String influxDBHostName;
    private volatile String influxDBPort;
    private volatile String influxDBDatabaseName;
    
    private volatile int connectionTimeout;
    private volatile int readTimeout;
    
    public RequestTaggingContext() {
        collectorSendDelayDuration = Duration.parse("PT1M");
        collectorResetDelayDuration = Duration.parse("P1D");

        consumer = new DefaultRequestTaggingStatusConsumer();
        executor = new ScheduledThreadPoolExecutor(1);
        nextResetReference = new AtomicReference<>(Instant.now()
                                                          .plus(collectorResetDelayDuration));
        
        defaultRequestTaggingStatusReporterSupplier = () -> DefaultReporter.forLogger(loggerInfo);
        
        loggerInfo = System.out::println;
        loggerWarn = (message, throwable) -> {
            System.out.println(message);
            throwable.printStackTrace();
        };
        
        localInstanceId = null;
        localHostName = null;
        
        reportToInfluxDB = false;
        influxDBProtocol = "http";
        influxDBHostName = "localhost";
        influxDBPort = "8086";
        influxDBDatabaseName = "request-tagging";
        connectionTimeout = 5000;
        readTimeout = 5000;
    }
    
    public void initialize() {
        loggerInfo.accept("Initializing request tagging context.");
        
        clock = Clock.tick(Clock.systemUTC(), collectorSendDelayDuration);
        
        if(localHostName == null) {
            try {
                localHostName = InetAddress.getLocalHost()
                                           .getHostName();
            } catch (UnknownHostException e) {
                throw new RuntimeException("Could not resolve local host name! Please configure 'localHostName' property.", e);
            }
        }

        if (reportToInfluxDB) {
            loggerInfo.accept("Using InfluxDB: '" + influxDBHostName + ":" + influxDBPort + "' for request tagging data reporting.");
            reporterFunctionReference = new InfluxDB(localHostName,
                                                     localInstanceId,
                                                     influxDBProtocol,
                                                     influxDBHostName,
                                                     influxDBPort,
                                                     influxDBDatabaseName,
                                                     connectionTimeout,
                                                     readTimeout)::reporter;
        } else {
            loggerInfo.accept("Could not create request tagging data reporter. Falling back to default.");
            reporterFunctionReference = defaultRequestTaggingStatusReporterSupplier.get();
        }

        loggerInfo.accept("Scheduling send process for request tagging data with delay of '" + collectorSendDelayDuration + "'.");
        long startDelay = Math.max(0, clock.instant().plus(collectorSendDelayDuration).toEpochMilli() - Instant.now().toEpochMilli());
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
            RequestTaggingStatusReporter reporter = reporterFunctionReference.apply(now);
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
        DefaultRequestTaggingStatus status = new DefaultRequestTaggingStatus(consumer);
        return new RequestTaggingRunnable(runnable, status);
    }
    
    public static class DefaultReporter implements RequestTaggingStatusReporter {
        
        private final Consumer<String> logger;
        private final Instant instant;
        
        public DefaultReporter(Consumer<String> logger, Instant instant) {
            this.logger = logger;
            this.instant = instant;
        }
        @Override
        public void accept(String resourceName, Map<String, Long> statusCodeCounters, Map<String, String> metaData) {
            logger.accept("Request Data: [" + instant.toString() + "] " + resourceName + ": " + statusCodeCounters + " - " + metaData);
        }
        @Override
        public void commit() {
            //Ignore because already logged data
        }
        public static Function<Instant, RequestTaggingStatusReporter> forLogger(Consumer<String> logger) {
            return instant -> new DefaultReporter(logger, instant);
        }
    }
    
    public void setDefaultRequestTaggingStatusReporterSupplier(Supplier<Function<Instant, RequestTaggingStatusReporter>> defaultRequestTaggingStatusReporterSupplier) {
        this.defaultRequestTaggingStatusReporterSupplier = Objects.requireNonNull(defaultRequestTaggingStatusReporterSupplier, "Can not set defaultRequestTaggingStatusReporterSupplier to null!");
    }
    public void setLoggerInfo(Consumer<String> loggerInfo) {
        this.loggerInfo = Objects.requireNonNull(loggerInfo, "Can not set loggerInfo to null!");
    }
    public void setLoggerWarn(BiConsumer<String, Throwable> loggerWarn) {
        this.loggerWarn = Objects.requireNonNull(loggerWarn, "Can not set loggerWarn to null!");
    }
    public void setCollectorSendDelayDuration(String collectorSendDelayDuration) {
        this.collectorSendDelayDuration = Duration.parse(collectorSendDelayDuration);
    }
    public void setCollectorResetDelayDuration(String collectorResetDelayDuration) {
        this.collectorResetDelayDuration = Duration.parse(collectorResetDelayDuration);
    }
    public void setLocalHostName(String localHostName) {
        this.localHostName = Objects.requireNonNull(localHostName, "Can not set localHostName to null!");
    }
    public void setLocalInstanceId(String localInstanceId) {
        this.localInstanceId = Objects.requireNonNull(localInstanceId, "Can not set localInstanceId to null!");
    }

    public void setReportToInfluxDB(boolean reportToInfluxDB) {
        this.reportToInfluxDB = reportToInfluxDB;
    }
    public void setInfluxDBProtocol(String influxDBProtocol) {
        this.influxDBProtocol = Objects.requireNonNull(influxDBProtocol, "Can not set influxDBProtocol to null!");
    }
    public void setInfluxDBHostName(String influxDBHostName) {
        this.influxDBHostName = Objects.requireNonNull(influxDBHostName, "Can not set influxDBHostName to null!");
    }
    public void setInfluxDBPort(String influxDBPort) {
        this.influxDBPort = Objects.requireNonNull(influxDBPort, "Can not set influxDBPort to null!");
    }
    public void setInfluxDBDatabaseName(String influxDBDatabaseName) {
        this.influxDBDatabaseName = Objects.requireNonNull(influxDBDatabaseName, "Can not set influxDBDatabaseName to null!");
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    
}