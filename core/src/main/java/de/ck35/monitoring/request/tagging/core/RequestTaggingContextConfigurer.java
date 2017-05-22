package de.ck35.monitoring.request.tagging.core;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus.StatusCode;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporterFactory;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporterFactory.ReportFormat;

/**
 * Contains all configuration keys for request-tagging components. It also
 * contains helper methods which can be used to load configurations from various
 * sources.
 * 
 * @author Christian Kaspari
 * @since 2.0.0
 */
public class RequestTaggingContextConfigurer {

    public enum ConfigKey {

            collectorSendDelayDuration("requestTagging.context.collectorSendDelayDuration"),
            requestIdEnabled("requestTagging.context.requestIdEnabled"),
            forceRequestIdOverwrite("requestTagging.context.forceRequestIdOverwrite"),
            requestIdParameterName("requestTagging.context.requestIdParameterName"),

            ignored("requestTagging.defaultStatus.ignored"),
            resourceName("requestTagging.defaultStatus.resourceName"),
            statusCode("requestTagging.defaultStatus.statusCode"),

            maxDurationsPerNode("requestTagging.statusConsumer.maxDurationsPerNode"),

            hostId("requestTagging.statusReporter.hostId"),
            instanceId("requestTagging.statusReporter.instanceId"),

            sendData("requestTagging.statusReporter.sendData"),
            reportFormat("requestTagging.statusReporter.reportFormat"),

            protocol("requestTagging.statusReporter.protocol"),
            hostName("requestTagging.statusReporter.hostName"),
            port("requestTagging.statusReporter.port"),
            pathPart("requestTagging.statusReporter.pathPart"),
            queryPart("requestTagging.statusReporter.queryPart"),
            connectionTimeout("requestTagging.statusReporter.connectionTimeout"),
            readTimeout("requestTagging.statusReporter.readTimeout"),

            elasticsearchDocumentType("requestTagging.statusReporter.elasticsearchDocumentType"),
            elasticsearchIndexPrefixTemplate("requestTagging.statusReporter.elasticsearchIndexPrefixTemplate"),

            algorithmName("requestTagging.hashAlgorithm.algorithmName");

        private final String name;

        private ConfigKey(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Function<String, String> properties;
    private final Consumer<String> infoLogger;

    public RequestTaggingContextConfigurer(Function<String, String> properties, Consumer<String> infoLogger) {
        this.properties = properties;
        this.infoLogger = infoLogger;
    }

    public void configure(RequestTaggingContext context) {
        configureDurationValue(ConfigKey.collectorSendDelayDuration, context::setCollectorSendDelayDuration);
        configureBooleanValue(ConfigKey.requestIdEnabled, context::setRequestIdEnabled);
        configureBooleanValue(ConfigKey.forceRequestIdOverwrite, context::setForceRequestIdOverwrite);
        configureStringValue(ConfigKey.requestIdParameterName, context::setRequestIdParameterName);

        DefaultRequestTaggingStatus status = context.getDefaultRequestTaggingStatus();
        configureBooleanValue(ConfigKey.ignored, status::setIgnored);
        configureStringValue(ConfigKey.resourceName, status::setResourceName);
        configureStatusCodeValue(ConfigKey.statusCode, status::setStatusCode);

        DefaultRequestTaggingStatusConsumer statusConsumer = context.getStatusConsumer();
        configureIntValue(ConfigKey.maxDurationsPerNode, statusConsumer::setMaxDurationsPerNode);
    }

    public void configure(StatusReporterFactory statusReporterFactory) {
        configureStringValue(ConfigKey.hostId, statusReporterFactory::setHostId);
        configureStringValue(ConfigKey.instanceId, statusReporterFactory::setInstanceId);

        configureBooleanValue(ConfigKey.sendData, statusReporterFactory::setSendData);
        configureReportFormatValue(ConfigKey.reportFormat, statusReporterFactory::setReportFormat);

        configureStringValue(ConfigKey.protocol, statusReporterFactory::setProtocol);
        configureStringValue(ConfigKey.hostName, statusReporterFactory::setHostName);
        configureIntValue(ConfigKey.port, statusReporterFactory::setPort);
        configureStringValue(ConfigKey.pathPart, statusReporterFactory::setPathPart);
        configureStringValue(ConfigKey.queryPart, statusReporterFactory::setQueryPart);

        configureIntValue(ConfigKey.connectionTimeout, statusReporterFactory::setConnectionTimeout);
        configureIntValue(ConfigKey.readTimeout, statusReporterFactory::setReadTimeout);

        configureStringValue(ConfigKey.elasticsearchDocumentType, statusReporterFactory::setElasticsearchDocumentType);
        configureStringValue(ConfigKey.elasticsearchIndexPrefixTemplate, statusReporterFactory::setElasticsearchIndexPrefixTemplate);
    }

    public void configure(HashAlgorithm hashAlgorithm) {
        configureStringValue(ConfigKey.algorithmName, hashAlgorithm::setAlgorithmName);
    }

    private void configureStringValue(ConfigKey key, Consumer<String> valueConsumer) {
        configure(key, x -> x, valueConsumer);
    }

    private void configureDurationValue(ConfigKey key, Consumer<Duration> valueConsumer) {
        configure(key, Duration::parse, valueConsumer);
    }

    private void configureBooleanValue(ConfigKey key, Consumer<Boolean> valueConsumer) {
        configure(key, Boolean::parseBoolean, valueConsumer);
    }

    private void configureIntValue(ConfigKey key, Consumer<Integer> valueConsumer) {
        configure(key, Integer::parseInt, valueConsumer);
    }

    private void configureStatusCodeValue(ConfigKey key, Consumer<StatusCode> valueConsumer) {
        configure(key, StatusCode::valueOf, valueConsumer);
    }

    private void configureReportFormatValue(ConfigKey key, Consumer<ReportFormat> valueConsumer) {
        configure(key, ReportFormat::valueOf, valueConsumer);
    }

    private <T> void configure(ConfigKey key, Function<String, T> transformer, Consumer<T> valueConsumer) {
        String value = Optional.ofNullable(properties.apply(key.getName()))
                               .map(String::trim)
                               .orElse(null);
        if (value == null) {
            return;
        }
        try {
            infoLogger.accept("Overwriting default value of: '" + key + "' with value: '" + value + "'!");
            valueConsumer.accept(transformer.apply(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Value: '" + value + "' of property: '" + key + "' is invalid!", e);
        }
    }

    public static void load(Function<String, String> source, BiConsumer<String, String> target) {
        Arrays.asList(ConfigKey.values())
              .stream()
              .forEach(key -> {
                  Optional.ofNullable(source.apply(key.getName()))
                          .map(String::trim)
                          .ifPresent(value -> target.accept(key.getName(), value));
                  Optional.ofNullable(source.apply(key.name()))
                          .map(String::trim)
                          .ifPresent(value -> target.accept(key.getName(), value));
              });
    }
}