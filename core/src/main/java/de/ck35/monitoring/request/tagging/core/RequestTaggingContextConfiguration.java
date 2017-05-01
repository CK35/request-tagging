package de.ck35.monitoring.request.tagging.core;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import de.ck35.monitoring.request.tagging.core.reporter.StatusReporterFactory;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporterFactory.ReportFormat;

public class RequestTaggingContextConfiguration {

    private final Function<String, String> properties;

    public RequestTaggingContextConfiguration(Function<String, String> properties) {
        this.properties = properties;
    }
    
    public void configure(RequestTaggingContext context) {
        configureStringValue("requestTagging.context.collectorSendDelayDuration", context::setCollectorSendDelayDuration);
        configureBooleanValue("requestTagging.context.requestIdEnabled", context::setRequestIdEnabled);
        configureBooleanValue("requestTagging.context.forceRequestIdOverwrite", context::setForceRequestIdOverwrite);
        configureStringValue("requestTagging.context.requestIdParameterName", context::setRequestIdParameterName);
        
        DefaultRequestTaggingStatus status = context.getDefaultRequestTaggingStatus();
        configureBooleanValue("requestTagging.defaultStatus.ignored", status::setIgnored);
        configureStringValue("requestTagging.defaultStatus.resourceName", status::setResourceName);
        configureStringValue("requestTagging.defaultStatus.statusCode", status::setStatusCode);
        
        DefaultRequestTaggingStatusConsumer statusConsumer = context.getStatusConsumer();
        configureIntValue("requestTagging.statusConsumer.maxDurationsPerNode", statusConsumer::setMaxDurationsPerNode);
    }
    
    public void configure(StatusReporterFactory statusReporterFactory) {
        configureStringValue("requestTagging.statusReporter.hostId", statusReporterFactory::setHostId);
        configureStringValue("requestTagging.statusReporter.instanceId", statusReporterFactory::setInstanceId);
        
        configureBooleanValue("requestTagging.statusReporter.sendData", statusReporterFactory::setSendData);
        configureReportFormatValue("requestTagging.statusReporter.reportFormat", statusReporterFactory::setReportFormat);
        
        configureStringValue("requestTagging.statusReporter.protocol", statusReporterFactory::setProtocol);
        configureStringValue("requestTagging.statusReporter.hostName", statusReporterFactory::setHostName);
        configureIntValue("requestTagging.statusReporter.port", statusReporterFactory::setPort);
        configureStringValue("requestTagging.statusReporter.pathPart", statusReporterFactory::setPathPart);
        configureStringValue("requestTagging.statusReporter.queryPart", statusReporterFactory::setQueryPart);
        
        configureIntValue("requestTagging.statusReporter.connectionTimeout", statusReporterFactory::setConnectionTimeout);
        configureIntValue("requestTagging.statusReporter.readTimeout", statusReporterFactory::setReadTimeout);
    }
    
    public void configure(HashAlgorithm hashAlgorithm) {
        configureStringValue("requestTagging.hashAlgorithm.algorithmName", hashAlgorithm::setAlgorithmName);
    }
    
    private void configureStringValue(String key, Consumer<String> valueConsumer) {
        getStringProperty(key).ifPresent(valueConsumer);
    }
    private void configureBooleanValue(String key, Consumer<Boolean> valueConsumer) {
        getStringProperty(key).map(Boolean::parseBoolean).ifPresent(valueConsumer);
    }
    private void configureIntValue(String key, Consumer<Integer> valueConsumer) {
        getStringProperty(key).map(Integer::parseInt).ifPresent(valueConsumer);
    }
    private void configureReportFormatValue(String key, Consumer<ReportFormat> valueConsumer) {
        getStringProperty(key).map(ReportFormat::valueOf).ifPresent(valueConsumer);
    }
    private Optional<String> getStringProperty(String key) {
        return Optional.ofNullable(properties.apply(key));
    }
}