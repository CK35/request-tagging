package de.ck35.monitoring.request.tagging.core;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import de.ck35.monitoring.request.tagging.RequestTagging;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter;

@RunWith(MockitoJUnitRunner.class)
public class RequestTaggingContextTest {

    private static final Logger LOG = LoggerFactory.getLogger(RequestTaggingContextTest.class);
    
    private Supplier<Function<Instant, RequestTaggingStatusReporter>> defaultRequestTaggingStatusReporterSupplier;
    private String collectorSendDelayDuration;
    private String collectorResetDelayDuration;

    @Mock RequestTaggingStatusReporter requestTaggingStatusReporter;
    @Mock Function<Instant, RequestTaggingStatusReporter> defaultRequestTaggingStatusReporter;

    @Before
    public void before() {
        collectorSendDelayDuration = "PT1s";
        collectorResetDelayDuration = "PT1M";
        when(defaultRequestTaggingStatusReporter.apply(any())).thenReturn(requestTaggingStatusReporter);
        defaultRequestTaggingStatusReporterSupplier = () -> defaultRequestTaggingStatusReporter;
    }

    public RequestTaggingContext requestTaggingContext() {
        RequestTaggingContext context = new RequestTaggingContext(defaultRequestTaggingStatusReporterSupplier);
        context.setCollectorSendDelayDuration(collectorSendDelayDuration);
        context.setCollectorResetDelayDuration(collectorResetDelayDuration);
        context.setLoggerInfo(LOG::info);
        context.setLoggerWarn(LOG::warn);
        context.initialize();
        return context;
    }
    
    @Test
    public void testSuccess() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.taggingRunnable(() -> {
                RequestTagging.get()
                              .withResourceName("test-resource")
                              .withMetaData("test-key", "test-value");
            }).run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept("test-resource", ImmutableMap.of("SUCCESS", 1L), ImmutableMap.of("test-key", "test-value"));
        }
    }
    
    @Test
    public void testClientError() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.taggingRunnable(() -> {
                RequestTagging.get()
                              .withResourceName("test-resource")
                              .clientError();
            }).run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept("test-resource", ImmutableMap.of("CLIENT_ERROR", 1L), ImmutableMap.of());
        }
    }
    
    @Test
    public void testServerError() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.taggingRunnable(() -> {
                RequestTagging.get()
                              .withResourceName("test-resource")
                              .serverError();
            }).run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept("test-resource", ImmutableMap.of("SERVER_ERROR", 1L), ImmutableMap.of());
        }
    }

    @Test
    public void testIgnore() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.taggingRunnable(() -> {
                RequestTagging.get()
                              .withResourceName("test-ignore")
                              .ignore();
            }).run();
            context.taggingRunnable(() -> {
                RequestTagging.get()
                              .withResourceName("test-success");
            }).run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept("test-success", ImmutableMap.of("SUCCESS", 1L), ImmutableMap.of());
        }
    }
    
    @Test
    public void testHeed() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.taggingRunnable(() -> {
                RequestTagging.get()
                              .withResourceName("test-heed")
                              .ignore()
                              .heed();
            }).run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept("test-heed", ImmutableMap.of("SUCCESS", 1L), ImmutableMap.of());
        }
    }
    
    @Test
    public void testDefaultErrorOnRuntimeException() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            RuntimeException test = new RuntimeException("test");
            try {
                context.taggingRunnable(() -> {
                    throw test;
                }).run();
            } catch(RuntimeException e) {
                assertEquals(test, e);
            }
            verify(requestTaggingStatusReporter, timeout(10_000)).accept("default", ImmutableMap.of("SERVER_ERROR", 1L), ImmutableMap.of("serverErrorCause", "java.lang.RuntimeException"));
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testReportingFails() {
        BiConsumer<String, Throwable> loggerWarn = mock(BiConsumer.class);
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.setLoggerWarn(loggerWarn);
            RuntimeException test = new RuntimeException("test");
            doThrow(test).when(requestTaggingStatusReporter).accept("error-while-reporting", ImmutableMap.of("SUCCESS", 1L), ImmutableMap.of());
            context.taggingRunnable(() -> {
                RequestTagging.get().withResourceName("error-while-reporting");
            }).run();
            verify(loggerWarn, timeout(10_000)).accept("Error while sending request tagging data!", test);
            context.taggingRunnable(() -> {
                RequestTagging.get().withResourceName("no-error-while-reporting");
            }).run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept("no-error-while-reporting", ImmutableMap.of("SUCCESS", 1L), ImmutableMap.of());
        }
    }
    
    @Test
    public void testDefaultSettings() {
        try (RequestTaggingContext context = new RequestTaggingContext()) {
            context.initialize();
            assertEquals(Duration.ofMinutes(1), context.getCollectorSendDelayDuration());
            assertEquals(Duration.ofDays(1), context.getCollectorResetDelayDuration());
            
            LocalDateTime timestamp = LocalDateTime.now(context.getClock());
            assertEquals(0, timestamp.get(ChronoField.SECOND_OF_MINUTE));
            assertEquals(0, timestamp.get(ChronoField.MILLI_OF_SECOND));
        }
    }
    
    @Test
    public void testSendWithReset() {
        collectorSendDelayDuration = "PT2s";
        collectorResetDelayDuration = "PT1s";
        try(RequestTaggingContext context = requestTaggingContext()) {
            context.taggingRunnable(() -> {
            }).run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept("default", ImmutableMap.of("SUCCESS", 1L), ImmutableMap.of());
            reset(requestTaggingStatusReporter);
            context.taggingRunnable(() -> {
            }).run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept("default", ImmutableMap.of("SUCCESS", 1L), ImmutableMap.of());
        }
    }
    

}