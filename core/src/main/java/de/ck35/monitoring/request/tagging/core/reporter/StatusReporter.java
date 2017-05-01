package de.ck35.monitoring.request.tagging.core.reporter;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

/**
 * Describes a reporter for request tagging data which is invoked by the request tagging provider.
 * A reporter will be called multiple times to add data. When all data has been added {@link #commit()}
 * will be called once so a batch can be created and send. 
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public interface StatusReporter extends Closeable {
    
    public static class Resource {
        
        private final String name;
        private final SortedMap<String, String> metaData;
        private final List<Measurement> measurements;
        
        public Resource(String name, SortedMap<String, String> metaData, List<Measurement> measurements) {
            this.name = name;
            this.metaData = metaData == null ? Collections.emptySortedMap() : metaData;
            this.measurements = Objects.requireNonNull(measurements);
        }
        
        public String getName() {
            return name;
        }

        public SortedMap<String, String> getMetaData() {
            return metaData;
        }

        public List<Measurement> getMeasurements() {
            return measurements;
        }

        @Override
        public String toString() {
            return "Resource [name=" + name + ", metaData=" + metaData + ", measurements=" + measurements + "]";
        }
    }
    
    public static class Measurement {
        
        private final String statusCodeName;
        private final long totalNumberOfInvocations;
        private final Map<String, List<Duration>> durations;
        
        public Measurement(String statusCodeName, long totalNumberOfInvocations, Map<String, List<Duration>> durations) {
            this.statusCodeName = Objects.requireNonNull(statusCodeName);
            this.totalNumberOfInvocations = totalNumberOfInvocations;
            this.durations = durations == null ? Collections.emptyMap() : durations;
        }
        
        public String getStatusCodeName() {
            return statusCodeName;
        }
        public long getTotalNumberOfInvocations() {
            return totalNumberOfInvocations;
        }
        public Map<String, List<Duration>> getDurations() {
            return durations;
        }
        @Override
        public String toString() {
            return "Measurement [statusCodeName=" + statusCodeName + ", totalNumberOfInvocations=" + totalNumberOfInvocations + ", durations=" + durations + "]";
        }
    }
    
    /**
     * Append the resource measurements to the report.
     * 
     * @param resource The resource measurements.
     */
    void accept(Resource resource);
    
    @Override
    default void close() {
        
    }
    
}