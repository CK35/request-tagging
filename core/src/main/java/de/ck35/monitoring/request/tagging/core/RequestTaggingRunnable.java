package de.ck35.monitoring.request.tagging.core;

import de.ck35.monitoring.request.tagging.RequestTagging;

public class RequestTaggingRunnable implements Runnable {
    
    private static final String EXCEPTION_CAUSE_KEY = "serverErrorCause";
    
    private final Runnable runnable;
    private final RequestTagging.Status status;

    public RequestTaggingRunnable(Runnable runnable,
                                  RequestTagging.Status status) {
        this.runnable = runnable;
        this.status = status;
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