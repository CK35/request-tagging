package de.ck35.monitoring.request.tagging.core.reporter;

import java.util.Map;

public interface RequestTaggingStatusReporter {

    void accept(String resourceName, Map<String, Long> statusCodeCounters, Map<String, String> metaData);
    
    void commit();
    
}