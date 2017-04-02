package de.ck35.monitoring.request.tagging.testing;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import de.ck35.monitoring.request.tagging.RequestTagging;
import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus.StatusCode;

@RunWith(Parameterized.class)
public class ExpectedStatusRuleTest {

    @Parameters(name = " {1} ")
    public static Iterable<Object[]> data() throws Exception {
        return Arrays.asList(ExpectedStatusRuleTest.class.getMethods())
                     .stream()
                     .filter(method -> method.getName()
                                             .startsWith("method"))
                     .map(FrameworkMethod::new)
                     .map(method -> new Object[] { method, method.getMethod()
                                                                 .getName() })
                     .collect(Collectors.toList());
    }

    private final FrameworkMethod method;

    public ExpectedStatusRuleTest(FrameworkMethod method, String testName) {
        this.method = method;
    }

    @Test
    public void testMethod() throws Throwable {
        InvokeMethod invokeMethod = new InvokeMethod(method, this);
        Statement statement = new ExpectedStatusRule().apply(invokeMethod, method, this);
        try {
            statement.evaluate();
        } catch (ExceptionWhichIsThrownByTestMethod e) {
            // Ignore
        } catch (AssertionError e) {
            handleAssertionError(e);
        }
    }

    public void handleAssertionError(AssertionError e) {
        switch (method.getName()) {
        case "methodWithExpectationsMismatch":
            assertEquals("Number of declared expectations (1) does not match number of consumed status elements (2)!", e.getMessage());
            break;
        case "methodWithInvalidExpectation":
            assertEquals("Meta data map does not contain an even number of elements: (1)!", e.getMessage());
            break;
        case "methodWithResourceMismatch":
            assertEquals("\nExpected:  Status <{ignored=false, metaData={}, resourceName=another_resource, statusCode=SUCCESS}>\n     but:  was <{ignored=false, metaData={}, resourceName=test_resource, statusCode=SUCCESS}>", e.getMessage());
            break;
        default:
            throw e;
        }
    }

    @ExpectedStatus(ignored = true)
    public void methodIgnored() {
        RequestTagging.get()
                      .ignore();
    }

    @ExpectedStatus
    public void methodSuccessDefault() {
    }

    @ExpectedStatus(resourceName = "second")
    @ExpectedStatus(resourceName = "first")
    public void methodSuccessMuliple() throws Exception {
        RequestTagging.get()
                      .withResourceName("first");
        Thread thread = new Thread(RequestTagging.get()
                                                 .handover(() -> {
                                                     RequestTagging.get()
                                                                   .withResourceName("second");
                                                 }));
        thread.start();
        thread.join();
    }

    @ExpectedStatus(resourceName = "second")
    public void methodWithExpectationsMismatch() throws Exception {
        RequestTagging.get()
                      .withResourceName("first");
        Thread thread = new Thread(RequestTagging.get()
                                                 .handover(() -> {
                                                     RequestTagging.get()
                                                                   .withResourceName("second");
                                                 }));
        thread.start();
        thread.join();
    }

    @ExpectedStatus(statusCode = StatusCode.SERVER_ERROR)
    public void methodErrorDefault() {
        RequestTagging.get()
                      .serverError();
    }

    @ExpectedStatus(resourceName = "test_resource", metaData = { "key1", "value1", "key2", "value2" })
    public void methodSuccessWithResourceNameAndMetaData() {
        RequestTagging.get()
                      .withResourceName("test_resource")
                      .withMetaData("key1", "value1")
                      .withMetaData("key2", "value2");
    }

    @ExpectedStatus(resourceName = "any")
    public void methodExpectedStatusIsNotTestedOnException() {
        throw new ExceptionWhichIsThrownByTestMethod();
    }

    public void methodWithoutExpectations() {
        RequestTagging.get()
                      .withResourceName("test_resource");
    }

    @ExpectedStatus(metaData = { "keyWithoutValue" })
    public void methodWithInvalidExpectation() {
    }
    
    @ExpectedStatus(resourceName="another_resource")
    public void methodWithResourceMismatch() {
        RequestTagging.get().withResourceName("test_resource");
    }

    public static class ExceptionWhichIsThrownByTestMethod extends RuntimeException {

    }
}