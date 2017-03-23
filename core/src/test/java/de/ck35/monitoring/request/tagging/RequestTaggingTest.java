package de.ck35.monitoring.request.tagging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import de.ck35.monitoring.request.tagging.RequestTagging.Status;
import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus;

public class RequestTaggingTest {
    
    @Test
    public void testInit() {
        assertFalse(RequestTagging.getOptional().isPresent());
        assertEquals(RequestTagging.EMPTY_STATUS, RequestTagging.get());
        DefaultRequestTaggingStatus expectedStatus = new DefaultRequestTaggingStatus(status -> {}, x -> x);
        try {
            RequestTagging.init(expectedStatus);
            assertEquals(expectedStatus, RequestTagging.get());
            assertEquals(expectedStatus, RequestTagging.getOptional().get());
        } finally {
            RequestTagging.remove();
        }
        assertFalse(RequestTagging.getOptional().isPresent());
        assertEquals(RequestTagging.EMPTY_STATUS, RequestTagging.get());
    }

    @Test
    public void testEmptyStatus() {
        Status status = RequestTagging.get();
        assertEquals(RequestTagging.EMPTY_STATUS, status);
        status.withResourceName("test").withMetaData("key", "value1");
        status.success().serverError().clientError();
        status.ignore().heed();

        Runnable expected = () -> {};
        assertEquals(expected, status.handover(expected));
        
        status.consume();
    }
    
}