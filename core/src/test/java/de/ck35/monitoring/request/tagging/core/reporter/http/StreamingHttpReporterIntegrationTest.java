package de.ck35.monitoring.request.tagging.core.reporter.http;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.noContent;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Condition.method;
import static com.xebialabs.restito.semantics.Condition.parameter;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.uri;
import static com.xebialabs.restito.semantics.Condition.withPostBodyContaining;

import java.time.Instant;

import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.xebialabs.restito.server.StubServer;

import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter.Measurement;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporter.Resource;
import de.ck35.monitoring.request.tagging.core.reporter.RequestTaggingStatusReporterFactory;
import de.ck35.monitoring.request.tagging.core.reporter.http.StreamingHttpReporter.HttpTransferException;

public class StreamingHttpReporterIntegrationTest {

    private static final Instant TIMESTAMP = Instant.parse("2007-12-03T10:15:30.00Z");

    private static StubServer server;

    private RequestTaggingStatusReporterFactory requestTaggingStatusReporterFactory;

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

        requestTaggingStatusReporterFactory = new RequestTaggingStatusReporterFactory();
        requestTaggingStatusReporterFactory.setReportToInfluxDB(true);
        requestTaggingStatusReporterFactory.setLocalHostName("my-test-host");
        requestTaggingStatusReporterFactory.setLocalInstanceId("a");
        requestTaggingStatusReporterFactory.setInfluxDBPort(Integer.toString(server.getPort()));
        requestTaggingStatusReporterFactory.setInfluxDBDatabaseName("test-db");
    }

    public RequestTaggingStatusReporter reporter() {
        return requestTaggingStatusReporterFactory.build()
                                                  .apply(TIMESTAMP);
    }

    @Test
    public void testSendToInfluxDBSucess() throws Exception {
        whenHttp(server).match(post("/write"))
                        .then(noContent());

        try (RequestTaggingStatusReporter reporter = reporter()) {
            reporter.accept(new Resource("my-test-resource", ImmutableSortedMap.of("my-key", "my-value"), ImmutableList.of(new Measurement("SUCCESS", 10L, null))));
        }

        verifyHttp(server).once(method(Method.POST),
                                uri("/write"),
                                parameter("db", "test-db"),
                                withPostBodyContaining("request_data,resource_name=\"my-test-resource\",host=\"my-test-host\",instanceId=\"a\",my-key=\"my-value\" SUCCESS=10 1196676930000000000"));
    }

    @Test(expected = HttpTransferException.class)
    public void testSendToInfluxDBWrongReturnCode() throws Exception {
        whenHttp(server).match(post("/write"))
                        .then(status(HttpStatus.BAD_REQUEST_400));

        try (RequestTaggingStatusReporter reporter = reporter()) {
            reporter.accept(new Resource("my-test-resource", ImmutableSortedMap.of("my-key", "my-value"), ImmutableList.of(new Measurement("SUCCESS", 10L, null))));
        }
    }

}