package de.ck35.monitoring.request.tagging;

import java.util.Objects;
import java.util.Optional;

/**
 * The entry point for request tagging. Start request tagging by invoking: {@link RequestTagging#get()}.
 * With the received {@link #STATUS} you can add request meta data e.g. a the name of the resource
 * which has been called with the current request.
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class RequestTagging {
    
    /**
     * The status for one request which can be changed while request processing. 
     *
     * @author Christian Kaspari
     * @since 1.0.0
     */
    public interface Status {
        
        /**
         * Ignore this request. No reporting will be triggered.
         * You have to call {@link #heed()} if reporting should be triggered again
         * after the ignore call.
         * 
         * @return This instance for further updates.
         */
        Status ignore();
        
        /**
         * Report the status of this request. If the request has been ignored before
         * by invoking {@link #ignore()} this will be reverted and reporting
         * is enabled again.
         * 
         * @return This instance for further updates.
         */
        Status heed();
        
        /**
         * Mark the request as successful processed.
         * 
         * @return This instance for further updates.
         */
        Status success();
        
        /**
         * Mark the request as failed because of an internal server error.
         * 
         * @return This instance for further updates.
         */
        Status serverError();
        
        /**
         * Mark the request as failed because of an client error.
         * 
         * @return This instance for further updates.
         */
        Status clientError();
        
        /**
         * Add an explicit resource name to the given request status for better separation.
         * 
         * @param name The resource which has been invoked by the current request.
         * @return This instance for further updates.
         */
        Status withResourceName(String name);
        
        /**
         * Add an optional key - value meta data pair for better separation. E.g. client
         * information could be added here or further resource information.
         * 
         * @param key The key for the meta data.
         * @param value The value for the meta data.
         * @return This instance for further updates.
         */
        Status withMetaData(String key, String value);
        
        /**
         * Add an optional key - value meta data pair. The given value will be hased with 
         * the configured message digest algorithm.
         * 
         * @param key The key for the meta data.
         * @param value The value which will be hashed.
         * @return This instance for further updates.
         * @since 1.1.0
         */
        Status withHashedMetaData(String key, String value);
        
        /**
         * Allow another runnable to report the status of this request. This is useful when
         * you have asynchronous request processing inside your application. The runnable
         * will receive its own copy of the current status so you can decide what should
         * happen with the current status.
         * 
         * @param runnable The runnable which should be able to report a request status. 
         * @return A runnable which wraps the given runnable and enables request tagging.
         */
        Runnable handover(Runnable runnable);

        /**
         * Invoke after status has been finally updated and is now ready for reporting.
         */
        void consume();
        
    }
    
    private static final ThreadLocal<Status> STATUS = new ThreadLocal<>();
    
    private RequestTagging(){};
    
    /**
     * Initialize request tagging with the given status. The caller of this method is responsible
     * for removing request tagging status when request processing is done with {@link #remove()}
     * <br>
     * <br>
     * <b>This method is intended for request tagging providers. 
     *    Users of request tagging should only call {@link #get()} or {@link #getOptional()}
     * </b>
     * 
     * @param status The status to set.
     */
    public static void init(Status status) {
        STATUS.set(Objects.requireNonNull(status));
    }
    
    /**
     * @return The status for the given request. Never <code>null</code>.
     */
    public static Status get() {
        return getOptional().orElse(EMPTY_STATUS);
    }
    
    /**
     * @return The status for the given request if request tagging is enabled for it. Otherwise {@link Optional#empty()}
     */
    public static Optional<Status> getOptional() {
        return Optional.ofNullable((Status) STATUS.get());
    }
    
    /**
     * Remove the current request tagging status.
     * <br>
     * <br>
     * <b>This method is intended for request tagging providers. 
     *    Users of request tagging should only call {@link #get()} or {@link #getOptional()}
     * </b>
     */
    public static void remove() {
        STATUS.remove();
    }
    
    /**
     * A default status implementation which is used when request tagging is disabled.
     */
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
        public Status withHashedMetaData(String key, String value) {
            return null;
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
        public Status clientError() {
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
        public Runnable handover(Runnable runnable) {
            return runnable;
        };
        @Override
        public void consume() {
            
        }
    };
}