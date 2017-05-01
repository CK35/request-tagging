package de.ck35.monitoring.request.tagging.core;

import static de.ck35.monitoring.request.tagging.core.ExpectedMeasurement.measurement;
import static de.ck35.monitoring.request.tagging.core.ExpectedResource.resource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ck35.monitoring.request.tagging.RequestTagging;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter.Resource;

@RunWith(MockitoJUnitRunner.class)
public class RequestTaggingContextTest {

    private static final Logger LOG = LoggerFactory.getLogger(RequestTaggingContextTest.class);

    private Supplier<Function<Instant, StatusReporter>> defaultRequestTaggingStatusReporterSupplier;
    private String collectorSendDelayDuration;

    @Mock StatusReporter requestTaggingStatusReporter;
    @Mock Function<Instant, StatusReporter> defaultRequestTaggingStatusReporter;
    @Mock Function<String, String> parameters;
    @Captor ArgumentCaptor<Resource> resourceCaptor;

    @Before
    public void before() {
        collectorSendDelayDuration = "PT1s";
        when(defaultRequestTaggingStatusReporter.apply(any())).thenReturn(requestTaggingStatusReporter);
        defaultRequestTaggingStatusReporterSupplier = () -> defaultRequestTaggingStatusReporter;
    }

    public RequestTaggingContext requestTaggingContext() {
        RequestTaggingContext context = new RequestTaggingContext(defaultRequestTaggingStatusReporterSupplier);
        context.setCollectorSendDelayDuration(collectorSendDelayDuration);
        context.setLoggerInfo(LOG::info);
        context.setLoggerWarn(LOG::warn);
        context.initialize();
        return context;
    }

