package de.ck35.monitoring.request.tagging.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus.StatusCode;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter.Measurement;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter.Resource;

/**
 * Default implementation of the request tagging status consumer which counts
 * status codes inside a meta data tree. The tree is build from the resource
 * name and the optional status meta data.
 *
 * @author Christian Kaspari
 * @since 1.0.0
 */
public class DefaultRequestTaggingStatusConsumer implements Consumer<DefaultRequestTaggingStatus> {

    private ReadWriteLock swapLock;
    private ConcurrentMap<String, ResourceNode> tree;

    private volatile int maxDurationsPerNode;

    public DefaultRequestTaggingStatusConsumer() {
        this.maxDurationsPerNode = maxDurationsPerNode;
        this.swapLock = new ReentrantReadWriteLock();
        this.tree = new ConcurrentHashMap<>();
    }

    @Override
    public void accept(DefaultRequestTaggingStatus status) {
        if (status.isIgnored()) {
            return;
        }
        this.swapLock.readLock()
                     .lock();
        try {
            TreeNode currentNode = tree.computeIfAbsent(status.getResourceName(), ResourceNode::new);
            Iterator<MetaDataPair> iter = status.getMetaData()
                                                .entrySet()
                                                .stream()
                                                .map(MetaDataPair::of)
                                                .iterator();
            while (iter.hasNext()) {
                currentNode = currentNode.children.computeIfAbsent(iter.next(), x -> new TreeNode());
            }
            MutableMeasurement measurement = currentNode.measurements.computeIfAbsent(status.getStatusCode(), x -> new MutableMeasurement(x.toString()));
            measurement.numberOfinvocations.increment();
            if (getMaxDurationsPerNode() <= 0) {
                return;
            }
            status.visitDurations((key, duration) -> {
                measurement.durations.computeIfAbsent(key, x -> new FixedSizeList<>(this::getMaxDurationsPerNode))
                                     .offer(duration);
            });
        } finally {
            this.swapLock.readLock()
                         .unlock();
        }
    }
    
    public Map<String, ResourceNode> swapTree() {
        this.swapLock.writeLock()
                     .lock();
        try {
            Map<String, ResourceNode> result = tree;
            tree = new ConcurrentHashMap<>(result.isEmpty() ? 16 : result.size());
            return result;
        } finally {
            this.swapLock.writeLock()
                         .unlock();
        }
    }

    public void report(RequestTaggingStatusReporter reporter) {
        swapTree().values()
                  .forEach(node -> node.report(reporter));
    }

    public int getMaxDurationsPerNode() {
        return maxDurationsPerNode;
    }

    public void setMaxDurationsPerNode(int maxDurationsPerNode) {
        this.maxDurationsPerNode = maxDurationsPerNode;
    }

    private static class ResourceNode extends TreeNode {

        private final String name;

        public ResourceNode(String name) {
            this.name = name;
        }

        public void report(RequestTaggingStatusReporter reporter) {
            report(name, Collections.emptySortedMap(), reporter);
        }
    }

    private static class TreeNode {

        private final ConcurrentMap<MetaDataPair, TreeNode> children;
        private final ConcurrentMap<StatusCode, MutableMeasurement> measurements;

        private TreeNode() {
            this.measurements = new ConcurrentHashMap<>();
            this.children = new ConcurrentHashMap<>();
        }

        public void report(String resourceName, SortedMap<String, String> currentMetaData, RequestTaggingStatusReporter reporter) {
            List<Measurement> measurements = this.measurements.values()
                                                              .stream()
                                                              .map(MutableMeasurement::toOptionalMeasurement)
                                                              .filter(Optional::isPresent)
                                                              .map(Optional::get)
                                                              .collect(Collectors.toList());
            if (!measurements.isEmpty()) {
                reporter.accept(new Resource(resourceName, currentMetaData, measurements));
            }
            children.forEach((metaData, child) -> {
                SortedMap<String, String> childMetaData = new TreeMap<>(currentMetaData);
                childMetaData.put(metaData.getKey(), metaData.getValue());
                child.report(resourceName, childMetaData, reporter);
            });
        }
    }

    private static class MutableMeasurement {

        private final String statusCodeName;
        private final LongAdder numberOfinvocations;
        private final ConcurrentMap<String, FixedSizeList<Duration>> durations;

        private MutableMeasurement(String statusCodeName) {
            this.statusCodeName = statusCodeName;
            this.numberOfinvocations = new LongAdder();
            this.durations = new ConcurrentHashMap<>(1);
        }

        public Optional<Measurement> toOptionalMeasurement() {
            long totalNumberOfinvocations = numberOfinvocations.sumThenReset();
            if (totalNumberOfinvocations <= 0) {
                return Optional.empty();
            }
            Map<String, List<Duration>> durations = new HashMap<>();
            this.durations.forEach((key, value) -> {
                List<Duration> drainResult = value.drain();
                if (!drainResult.isEmpty()) {
                    durations.put(key, drainResult);
                }
            });
            return Optional.of(new Measurement(statusCodeName, totalNumberOfinvocations, durations));
        }
    }

    public static class FixedSizeList<E> {

        private final Lock lock;
        private final Supplier<Integer> maxSize;
        private List<E> elements;

        public FixedSizeList(Supplier<Integer> maxSize) {
            this.lock = new ReentrantLock();
            this.maxSize = maxSize;
        }

        public boolean offer(E element) {
            this.lock.lock();
            try {
                if (elements == null) {
                    elements = new ArrayList<>();
                }
                if (elements.size() < maxSize.get()) {
                    elements.add(element);
                    return true;
                } else {
                    return false;
                }
            } finally {
                this.lock.unlock();
            }
        }

        public List<E> drain() {
            this.lock.lock();
            try {
                List<E> result = elements == null ? Collections.emptyList() : elements;
                elements = null;
                return result;
            } finally {
                this.lock.unlock();
            }
        }
    }

    public static class MetaDataPair {

        private final String key;
        private final String value;
        private final int hashCode;

        public MetaDataPair(String key, String value) {
            this.key = Objects.requireNonNull(key);
            this.value = Objects.requireNonNull(value);
            this.hashCode = Objects.hash(key, value);
        }

        public static MetaDataPair of(Entry<String, String> entry) {
            return new MetaDataPair(entry.getKey(), entry.getValue());
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
            if (!(obj instanceof MetaDataPair)) {
                return false;
            }
            MetaDataPair other = (MetaDataPair) obj;
            return Objects.equals(key, other.key) && Objects.equals(value, other.value);
        }

        @Override
        public String toString() {
            return "MetaData [key=" + key + ", value=" + value + ", hashCode=" + hashCode + "]";
        }
    }
}