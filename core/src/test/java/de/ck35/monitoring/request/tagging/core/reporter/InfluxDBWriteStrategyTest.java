package de.ck35.monitoring.request.tagging.core.reporter;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.io.ByteStreams;

import de.ck35.monitoring.request.tagging.core.reporter.InfluxDBStatusReporter;
import de.ck35.monitoring.request.tagging.core.reporter.InfluxDBStatusReporter.Line;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter.Measurement;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter.Resource;

public class InfluxDBWriteStrategyTest {

    private String hostId;
    private String instanceId;
    private Instant instant;
    private StringBuilder result;

    public InfluxDBWriteStrategyTest() {
        this.hostId = "my-host";
        this.instanceId = "my-instance";
        this.instant = Instant.parse("2007-12-03T10:15:30.00Z");
        this.result = new StringBuilder();
    }

    public InfluxDBStatusReporter influxDBStatusReporter() {
        return new InfluxDBStatusReporter(instant, hostId, instanceId, result::append);
    }
    
    public Line line() {
        return new Line(instant);
    }

    @Test
    public void testWrite() throws Exception {
        Measurement m1 = new Measurement("SUCCESS", 5, Collections.emptyMap());
        Measurement m2 = new Measurement("CLIENT_ERROR", 6, Collections.emptyMap());
        List<Measurement> measurements = ImmutableList.of(m1, m2);
        SortedMap<String, String> metaData = ImmutableSortedMap.of("my-meta-data-key", "my-meta-data-value");
        String name = "my-test-resource";
        Resource resource = new Resource(name, metaData, measurements);
        
        influxDBStatusReporter().accept(resource);

        assertEqualsContent("/InfluxDBWriteStrategyTest_Expected.txt", result.toString());
    }
    
    @Test
    public void testWriteWithDurations() throws Exception {
        Measurement m1 = new Measurement("SUCCESS", 5, ImmutableMap.of("total_request_duration", ImmutableList.of(Duration.ofMillis(10), Duration.ofMillis(11), Duration.ofMillis(12))));
        Measurement m2 = new Measurement("CLIENT_ERROR", 6, ImmutableMap.of("total_request_duration", ImmutableList.of(Duration.ofMillis(13), Duration.ofMillis(14))));
        List<Measurement> measurements = ImmutableList.of(m1, m2);
        SortedMap<String, String> metaData = ImmutableSortedMap.of("my-meta-data-key", "my-meta-data-value");
        String name = "my-test-resource";
        Resource resource = new Resource(name, metaData, measurements);
        
        influxDBStatusReporter().accept(resource);

        assertEqualsContent("/InfluxDBWriteStrategyTest_Expected_with_durations.txt", result.toString());
    }

    private static void assertEqualsContent(String expectedResourceLocation, String actual) throws IOException {
        String expected;
        try (InputStream in = InfluxDBWriteStrategyTest.class.getResourceAsStream(expectedResourceLocation)) {
            expected = new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8);
        }
        Splitter splitter = Splitter.on("\n");
        List<String> actualList = splitter.splitToList(actual);
        List<String> expectedList = splitter.splitToList(expected);
        assertEquals(expectedList.size(), actualList.size());
        for (int index = 0; index < expectedList.size(); index++) {
            assertEquals("At index: " + index, expectedList.get(index), actualList.get(index));
        }
    }
    
    @Test(expected=IllegalStateException.class)
    public void testWriteTagAfterField() {
        Line line = line();
        failOnIllegalStateException(() -> line.writeField("field-key", 5));
        line.writeTag("invalid-tag", "value");
    }
    
    @Test(expected=IllegalStateException.class)
    public void testWriteTagAfterEnd() {
        Line line = line();
        failOnIllegalStateException(() -> line.writeField("field-key", 5));
        failOnIllegalStateException(() -> line.getCompleteLine());
        line.writeTag("invalid-tag", "value");
    }
    
    @Test(expected=IllegalStateException.class)
    public void testWriteFieldAfterEnd() {
        Line line = line();
        failOnIllegalStateException(() -> line.writeField("field-key", 5));
        failOnIllegalStateException(() -> line.getCompleteLine());
        line.writeField("invalid-field", 5);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testGetCompleteLineBeforeFields() {
        Line line = line();
        line.getCompleteLine();
    }
    
    @Test
    public void testGetCompleteLineTwiceWillResultInSameLine() {
        Line line = line();
        line.writeField("field-key", 5);
        String completeLine = line.getCompleteLine();
        assertEquals(completeLine, line.getCompleteLine());
    }

    private static void failOnIllegalStateException(Runnable runnable) {
        try {            
            runnable.run();
        } catch(IllegalStateException e) {
            fail("IllegalState not expected here!");
        }
    }
}