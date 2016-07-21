package de.ck35.monitoring.request.integration.tomcat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus;
import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatusConsumer;
import de.ck35.monitoring.request.tagging.core.RequestTaggingRunnable;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter;
import de.ck35.monitoring.request.tagging.core.reporter.influxdb.InfluxDB;
import de.ck35.monitoring.request.tagging.integration.tomcat.reporter.Logger;

public class RequestTaggingValve extends ValveBase {

    private static final Log LOG = LogFactory.getLog(RequestTaggingValve.class);

    private final DefaultRequestTaggingStatusConsumer consumer;
    private final ScheduledThreadPoolExecutor executor;
    private final AtomicReference<Instant> nextResetReference;

    private volatile Clock clock;
    private volatile Duration collectorSendDelayDuration;
    private volatile Duration collectorResetDelayDuration;
    private volatile Function<Instant, RequestTaggingStatusReporter> reporterFunctionReference;

    private volatile String localHostName;
    private volatile String localInstanceId;

    private volatile String influxDBProtocol;
    private volatile String influxDBHostName;
    private volatile String influxDBPort;
    private volatile String influxDBDatabaseName;
    
    private volatile int connectionTimeout;
    private volatile int readTimeout;

    public RequestTaggingValve() {
        super(true);

        collectorSendDelayDuration = Duration.parse("PT1M");
        collectorResetDelayDuration = Duration.parse("P1D");

        consumer = new DefaultRequestTaggingStatusConsumer();
        executor = new ScheduledThreadPoolExecutor(1);
        nextResetReference = new AtomicReference<>(Instant.now()
                                                          .plus(collectorResetDelayDuration));

        localInstanceId = null;
        localHostName = null;

        influxDBProtocol = "http";
        influxDBHostName = "localhost";
        influxDBPort = "8086";
        influxDBDatabaseName = null;
        connectionTimeout = 5000;
        readTimeout = 5000;
    }

    @Override
    protected synchronized void startInternal() throws LifecycleException {
        super.startInternal();

        clock = Clock.tick(Clock.systemUTC(), collectorSendDelayDuration);
        
        if(localHostName == null) {
            try {
                localHostName = InetAddress.getLocalHost()
                                           .getHostName();
            } catch (UnknownHostException e) {
                throw new RuntimeException("Could not resolve local host name! Please configure 'localHostName' property.", e);
            }
        }

        String influxDBDatabaseName = this.influxDBDatabaseName;
        if (influxDBDatabaseName != null) {
            LOG.info("Using InfluxDB: '" + influxDBHostName + ":" + influxDBPort + "' for request tagging data reporting.");
            reporterFunctionReference = new InfluxDB(localHostName,
                                                     localInstanceId,
                                                     influxDBProtocol,
                                                     influxDBHostName,
                                                     influxDBPort,
                                                     influxDBDatabaseName,
                                                     connectionTimeout,
                                                     readTimeout)::reporter;
        } else {
            LOG.info("Using logger for request taggging data reporting.");
            reporterFunctionReference = new Logger()::reporter;
        }

        LOG.info("Scheduling send process for request tagging data with delay of '" + collectorSendDelayDuration + "'.");
        long startDelay = Math.max(0, clock.instant().plus(collectorSendDelayDuration).toEpochMilli() - Instant.now().toEpochMilli());
        executor.scheduleWithFixedDelay(this::send, startDelay, collectorSendDelayDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    protected synchronized void stopInternal() throws LifecycleException {
        super.stopInternal();
        LOG.info("Stopping send process for request tagging data.");
        executor.shutdown();
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            taggingRunnable(() -> {
                try {
                    next.invoke(request, response);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (ServletException e) {
                    throw new UncheckedServletException(e);
                }
            }).run();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } catch (UncheckedServletException e) {
            throw e.getCause();
        }
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
            LOG.warn("Error while sending request tagging data!", e);
        }
    }

    protected RequestTaggingRunnable taggingRunnable(Runnable runnable) {
        DefaultRequestTaggingStatus status = new DefaultRequestTaggingStatus(consumer);
        return new RequestTaggingRunnable(runnable, status);
    }

    @Override
    public String getInfo() {
        return "A request tagging Tomcat Valve implementation.";
    }
    
    public void setLocalHostName(String localHostName) {
        this.localHostName = localHostName;
    }
    public void setLocalInstanceId(String localInstanceId) {
        this.localInstanceId = localInstanceId;
    }

    public void setCollectorResetDelayDuration(String collectorResetDelayDuration) {
        this.collectorResetDelayDuration = Duration.parse(collectorResetDelayDuration);
    }
    public void setCollectorSendDelayDuration(String collectorSendDelayDuration) {
        this.collectorSendDelayDuration = Duration.parse(collectorSendDelayDuration);
    }

    public void setInfluxDBProtocol(String influxDBProtocol) {
        this.influxDBProtocol = influxDBProtocol;
    }
    public void setInfluxDBHostName(String influxDBHostName) {
        this.influxDBHostName = influxDBHostName;
    }
    public void setInfluxDBPort(String influxDBPort) {
        this.influxDBPort = influxDBPort;
    }
    public void setInfluxDBDatabaseName(String influxDBDatabaseName) {
        this.influxDBDatabaseName = influxDBDatabaseName;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    private static class UncheckedServletException extends RuntimeException {

        public UncheckedServletException(ServletException servletException) {
            super(servletException);
        }
        @Override
        public ServletException getCause() {
            return (ServletException) super.getCause();
        }
    }
}