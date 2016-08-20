package de.ck35.monitoring.request.tagging.core.reporter;

import java.util.Map;

/**
 * Describes a reporter for request tagging data which is invoked by the request tagging provider.
 * A reporter will be called multiple times to add data. When all data has been added {@link #commit()}
 * will be called once so a batch can be created and send. 
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public interface RequestTaggingStatusReporter {

    /**
     * Append the given status code counters to the batch.
     * 
     * @param resourceName The resource name.
     * @param statusCodeCounters The counter keys and values.
     * @param metaData The meta data for the given counter values.
     */
    void accept(String resourceName, Map<String, Long> statusCodeCounters, Map<String, String> metaData);
    
    /**
     * All data has been added. Now the batch of data should be send.
     */
    void commit();
    
}