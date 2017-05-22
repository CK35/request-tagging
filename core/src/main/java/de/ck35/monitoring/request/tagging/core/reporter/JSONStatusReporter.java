package de.ck35.monitoring.request.tagging.core.reporter;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A generic status reporter which writes request data in a JSON format which is
 * suitable for various endpoints.
 * 
 * @author Christian Kaspari
 * @since 2.0.0
 */
public class JSONStatusReporter implements StatusReporter {

    private final Instant instant;
    private final Optional<String> hostId;
    private final Optional<String> instanceId;
    protected final Consumer<String> writer;

    private boolean firstMeasurement;

    public JSONStatusReporter(Instant instant, String hostId, String instanceId, Consumer<String> writer) {
        this.writer = writer;
        this.instant = Objects.requireNonNull(instant);
        this.hostId = Optional.ofNullable(hostId);
        this.instanceId = Optional.ofNullable(instanceId);
        this.firstMeasurement = true;
        beforeMeasurements();
    }

    protected void beforeMeasurements() {
        writer.accept("[");
    }

    protected void afterMeasurements() {
        writer.accept("]");
    }

    protected void appendMeasurement(JsonObject measurementObject) {
        if (firstMeasurement) {
            firstMeasurement = false;
        } else {
            writer.accept(",");
        }
        writer.accept(measurementObject.toJSON());
    }

    @Override
    public void close() {
        afterMeasurements();
    }

    @Override
    public void accept(Resource resource) {
        JsonObject object = new JsonObject();
        object.appendField("timestamp", instant.toString());
        object.appendField("key", "request_data");
        object.appendField("resource_name", resource.getName());
        hostId.ifPresent(id -> object.appendField("host", id));
        instanceId.ifPresent(id -> object.appendField("instanceId", id));
        resource.getMetaData()
                .forEach(object::appendField);
        resource.getMeasurements()
                .forEach(measurement -> {
                    JsonObject measurementObject = new JsonObject(object);
                    measurementObject.appendField("statusCodeName", measurement.getStatusCodeName());
                    measurementObject.appendField("totalNumberOfInvocations", measurement.getTotalNumberOfInvocations());
                    measurement.getDurations()
                               .forEach(measurementObject::appendField);
                    appendMeasurement(measurementObject);
                });
    }

    public static class JsonObject {

        private final StringBuilder builder;
        private boolean firstField;

        public JsonObject() {
            builder = new StringBuilder();
            firstField = true;
            builder.append("{");
        }

        public JsonObject(JsonObject other) {
            builder = new StringBuilder(other.builder);
            firstField = other.firstField;
        }

        public void appendField(String name, String value) {
            appendFieldName(name);
            builder.append("\"")
                   .append(value.replace("\"", "\"\""))
                   .append("\"");
        }

        public void appendField(String name, long value) {
            appendFieldName(name);
            builder.append(value);
        }

        public void appendField(String name, List<Duration> durations) {
            if (durations.isEmpty()) {
                return;
            }
            appendFieldName(name);
            builder.append("[");
            Iterator<Duration> iter = durations.iterator();
            builder.append(iter.next()
                               .toMillis());
            iter.forEachRemaining(duration -> {
                builder.append(",");
                builder.append(duration.toMillis());
            });
            builder.append("]");
        }

        private void appendFieldName(String fieldName) {
            if (firstField) {
                firstField = false;
            } else {
                builder.append(",");
            }
            builder.append("\"")
                   .append(fieldName)
                   .append("\":");
        }

        public String toJSON() {
            return builder.append("}")
                          .toString();
        }
    }
}