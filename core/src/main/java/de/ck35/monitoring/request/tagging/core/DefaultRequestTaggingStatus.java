package de.ck35.monitoring.request.tagging.core;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import de.ck35.monitoring.request.tagging.RequestTagging;
import de.ck35.monitoring.request.tagging.RequestTagging.Status;

/**
 * Default implementation of the request tagging status which uses an Enum for
 * the status changes (success, client error or server error).
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class DefaultRequestTaggingStatus implements RequestTagging.Status {

    public static final String DEFAULT_RESOURCE_NAME = "default";

    public static enum StatusCode {

            SUCCESS,

            CLIENT_ERROR,

            SERVER_ERROR

    }

    private final Consumer<DefaultRequestTaggingStatus> statusConsumer;
    private final Function<String, String> hashAlgorithm;

    private final ReadWriteLock metaDataLock;
    private SortedMap<String, String> metaData;

    private volatile boolean ignored;
    private volatile String resourceName;
    private volatile StatusCode statusCode;

    public DefaultRequestTaggingStatus(Consumer<DefaultRequestTaggingStatus> statusConsumer, Function<String, String> hashAlgorithm) {
        this.metaDataLock = new ReentrantReadWriteLock();
        this.statusConsumer = Objects.requireNonNull(statusConsumer);
        this.hashAlgorithm = Objects.requireNonNull(hashAlgorithm);
        this.ignored = false;
        this.resourceName = DEFAULT_RESOURCE_NAME;
        this.statusCode = StatusCode.SUCCESS;
    }

    public DefaultRequestTaggingStatus(DefaultRequestTaggingStatus status) {
        this.metaDataLock = new ReentrantReadWriteLock();
        this.statusConsumer = status.getStatusConsumer();
        this.hashAlgorithm = status.getHashAlgorithm();
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
    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
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
    public void setStatusCode(String statusCode) {
        this.statusCode = StatusCode.valueOf(statusCode);
    }

    @Override
    public RequestTagging.Status withResourceName(String name) {
        resourceName = Objects.requireNonNull(name);
        return this;
    }

    public String getResourceName() {
        return resourceName;
    }
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public RequestTagging.Status withMetaData(String key, String value) {
        metaDataLock.writeLock().lock();
        try {
            if (metaData == null) {
                metaData = new TreeMap<>();
            }
            metaData.put(key, value);
        } finally {
            metaDataLock.writeLock().unlock();
        }
        return this;
    }

    @Override
    public Status withHashedMetaData(String key, String value) {
        return withMetaData(key, hashAlgorithm.apply(value));
    }

    public Function<String, String> getHashAlgorithm() {
        return hashAlgorithm;
    }

    public SortedMap<String, String> drainMetaData() {
        metaDataLock.writeLock().lock();
        try {
            if (metaData == null) {
                return Collections.emptySortedMap();
            } else {
                SortedMap<String, String> result = metaData;
                metaData = null;
                return result;
            }
        } finally {
            metaDataLock.writeLock().unlock();
        }
    }

    public SortedMap<String, String> getMetaData() {
        metaDataLock.readLock().lock();
        try {
            if (metaData == null) {
                return null;
            } else {
                return new TreeMap<>(metaData);
            }
        } finally {
            metaDataLock.readLock().unlock();
        }
    }

    @Override
    public Runnable handover(Runnable runnable) {
        RequestTagging.Status status = RequestTagging.get();
        if (status instanceof DefaultRequestTaggingStatus) {
            return new RequestTaggingRunnable(runnable, new DefaultRequestTaggingStatus((DefaultRequestTaggingStatus) status));
        } else {
            return runnable;
        }
    }

    public void consume() {
        statusConsumer.accept(this);
    }

    public Consumer<DefaultRequestTaggingStatus> getStatusConsumer() {
        return statusConsumer;
    }
}