    @Test
    public void testSuccess() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("test-resource")
                              .withMetaData("test-key", "test-value");
            })
                   .run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            assertThat(resourceCaptor.getValue(), resource().withName("test-resource")
                                                            .withMetaData("test-key", "test-value")
                                                            .withMeasurement(measurement().withStatusCodeName("SUCCESS")
                                                                                          .withTotalNumberOfInvocations(1))
                                                            .matches());
        }
    }

    @Test
    public void testSuccessWithHashedValues() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("test-resource")
                              .withHashedMetaData("test-key", "test-value");
            })
                   .run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            assertThat(resourceCaptor.getValue(), resource().withName("test-resource")
                                                            .withMetaData("test-key", "83B3C112B82DCCA8376DA029E8101BCC")
                                                            .withMeasurement(measurement().withStatusCodeName("SUCCESS")
                                                                                          .withTotalNumberOfInvocations(1))
                                                            .matches());
        }
    }

    @Test
    public void testClientError() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("test-resource")
                              .clientError();
            })
                   .run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            assertThat(resourceCaptor.getValue(), resource().withName("test-resource")
                                                            .withMeasurement(measurement().withStatusCodeName("CLIENT_ERROR")
                                                                                          .withTotalNumberOfInvocations(1))
                                                            .matches());
        }
    }

    @Test
    public void testServerError() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("test-resource")
                              .serverError();
            })
                   .run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            assertThat(resourceCaptor.getValue(), resource().withName("test-resource")
                                                            .withMeasurement(measurement().withStatusCodeName("SERVER_ERROR")
                                                                                          .withTotalNumberOfInvocations(1))
                                                            .matches());
        }
    }

    @Test
    public void testIgnore() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("test-ignore")
                              .ignore();
            })
                   .run();
            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("test-success");
            })
                   .run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            assertThat(resourceCaptor.getValue(), resource().withName("test-success")
                                                            .withMeasurement(measurement().withStatusCodeName("SUCCESS")
                                                                                          .withTotalNumberOfInvocations(1))
                                                            .matches());
        }
    }

    @Test
    public void testHeed() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("test-heed")
                              .ignore()
                              .heed();
            })
                   .run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            assertThat(resourceCaptor.getValue(), resource().withName("test-heed")
                                                            .withMeasurement(measurement().withStatusCodeName("SUCCESS")
                                                                                          .withTotalNumberOfInvocations(1))
                                                            .matches());
        }
    }

    @Test
    public void testDefaultErrorOnRuntimeException() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            RuntimeException test = new RuntimeException("test");
            try {
                context.taggingRunnable(parameters, () -> {
                    throw test;
                })
                       .run();
            } catch (RuntimeException e) {
                assertEquals(test, e);
            }
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            assertThat(resourceCaptor.getValue(), resource().withName("default")
                                                            .withMetaData("serverErrorCause", "java.lang.RuntimeException")
                                                            .withMeasurement(measurement().withStatusCodeName("SERVER_ERROR")
                                                                                          .withTotalNumberOfInvocations(1))
                                                            .matches());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReportingFails() {
        BiConsumer<String, Throwable> loggerWarn = mock(BiConsumer.class);
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.setLoggerWarn(loggerWarn);
            RuntimeException test = new RuntimeException("test");
            doThrow(test).when(requestTaggingStatusReporter)
                         .accept(any(Resource.class));

            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("error-while-reporting");
            })
                   .run();
            verify(loggerWarn, timeout(10_000)).accept("Error while sending request tagging data!", test);
            reset(requestTaggingStatusReporter);

            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("no-error-while-reporting");
            })
                   .run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            assertThat(resourceCaptor.getValue(), resource().withName("no-error-while-reporting")
                                                            .withMeasurement(measurement().withStatusCodeName("SUCCESS")
                                                                                          .withTotalNumberOfInvocations(1))
                                                            .matches());
        }
    }

    @Test
    public void testDefaultSettings() {
        try (RequestTaggingContext context = new RequestTaggingContext()) {
            context.initialize();
            assertEquals(Duration.ofMinutes(1), context.getCollectorSendDelayDuration());

            LocalDateTime timestamp = LocalDateTime.now(context.getSendIntervalClock());
            assertEquals(0, timestamp.get(ChronoField.SECOND_OF_MINUTE));
            assertEquals(0, timestamp.get(ChronoField.MILLI_OF_SECOND));
            
            assertFalse(context.isRequestIdEnabled());
            assertFalse(context.isForceRequestIdOverwrite());
            assertEquals("X-Request-ID", context.getRequestIdParameterName());
        }
    }
    
    @Test
    public void testRequestIdIsGenerated() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.setRequestIdEnabled(true);
            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("test-resource")
                              .withMetaData("test-key", "test-value");
            })
                   .run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            
            Resource resource = resourceCaptor.getValue();
            assertNotNull(resource.getMetaData().get("X-Request-ID"));
        }
    }
    
    @Test
    public void testRequestIdIsGeneratedAndHandedOver() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.setRequestIdEnabled(true);
            
            context.taggingRunnable(parameters, () -> {
                Thread other = new Thread(RequestTagging.get().handover(() -> {
                    
                }));
                other.start();
                try {
                    other.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            
            Resource resource = resourceCaptor.getValue();
            assertNotNull(resource.getMetaData().get("X-Request-ID"));
            assertEquals(2, resource.getMeasurements().get(0).getTotalNumberOfInvocations());
        }
    }
    
    @Test
    public void testRequestIdIsGeneratedWithDifferentName() {
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.setRequestIdEnabled(true);
            context.setRequestIdParameterName("X-Correlation-ID");
            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("test-resource")
                              .withMetaData("test-key", "test-value");
            })
                   .run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            
            Resource resource = resourceCaptor.getValue();
            assertNotNull(resource.getMetaData().get("X-Correlation-ID"));
        }
    }
    
    @Test
    public void testRequestIdIsUsedFromParameters() {
        when(parameters.apply("X-Request-ID")).thenReturn("123");
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.setRequestIdEnabled(true);
            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("test-resource")
                              .withMetaData("test-key", "test-value");
            })
                   .run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            
            Resource resource = resourceCaptor.getValue();
            assertEquals("123", resource.getMetaData().get("X-Request-ID"));
        }
    }
    
    @Test
    public void testRequestIdIsIgnoredFromParameters() {
        when(parameters.apply("X-Request-ID")).thenReturn("123");
        try (RequestTaggingContext context = requestTaggingContext()) {
            context.setRequestIdEnabled(true);
            context.setForceRequestIdOverwrite(true);
            context.taggingRunnable(parameters, () -> {
                RequestTagging.get()
                              .withResourceName("test-resource")
                              .withMetaData("test-key", "test-value");
            })
                   .run();
            verify(requestTaggingStatusReporter, timeout(10_000)).accept(resourceCaptor.capture());
            
            Resource resource = resourceCaptor.getValue();
            assertNotNull(resource.getMetaData().get("X-Request-ID"));
            assertNotEquals("123", resource.getMetaData().get("X-Request-ID"));
        }
    }

}