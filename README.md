# request-tagging

*Monitoring from the perspective of a (web) request.*

[![Build Status](https://travis-ci.org/CK35/request-tagging.svg?branch=master)](https://travis-ci.org/CK35/request-tagging)
[![Coverage Status](https://coveralls.io/repos/github/CK35/request-tagging/badge.svg?branch=master)](https://coveralls.io/github/CK35/request-tagging?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.ck35.monitoring/request-tagging-core/badge.svg?style=flat)](http://search.maven.org/#search|ga|1|g%3Ade.ck35.monitoring)

This project delivers a lightweight mechanism for request tagging with zero dependencies. Tag your requests anywhere in your Java based web application where you have all required information. All you have to do is retrieving the currently active Request Tagging Status and attach your local information. A simple reporting mechanism is also included which allows you to send the tagged request data to an InfluxDB.

For example inside an Apache Wicket web application the source looks like this:

```java

import de.ck35.monitoring.request.tagging.RequestTagging;

public class HomePage extends BasePage {
    
    public HomePage() {
        //Set a special resource name.
        RequestTagging.get().withResourceName("homepage");
        
        
        // Add a login link 
        add(new AjaxLink<String>("doLogin") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                //Set a resource name and add optional meta data.
                RequestTagging.get().withResourceName("homepage").withMetaData("action", "login");
            }
        });
    }
}
```
This example demonstrates the usage of the Request Tagging mechanism. As you can see it is not bound to any framework and can be accessed inside any web application e.g. Spring MVC.

#####Maven dependency
```xml
<dependency>
    <groupId>de.ck35.monitoring</groupId>
    <artifactId>request-tagging-core</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

#####Use Apache Tomcat integration
There is an ready to use Tomcat integration which enables the Request Tagging mechanism for all web applications running inside the Servlet Container. Simply add the following Tomcat Valve to your server.xml file.
```xml
<Valve className="de.ck35.monitoring.request.tagging.integration.tomcat.RequestTaggingValve" 
       influxDBDatabaseName="your-db-name" reportToInfluxDB="true" />
```
Download the required "jar-with-dependencies.jar" from [Maven Central](http://search.maven.org/#search|ga|1|a%3A%22request-tagging-integration-tomcat%22) and add it to the tomcat/lib directory.
This is should be the preferred way of integration because it keeps the web application free from infrastructure configuration aspects.  

#####Use Servlet filter integration
If modifying Tomcat installation is not possible you can integrate request-tagging with the help of a Servlet filter. First add the following Maven depedencies to your project:
```xml
<dependency>
    <groupId>de.ck35.monitoring</groupId>
    <artifactId>request-tagging-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>de.ck35.monitoring</groupId>
    <artifactId>request-tagging-integration-filter</artifactId>
    <version>1.0.0</version>
</dependency>
```
Now you can add the `de.ck35.monitoring.request.tagging.integration.filter.RequestTaggingFilter` to your `web.xml` or `WebApplicationInitializer`.

#####Sending requets-tagging data to InfluxDB
By default a simple reporting mechanism is already included which sends out the request-tagging data to an InfluxDB every minute.
All properties can be changed via server.xml or filter init parameters:

| Property                  | Description                                       |
|---------------------------|----------------------------------------------------------------------------|
| localHostName| The name of the host which sends the request-tagging data. Default: Reverse name lookup |
| localInstanceId| An id which separates different software on the same host. Default: `null`|
| reportToInfluxDB| Enable the reporting to InfluxDB. Default: `false`|
| influxDBProtocol| The protocol which should be used for sending. Default: `http`|
| influxDBHostName| The host name of the InfluxDB. Default: `localhost`|
| influxDBPort| The port of the InfluxDB. Default: `8086`|
| influxDBDatabaseName| The name of the InfluxDB database. Default: `request-tagging`|
| connectionTimeout| The socket connection timout for sending request-tagging data. Default: `5000` ms|
| readTimeout| The socket read tiemout for sending request-tagging data. Default: `5000` ms|
