package de.ck35.monitoring.request.tagging.core.reporter;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Factory which creates the different request tagging status reporters.
 * 
 * @author Christian Kaspari
 * @since 2.0.0
 */
public class StatusReporterFactory {

    public enum ReportFormat {
            INFLUX_DB, JSON;
    }

    private Consumer<String> loggerInfo;

    private String hostId;
    private String instanceId;

    private boolean sendData;
    private ReportFormat reportFormat;

    private String protocol;
    private String hostName;
    private int port;
    private String pathPart;
    private String queryPart;

    private int connectionTimeout;
    private int readTimeout;

    public StatusReporterFactory() {
        loggerInfo = System.out::println;

        reportFormat = ReportFormat.JSON;

        protocol = "http";
        hostName = "localhost";
        port = 8086;
        pathPart = "/write";
        queryPart = "db=request_data";

        connectionTimeout = 5000;
        readTimeout = 5000;
    }

    public Function<Instant, StatusReporter> build() {

        if (hostId == null) {
            try {
                hostId = InetAddress.getLocalHost()
                                    .getHostName();
            } catch (UnknownHostException e) {
                throw new RuntimeException("Could not resolve local host name! Please configure 'hostId' property.", e);
            }
        }

        BiFunction<Instant, Consumer<String>, StatusReporter> reporters = reporters();
        if (sendData) {
            return HttpStatusReporter.statusReporter(buildURL(), connectionTimeout, readTimeout, reporters);
        } else {
            return LoggingStatusReporter.statusReporter(loggerInfo, reporters);
        }
    }

    private URL buildURL() {
        try {
            return new URI(protocol, null, hostName, port, pathPart, queryPart, null).toURL();
        } catch (NumberFormatException | URISyntaxException | MalformedURLException e) {
            throw new IllegalArgumentException("Can not create url!", e);
        }
    }

    private BiFunction<Instant, Consumer<String>, StatusReporter> reporters() {
        switch (reportFormat) {
        case INFLUX_DB:
            return (instant, writer) -> new InfluxDBStatusReporter(instant, hostId, instanceId, writer);
        case JSON:
            return (instant, writer) -> new JSONStatusReporter(instant, hostId, instanceId, writer);
        default:
            throw new IllegalStateException("Unknown reporting format: '" + reportFormat + "'!");
        }
    }

    public void setLoggerInfo(Consumer<String> loggerInfo) {
        this.loggerInfo = Objects.requireNonNull(loggerInfo, "Can not set info logger to null.");
    }

    public Consumer<String> getLoggerInfo() {
        return loggerInfo;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostId() {
        return hostId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setSendData(boolean sendData) {
        this.sendData = sendData;
    }

    public boolean isSendData() {
        return sendData;
    }

    public void setReportFormat(ReportFormat reportFormat) {
        this.reportFormat = reportFormat;
    }

    public ReportFormat getReportFormat() {
        return reportFormat;
    }

    public void setWriteStrategy(ReportFormat writeStrategy) {
        this.reportFormat = Objects.requireNonNull(writeStrategy, "Can not set writeStrategy to null!");
    }

    public ReportFormat getWriteStrategy() {
        return reportFormat;
    }

    public void setProtocol(String protocol) {
        this.protocol = Objects.requireNonNull(protocol, "Can not set protocol to null!");
    }

    public String getProtocol() {
        return protocol;
    }

    public void setHostName(String hostName) {
        this.hostName = Objects.requireNonNull(hostName, "Can not set hostName to null!");
    }

    public String getHostName() {
        return hostName;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setPathPart(String pathPart) {
        this.pathPart = Optional.ofNullable(pathPart)
                                .map(x -> x.isEmpty() ? "/" : x)
                                .orElse("/");
    }

    public String getPathPart() {
        return pathPart;
    }

    public void setQueryPart(String queryPart) {
        this.queryPart = Optional.ofNullable(queryPart)
                                 .map(x -> x.isEmpty() ? null : x)
                                 .orElse(null);
    }

    public String getQueryPart() {
        return queryPart;
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

}