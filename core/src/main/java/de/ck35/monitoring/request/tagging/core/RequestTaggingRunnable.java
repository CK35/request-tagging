package de.ck35.monitoring.request.tagging.core;

import java.util.Objects;

import de.ck35.monitoring.request.tagging.RequestTagging;

/**
 * Provides the request tagging mechanism. Wraps a {@link Runnable} which can access the
 * given status while processing. This implementation ensures that request tagging status
 * is cleared after the {@link Runnable} has been invoked. If the given {@link Runnable}
 * throws an unchecked exception a server error status will be applied automatically.
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class RequestTaggingRunnable implements Runnable {
    
    private static final String EXCEPTION_CAUSE_KEY = "serverErrorCause";
    
    private final Runnable runnable;
    private final RequestTagging.Status status;

    public RequestTaggingRunnable(Runnable runnable,
                                  RequestTagging.Status status) {
        this.runnable = Objects.requireNonNull(runnable);
        this.status = Objects.requireNonNull(status);
    }
    
    @Override
    public void run() {
        RequestTagging.init(status);
        try {
            try {                
                runnable.run();
            } catch(RuntimeException e) {                
                tagServerError(status, e);
                throw e;
            } finally {                
                status.consume();
            }
        } finally {
            RequestTagging.remove();
        }
    }
    
    public static RequestTagging.Status tagServerError(RequestTagging.Status status, Throwable e) {
        return status.serverError().withMetaData(EXCEPTION_CAUSE_KEY, e.getClass().getName());
    }
    
}