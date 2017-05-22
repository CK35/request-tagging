package de.ck35.monitoring.request.tagging.core.reporter;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A status reporter which writes request data in a format which is suitable for
 * the Elasticsearch bulk HTTP endpoint.
 * 
 * @author Christian Kaspari
 * @since 2.0.0
 */
public class ElasticsearchStatusReporter extends JSONStatusReporter {

    private final String index;
    private final String type;

    public ElasticsearchStatusReporter(Instant instant, String hostId, String instanceId, Consumer<String> writer, String index, String type) {
        super(instant, hostId, instanceId, writer);
        this.index = index;
        this.type = type;
    }

    @Override
    protected void appendMeasurement(JsonObject measurementObject) {
        writer.accept("{\"index\": { \"_index\": \"" + index + "\", \"_type\": \"" + type + "\", \"_id\": \"" + generateMeasurementId() + "\"}}\n");
        writer.accept(measurementObject.toJSON());
        writer.accept("\r\n");
    }

    @Override
    protected void beforeMeasurements() {
    }

    @Override
    protected void afterMeasurements() {
    }

    protected String generateMeasurementId() {
        return UUID.randomUUID()
                   .toString();
    }
}