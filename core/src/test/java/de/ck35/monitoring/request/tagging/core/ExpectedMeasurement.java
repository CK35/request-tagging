package de.ck35.monitoring.request.tagging.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus.StatusCode;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter.Measurement;

public class ExpectedMeasurement {
    
    private String statusCodeName;
    private long totalNumberOfInvocations;
    private Map<String, List<Duration>> durations;
    
    public ExpectedMeasurement() {
        this.statusCodeName = StatusCode.SUCCESS.name();
        this.totalNumberOfInvocations = 1;
        this.durations = new HashMap<>();
    }
    
    public static ExpectedMeasurement measurement() {
        return new ExpectedMeasurement();
    }
    public static ExpectedMeasurement of(Measurement measurement) {
        ExpectedMeasurement result = new ExpectedMeasurement();
        result.withStatusCodeName(measurement.getStatusCodeName());
        result.withTotalNumberOfInvocations(measurement.getTotalNumberOfInvocations());
        measurement.getDurations().forEach(result::withDurations);
        return result;
    }
    
    public ExpectedMeasurement withStatusCodeName(String statusCodeName) {
        this.statusCodeName = statusCodeName;
        return this;
    }
    public ExpectedMeasurement withTotalNumberOfInvocations(long totalNumberOfInvocations) {
        this.totalNumberOfInvocations = totalNumberOfInvocations;
        return this;
    }
    public ExpectedMeasurement withDurations(String durationName, List<Duration> durations) {
        this.durations.put(durationName, new ArrayList<>(durations));
        return this;
    }
    public ExpectedMeasurement withDuration(String durationName, Duration duration) {
        this.durations.computeIfAbsent(durationName, x -> new ArrayList<>()).add(duration);
        return this;
    }

    @Override
    public String toString() {
        return "[statusCodeName=" + statusCodeName + ", totalNumberOfInvocations=" + totalNumberOfInvocations + ", durations=" + durations + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((durations == null) ? 0 : durations.hashCode());
        result = prime * result + ((statusCodeName == null) ? 0 : statusCodeName.hashCode());
        result = prime * result + (int) (totalNumberOfInvocations ^ (totalNumberOfInvocations >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ExpectedMeasurement)) {
            return false;
        }
        ExpectedMeasurement other = (ExpectedMeasurement) obj;
        if (durations == null) {
            if (other.durations != null) {
                return false;
            }
        } else if (!durations.equals(other.durations)) {
            return false;
        }
        if (statusCodeName == null) {
            if (other.statusCodeName != null) {
                return false;
            }
        } else if (!statusCodeName.equals(other.statusCodeName)) {
            return false;
        }
        if (totalNumberOfInvocations != other.totalNumberOfInvocations) {
            return false;
        }
        return true;
    }
}