package de.ck35.monitoring.request.tagging.testing;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.model.FrameworkMethod;

import de.ck35.monitoring.request.tagging.RequestTagging;
import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus;

public class ExpectedStatusRuleIntegrationTest {

    @Rule public ExpectedStatusRule rule = new ExpectedStatusRule() {
        @Override
        protected void afterEvaluate(FrameworkMethod method) {
            assertEquals(Arrays.asList("second", "first"), getStatusList().stream()
                                                                          .map(DefaultRequestTaggingStatus::getResourceName)
                                                                          .collect(Collectors.toList()));

        };
    };

    @Rule public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testRequestTaggingStatusIsConsumed() throws Exception {
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

    @Test
    public void testRequestTaggingStatusIsNotConsumedOnException() {
        expectedException.expect(IllegalArgumentException.class);
        throw new IllegalArgumentException("Test");
    }

}