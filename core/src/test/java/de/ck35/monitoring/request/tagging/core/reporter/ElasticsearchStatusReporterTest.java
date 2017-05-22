package de.ck35.monitoring.request.tagging.core.reporter;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.function.Consumer;

import org.junit.Test;

import com.google.common.io.ByteStreams;

import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter.Measurement;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter.Resource;

public class ElasticsearchStatusReporterTest {

    private StringBuilder result;

    private Instant instant;
    private String hostId;
    private String instanceId;
    private Consumer<String> writer;
    private String index;
    private String type;
    
    public ElasticsearchStatusReporterTest() {
        result = new StringBuilder();
        instant = Instant.parse("2007-12-03T10:15:30.00Z");
        hostId = "my-host-id";
        instanceId = "my-instance-id";
        writer = result::append;
        index = "my-index";
        type = "my-type";
    }

    public ElasticsearchStatusReporter elasticsearchStatusReporter() {
        return new ElasticsearchStatusReporter(instant, hostId, instanceId, writer, index, type) {
            @Override
            protected String generateMeasurementId() {
                return "123";
            }
        };
    }
    
    @Test
    public void testElasticsearchStatusReporter() throws IOException {
        Measurement measurement = new Measurement("SUCCESS", 5, null);
        Resource resource = new Resource("test-resource", null, Collections.singletonList(measurement));
        try(ElasticsearchStatusReporter reporter = elasticsearchStatusReporter()) {
            reporter.accept(resource);
        }
        
        String expected;
        try (InputStream in = InfluxDBStatusReporterTest.class.getResourceAsStream("/ElasticsearchStatusReporterTest_Expected.txt")) {
            expected = new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8);
        }
        String actual = result.toString();
        
        assertEquals(expected, actual);
    }

}