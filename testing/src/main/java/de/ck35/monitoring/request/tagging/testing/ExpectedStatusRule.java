package de.ck35.monitoring.request.tagging.testing;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus;
import de.ck35.monitoring.request.tagging.core.HashAlgorithm;
import de.ck35.monitoring.request.tagging.core.RequestTaggingRunnable;

/**
 * JUnit test rule which should be used together with {@link ExpectedStatus}
 * annotation.
 * 
 * @see ExpectedStatus
 * @author Christian Kaspari
 * @since 1.1.0
 */
public class ExpectedStatusRule implements MethodRule {

    private final DefaultRequestTaggingStatus defaultStatus;
    private final List<DefaultRequestTaggingStatus> statusList;

    public ExpectedStatusRule() {
        this.statusList = new ArrayList<>();
        this.defaultStatus = new DefaultRequestTaggingStatus(statusList::add, new HashAlgorithm()::hash, Clock.systemUTC());
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    beforeEvaluate(method);
                    new RequestTaggingRunnable(evaluateStatement(base), new DefaultRequestTaggingStatus(defaultStatus)).run();
                    afterEvaluate(method);
                } catch (ThrowableWrapperException e) {
                    throw e.getCause();
                }
            }
        };
    }

    /**
     * Hook for subclasses which need to customize the evaluation process.
     * Default is empty.
     * 
     * @param method
     *            The method which will be invoked.
     */
    protected void beforeEvaluate(FrameworkMethod method) {

    }

    /**
     * Hook for subclasses which need to customize the evaluation process.
     * Default implementation will read {@link ExpectedStatus} annotation and
     * verifies the consumed status elements. This method will only be called
     * when no exception has been thrown by method invocation.
     * 
     * @param method
     *            The method which has been invoked.
     */
    protected void afterEvaluate(FrameworkMethod method) {
        List<ExpectedStatus> expected = Optional.ofNullable(method.getAnnotation(ExpectedStatus.Repeat.class))
                                                .map(repeat -> Arrays.asList(repeat.value()))
                                                .orElse(Optional.ofNullable(method.getAnnotation(ExpectedStatus.class))
                                                                .map(Collections::singletonList)
                                                                .orElse(Collections.emptyList()));
        if (expected.isEmpty()) {
            return;
        }
        assertTrue("Number of declared expectations (" + expected.size() + ") does not match number of consumed status elements (" + statusList.size() + ")!", expected.size() == statusList.size());
        for (int index = 0; index < expected.size(); index++) {
            assertThat(statusList.get(index), ExpectedStatusMatcher.matches(expected.get(index)));
        }
    }

    public List<DefaultRequestTaggingStatus> getStatusList() {
        return statusList;
    }

    public DefaultRequestTaggingStatus getDefaultStatus() {
        return defaultStatus;
    }

    private Runnable evaluateStatement(Statement statement) {
        return () -> {
            try {
                statement.evaluate();
            } catch (Throwable e) {
                throw new ThrowableWrapperException(e);
            }
        };
    }

    private static class ThrowableWrapperException extends RuntimeException {

        public ThrowableWrapperException(Throwable cause) {
            super(cause);
        }
    }
}