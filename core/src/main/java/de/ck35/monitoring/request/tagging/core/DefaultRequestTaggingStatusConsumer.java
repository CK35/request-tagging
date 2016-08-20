package de.ck35.monitoring.request.tagging.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus.StatusCode;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter;

/**
 * Default implementation of the request tagging status consumer which counts status codes
 * inside a meta data tree. The tree is build from the resource name and the optional
 * status meta data.
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class DefaultRequestTaggingStatusConsumer implements Consumer<DefaultRequestTaggingStatus>{

    private ReadWriteLock resetLock;
    private ConcurrentMap<String, TaggingStatusEntry> data;

    public DefaultRequestTaggingStatusConsumer() {
        this.resetLock = new ReentrantReadWriteLock();
        this.data = new ConcurrentHashMap<>();
    }

    @Override
    public void accept(DefaultRequestTaggingStatus status) {
        if (status.isIgnored()) {
            return;
        }
        this.resetLock.readLock().lock();
        try {
            data.computeIfAbsent(status.getResourceName(), key -> new TaggingStatusEntry())
                .increment(status.drainMetaData()
                                 .entrySet()
                                 .iterator(),
                           status.getStatusCode());
        } finally {
            this.resetLock.readLock().unlock();
        }
    }

    public void report(RequestTaggingStatusReporter reporter) {
        this.resetLock.readLock().lock();
        try {
            data.forEach((key, entry) -> {
                entry.report(key, Collections.emptyMap(), reporter);
            });
        } finally {
            this.resetLock.readLock().unlock();
        }
    }

    public void reportAndReset(RequestTaggingStatusReporter reporter) {
        this.resetLock.writeLock().lock();
        try {
            data.forEach((key, entry) -> {
                entry.report(key, Collections.emptyMap(), reporter);
            });
            data.clear();
        } finally {
            this.resetLock.writeLock().unlock();
        }
    }

    public static class TaggingStatusEntry {

        private final ConcurrentMap<StatusCode, LongAdder> statusCodeCounters;
        private final ConcurrentMap<MetaData, TaggingStatusEntry> children;

        public TaggingStatusEntry() {
            this.statusCodeCounters = new ConcurrentHashMap<>();
            this.children = new ConcurrentHashMap<>();
        }

        public void increment(Iterator<Entry<String, String>> entries, StatusCode statusCode) {
            if (entries.hasNext()) {
                children.computeIfAbsent(MetaData.of(entries.next()), key -> new TaggingStatusEntry())
                        .increment(entries, statusCode);
            } else {
                statusCodeCounters.computeIfAbsent(statusCode, key -> new LongAdder())
                                  .increment();
            }
        }

        public void report(String resourceName, Map<String, String> currentMetaData, RequestTaggingStatusReporter reporter) {
            Map<String, Long> statusCodes = new HashMap<>();
            statusCodeCounters.forEach((statusCode, longAdder) -> {
                long sum = longAdder.sumThenReset();
                if (sum > 0) {
                    statusCodes.put(statusCode.toString(), sum);
                }
            });
            if (!statusCodeCounters.isEmpty()) {
                reporter.accept(resourceName, statusCodes, currentMetaData);
            }
            children.forEach((metaData, child) -> {
                Map<String, String> childMetaData = new HashMap<>(currentMetaData);
                childMetaData.put(metaData.getKey(), metaData.getValue());
                child.report(resourceName, childMetaData, reporter);
            });
        }
    }

    public static class MetaData {

        private final String key;
        private final String value;
        private final int hashCode;

        public MetaData(String key, String value) {
            this.key = key;
            this.value = value;
            this.hashCode = Objects.hash(key, value);
        }

        public static MetaData of(Entry<String, String> entry) {
            return new MetaData(entry.getKey(), entry.getValue());
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof MetaData)) {
                return false;
            }
            MetaData other = (MetaData) obj;
            return Objects.equals(key, other.key) && Objects.equals(value, other.value);
        }

        @Override
        public String toString() {
            return "MetaData [key=" + key + ", value=" + value + ", hashCode=" + hashCode + "]";
        }
    }
}