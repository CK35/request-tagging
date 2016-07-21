package de.ck35.monitoring.request.tagging.core;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import de.ck35.monitoring.request.tagging.RequestTagging;

public class DefaultRequestTaggingStatus implements RequestTagging.Status {
    
    public static final String DEFAULT_RESOURCE_NAME = "default";
    
    public static enum StatusCode {

        SUCCESS,
        
        CLIENT_ERROR,
        
        SERVER_ERROR
        
    }
    
    private final Consumer<DefaultRequestTaggingStatus> statusConsumer;
    
    private SortedMap<String, String> metaData;

    private volatile boolean ignored;
    private volatile String resourceName;
    private volatile StatusCode statusCode;
    
    public DefaultRequestTaggingStatus(Consumer<DefaultRequestTaggingStatus> statusConsumer) {
        this.statusConsumer = statusConsumer;
        this.ignored = false;
        this.resourceName = DEFAULT_RESOURCE_NAME;
        this.statusCode = StatusCode.SUCCESS;
    }
    public DefaultRequestTaggingStatus(DefaultRequestTaggingStatus status) {
        this.statusConsumer = status.statusConsumer;
        this.ignored = status.isIgnored();
        this.resourceName = status.getResourceName();
        this.statusCode = status.getStatusCode();
        this.metaData = status.getMetaData();
    }
    
    @Override
    public RequestTagging.Status ignore() {
        ignored = true;
        return this;
    }
    @Override
    public RequestTagging.Status heed() {
        ignored = false;
        return this;
    }
    public boolean isIgnored() {
        return ignored;
    }
    @Override
    public RequestTagging.Status success() {
        statusCode = StatusCode.SUCCESS;
        return this;
    }
    @Override
    public RequestTagging.Status serverError() {
        statusCode = StatusCode.SERVER_ERROR;
        return this;
    }
    @Override
    public RequestTagging.Status clientError() {
        statusCode = StatusCode.CLIENT_ERROR;
        return this;
    }
    public StatusCode getStatusCode() {
        return statusCode;
    }
    @Override
    public RequestTagging.Status withResourceName(String name) {
        resourceName = Objects.requireNonNull(name);
        return this;
    }
    public String getResourceName() {
        return resourceName;
    }
    @Override
    public RequestTagging.Status withMetaData(String key, String value) {
        synchronized (this) {
            if(metaData == null) {
                metaData = new TreeMap<>(); 
            }
            metaData.put(key, value);
        }
        return this;
    }
    @Override
    public Runnable handover(Runnable runnable) {
        RequestTagging.Status status = RequestTagging.get();
        if(status instanceof DefaultRequestTaggingStatus) {
            return new RequestTaggingRunnable(runnable, new DefaultRequestTaggingStatus((DefaultRequestTaggingStatus) status));
        } else {
            return runnable;
        }
    }
    public SortedMap<String, String> drainMetaData() {
        synchronized (this) {
            if(metaData == null) {
                return Collections.emptySortedMap();
            } else {
                SortedMap<String, String> result = metaData;
                metaData = null;
                return result;
            }
        }
    }
    public SortedMap<String, String> getMetaData() {
        synchronized (this) {
            if(metaData == null) {
                return null;
            } else {
                return new TreeMap<>(metaData);
            }
        }
    }
    public void consume() {
        statusConsumer.accept(this);
    }
}