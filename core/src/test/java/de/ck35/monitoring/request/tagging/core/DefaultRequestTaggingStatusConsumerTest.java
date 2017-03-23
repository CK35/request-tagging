package de.ck35.monitoring.request.tagging.core;

import static de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus.DEFAULT_RESOURCE_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus.StatusCode;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter;

@RunWith(MockitoJUnitRunner.class)
public class DefaultRequestTaggingStatusConsumerTest {

    @Mock RequestTaggingStatusReporter reporter;
    @Captor ArgumentCaptor<String> resourceNameCaptor;
    @Captor ArgumentCaptor<Map<String, Long>> statusCodeCaptor;
    @Captor ArgumentCaptor<Map<String, String>> metaDataCaptor;
    
    public DefaultRequestTaggingStatusConsumer defaultRequestTaggingStatusConsumer() {
        return new DefaultRequestTaggingStatusConsumer();
    }
    
    @Test
    public void testCollectAndConsume() {
        DefaultRequestTaggingStatusConsumer consumer = defaultRequestTaggingStatusConsumer();
        new DefaultRequestTaggingStatus(consumer, x -> x).serverError().withMetaData("B", "b1").withMetaData("A", "a1").consume();
        new DefaultRequestTaggingStatus(consumer, x -> x).serverError().withMetaData("B", "b1").withMetaData("A", "a1").consume();
        new DefaultRequestTaggingStatus(consumer, x -> x).clientError().withMetaData("A", "a1").consume();
        new DefaultRequestTaggingStatus(consumer, x -> x).success().consume();
        
        consumer.report(reporter);
        verify(reporter ,times(3)).accept(resourceNameCaptor.capture(), statusCodeCaptor.capture(), metaDataCaptor.capture());
        
        assertEquals(ImmutableList.of(DEFAULT_RESOURCE_NAME, DEFAULT_RESOURCE_NAME, DEFAULT_RESOURCE_NAME), resourceNameCaptor.getAllValues());
        assertEquals(ImmutableList.of(ImmutableMap.of(StatusCode.SUCCESS.toString(), 1L),
                                      ImmutableMap.of(StatusCode.CLIENT_ERROR.toString(), 1L),
                                      ImmutableMap.of(StatusCode.SERVER_ERROR.toString(), 2L)), statusCodeCaptor.getAllValues());
        assertEquals(ImmutableList.of(Collections.emptyMap(),
                                      ImmutableMap.of("A", "a1"),
                                      ImmutableMap.of("A", "a1", "B", "b1")), metaDataCaptor.getAllValues());
    }

}