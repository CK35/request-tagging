package de.ck35.monitoring.request.tagging.integration.tomcat;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Condition.method;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.uri;
import static com.xebialabs.restito.semantics.Condition.withPostBodyContainingJsonPath;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.startup.Catalina;
import org.apache.coyote.AbstractProtocol;
import org.apache.tomcat.util.net.JIoEndpoint;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.server.StubServer;

public class RequestTaggingValveTest {

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
    
    private StubServer stubServer;
    private CountDownLatch latch;

    @Before
    public void before() {
        latch = new CountDownLatch(1);
        stubServer = new StubServer(5555);
        stubServer.start();

        whenHttp(stubServer).match(post("/test"))
                            .then(Action.custom(in -> {
                                in.setStatus(HttpStatus.OK_200);
                                latch.countDown();
                                return in;
                            }));
    }

    @After
    public void after() {
        stubServer.stop();
    }

    @Test
    public void testValve() throws Exception {
        Catalina catalina = new Catalina();
        catalina.setConfigFile(getClass().getResource("/server.xml")
                                         .getFile());
        catalina.start();

        HttpURLConnection connection = (HttpURLConnection) new URL("http", "localhost", getPort(catalina), "/any").openConnection();
        connection.addRequestProperty("X-Request-ID", "4711");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.connect();

        connection.getResponseCode();
        connection.disconnect();

        latch.await(10, TimeUnit.SECONDS);
        
        verifyHttp(stubServer).once(method(Method.POST), uri("/test"), 
                                    withPostBodyContainingJsonPath("[0].resource_name", "default-test-resource"),
                                    withPostBodyContainingJsonPath("[0].host", "my-test-host"),
                                    withPostBodyContainingJsonPath("[0].instanceId", "my-test-instance"),
                                    withPostBodyContainingJsonPath("[0].X-Request-ID", "4711"),
                                    withPostBodyContainingJsonPath("[0].statusCodeName", "SUCCESS"),
                                    withPostBodyContainingJsonPath("[0].totalNumberOfInvocations", 1));

        catalina.stop();
    }

    private int getPort(Catalina catalina) throws NoSuchFieldException, IllegalAccessException {
        StandardService service = (StandardService) catalina.getServer()
                                                            .findService("Catalina");
        Connector connector = service.findConnectors()[0];
        Field endpointField = AbstractProtocol.class.getDeclaredField("endpoint");
        endpointField.setAccessible(true);
        JIoEndpoint endpoint = (JIoEndpoint) endpointField.get(connector.getProtocolHandler());
        int port = endpoint.getLocalPort();
        return port;
    }

}