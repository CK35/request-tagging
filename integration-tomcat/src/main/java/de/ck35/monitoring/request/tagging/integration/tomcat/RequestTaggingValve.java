package de.ck35.monitoring.request.tagging.integration.tomcat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import de.ck35.monitoring.request.tagging.core.HashAlgorithm;
import de.ck35.monitoring.request.tagging.core.RequestTaggingContext;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporterFactory;

/**
 * Tomcat Valve implementation which adds the request tagging mechanism to all incoming requests. 
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class RequestTaggingValve extends ValveBase {

    private static final Log LOG = LogFactory.getLog(RequestTaggingValve.class);

    private final RequestTaggingContext context;
    private final RequestTaggingStatusReporterFactory statusReporterFactory;
    private final HashAlgorithm hashAlgorithm;
    private final Clock stopWatchClock;

    public RequestTaggingValve() {
        super(true);
        statusReporterFactory = new RequestTaggingStatusReporterFactory();
        hashAlgorithm = new HashAlgorithm();
        stopWatchClock = Clock.systemUTC();
        context = new RequestTaggingContext(statusReporterFactory::build, hashAlgorithm::hash, stopWatchClock);
        context.setLoggerInfo(LOG::info);
        context.setLoggerWarn(LOG::warn);
    }

    @Override
    protected synchronized void startInternal() throws LifecycleException {
        super.startInternal();
        context.initialize();
    }

    @Override
    protected synchronized void stopInternal() throws LifecycleException {
        super.stopInternal();
        context.close();
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            context.runWithinContext(() -> {
                try {
                    next.invoke(request, response);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (ServletException e) {
                    throw new UncheckedServletException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } catch (UncheckedServletException e) {
            throw e.getCause();
        }
    }
    
    @Override
    public String getInfo() {
        return "A request tagging Tomcat Valve implementation.";
    }
    
    
    public void setCollectorSendDelayDuration(String collectorSendDelayDuration) {
        context.setCollectorSendDelayDuration(collectorSendDelayDuration);
    }
    public void setLocalHostName(String localHostName) {
        statusReporterFactory.setLocalHostName(localHostName);
    }
    public void setLocalInstanceId(String localInstanceId) {
        statusReporterFactory.setLocalInstanceId(localInstanceId);
    }
    
    public void setReportToInfluxDB(boolean reportToInfluxDB) {
        statusReporterFactory.setReportToInfluxDB(reportToInfluxDB);
    }
    public void setInfluxDBProtocol(String influxDBProtocol) {
        statusReporterFactory.setInfluxDBProtocol(influxDBProtocol);
    }
    public void setInfluxDBHostName(String influxDBHostName) {
        statusReporterFactory.setInfluxDBHostName(influxDBHostName);
    }
    public void setInfluxDBPort(String influxDBPort) {
        statusReporterFactory.setInfluxDBPort(influxDBPort);
    }
    public void setInfluxDBDatabaseName(String influxDBDatabaseName) {
        statusReporterFactory.setInfluxDBDatabaseName(influxDBDatabaseName);
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        statusReporterFactory.setConnectionTimeout(connectionTimeout);
    }
    public void setReadTimeout(int readTimeout) {
        statusReporterFactory.setReadTimeout(readTimeout);
    }

    public void setHashAlgorithmName(String hashAlgorithmName) {
        hashAlgorithm.setAlgorithmName(hashAlgorithmName);
    }
    
    
    public void setDefaultRequestTaggingStatusIgnored(boolean defaultRequestTaggingStatusIgnored) {
        context.getDefaultRequestTaggingStatus().setIgnored(defaultRequestTaggingStatusIgnored);
    }
    public void setDefaultRequestTaggingStatusResourceName(String defaultRequestTaggingStatusResourceName) {
        context.getDefaultRequestTaggingStatus().setResourceName(defaultRequestTaggingStatusResourceName);
    }
    public void setDefaultRequestTaggingStatusCode(String defaultRequestTaggingStatusCode) {
        context.getDefaultRequestTaggingStatus().setStatusCode(defaultRequestTaggingStatusCode);
    }
    
    public void setMaxDurationsPerNode(int maxDurationsPerNode) {
        context.getStatusConsumer().setMaxDurationsPerNode(maxDurationsPerNode);
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