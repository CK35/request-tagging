package de.ck35.monitoring.request.tagging.core.reporter;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class InfluxDBStatusReporter implements StatusReporter {

    private final Instant instant;
    private final Optional<String> hostId;
    private final Optional<String> instanceId;
    private final Consumer<String> writer;

    public InfluxDBStatusReporter(Instant instant, String hostId, String instanceId, Consumer<String> writer) {
        this.writer = writer;
        this.instant = Objects.requireNonNull(instant);
        this.hostId = Optional.ofNullable(hostId);
        this.instanceId = Optional.ofNullable(instanceId);
    }

    @Override
    public void accept(Resource resource) {
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

        resource.getMeasurements()
                .forEach(measurement -> {
                    Line lineWithStatusCode = new Line(lineWithMetaData);
                    lineWithStatusCode.writeTag("statusCodeName", measurement.getStatusCodeName());
                    measurement.getDurations()
                               .forEach((key, durations) -> {
                                   for (int index = 0; index < durations.size(); index++) {
                                       Line lineWithDuration = new Line(lineWithStatusCode);
                                       lineWithDuration.writeTag("uniqueDurationIndex", Integer.toString(index));
                                       lineWithDuration.writeField(key, durations.get(index)
                                                                                 .toMillis());
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
                append(",");
            } else {
                throw new IllegalStateException("Can not append tag at this position: '" + position + "'!");
            }
            this.appendEscaped(tagKey)
                .append("=")
                .appendEscaped(tagValue);
        }

        public void writeField(String key, long value) {
            if (position == WritePosition.TAGS) {
                append(" ");
                position = WritePosition.FIELDS;
            } else if (position == WritePosition.FIELDS) {
                append(",");
            } else {
                throw new IllegalStateException("Can not append field at this position: '" + position + "'!");
            }
            this.appendEscaped(key)
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

        private Line append(String token) {
            builder.append(token);
            return this;
        }

        private Line append(long value) {
            builder.append(value);
            return this;
        }

        private Line appendEscaped(String token) {
            for (int index = 0; index < token.length(); index++) {
                char current = token.charAt(index);
                if (current == ',') {
                    builder.append("\\,");
                } else if (current == '=') {
                    builder.append("\\=");
                } else if (current == ' ') {
                    builder.append("\\ ");
                } else if (current == '\"') {
                    builder.append("\\\"");
                } else {
                    builder.append(current);
                }
            }
            return this;
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