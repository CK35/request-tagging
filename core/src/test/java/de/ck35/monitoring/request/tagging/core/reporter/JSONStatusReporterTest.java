package de.ck35.monitoring.request.tagging.core.reporter;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.io.ByteStreams;

import de.ck35.monitoring.request.tagging.core.reporter.JSONStatusReporter;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter.Measurement;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter.Resource;

public class JSONStatusReporterTest {

    private String hostId;
    private String instanceId;
    private Instant instant;
    private StringBuilder result;

    public JSONStatusReporterTest() {
        this.hostId = "my-host";
        this.instanceId = "my-instance";
        this.instant = Instant.parse("2007-12-03T10:15:30.00Z");
        this.result = new StringBuilder();
    }

    public JSONStatusReporter jsonStatusReporter() {
        return new JSONStatusReporter(instant, hostId, instanceId, result::append);
    }
    
    @Test
    public void testWrite() throws Exception {
        Measurement m1 = new Measurement("SUCCESS", 5, Collections.emptyMap());
        Measurement m2 = new Measurement("CLIENT_ERROR", 6, Collections.emptyMap());
        List<Measurement> measurements = ImmutableList.of(m1, m2);
        SortedMap<String, String> metaData = ImmutableSortedMap.of("my-meta-data-key", "my-meta-data-value");
        String name = "my-test-resource";
        Resource resource = new Resource(name, metaData, measurements);
        
        try(JSONStatusReporter jsonStatusReporter = jsonStatusReporter()) {            
            jsonStatusReporter.accept(resource);
        }
        
        String expected;
        try (InputStream in = InfluxDBStatusReporterTest.class.getResourceAsStream("/JSONStatusReporter_Expected.json")) {
            expected = new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8).replaceAll("\\s", "").replace("\n", "");
        }
        String actual = result.toString();
        
        assertEquals(expected, actual);
    }
    
    @Test
    public void testWriteWithEmptyDurations() throws Exception {
        Measurement m1 = new Measurement("SUCCESS", 5, ImmutableMap.of("total_request_duration", Collections.emptyList()));
        Measurement m2 = new Measurement("CLIENT_ERROR", 6, ImmutableMap.of("total_request_duration", Collections.emptyList()));
        List<Measurement> measurements = ImmutableList.of(m1, m2);
        SortedMap<String, String> metaData = ImmutableSortedMap.of("my-meta-data-key", "my-meta-data-value");
        String name = "my-test-resource";
        Resource resource = new Resource(name, metaData, measurements);
        
        try(JSONStatusReporter jsonStatusReporter = jsonStatusReporter()) {            
            jsonStatusReporter.accept(resource);
        }
        
        String expected;
        try (InputStream in = InfluxDBStatusReporterTest.class.getResourceAsStream("/JSONStatusReporter_Expected.json")) {
            expected = new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8).replaceAll("\\s", "").replace("\n", "");
        }
        String actual = result.toString();
        
        assertEquals(expected, actual);
    }
    
    @Test
    public void testWriteWithDurations() throws Exception {
        Measurement m1 = new Measurement("SUCCESS", 5, ImmutableMap.of("total_request_duration", ImmutableList.of(Duration.ofMillis(10), Duration.ofMillis(11), Duration.ofMillis(12))));
        Measurement m2 = new Measurement("CLIENT_ERROR", 6, ImmutableMap.of("total_request_duration", ImmutableList.of(Duration.ofMillis(13), Duration.ofMillis(14))));
        List<Measurement> measurements = ImmutableList.of(m1, m2);
        SortedMap<String, String> metaData = ImmutableSortedMap.of("my-meta-data-key", "my-meta-data-value");
        String name = "my-test-resource";
        Resource resource = new Resource(name, metaData, measurements);
        
        try(JSONStatusReporter jsonStatusReporter = jsonStatusReporter()) {
            jsonStatusReporter.accept(resource);
            jsonStatusReporter.accept(resource);            
        }
        
        String expected;
        try (InputStream in = InfluxDBStatusReporterTest.class.getResourceAsStream("/JSONStatusReporter_Expected_with_durations.json")) {
            expected = new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8).replaceAll("\\s", "").replace("\n", "");
        }
        String actual = result.toString();
        
        assertEquals(expected, actual);
    }

}