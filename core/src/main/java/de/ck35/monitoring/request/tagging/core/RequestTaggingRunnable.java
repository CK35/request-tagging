package de.ck35.monitoring.request.tagging.core;

import java.util.Objects;

import de.ck35.monitoring.request.tagging.RequestTagging;
import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus.StatusCode;

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
    private static final String DEFAULT_TIMER_KEY = "total_request_duration";
    
    private final Runnable runnable;
    private final DefaultRequestTaggingStatus status;

    public RequestTaggingRunnable(Runnable runnable,
                                  DefaultRequestTaggingStatus status) {
        this.runnable = Objects.requireNonNull(runnable);
        this.status = Objects.requireNonNull(status);
    }
    
    @Override
    public void run() {
        RequestTagging.init(status);
        try {
            status.startTimer(DEFAULT_TIMER_KEY);
            try {                
                runnable.run();
            } catch(RuntimeException e) { 
                if(e instanceof WrappedException) {
                    tagServerError(e.getCause());
                } else {
                    tagServerError(e);
                }
                throw e;
            } finally {
                status.stopTimer(DEFAULT_TIMER_KEY);
                status.consume();
            }
        } finally {
            RequestTagging.remove();
        }
    }
    
    private void tagServerError(Throwable e) {
        if(status.getStatusCode() == StatusCode.SUCCESS) {            
            status.serverError().withMetaData(EXCEPTION_CAUSE_KEY, e.getClass().getName());
        }
    }
    
    public static class WrappedException extends RuntimeException {
        
        private final Throwable source;
        
        public WrappedException(Throwable cause) {
            this(cause, cause);
        }
        public WrappedException(Throwable source, Throwable cause) {
            super(cause);
            this.source = source;
        }
        public Throwable getSource() {
            return source;
        }
    }
}