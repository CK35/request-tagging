package de.ck35.monitoring.request.tagging.core.reporter.http;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.SortedMap;

import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.io.ByteStreams;

import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter.Measurement;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter.Resource;

public class InfluxDBWriteStrategyTest {

    private String hostId;
    private String instanceId;
    private Instant instant;

    public InfluxDBWriteStrategyTest() {
        this.hostId = "my-host";
        this.instanceId = "my-instance";
        this.instant = Instant.parse("2007-12-03T10:15:30.00Z");
    }

    public InfluxDBWriteStrategy influxDBWriteStrategy() {
        return (InfluxDBWriteStrategy) InfluxDBWriteStrategy.writeStrategy(hostId, instanceId)
                                                            .apply(instant);
    }

    @Test
    public void testWrite() throws Exception {
        StringBuilder result = new StringBuilder();
        Measurement m1 = new Measurement("SUCCESS", 5, ImmutableMap.of("total_request_duration", ImmutableList.of(Duration.ofMillis(10), Duration.ofMillis(11), Duration.ofMillis(12))));
        Measurement m2 = new Measurement("CLIENT_ERROR", 6, ImmutableMap.of("total_request_duration", ImmutableList.of(Duration.ofMillis(13), Duration.ofMillis(14))));
        List<Measurement> measurements = ImmutableList.of(m1, m2);
        SortedMap<String, String> metaData = ImmutableSortedMap.of("my-meta-data-key", "my-meta-data-value");
        String name = "my-test-resource";
        Resource resource = new Resource(name, metaData, measurements);
        InfluxDBWriteStrategy strategy = influxDBWriteStrategy();
        strategy.write(resource, result::append);

        String actual = result.toString();
        String expected;
        try (InputStream in = getClass().getResourceAsStream("/InfluxDBWriteStrategyTest_Expected.txt")) {
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

}