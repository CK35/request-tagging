package de.ck35.monitoring.request.tagging.integration.filter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ck35.monitoring.request.tagging.core.HashAlgorithm;
import de.ck35.monitoring.request.tagging.core.RequestTaggingContext;
import de.ck35.monitoring.request.tagging.core.RequestTaggingContextConfiguration;
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
            context.runWithinContext(request::getParameter, () -> {
                try {
                    chain.doFilter(request, response);
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
    public void init(FilterConfig filterConfig) throws ServletException {
        RequestTaggingContextConfiguration configuration = new RequestTaggingContextConfiguration(filterConfig::getInitParameter);
        configuration.configure(hashAlgorithm);
        configuration.configure(context);
        configuration.configure(statusReporterFactory);
        context.initialize();
    }

    @Override
    public void destroy() {
        context.close();
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