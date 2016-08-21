package de.ck35.monitoring.request.tagging.core.reporter.influxdb;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.noContent;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Condition.method;
import static com.xebialabs.restito.semantics.Condition.parameter;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.uri;
import static com.xebialabs.restito.semantics.Condition.withPostBodyContaining;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.function.Consumer;

import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.xebialabs.restito.server.StubServer;

import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus.StatusCode;

@RunWith(MockitoJUnitRunner.class)
public class InfluxDBTest {

    private static StubServer server;

    @Mock Consumer<String> bodyConsumer;
    @Captor ArgumentCaptor<String> bodyCaptor;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = new StubServer();
        server.start();
    }

    @AfterClass
    public static void afterClass() {
        server.stop();
    }
    
    @Before
    public void before() {
        server.clear();
    }
    
    public InfluxDB influxDB() {
        return new InfluxDB("my-test-host", "a", "http", "localhost", Integer.toString(server.getPort()), "test-db", 5000, 5000);
    }

    public InfluxDB.Reporter reporter() {
        return new InfluxDB.Reporter(Instant.parse("2007-12-03T10:15:30.00Z"), "my-host", "A", bodyConsumer);
    }

    @Test
    public void testSendToInfluxDBSucess() throws Exception {
        whenHttp(server).match(post("/write"))
                        .then(noContent());
        influxDB().send("test-body");
        verifyHttp(server).once(method(Method.POST), uri("/write"), parameter("db", "test-db"), withPostBodyContaining("test-body"));
    }
    
    @Test
    public void testSendEmptyToInfluxDB() throws Exception {
        whenHttp(server).match(post("/write"))
                        .then(noContent());
        influxDB().send("");
        verifyHttp(server).never(method(Method.POST), uri("/write"));
    }
    
    @Test(expected=UncheckedIOException.class)
    public void testSendToInfluxDBWrongReturnCode() throws Exception {
        whenHttp(server).match(post("/write"))
                        .then(status(HttpStatus.BAD_REQUEST_400));
        influxDB().send("test-body");
    }
    
    @Test
    public void testReporter() {
        InfluxDB.Reporter reporter = reporter();
        reporter.accept("my-resource-1",
                        ImmutableMap.of(StatusCode.SUCCESS.toString(), 5L, StatusCode.SERVER_ERROR.toString(), 1L),
                        ImmutableMap.of("key1", "value1"));
        reporter.accept("my-resource-2", ImmutableMap.of(StatusCode.SUCCESS.toString(), 10L), ImmutableMap.of());
        reporter.commit();
        verify(bodyConsumer).accept(bodyCaptor.capture());

        assertEquals("request_data,resource_name=\"my-resource-1\",host=\"my-host\",instanceId=\"A\",key1=\"value1\" SUCCESS=5,SERVER_ERROR=1 1196676930000000000\n"
                + "request_data,resource_name=\"my-resource-2\",host=\"my-host\",instanceId=\"A\" SUCCESS=10 1196676930000000000\n", bodyCaptor.getValue());
    }

}