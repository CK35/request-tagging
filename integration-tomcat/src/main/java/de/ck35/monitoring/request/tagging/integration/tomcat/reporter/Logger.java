package de.ck35.monitoring.request.tagging.integration.tomcat.reporter;

import java.time.Instant;
import java.util.Map;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter;

public class Logger {
    
    public Reporter reporter(Instant instant) {
        return new Reporter(instant);
    }
    
    public static class Reporter implements RequestTaggingStatusReporter {
        
        private static final Log LOG = LogFactory.getLog(Reporter.class);
        
        private final Instant instant;
        
        public Reporter(Instant instant) {
            this.instant = instant;
        }

        @Override
        public void accept(String resourceName, Map<String, Long> statusCodeCounters, Map<String, String> metaData) {
            LOG.info("Request Data: [" + instant.toString() + "] " + resourceName + ": " + statusCodeCounters + " - " + metaData);
        }

        @Override
        public void commit() {
            //Ignore
        }
        
    }
    
}