package de.ck35.monitoring.request.tagging.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import de.ck35.monitoring.request.tagging.RequestTagging;
import de.ck35.monitoring.request.tagging.RequestTagging.Status;

/**
 * Default implementation of the request tagging status which uses an Enum for
 * the status changes (success, client error or server error).
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class DefaultRequestTaggingStatus implements RequestTagging.Status {

    public static final String DEFAULT_RESOURCE_NAME = "default";

    public static enum StatusCode {

            SUCCESS,

            CLIENT_ERROR,

            SERVER_ERROR

    }

    private final Consumer<DefaultRequestTaggingStatus> statusConsumer;
    private final Function<String, String> hashAlgorithm;
    private final Clock stopWatchClock;

    private boolean ignored;
    private String resourceName;
    private StatusCode statusCode;
    private SortedMap<String, String> metaData;
    private Map<String, StopWatch> stopWatches;

    public DefaultRequestTaggingStatus(Consumer<DefaultRequestTaggingStatus> statusConsumer) {
        this(statusConsumer, new HashAlgorithm()::hash, Clock.systemUTC());
    }
    
    public DefaultRequestTaggingStatus(Consumer<DefaultRequestTaggingStatus> statusConsumer, Function<String, String> hashAlgorithm, Clock stopWatchClock) {
        this.statusConsumer = Objects.requireNonNull(statusConsumer);
        this.hashAlgorithm = Objects.requireNonNull(hashAlgorithm);
        this.stopWatchClock = Objects.requireNonNull(stopWatchClock);
        this.ignored = false;
        this.resourceName = DEFAULT_RESOURCE_NAME;
        this.statusCode = StatusCode.SUCCESS;
    }

    public DefaultRequestTaggingStatus(DefaultRequestTaggingStatus status) {
        this.statusConsumer = status.statusConsumer;
        this.hashAlgorithm = status.hashAlgorithm;
        this.stopWatchClock = status.stopWatchClock;
        this.ignored = status.ignored;
        this.resourceName = status.resourceName;
        this.statusCode = status.statusCode;
        this.metaData = status.metaData == null ? null : new TreeMap<>(status.metaData);
        this.stopWatches = status.copyMeasurements();
    }

    @Override
    public RequestTagging.Status ignore() {
        ignored = true;
        return this;
    }

    @Override
    public RequestTagging.Status heed() {
        ignored = false;
        return this;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    @Override
    public RequestTagging.Status success() {
        statusCode = StatusCode.SUCCESS;
        return this;
    }

    @Override
    public RequestTagging.Status serverError() {
        statusCode = StatusCode.SERVER_ERROR;
        return this;
    }

    @Override
    public RequestTagging.Status clientError() {
        statusCode = StatusCode.CLIENT_ERROR;
        return this;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = StatusCode.valueOf(statusCode);
    }

    @Override
    public RequestTagging.Status withResourceName(String name) {
        resourceName = Objects.requireNonNull(name);
        return this;
    }
    
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }

    @Override
    public RequestTagging.Status withMetaData(String key, String value) {
        if (metaData == null) {
            metaData = new TreeMap<>();
        }
        metaData.put(key, value);
        return this;
    }

    @Override
    public Status withHashedMetaData(String key, String value) {
        return withMetaData(key, hashAlgorithm.apply(value));
    }

    public SortedMap<String, String> getMetaData() {
        if(metaData == null) {
            return Collections.emptySortedMap();
        } else {            
            return metaData;
        }
    }

    @Override
    public Status startTimer(String id) {
        if (stopWatches == null) {
            stopWatches = new HashMap<>();
        }
        stopWatches.computeIfAbsent(id, x -> new StopWatch())
                    .setStart(stopWatchClock.instant());
        return this;
    }

    @Override
    public Status stopTimer(String id) {
        Optional.ofNullable(stopWatches)
                .map(x -> x.get(id))
                .ifPresent(measurement -> measurement.setEnd(stopWatchClock.instant()));
        return this;
    }

    private Map<String, StopWatch> copyMeasurements() {
        if (stopWatches == null) {
            return null;
        }
        Map<String, StopWatch> result = new HashMap<>();
        stopWatches.forEach((key, value) -> {
            result.put(key, new StopWatch(value));
        });
        return result;
    }

    public void visitDurations(BiConsumer<String, Duration> visitor) {
        if (stopWatches == null) {
            return;
        }
        stopWatches.forEach((key, value) -> {
            value.toDuration()
                 .ifPresent(duration -> visitor.accept(key, duration));
        });
    }

    @Override
    public Runnable handover(Runnable runnable) {
        RequestTagging.Status status = RequestTagging.get();
        if (status instanceof DefaultRequestTaggingStatus) {
            return new RequestTaggingRunnable(runnable, new DefaultRequestTaggingStatus((DefaultRequestTaggingStatus) status));
        } else {
            return runnable;
        }
    }

    public void consume() {
        statusConsumer.accept(this);
    }

    public static class StopWatch {

        private Instant start;
        private Instant end;

        StopWatch() {
            this(null, null);
        }

        StopWatch(StopWatch stopWatch) {
            this(stopWatch.start, stopWatch.end);
        }

        StopWatch(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

        public Optional<Instant> getStart() {
            return Optional.ofNullable(start);
        }

        public void setStart(Instant start) {
            this.start = Objects.requireNonNull(start);
        }

        public Optional<Instant> getEnd() {
            return Optional.ofNullable(end);
        }

        public void setEnd(Instant end) {
            this.end = Objects.requireNonNull(end);
        }

        public Optional<Duration> toDuration() {
            return getStart().flatMap(start -> getEnd().map(end -> Duration.between(start, end)));
        }
    }
}