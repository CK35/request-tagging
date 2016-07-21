package de.ck35.monitoring.request.tagging;

import java.util.Objects;
import java.util.Optional;

public class RequestTagging {
    
    public interface Status {
        
        Status ignore();
        
        Status heed();
        
        Status success();
        
        Status serverError();
        
        Status clientError();

        Status withResourceName(String name);
        
        Status withMetaData(String key, String value);
        
        Runnable handover(Runnable runnable);

        void consume();
        
    }
    
    private static final ThreadLocal<Status> STATUS = new ThreadLocal<>();
    
    public static void init(Status status) {
        STATUS.set(Objects.requireNonNull(status));
    }
    
    public static Status get() {
        return getOptional().orElse(EMPTY_STATUS);
    }
    
    public static Optional<Status> getOptional() {
        return Optional.ofNullable((Status) STATUS.get());
    }
    
    public static void remove() {
        STATUS.remove();
    }
    
    public static final Status EMPTY_STATUS = new Status() {
        @Override
        public Status withResourceName(String name) {
            return this;
        }
        @Override
        public Status withMetaData(String key, String value) {
            return this;
        }
        @Override
        public Status success() {
            return this;
        }
        @Override
        public Status serverError() {
            return this;
        }
        @Override
        public Status ignore() {
            return this;
        }
        @Override
        public Status heed() {
            return this;
        }
        @Override
        public Status clientError() {
            return this;
        }
        public Runnable handover(Runnable runnable) {
            return runnable;
        };
        @Override
        public void consume() {
            
        }
    };
}