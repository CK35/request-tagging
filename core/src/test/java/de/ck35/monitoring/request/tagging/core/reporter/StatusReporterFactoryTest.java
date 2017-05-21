package de.ck35.monitoring.request.tagging.core.reporter;

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

import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter.Measurement;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter.Resource;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporterFactory;
import de.ck35.monitoring.request.tagging.core.reporter.StatusReporterFactory.ReportFormat;
import de.ck35.monitoring.request.tagging.core.reporter.HttpStatusReporter.HttpTransferException;

public class StatusReporterFactoryTest {

    private static final Instant TIMESTAMP = Instant.parse("2007-12-03T10:15:30.00Z");

    private static StubServer server;

    private StatusReporterFactory requestTaggingStatusReporterFactory;

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

        requestTaggingStatusReporterFactory = new StatusReporterFactory();
        requestTaggingStatusReporterFactory.setSendData(true);
        requestTaggingStatusReporterFactory.setReportFormat(ReportFormat.INFLUX_DB);
        requestTaggingStatusReporterFactory.setHostId("my-test-host");
        requestTaggingStatusReporterFactory.setInstanceId("a");
        requestTaggingStatusReporterFactory.setPort(server.getPort());
    }

    public StatusReporter reporter() {
        return requestTaggingStatusReporterFactory.build()
                                                  .apply(TIMESTAMP);
    }

    @Test
    public void testSendToInfluxDBSucess() throws Exception {
        whenHttp(server).match(post("/write"))
                        .then(noContent());

        try (StatusReporter reporter = reporter()) {
            reporter.accept(new Resource("my-test-resource", ImmutableSortedMap.of("my-key", "my-value"), ImmutableList.of(new Measurement("SUCCESS", 10L, null))));
        }

        verifyHttp(server).once(method(Method.POST),
                                uri("/write"),
                                parameter("db", "request_data"),
                                withPostBodyContaining("request_data,resource_name=my-test-resource,host=my-test-host,instanceId=a,my-key=my-value SUCCESS=10 1196676930000000000"));
    }

    @Test(expected = HttpTransferException.class)
    public void testSendToInfluxDBWrongReturnCode() throws Exception {
        whenHttp(server).match(post("/write"))
                        .then(status(HttpStatus.BAD_REQUEST_400));

        try (StatusReporter reporter = reporter()) {
            reporter.accept(new Resource("my-test-resource", ImmutableSortedMap.of("my-key", "my-value"), ImmutableList.of(new Measurement("SUCCESS", 10L, null))));
        }
    }

}