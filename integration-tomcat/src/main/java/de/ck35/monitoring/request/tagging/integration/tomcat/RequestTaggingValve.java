package de.ck35.monitoring.request.tagging.integration.tomcat;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import de.ck35.monitoring.request.tagging.core.HashAlgorithm;
import de.ck35.monitoring.request.tagging.core.RequestTaggingContext;
import de.ck35.monitoring.request.tagging.core.RequestTaggingContextConfigurer;
import de.ck35.monitoring.request.tagging.core.RequestTaggingContextConfigurer.ConfigKey;
import de.ck35.monitoring.request.tagging.core.RequestTaggingRunnable.WrappedException;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporterFactory;

/**
 * Tomcat Valve implementation which adds the request tagging mechanism to all incoming requests. 
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class RequestTaggingValve extends ValveBase {

    private static final Log LOG = LogFactory.getLog(RequestTaggingValve.class);

    private final RequestTaggingContext context;
    private final StatusReporterFactory statusReporterFactory;
    private final HashAlgorithm hashAlgorithm;
    private final Clock stopWatchClock;
    private final Map<String, String> properties;

    public RequestTaggingValve() {
        super(true);
        statusReporterFactory = new StatusReporterFactory();
        statusReporterFactory.setLoggerInfo(LOG::info);
        hashAlgorithm = new HashAlgorithm();
        stopWatchClock = Clock.systemUTC();
        properties = new HashMap<>();
        context = new RequestTaggingContext(statusReporterFactory::build, hashAlgorithm::hash, stopWatchClock);
        context.setLoggerInfo(LOG::info);
        context.setLoggerWarn(LOG::warn);
    }

    @Override
    protected synchronized void startInternal() throws LifecycleException {
        super.startInternal();
        RequestTaggingContextConfigurer configurer = new RequestTaggingContextConfigurer(properties::get, LOG::info);
        configurer.configure(hashAlgorithm);
        configurer.configure(context);
        configurer.configure(statusReporterFactory);
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
            context.runWithinContext(request::getHeader, () -> {
                try {
                    next.invoke(request, response);
                } catch (IOException e) {
                    throw new WrappedException(e);
                } catch(ServletException e) {
                    throw new WrappedException(e, e.getCause());
                }
            });
        } catch (WrappedException e) {
            Throwable source = e.getSource();
            if(source instanceof IOException) {
                throw (IOException) source;
            } else if (source instanceof ServletException) {
                throw (ServletException) source;
            } else {
                throw e;
            }
        }
    }
    
    @Override
    public String getInfo() {
        return "A  Tomcat Valve for Request Tagging.";
    }
    
    public void setCollectorSendDelayDuration(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setRequestIdEnabled(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setForceRequestIdOverwrite(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setRequestIdParameterName(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setIgnored(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setResourceName(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setStatusCode(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setMaxDurationsPerNode(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setHostId(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setInstanceId(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setSendData(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setReportFormat(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setProtocol(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setHostName(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setPort(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setPathPart(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setQueryPart(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setConnectionTimeout(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setReadTimeout(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    public void setAlgorithmName(String value) {
        putPropertyWithNameFromStackTrace(value);
    }
    
    private void putPropertyWithNameFromStackTrace(String value) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String methodName = stackTrace[2].getMethodName().substring("set".length());
        methodName = methodName.substring(0, 1).toLowerCase() + methodName.substring(1, methodName.length());
        ConfigKey configKey = ConfigKey.valueOf(methodName);
        properties.put(configKey.getName(), value);
    }
    
}