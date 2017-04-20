package de.ck35.monitoring.request.tagging.core.reporter.http;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter.Resource;

public class StreamingHttpReporter implements Function<Instant, RequestTaggingStatusReporter>{

    public static interface WriteStrategy {

        default void beforeConnect(HttpURLConnection connection) {
        };

        default void beforeWrite(Consumer<String> writer) {
        };

        default void write(Resource resource, Consumer<String> writer) {
        };

        default void afterWrite(Consumer<String> writer) {
        };

        default void beforeDisconnect(HttpURLConnection connection) {
        };

    }

    private final URL url;
    private final int connectionTimeout;
    private final int readTimeout;
    private final Function<Instant, WriteStrategy> writeStrategy;

    public StreamingHttpReporter(URL url, int connectionTimeout, int readTimeout, Function<Instant, WriteStrategy> writeStrategy) {
        this.url = Objects.requireNonNull(url);
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.writeStrategy = writeStrategy;
    }
    
    @Override
    public RequestTaggingStatusReporter apply(Instant instant) {
        return connect(writeStrategy.apply(instant));
    }

    public Connection connect(WriteStrategy writeStrategy) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            connection.setChunkedStreamingMode(0);
            writeStrategy.beforeConnect(connection);
            connection.connect();
            return new Connection(new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)), connection, writeStrategy);
        } catch (IOException e) {
            throw new HttpTransferException("Could not create connection to: '" + url + "'!", e);
        }
    }

    public class Connection implements RequestTaggingStatusReporter {

        private final BufferedWriter writer;
        private final HttpURLConnection connection;
        private final WriteStrategy writeStrategy;

        private boolean firstWrite;

        public Connection(BufferedWriter writer, HttpURLConnection connection, WriteStrategy writeStrategy) {
            this.writeStrategy = writeStrategy;
            this.writer = Objects.requireNonNull(writer);
            this.connection = Objects.requireNonNull(connection);
            firstWrite = true;
        }

        @Override
        public void accept(Resource resource) {
            if (firstWrite) {
                firstWrite = false;
                writeStrategy.beforeWrite(token -> {
                    try {
                        writer.write(token);
                    } catch (IOException e) {
                        throw new HttpTransferException("Error while preparing writer with token: '" + token + "' to write to: '" + url + "'!", e);
                    }
                });
            }
            writeStrategy.write(resource, token -> {
                try {
                    writer.write(token);
                } catch (IOException e) {
                    throw new HttpTransferException("Error while writing resource: '" + resource + "' with current token: '" + token + "' to: '" + url + "'!", e);
                }
            });
        }

        @Override
        public void close() {
            try {
                try {
                    writeStrategy.afterWrite(token -> {
                        try {
                            writer.write(token);
                        } catch (IOException e) {
                            throw new HttpTransferException("Error while writing end of data: '" + token + "' to: '" + url + "'!", e);
                        }
                    });
                } finally {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        throw new HttpTransferException("Could not close writer after writing to: '" + url + "'!", e);
                    }
                }
                final int code;
                try {
                    code = connection.getResponseCode();
                } catch (IOException e) {
                    throw new HttpTransferException("Could not read response code after writing to: '" + url + "'!", e);
                }
                if (code < 200 || code > 299) {
                    throw new HttpTransferException("Invalid response code: '" + code + "' received after writing to: '" + url + "'. Expected 2xx!");
                }
                writeStrategy.beforeDisconnect(connection);
            } finally {
                connection.disconnect();
            }
        }
    }

    public static class HttpTransferException extends RuntimeException {

        public HttpTransferException(String message) {
            super(message);
        }

        public HttpTransferException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}