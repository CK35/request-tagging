# request-tagging

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
    <version>0.5.0</version>
    <scope>provided</scope>
</dependency>
```

#####Prepare Apache Tomcat and write to InfluxDB
There is an ready to use Tomcat integration which enables the Request Tagging mechanism for all web applications running inside the Servlet Container. Simply add the following Tomcat Valve to your server.xml file.
```xml
<Valve className="de.ck35.monitoring.request.integration.tomcat.RequestTaggingValve" 
       influxDBDatabaseName="your-db-name" />
```
With this minimal configuration all request tagging data will be send to InfluxDB on the same host. You can change many of the parameters e.g. 'influxDBHostName' and 'influxDBPort'.
