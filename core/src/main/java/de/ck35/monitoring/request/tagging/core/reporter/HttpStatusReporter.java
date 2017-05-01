package de.ck35.monitoring.request.tagging.core.reporter;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class HttpStatusReporter implements StatusReporter {

    private final Instant instant;
    private final Connection connection;
    private final BiFunction<Instant, Consumer<String>, StatusReporter> reporters;

    private StatusReporter reporter;
    
    public HttpStatusReporter(Instant instant, Connection connection, BiFunction<Instant, Consumer<String>, StatusReporter> reporters) {
        this.instant = instant;
        this.connection = connection;
        this.reporters = reporters;
    }

    public static Function<Instant, StatusReporter> statusReporter(URL url, int connectionTimeout, int readTimeout, BiFunction<Instant, Consumer<String>, StatusReporter> reporters) {
        return instant -> new HttpStatusReporter(instant, Connection.connect(url, connectionTimeout, readTimeout), reporters);
    }
    
    @Override
    public void accept(Resource resource) {
        if (reporter == null) {
            reporter = reporters.apply(instant, connection);
        }
        reporter.accept(resource);
    }

    @Override
    public void close() {
        try {
            Optional.ofNullable(reporter)
                    .ifPresent(StatusReporter::close);
        } finally {
            connection.close();
        }
    }
    
    public static class Connection implements Consumer<String>, Closeable {

        private final HttpURLConnection connection;

        private BufferedWriter writer;

        public Connection(HttpURLConnection connection) {
            this.connection = Objects.requireNonNull(connection);
        }
        
        public static Connection connect(URL url, int connectionTimeout, int readTimeout) {
            try {
                boolean disconnect = true;
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                try {
                    connection.setConnectTimeout(connectionTimeout);
                    connection.setReadTimeout(readTimeout);
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
                    connection.setChunkedStreamingMode(0);
                    connection.connect();
                    disconnect = false;
                    return new Connection(connection);
                } finally {
                    if (disconnect) {
                        connection.disconnect();
                    }
                }
            } catch (IOException e) {
                throw new HttpTransferException("Could not create connection to: '" + url + "'!", e);
            }
        }

        public BufferedWriter getWriter() {
            if (writer == null) {
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new HttpTransferException("Could not create output stream to: '" + connection.getURL() + "'!", e);
                }
            }
            return writer;
        }

        @Override
        public void accept(String token) {
            try {
                getWriter().write(token);
            } catch (IOException e) {
                throw new HttpTransferException("Error while appending token: '" + token + "' to output stream: '" + connection.getURL() + "'!", e);
            }
        }

        @Override
        public void close() {
            try {
                try {
                    if (writer == null) {
                        return;
                    } else {
                        writer.close();
                    }
                } catch (IOException e) {
                    throw new HttpTransferException("Could not close writer after writing to: '" + connection.getURL() + "'!", e);
                }
                final int code;
                try {
                    code = connection.getResponseCode();
                } catch (IOException e) {
                    throw new HttpTransferException("Could not read response code after writing to: '" + connection.getURL() + "'!", e);
                }
                if (code < 200 || code > 299) {
                    throw new HttpTransferException("Invalid response code: '" + code + "' received after writing to: '" + connection.getURL() + "'. Expected 2xx!");
                }
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