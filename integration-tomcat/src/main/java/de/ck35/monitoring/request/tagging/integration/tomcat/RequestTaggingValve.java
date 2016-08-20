package de.ck35.monitoring.request.tagging.integration.tomcat;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import de.ck35.monitoring.request.tagging.core.RequestTaggingContext;

/**
 * Tomcat Valve implementation which adds the request tagging mechanism to all incoming requests. 
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class RequestTaggingValve extends ValveBase {

    private static final Log LOG = LogFactory.getLog(RequestTaggingValve.class);

    private final RequestTaggingContext context;

    public RequestTaggingValve() {
        super(true);
        context = new RequestTaggingContext();
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
            context.taggingRunnable(() -> {
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
    
    @Override
    public String getInfo() {
        return "A request tagging Tomcat Valve implementation.";
    }
    
    public void setLocalHostName(String localHostName) {
        context.setLocalHostName(localHostName);
    }
    public void setLocalInstanceId(String localInstanceId) {
        context.setLocalInstanceId(localInstanceId);
    }

    public void setCollectorResetDelayDuration(String collectorResetDelayDuration) {
        context.setCollectorResetDelayDuration(collectorResetDelayDuration);
    }
    public void setCollectorSendDelayDuration(String collectorSendDelayDuration) {
        context.setCollectorSendDelayDuration(collectorSendDelayDuration);
    }

    public void setInfluxDBProtocol(String influxDBProtocol) {
        context.setInfluxDBProtocol(influxDBProtocol);
    }
    public void setInfluxDBHostName(String influxDBHostName) {
        context.setInfluxDBHostName(influxDBHostName);
    }
    public void setInfluxDBPort(String influxDBPort) {
        context.setInfluxDBPort(influxDBPort);
    }
    public void setInfluxDBDatabaseName(String influxDBDatabaseName) {
        context.setInfluxDBDatabaseName(influxDBDatabaseName);
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        context.setConnectionTimeout(connectionTimeout);
    }
    public void setReadTimeout(int readTimeout) {
        context.setReadTimeout(readTimeout);
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