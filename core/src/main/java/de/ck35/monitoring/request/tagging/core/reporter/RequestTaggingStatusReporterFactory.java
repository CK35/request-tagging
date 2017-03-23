package de.ck35.monitoring.request.tagging.core.reporter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import de.ck35.monitoring.request.tagging.core.reporter.influxdb.InfluxDB;

/**
 * Factory which creates the different request tagging status reporters.
 * 
 * @author Christian Kaspari
 * @since 1.1.0
 */
public class RequestTaggingStatusReporterFactory {

    private Consumer<String> loggerInfo;
    
    private String localHostName;
    private String localInstanceId;
    
    private boolean reportToInfluxDB;
    private String influxDBProtocol;
    private String influxDBHostName;
    private String influxDBPort;
    private String influxDBDatabaseName;

    private int connectionTimeout;
    private int readTimeout;

    public RequestTaggingStatusReporterFactory() {
        loggerInfo = System.out::println;
        
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
    
    public void setLoggerInfo(Consumer<String> loggerInfo) {
        this.loggerInfo = Objects.requireNonNull(loggerInfo, "Can not set info logger to null.");
    }
    public Consumer<String> getLoggerInfo() {
        return loggerInfo;
    }
    
    public void setLocalInstanceId(String localInstanceId) {
        this.localInstanceId = localInstanceId;
    }
    public String getLocalInstanceId() {
        return localInstanceId;
    }
    public void setLocalHostName(String localHostName) {
        this.localHostName = localHostName;
    }
    public String getLocalHostName() {
        return localHostName;
    }
    
    public void setReportToInfluxDB(boolean reportToInfluxDB) {
        this.reportToInfluxDB = reportToInfluxDB;
    }
    public boolean isReportToInfluxDB() {
        return reportToInfluxDB;
    }
    public void setInfluxDBProtocol(String influxDBProtocol) {
        this.influxDBProtocol = Objects.requireNonNull(influxDBProtocol, "Can not set influxDBProtocol to null!");
    }
    public String getInfluxDBProtocol() {
        return influxDBProtocol;
    }
    public void setInfluxDBHostName(String influxDBHostName) {
        this.influxDBHostName = Objects.requireNonNull(influxDBHostName, "Can not set influxDBHostName to null!");
    }
    public String getInfluxDBHostName() {
        return influxDBHostName;
    }
    public void setInfluxDBPort(String influxDBPort) {
        this.influxDBPort = Objects.requireNonNull(influxDBPort, "Can not set influxDBPort to null!");
    }
    public String getInfluxDBPort() {
        return influxDBPort;
    }
    public void setInfluxDBDatabaseName(String influxDBDatabaseName) {
        this.influxDBDatabaseName = Objects.requireNonNull(influxDBDatabaseName, "Can not set influxDBDatabaseName to null!");
    }
    public String getInfluxDBDatabaseName() {
        return influxDBDatabaseName;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    public int getReadTimeout() {
        return readTimeout;
    }

    public Function<Instant, RequestTaggingStatusReporter> build() {
        if (reportToInfluxDB) {
            
            if(localHostName == null) {
                try {
                    localHostName = InetAddress.getLocalHost()
                                               .getHostName();
                } catch (UnknownHostException e) {
                    throw new RuntimeException("Could not resolve local host name! Please configure 'localHostName' property.", e);
                }
            }
            
            loggerInfo.accept("Using InfluxDB: '" + influxDBHostName + ":" + influxDBPort + "' for request tagging data reporting.");
            return new InfluxDB(localHostName, localInstanceId, influxDBProtocol, influxDBHostName, influxDBPort, influxDBDatabaseName, connectionTimeout, readTimeout)::reporter;
        } else {
            loggerInfo.accept("Could not create request tagging data reporter. Falling back to default.");
            return DefaultReporter.forLogger(loggerInfo);
        }
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
            // Ignore because already logged data
        }

        public static Function<Instant, RequestTaggingStatusReporter> forLogger(Consumer<String> logger) {
            return instant -> new DefaultReporter(logger, instant);
        }
    }
}