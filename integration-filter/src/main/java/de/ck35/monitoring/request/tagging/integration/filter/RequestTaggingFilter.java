package de.ck35.monitoring.request.tagging.integration.filter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Consumer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ck35.monitoring.request.tagging.core.RequestTaggingContext;

/**
 * Enable request tagginig for all requests which are send through this servlet filter.
 * 
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class RequestTaggingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestTaggingFilter.class);

    private final RequestTaggingContext context;

    public RequestTaggingFilter() {
        context = new RequestTaggingContext();
        context.setLoggerInfo(LOG::info);
        context.setLoggerWarn(LOG::warn);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            context.taggingRunnable(() -> {
                try {
                    chain.doFilter(request, response);
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
    public void init(FilterConfig filterConfig) throws ServletException {
        FilterConfigWrapper config = new FilterConfigWrapper(filterConfig);
        config.apply("collectorSendDelayDuration", context::setCollectorSendDelayDuration);
        config.apply("collectorResetDelayDuration", context::setCollectorResetDelayDuration);
        config.apply("localHostName", context::setLocalHostName);
        config.apply("localInstanceId", context::setLocalInstanceId);

        config.applyBoolean("reportToInfluxDB", context::setReportToInfluxDB);
        config.apply("influxDBProtocol", context::setInfluxDBProtocol);
        config.apply("influxDBHostName", context::setInfluxDBHostName);
        config.apply("influxDBPort", context::setInfluxDBPort);
        config.apply("influxDBDatabaseName", context::setInfluxDBDatabaseName);

        config.applyInt("connectionTimeout", context::setConnectionTimeout);
        config.applyInt("readTimeout", context::setReadTimeout);

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

    private static class FilterConfigWrapper {

        private final FilterConfig config;

        public FilterConfigWrapper(FilterConfig config) {
            this.config = config;
        }

        private Optional<String> get(String parameterName) {
            return Optional.ofNullable(config.getInitParameter(parameterName))
                           .map(String::trim)
                           .map(value -> value.isEmpty() ? null : value);
        }

        public void apply(String parameterName, Consumer<String> parameterValueConsumer) {
            get(parameterName).ifPresent(parameterValueConsumer);
        }

        public void applyBoolean(String parameterName, Consumer<Boolean> parameterValueConsumer) {
            get(parameterName).map(Boolean::valueOf)
                              .ifPresent(parameterValueConsumer);
        }

        public void applyInt(String parameterName, Consumer<Integer> parameterValueConsumer) {
            get(parameterName).map(Integer::valueOf)
                              .ifPresent(parameterValueConsumer);
        }
    }
}