package de.ck35.monitoring.request.tagging.core.reporter.influxdb;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.function.Consumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus.StatusCode;

@RunWith(MockitoJUnitRunner.class)
public class InfluxDBTest {

    @Mock Consumer<String> bodyConsumer;
    @Captor ArgumentCaptor<String> bodyCaptor;
    
    public InfluxDB.Reporter reporter() {
        return new InfluxDB.Reporter(Instant.parse("2007-12-03T10:15:30.00Z"), "my-host", "A", bodyConsumer);
    }
    
    @Test
    public void testReporter() {
        InfluxDB.Reporter reporter = reporter();
        reporter.accept("my-resource-1", ImmutableMap.of(StatusCode.SUCCESS.toString(), 5L, StatusCode.SERVER_ERROR.toString(), 1L), ImmutableMap.of("key1", "value1"));
        reporter.accept("my-resource-2", ImmutableMap.of(StatusCode.SUCCESS.toString(), 10L), ImmutableMap.of());
        reporter.commit();
        verify(bodyConsumer).accept(bodyCaptor.capture());
        
        assertEquals("request_data,resource_name=\"my-resource-1\",host=\"my-host\",instanceId=\"A\",key1=\"value1\" SUCCESS=5,SERVER_ERROR=1 1196676930000000000\n" +
                     "request_data,resource_name=\"my-resource-2\",host=\"my-host\",instanceId=\"A\" SUCCESS=10 1196676930000000000\n", bodyCaptor.getValue());
    }

}