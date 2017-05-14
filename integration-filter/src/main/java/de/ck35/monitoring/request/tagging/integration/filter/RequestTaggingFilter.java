package de.ck35.monitoring.request.tagging.integration.filter;

import java.io.IOException;
import java.time.Clock;
import java.util.function.Function;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ck35.monitoring.request.tagging.core.HashAlgorithm;
import de.ck35.monitoring.request.tagging.core.RequestTaggingContext;
import de.ck35.monitoring.request.tagging.core.RequestTaggingContextConfigurer;
import de.ck35.monitoring.request.tagging.core.RequestTaggingRunnable.WrappedException;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporterFactory;

/**
 * Enable Request Tagginig for all requests which are send through this servlet filter.
 * 
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class RequestTaggingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestTaggingFilter.class);

    private final RequestTaggingContext context;
    private final StatusReporterFactory statusReporterFactory;
    private final HashAlgorithm hashAlgorithm;
    private final Clock stopWatchClock;

    public RequestTaggingFilter() {
        statusReporterFactory = new StatusReporterFactory();
        statusReporterFactory.setLoggerInfo(LOG::info);
        hashAlgorithm = new HashAlgorithm();
        stopWatchClock = Clock.systemUTC();
        context = new RequestTaggingContext(statusReporterFactory::build, hashAlgorithm::hash, stopWatchClock);
        context.setLoggerInfo(LOG::info);
        context.setLoggerWarn(LOG::warn);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            context.runWithinContext(header(request), () -> {
                try {
                    chain.doFilter(request, response);
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
    public void init(FilterConfig filterConfig) throws ServletException {
        RequestTaggingContextConfigurer configurer = new RequestTaggingContextConfigurer(filterConfig::getInitParameter, LOG::info);
        configurer.configure(hashAlgorithm);
        configurer.configure(context);
        configurer.configure(statusReporterFactory);
        context.initialize();
    }

    @Override
    public void destroy() {
        context.close();
    }
    
    private Function<String, String> header(ServletRequest request) {
        if(request instanceof HttpServletRequest) {
            return ((HttpServletRequest) request)::getHeader;
        } else {
            return x -> null;
        }
    }
    
}