package de.ck35.monitoring.request.tagging.testing;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus;
import de.ck35.monitoring.request.tagging.core.DefaultRequestTaggingStatus.StatusCode;

/**
 * A matcher for {@link DefaultRequestTaggingStatus} which works on data from {@link ExpectedStatus} method annotation.
 * 
 * @see ExpectedStatus
 * @author Christian Kaspari
 * @since 1.1.0
 */
public class ExpectedStatusMatcher extends TypeSafeMatcher<DefaultRequestTaggingStatus> {

    private final Map<String, Object> expectedStatus;

    public ExpectedStatusMatcher(Map<String, Object> expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    @Override
    protected boolean matchesSafely(DefaultRequestTaggingStatus item) {
        return expectedStatus.equals(toMap(item));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(" Status ")
                   .appendValue(expectedStatus);
    }

    @Override
    protected void describeMismatchSafely(DefaultRequestTaggingStatus item, Description mismatchDescription) {
        mismatchDescription.appendText(" was ")
                           .appendValue(toMap(item));
    }

    public static Matcher<DefaultRequestTaggingStatus> matches(ExpectedStatus expectedStatus) {
        return new ExpectedStatusMatcher(toMap(expectedStatus));
    }

    private static Map<String, Object> toMap(ExpectedStatus expectedStatus) {
        return toMap(expectedStatus.statusCode(), expectedStatus.resourceName(), expectedStatus.ignored(), toMetaDataMap(expectedStatus.metaData()));
    }

    private static Map<String, Object> toMap(DefaultRequestTaggingStatus item) {
        return toMap(item.getStatusCode(), item.getResourceName(), item.isIgnored(), Optional.ofNullable(item.getMetaData())
                                                                                             .orElse(Collections.emptySortedMap()));
    }

    private static Map<String, Object> toMap(StatusCode statusCode, String resourceName, boolean ignored, Map<String, String> metaData) {
        Map<String, Object> map = new TreeMap<>();
        map.put("statusCode", statusCode);
        map.put("resourceName", resourceName);
        map.put("ignored", ignored);
        map.put("metaData", metaData);
        return map;
    }
    
    private static Map<String, String> toMetaDataMap(String[] metaData) {
        assertTrue("Meta data map does not contain an even number of elements: (" + metaData.length + ")!", metaData.length % 2 == 0);
        Map<String, String> map = new TreeMap<>();
        for (int index = 0; index < metaData.length - 1; index = index + 2) {
            map.put(metaData[index], metaData[index + 1]);
        }
        return map;
    }
}