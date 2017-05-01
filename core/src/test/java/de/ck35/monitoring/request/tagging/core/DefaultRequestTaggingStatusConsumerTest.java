package de.ck35.monitoring.request.tagging.core;

import static de.ck35.monitoring.request.tagging.core.ExpectedMeasurement.measurement;
import static de.ck35.monitoring.request.tagging.core.ExpectedResource.resource;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.ck35.monitoring.request.tagging.RequestTagging;
import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatusConsumer.MetaDataPair;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter.Resource;

@RunWith(MockitoJUnitRunner.class)
public class DefaultRequestTaggingStatusConsumerTest {

    @Mock StatusReporter reporter;
    @Captor ArgumentCaptor<Resource> resourceCaptor;

    public DefaultRequestTaggingStatusConsumer defaultRequestTaggingStatusConsumer() {
        return new DefaultRequestTaggingStatusConsumer();
    }

    @Test
    public void testCollectAndConsume() {
        DefaultRequestTaggingStatusConsumer consumer = defaultRequestTaggingStatusConsumer();

        new RequestTaggingRunnable(() -> {
            RequestTagging.get()
                          .serverError()
                          .withMetaData("B", "b1")
                          .withMetaData("A", "a1");
        }, new DefaultRequestTaggingStatus(consumer)).run();

        new RequestTaggingRunnable(() -> {
            RequestTagging.get()
                          .serverError()
                          .withMetaData("B", "b1")
                          .withMetaData("A", "a1");
        }, new DefaultRequestTaggingStatus(consumer)).run();

        new RequestTaggingRunnable(() -> {
            RequestTagging.get()
                          .clientError()
                          .withMetaData("A", "a1");
        }, new DefaultRequestTaggingStatus(consumer)).run();

        new RequestTaggingRunnable(() -> {
            RequestTagging.get()
                          .success();
        }, new DefaultRequestTaggingStatus(consumer)).run();

        consumer.report(reporter);
        verify(reporter, times(3)).accept(resourceCaptor.capture());

        assertThat(resourceCaptor.getAllValues()
                                 .get(0),
                   resource().withName("default")
                             .withMeasurement(measurement().withStatusCodeName("SUCCESS")
                                                           .withTotalNumberOfInvocations(1))
                             .matches());

        assertThat(resourceCaptor.getAllValues()
                                 .get(1),
                   resource().withName("default")
                             .withMeasurement(measurement().withStatusCodeName("CLIENT_ERROR")
                                                           .withTotalNumberOfInvocations(1))
                             .withMetaData("A", "a1")
                             .matches());

        assertThat(resourceCaptor.getAllValues()
                                 .get(2),
                   resource().withName("default")
                             .withMeasurement(measurement().withStatusCodeName("SERVER_ERROR")
                                                           .withTotalNumberOfInvocations(2))
                             .withMetaData("A", "a1")
                             .withMetaData("B", "b1")
                             .matches());
    }

    @Test
    public void testDisableDurationsAtRuntime() {
        DefaultRequestTaggingStatusConsumer consumer = defaultRequestTaggingStatusConsumer();
        consumer.setMaxDurationsPerNode(1);

        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.parse("2017-04-11T10:00:00.00Z"))
                             .thenReturn(Instant.parse("2017-04-11T10:00:10.00Z"))
                             .thenReturn(Instant.parse("2017-04-11T10:00:20.00Z"))
                             .thenReturn(Instant.parse("2017-04-11T10:00:30.00Z"));

        new RequestTaggingRunnable(() -> {
            RequestTagging.get()
                          .success();
        }, new DefaultRequestTaggingStatus(consumer, x -> x, clock)).run();

        consumer.report(reporter);
        verify(reporter).accept(resourceCaptor.capture());

        assertThat(resourceCaptor.getValue(), resource().withName("default")
                                                        .withMeasurement(measurement().withStatusCodeName("SUCCESS")
                                                                                      .withTotalNumberOfInvocations(1)
                                                                                      .withDuration("total_request_duration", Duration.ofSeconds(10)))
                                                        .matches());

        consumer.setMaxDurationsPerNode(0); // Disable duration collecting.
        reset(reporter);
        resourceCaptor = ArgumentCaptor.forClass(Resource.class);

        new RequestTaggingRunnable(() -> {
            RequestTagging.get()
                          .success();
        }, new DefaultRequestTaggingStatus(consumer, x -> x, clock)).run();

        consumer.report(reporter);
        verify(reporter).accept(resourceCaptor.capture());

