package de.ck35.monitoring.request.tagging.testing;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus;
import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus.StatusCode;;

/**
 * Annotation for test methods. Use this annotation together with the
 * {@link ExpectedStatusRule} inside your tests to verify that RequestTagging
 * has been invoked with the expected arguments during test run.
 * <p>
 * Example:
 * 
 * <pre>
 *   &#064;Rule public ExpectedStatusRule rule = new ExpectedStatusRule();
 * 
 *   &#064;Test
 *   &#064;ExpectedStatus
 *   public void testWithMinimalExpectation() {
 *      ...
 *   }
 * 
 *   &#064;Test
 *   &#064;ExpectedStatus(resourceName = "my_resource")
 *   public void testWithResourceName) {
 *      ...
 *   }
 * 
 *   &#064;Test
 *   &#064;ExpectedStatus(statusCode = StatusCode.SERVER_ERROR, resourceName = "my_resource", metaData = { "key1", "value1", "key2", "value2" })
 *   public void testMethodWithServerError() {
 *      ...
 *   }
 *   
 *   &#064;Test
 *   &#064;ExpectedStatus(resourceName = "first_resource", metaData = { "key1", "value1" })
 *   &#064;ExpectedStatus(resourceName = "second_resource")
 *   public void testMethodWithMultipleStatus() {
 *      ...
 *   }
 * </pre>
 * 
 * 
 * @author Christian Kaspari
 * @since 1.1.0
 */
@Target(METHOD)
@Retention(RUNTIME)
@Repeatable(value = ExpectedStatus.Repeat.class)
public @interface ExpectedStatus {

    @Target(METHOD)
    @Retention(RUNTIME)
    @interface Repeat {

        ExpectedStatus[] value();

    }

    /**
     * @return The expected status code. Default is {@link StatusCode#SUCCESS}
     */
    StatusCode statusCode() default StatusCode.SUCCESS;

    /**
     * @return The expected resource name. Default is
     *         {@link DefaultRequestTaggingStatus#DEFAULT_RESOURCE_NAME}.
     */
    String resourceName() default DefaultRequestTaggingStatus.DEFAULT_RESOURCE_NAME;

    /**
     * @return The ignored status of the tested request. Default is
     *         <code>false</code>.
     */
    boolean ignored() default false;

    /**
     * @return The meta data map as array. Will be transformed to a map by
     *         selecting always pairs of key and value. The total number of
     *         entries inside the array must be even.
     */
    String[] metaData() default {};

}