package de.ck35.monitoring.request.tagging.core.reporter.influxdb;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter;

/**
 * An reporter implementation which sends data to an InfluxDB instance. 
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class InfluxDB {

    private static final int STATUS_CODE_SUCCESS = 204;
    
    private final String localHost;
    private final String localInstanceId;
    private final URL url;

    private final int connectionTimeout;
    private final int readTimeout;

    public InfluxDB(String localHost, 
                    String localInstanceId, 
                    String influxDBProtocol, 
                    String influxDBHostName, 
                    String influxDBPort, 
                    String databaseName,
                    int connectionTimeout,
                    int readTimeout) {
        this(localHost, localInstanceId, url(influxDBProtocol, influxDBHostName, influxDBPort, databaseName), connectionTimeout, readTimeout);
    }

    public InfluxDB(String localHost, String localInstanceId, URL url, int connectionTimeout, int readTimeout) {
        this.localHost = localHost;
        this.localInstanceId = localInstanceId;
        this.url = url;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    public RequestTaggingStatusReporter reporter(Instant instant) {
        return new Reporter(instant, localHost, localInstanceId, this::send);
    }

    public void send(String body) {
        if(body.isEmpty()) {
            return;
        }
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));
            connection.connect();
            try {
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(bytes);
                    outputStream.flush();
                    int responseCode = connection.getResponseCode();
                    if(responseCode != STATUS_CODE_SUCCESS) {
                        throw new IOException("Invalid response code received: '" + responseCode + "' expected: '" + STATUS_CODE_SUCCESS + "'!");
                    }
                }
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    public static URL url(String influxDBProtocol, String influxDBHostName, String influxDBPort, String databaseName) {
        try {
            return new URL(influxDBProtocol + "://" + influxDBHostName + ":" + influxDBPort + "/write?db=" + databaseName);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Can not create InfluxDB url!", e);
        }
    }

    public static class Reporter implements RequestTaggingStatusReporter {

        private final Instant instant;
        private final String host;
        private final String instanceId;
        private final Consumer<String> bodyConsumer;

        private final StringBuilder body;

        public Reporter(Instant instant, String host, String instanceId, Consumer<String> bodyConsumer) {
            this.instant = instant;
            this.host = host;
            this.instanceId = instanceId;
            this.bodyConsumer = bodyConsumer;
            this.body = new StringBuilder();
        }

        @Override
        public void commit() {
            bodyConsumer.accept(body.toString());
        }

        @Override
        public void accept(String resourceName, Map<String, Long> statusCodeCounters, Map<String, String> metaData) {
            Iterator<Entry<String, Long>> iter = statusCodeCounters.entrySet()
                                                                   .iterator();
            if (!iter.hasNext()) {
                return;
            }
            writeMeasurement();
            writeTag("resource_name", resourceName);
            if (host != null) {
                writeTag("host", host);
            }
            if (instanceId != null) {
                writeTag("instanceId", instanceId);
            }
            metaData.forEach(this::writeTag);
            writeFirstField(iter.next());
            iter.forEachRemaining(this::writeNextField);
            writeTimestamp();
        }

        public void writeMeasurement() {
            body.append("request_data");
        }

        public void writeTag(String tagKey, String tagValue) {
            body.append(",")
                .append(tagKey)
                .append("=\"")
                .append(tagValue.replace("\"", "\\\""))
                .append("\"");
        }

        public void writeFirstField(Entry<String, Long> entry) {
            writeField(" ", entry);
        }

        public void writeNextField(Entry<String, Long> entry) {
            writeField(",", entry);
        }

        public void writeField(String separator, Entry<String, Long> entry) {
            body.append(separator)
                .append(entry.getKey())
                .append("=")
                .append(entry.getValue());
        }

        public void writeTimestamp() {
            body.append(" ")
                .append(instant.toEpochMilli())
                .append("000000")
                .append("\n");
        }
    }

}