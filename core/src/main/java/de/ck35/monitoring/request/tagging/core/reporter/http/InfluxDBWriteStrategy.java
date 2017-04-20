package de.ck35.monitoring.request.tagging.core.reporter.http;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter.Resource;
import de.ck35.monitoring.request.tagging.core.reporter.http.StreamingHttpReporter.WriteStrategy;

public class InfluxDBWriteStrategy implements StreamingHttpReporter.WriteStrategy {

    private final Instant instant;
    private final Optional<String> hostId;
    private final Optional<String> instanceId;

    public InfluxDBWriteStrategy(Instant instant, String hostId, String instanceId) {
        this.instant = Objects.requireNonNull(instant);
        this.hostId = Optional.ofNullable(hostId);
        this.instanceId = Optional.ofNullable(instanceId);
    }

    public static Function<Instant, WriteStrategy> writeStrategy(String hostId, String instanceId) {
        return instant -> new InfluxDBWriteStrategy(instant, hostId, instanceId);
    }

    @Override
    public void write(Resource resource, Consumer<String> writer) {
        Line lineWithMetaData = new Line(instant);
        lineWithMetaData.writeTag("resource_name", resource.getName());
        hostId.ifPresent(id -> lineWithMetaData.writeTag("host", id));
        instanceId.ifPresent(id -> lineWithMetaData.writeTag("instanceId", id));
        resource.getMetaData()
                .forEach(lineWithMetaData::writeTag);

        Line totalNumberOfInvocations = new Line(lineWithMetaData);
        resource.getMeasurements()
                .forEach(measurement -> totalNumberOfInvocations.writeField(measurement.getStatusCodeName(), measurement.getTotalNumberOfInvocations()));
        writer.accept(totalNumberOfInvocations.getCompleteLine());
        
        resource.getMeasurements().forEach(measurement -> {
            Line lineWithStatusCode = new Line(lineWithMetaData);
            lineWithStatusCode.writeTag("statusCodeName", measurement.getStatusCodeName());
            measurement.getDurations().forEach((key, durations) -> {
                for(int index = 0 ; index < durations.size() ; index++) {
                    Line lineWithDuration = new Line(lineWithStatusCode);
                    lineWithDuration.writeTag("uniqueDurationIndex", Integer.toString(index));
                    lineWithDuration.writeField(key, durations.get(index).toMillis());
                    writer.accept(lineWithDuration.getCompleteLine());
                }
            });
        });
    }

    public static class Line {

        private enum WritePosition {
                TAGS, FIELDS, END
        }

        private final String lineEnding;
        private final StringBuilder builder;

        private WritePosition position;

        public Line(Instant timestamp) {
            this.lineEnding = toLineEnding(timestamp);
            this.builder = new StringBuilder("request_data");
            this.position = WritePosition.TAGS;
        }

        public Line(Line other) {
            this.lineEnding = other.lineEnding;
            this.builder = new StringBuilder(other.builder);
            this.position = other.position;
        }

        public void writeTag(String tagKey, String tagValue) {
            if (position == WritePosition.TAGS) {
                builder.append(",");
            } else {
                throw new IllegalStateException("Can not append tag at this position: '" + position + "'!");
            }
            builder.append(tagKey)
                   .append("=\"")
                   .append(tagValue.replace("\"", "\\\""))
                   .append("\"");
        }

        public void writeField(String key, long value) {
            if (position == WritePosition.TAGS) {
                builder.append(" ");
                position = WritePosition.FIELDS;
            } else if (position == WritePosition.FIELDS) {
                builder.append(",");
            } else {
                throw new IllegalStateException("Can not append field at this position: '" + position + "'!");
            }
            builder.append(key)
                   .append("=")
                   .append(value);
        }

        public String getCompleteLine() {
            if (position == WritePosition.END) {
                return builder.toString();
            } else if (position == WritePosition.FIELDS) {
                position = WritePosition.END;
                return builder.append(lineEnding)
                              .toString();
            } else {
                throw new IllegalStateException("Can not complete line at position: '" + position + "'!");
            }
        }

        private static String toLineEnding(Instant instant) {
            StringBuilder builder = new StringBuilder();
            builder.append(" ")
                   .append(instant.toEpochMilli())
                   .append("000000")
                   .append("\n");
            return builder.toString();
        }
    }
}