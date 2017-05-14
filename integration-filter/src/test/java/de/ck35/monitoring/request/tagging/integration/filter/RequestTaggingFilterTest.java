package de.ck35.monitoring.request.tagging.integration.filter;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Condition.method;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.uri;
import static com.xebialabs.restito.semantics.Condition.withPostBodyContainingJsonPath;
import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.RegistrationBean;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.server.StubServer;

import de.ck35.monitoring.request.tagging.core.RequestTaggingContextConfigurer;

@RunWith(SpringJUnit4ClassRunner.class)
@WebIntegrationTest(randomPort=true)
@SpringApplicationConfiguration(classes=RequestTaggingFilterTest.TestConfiguration.class)
public class RequestTaggingFilterTest {
    
    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
    
    @Autowired Environment env;
    
    private static StubServer stubServer;
    private static CountDownLatch latch;

    @BeforeClass
    public static void before() {
        latch = new CountDownLatch(1);
        stubServer = new StubServer(6666);
        stubServer.start();

        whenHttp(stubServer).match(post("/test"))
                            .then(Action.custom(in -> {
                                in.setStatus(HttpStatus.OK_200);
                                latch.countDown();
                                return in;
                            }));
    }

    @AfterClass
    public static void after() {
        stubServer.stop();
    }
    
    @Test
    public void testRequestTaggingFilter() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("http", "localhost", env.getRequiredProperty("local.server.port", Integer.TYPE), "/").openConnection();
        connection.addRequestProperty("X-Request-ID", "4711");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.connect();

        assertEquals(HttpStatus.OK_200.getStatusCode(), connection.getResponseCode());
        connection.disconnect();

        latch.await(10, TimeUnit.SECONDS);
        
        verifyHttp(stubServer).once(method(Method.POST), uri("/test"), 
                                    withPostBodyContainingJsonPath("[0].resource_name", "default-test-resource"),
                                    withPostBodyContainingJsonPath("[0].host", "my-test-host"),
                                    withPostBodyContainingJsonPath("[0].instanceId", "my-test-instance"),
                                    withPostBodyContainingJsonPath("[0].X-Request-ID", "4711"),
                                    withPostBodyContainingJsonPath("[0].statusCodeName", "SUCCESS"),
                                    withPostBodyContainingJsonPath("[0].totalNumberOfInvocations", 1));
    }

    
    @Configuration
    @EnableAutoConfiguration
    @PropertySource("classpath:request_tagging.properties")
    public static class TestConfiguration {
        
        @Autowired Environment env;
        
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
                    RequestTaggingContextConfigurer.load(env::getProperty, filter::setInitParameter);
                    filter.addMappingForServletNames(EnumSet.allOf(DispatcherType.class), true, "testRequestHandler");
                    
                    ServletRegistration.Dynamic servlet = servletContext.addServlet("testRequestHandler", HttpRequestHandlerServlet.class);
                    servlet.setLoadOnStartup(1);
                    servlet.addMapping("/");
                }
            };
        }
    }
}