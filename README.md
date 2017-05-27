# request-tagging

*Monitoring from the perspective of a (web) request.*

[![Build Status](https://travis-ci.org/CK35/request-tagging.svg?branch=master)](https://travis-ci.org/CK35/request-tagging)
[![Coverage Status](https://coveralls.io/repos/github/CK35/request-tagging/badge.svg?branch=master)](https://coveralls.io/github/CK35/request-tagging?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.ck35.monitoring/request-tagging-core/badge.svg?style=flat)](http://search.maven.org/#search|ga|1|g%3Ade.ck35.monitoring)

With request-tagging you can tag information during a running request cycle so that at the end of the request all relevant information
is available at one place. For example you could tag user related information at the start of the request and later on you could
tag application related information e.g. a cache-miss or of course any errors that occurred.

With request-tagging you can collect request related data as easy as calling a simple logger.

```java
RequestTagging.get().withResourceName("homepage");
```

The collected request data is pushed asynchronously to your favorite monitoring system via HTTP. Currently supported endpoints 
are InfluxDB and Elasticsearch. The data is multi-dimensional and no further computation is done inside request-tagging 
so you can collect the request data from multiple hosts and do computations inside your monitoring system. E.g. calculate percentiles
over all collected data sets.

```json
{
        "timestamp": "2017-12-03T10:15:30Z",
        "key": "request_data",
        "resource_name": "homepage",
        "host": "my-test-host01",
        "instanceId": "A",
        "cacheHit": "true",
        "statusCodeName": "SUCCESS",
        "totalNumberOfInvocations": 5,
        "total_request_duration": [
            10,
            11,
            12
        ]
}
``` 
### Feature overview
- Tag a resource or usecase name.
- Tag any meta-data anywhere inside your application.
- Tag sensitive data as hashed value.
- Tag total request duration.
- Tag any custom duration.
- Automatically tag a Request-ID for later request correlation.
- Easy testing with a Junit rule and test method annotations.
- Report request-tagging data to an InfluxDB or Elasticsearch.

### Getting started
Currently you can decide between two prepared integration options. Basically request-tagging can be integrated into any application. The core module is always required and can be included inside your project e.g. with Maven. The core module itself does not contain any further dependencies. It`s only dependency is Java 8.

```xml
<dependency>
    <groupId>de.ck35.monitoring</groupId>
    <artifactId>request-tagging-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

#### Integrate with the Servlet Filter
There are many ways today to add Servlet Filters to your web application which mainly depends on the used Servlet API version.
Integration of request-tagging works with both major versions two and three. With Maven you simply add the following dependencies 
to your project.

```xml
<dependency>
    <groupId>de.ck35.monitoring</groupId>
    <artifactId>request-tagging-core</artifactId>
    <version>2.0.0</version>
</dependency>
<dependency>
    <groupId>de.ck35.monitoring</groupId>
    <artifactId>request-tagging-integration-filter</artifactId>
    <version>2.0.0</version>
</dependency>
```

The filter class is named: `de.ck35.monitoring.request.tagging.integration.filter.RequestTaggingFilter`. You can checkout the request-tagging-demo to see one possible integration within a Spring Boot application.

#### Integrate with the Apache Tomcat Valve
There are some advantages when request-tagging is directly integrated into Apache Tomcat. The web application does not need to care about the
configuration which mainly depends on infrastructure related things. With the Valve based integration you can get rid of these configurations
inside your application.

To add the Valve you need to customize the server.xml file of your Apache Tomcat installation.
```xml
<Server ...>
  <Service ...>
     <Engine ...>
        <Host ...>
           <Valve className="de.ck35.monitoring.request.tagging.integration.tomcat.RequestTaggingValve" sendData="true" reportFormat="INFLUX_DB" hostName="..." />
        </Host>
     </Engine>
  </Service>
</Server>
```
Next you need to download the required "jar-with-dependencies.jar" from [Maven Central](http://search.maven.org/#search|ga|1|a%3A%22request-tagging-integration-tomcat%22) and add it to the tomcat/lib directory. Inside your web application you declare the core module as provided dependency.

### Configuration
All possible configuration options are collected inside one enum: [RequestTaggingContextConfigurer.ConfigKey](https://github.com/CK35/request-tagging/blob/master/core/src/main/java/de/ck35/monitoring/request/tagging/core/RequestTaggingContextConfigurer.java#L16) there you will also find helper methods for dealing with several configuration environments e.g. the filter init parameters and the Spring environment. If you use the `FilterRegistration.Dynamic`you can call the following helper method to transfer the configuration of the Spring environment to the init parameters.

```java
RequestTaggingContextConfigurer.load(env::getProperty, filter::setInitParameter);
``` 
All configuration options have a sensible default value, so you only need to define the properties which need a different value. Generally you can define configuration properties with a short key name e.g. `hostName` or with the fully qualified name: `requestTagging.statusReporter.hostName`. The property value with the short key name will overwrite the value of the property with the fully qualified name. Inside the Tomcat server.xml file you can only use the short key names.

| Short key                        | FQN key                                                        | Default         |
|----------------------------------|----------------------------------------------------------------|-----------------|
| collectorSendDelayDuration       | requestTagging.context.collectorSendDelayDuration              | PT1m            |
| requestIdEnabled                 | requestTagging.context.requestIdEnabled                        | false           |
| forceRequestIdOverwrite          | requestTagging.context.forceRequestIdOverwrite                 | false           |
| requestIdParameterName           | requestTagging.context.requestIdParameterName                  | X-Request-ID    |
| ignored                          | requestTagging.defaultStatus.ignored                           | false           |
| resourceName                     | requestTagging.defaultStatus.resourceName                      | default         |
| statusCode                       | requestTagging.defaultStatus.statusCode                        | SUCCESS         |
| maxDurationsPerNode              | requestTagging.statusConsumer.maxDurationsPerNode              | 0               |
| hostId                           | requestTagging.statusReporter.hostId                           | <hostname>      |
| instanceId                       | requestTagging.statusReporter.instanceId                       |                 |
| sendData                         | requestTagging.statusReporter.sendData                         | false           |
| reportFormat                     | requestTagging.statusReporter.reportFormat                     | JSON            |
| protocol                         | requestTagging.statusReporter.protocol                         | http            |
| hostName                         | requestTagging.statusReporter.hostName                         | localhost       |
| port                             | requestTagging.statusReporter.port                             | 8086            |
| pathPart                         | requestTagging.statusReporter.pathPart                         | /write          |
| queryPart                        | requestTagging.statusReporter.queryPart                        | db=request_data |
| connectionTimeout                | requestTagging.statusReporter.connectionTimeout                | 5000            |
| readTimeout                      | requestTagging.statusReporter.readTimeout                      | 5000            |
| elasticsearchDocumentType        | requestTagging.statusReporter.elasticsearchDocumentType        | request_data    |
| elasticsearchIndexPrefixTemplate | requestTagging.statusReporter.elasticsearchIndexPrefixTemplate | YYYYMMdd        |
| algorithmName                    | requestTagging.hashAlgorithm.algorithmName                     | MD5             |



### Testing
With the testing module you can test your application if request-tagging is invoked as expected. The testing module requires JUnit.

```xml
<dependency>
    <groupId>de.ck35.monitoring</groupId>
    <artifactId>request-tagging-testing</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>
```

First you add the `ExpectedStatusRule`to your test class. This will activate request-tagging for each test method run. To verify that request-tagging has been called as expected you can add a `@ExpectedStatus`annotation to your test method. At the end of the test the test rule will check the request-tagging status.

```java
    @Rule public ExpectedStatusRule rule = new ExpectedStatusRule();
    
    @Test
    @ExpectedStatus(resourceName = "my-default-usecase", statusCode = StatusCode.SUCCESS, metaData = { "my-extra-data", "any-value" })
    public void testInvoke() {
        defaultUsecase().invoke();
    }
```

### Report data to an InfluxDB instance
The collected request-tagging status data can be reported to an InfluxDB instance. You need to set at least the following properties:

```properties
# Enable sending of data to a remote destination e.g. InfluxDB or Elasticsearch
requestTagging.statusReporter.sendData=true

# Enable the InfluxDB line format
requestTagging.statusReporter.reportFormat=INFLUX_DB

# Set the remote destination host name
requestTagging.statusReporter.hostName=my-influx-host
```

On the InfluxDB a database with the name `request_data` must be created before reporting data.

### Report data to an Elasticsearch instance
An alternative remote destination for request-tagging data is Elasticsearch. The data is reported to the HTTP Bulk endpoint of Elasticsearch. You need at least the following properties set for reporting to Elasticsearch:

```properties
# Enable sending of data to a remote destination e.g. InfluxDB or Elasticsearch
requestTagging.statusReporter.sendData=true

# Enable the Elasticsearch bulk JSON format and configure the endpoint
requestTagging.statusReporter.reportFormat=ELASTICSEARCH
requestTagging.statusReporter.port=9200
requestTagging.statusReporter.pathPart=/_bulk
requestTagging.statusReporter.queryPart=

# Set the remote destination host name
requestTagging.statusReporter.hostName=my-elasticsearch-host
```