package de.ck35.monitoring.request.tagging.core;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import de.ck35.monitoring.request.tagging.core.reporter.StatusReporter.Resource;

public class ExpectedResource {
    
    private String name;
    private TreeMap<String, String> metaData;
    private Set<ExpectedMeasurement> expectedMeasurements;
    
    public ExpectedResource() {
        this.name = DefaultRequestTaggingStatus.DEFAULT_RESOURCE_NAME;
        this.metaData = new TreeMap<>();
        this.expectedMeasurements = new HashSet<>();
    }
    public static ExpectedResource resource() {
        return new ExpectedResource();
    }
    public static ExpectedResource of(Resource resource) {
        ExpectedResource result = new ExpectedResource();
        result.withName(resource.getName());
        resource.getMetaData().forEach(result::withMetaData);
        resource.getMeasurements().forEach(x -> result.withMeasurement(ExpectedMeasurement.of(x)));
        return result;
    }
    
    public ExpectedResource withName(String name) {
        this.name = name;
        return this;
    }
    public ExpectedResource withMetaData(String key, String value) {
        this.metaData.put(key, value);
        return this;
    }
    
    public ExpectedResource withMeasurement(ExpectedMeasurement measurement) {
        this.expectedMeasurements.add(measurement);
        return this;
    }
    
    public Matcher<Resource> matches() {
        return new TypeSafeMatcher<Resource>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(" Resource ").appendValue(ExpectedResource.this);
            }
            @Override
            protected void describeMismatchSafely(Resource item, Description mismatchDescription) {
                mismatchDescription.appendText(" was ").appendValue(of(item));
            }
            @Override
            protected boolean matchesSafely(Resource item) {
                return ExpectedResource.this.equals(of(item));
            }
        };
    }
    
    @Override
    public String toString() {
        return "[name=" + name + ", metaData=" + metaData + ", expectedMeasurements=" + expectedMeasurements + "]";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((expectedMeasurements == null) ? 0 : expectedMeasurements.hashCode());
        result = prime * result + ((metaData == null) ? 0 : metaData.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ExpectedResource)) {
            return false;
        }
        ExpectedResource other = (ExpectedResource) obj;
        if (expectedMeasurements == null) {
            if (other.expectedMeasurements != null) {
                return false;
            }
        } else if (!expectedMeasurements.equals(other.expectedMeasurements)) {
            return false;
        }
        if (metaData == null) {
            if (other.metaData != null) {
                return false;
            }
        } else if (!metaData.equals(other.metaData)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
}