        assertThat(resourceCaptor.getValue(), resource().withName("default")
                                                        .withMeasurement(measurement().withStatusCodeName("SUCCESS")
                                                                                      .withTotalNumberOfInvocations(1))
                                                        .matches());
    }

    @Test
    public void testCollectAndConsumeWithDurations() {
        DefaultRequestTaggingStatusConsumer consumer = defaultRequestTaggingStatusConsumer();
        consumer.setMaxDurationsPerNode(2);
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.parse("2017-04-11T10:00:00.00Z"))
                             .thenReturn(Instant.parse("2017-04-11T10:00:15.00Z"))
                             .thenReturn(Instant.parse("2017-04-11T10:00:20.00Z"))
                             .thenReturn(Instant.parse("2017-04-11T10:00:25.00Z"))

                             .thenReturn(Instant.parse("2017-04-11T10:00:30.00Z"))
                             .thenReturn(Instant.parse("2017-04-11T10:00:40.00Z"))

                             .thenReturn(Instant.parse("2017-04-11T10:00:50.00Z"))
                             .thenReturn(Instant.parse("2017-04-11T10:01:00.00Z"));

        new RequestTaggingRunnable(() -> {
            RequestTagging.get()
                          .startTimer("test");
            RequestTagging.get()
                          .stopTimer("test");
        }, new DefaultRequestTaggingStatus(consumer, x -> x, clock)).run();

        new RequestTaggingRunnable(() -> {
            RequestTagging.get()
                          .success();
        }, new DefaultRequestTaggingStatus(consumer, x -> x, clock)).run();

        // This call will be counted but the duration will be ignored due to max
        // durations per node == 2.
        new RequestTaggingRunnable(() -> {
            RequestTagging.get()
                          .success();
        }, new DefaultRequestTaggingStatus(consumer, x -> x, clock)).run();

        consumer.report(reporter);
        verify(reporter).accept(resourceCaptor.capture());

        assertThat(resourceCaptor.getValue(), resource().withName("default")
                                                        .withMeasurement(measurement().withStatusCodeName("SUCCESS")
                                                                                      .withTotalNumberOfInvocations(3)
                                                                                      .withDuration("total_request_duration", Duration.ofSeconds(25))
                                                                                      .withDuration("total_request_duration", Duration.ofSeconds(10))
                                                                                      .withDuration("test", Duration.ofSeconds(5)))
                                                        .matches());
    }

    @Test
    public void testIgnoredStatus() {
        DefaultRequestTaggingStatusConsumer consumer = defaultRequestTaggingStatusConsumer();
        new RequestTaggingRunnable(() -> {
            RequestTagging.get()
                          .ignore();
        }, new DefaultRequestTaggingStatus(consumer)).run();
        consumer.report(reporter);
        verifyZeroInteractions(reporter);
    }

    @Test
    public void testReportDifference() {
        DefaultRequestTaggingStatusConsumer consumer = defaultRequestTaggingStatusConsumer();

        new RequestTaggingRunnable(() -> {
            RequestTagging.get()
                          .success();
        }, new DefaultRequestTaggingStatus(consumer)).run();

        consumer.report(reporter);
        verify(reporter).accept(resourceCaptor.capture());

        assertThat(resourceCaptor.getValue(), resource().withName("default")
                                                        .withMeasurement(measurement().withStatusCodeName("SUCCESS")
                                                                                      .withTotalNumberOfInvocations(1))
                                                        .matches());

        reset(reporter);
        resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        consumer.report(reporter);
        verifyZeroInteractions(reporter);

        new RequestTaggingRunnable(() -> {
            RequestTagging.get()
                          .success();
        }, new DefaultRequestTaggingStatus(consumer)).run();

        consumer.report(reporter);
        verify(reporter).accept(resourceCaptor.capture());

        assertThat(resourceCaptor.getValue(), resource().withName("default")
                                                        .withMeasurement(measurement().withStatusCodeName("SUCCESS")
                                                                                      .withTotalNumberOfInvocations(1))
                                                        .matches());
    }

    @Test
    public void testMetaDataPair() {
        MetaDataPair pair = new MetaDataPair("A", "a1");
        assertTrue(pair.equals(pair));
        assertFalse(pair.equals(null));
        assertFalse(pair.equals("A"));
        assertTrue(pair.equals(new MetaDataPair("A", "a1")));
        assertFalse(pair.equals(new MetaDataPair("A", "b1")));
        assertFalse(pair.equals(new MetaDataPair("B", "a1")));

        assertTrue(pair.toString()
                       .contains("key=A"));
        assertTrue(pair.toString()
                       .contains("value=a1"));
    }
}