package de.ck35.monitoring.request.tagging.integration.filter;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.RegistrationBean;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

@RunWith(SpringJUnit4ClassRunner.class)
@WebIntegrationTest(randomPort=true)
@SpringApplicationConfiguration(classes=RequestTaggingFilterTest.TestConfiguration.class)
public class RequestTaggingFilterTest {
    
    @Autowired Environment env;
    
    @Test
    public void testRequestTaggingFilter() throws Exception {
        URL url = new URL("http", "localhost", env.getRequiredProperty("local.server.port", Integer.TYPE), "/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.connect();
            assertEquals(200, connection.getResponseCode());
        } finally {
            connection.disconnect();
        }
    }

    
    @Configuration
    @EnableAutoConfiguration
    public static class TestConfiguration {
        
        @Bean
        public TestRequestHandler testRequestHandler() {
            return new TestRequestHandler();
        }
        
        @Bean
        public RegistrationBean registrationBean() {
            return new RegistrationBean() {
                @Override
                public void onStartup(ServletContext servletContext) throws ServletException {
                    FilterRegistration.Dynamic filter = servletContext.addFilter("request-tagging", RequestTaggingFilter.class);
                    filter.addMappingForServletNames(EnumSet.allOf(DispatcherType.class), true, "testRequestHandler");
                    
                    ServletRegistration.Dynamic servlet = servletContext.addServlet("testRequestHandler", HttpRequestHandlerServlet.class);
                    servlet.addMapping("/");
                }
            };
        }
        
    }
}