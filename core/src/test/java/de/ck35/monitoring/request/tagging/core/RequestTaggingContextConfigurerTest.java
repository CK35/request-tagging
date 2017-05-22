package de.ck35.monitoring.request.tagging.core;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class RequestTaggingContextConfigurerTest {

    @Test
    public void testLoadKeyOverwrite() {
        Map<String, String> source = new HashMap<>();
        source.put("requestTagging.statusReporter.hostId", "A");
        source.put("hostId", "B");
        
        Map<String, String> target = new HashMap<>();
        
        RequestTaggingContextConfigurer.load(source::get, target::put);
        assertEquals(ImmutableMap.of("requestTagging.statusReporter.hostId", "B"), target);
    }

}