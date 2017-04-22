package de.ck35.monitoring.request.tagging.core.reporter.http;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.SortedMap;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter.Measurement;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter.Resource;

public class JSONWriteStrategyTest {

    private String hostId;
    private String instanceId;
    private Instant instant;

    public JSONWriteStrategyTest() {
        this.hostId = "my-host";
        this.instanceId = "my-instance";
        this.instant = Instant.parse("2007-12-03T10:15:30.00Z");
    }

    public JSONWriteStrategy jsonWriteStrategy() {
        return (JSONWriteStrategy) JSONWriteStrategy.writeStrategy(hostId, instanceId)
                                                            .apply(instant);
    }
    
    @Test
    public void testWriteWithDurations() throws Exception {
        StringBuilder result = new StringBuilder();
        Measurement m1 = new Measurement("SUCCESS", 5, ImmutableMap.of("total_request_duration", ImmutableList.of(Duration.ofMillis(10), Duration.ofMillis(11), Duration.ofMillis(12))));
        Measurement m2 = new Measurement("CLIENT_ERROR", 6, ImmutableMap.of("total_request_duration", ImmutableList.of(Duration.ofMillis(13), Duration.ofMillis(14))));
        List<Measurement> measurements = ImmutableList.of(m1, m2);
        SortedMap<String, String> metaData = ImmutableSortedMap.of("my-meta-data-key", "my-meta-data-value");
        String name = "my-test-resource";
        Resource resource = new Resource(name, metaData, measurements);
        
        JSONWriteStrategy jsonWriteStrategy = jsonWriteStrategy();
        jsonWriteStrategy.beforeWrite(result::append);
        jsonWriteStrategy.write(resource, result::append);
        jsonWriteStrategy.write(resource, result::append);
        jsonWriteStrategy.afterWrite(result::append);
    }

}