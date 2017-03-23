package de.ck35.monitoring.request.tagging.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class HashAlgorithmTest {

    @Test
    public void testHash() {
        assertEquals("098F6BCD4621D373CADE4E832627B4F6", new HashAlgorithm().hash("test"));
    }

    @Test
    public void testSetAlgorithmName() {
        HashAlgorithm algorithm = new HashAlgorithm();
        algorithm.setAlgorithmName("SHA1");
        assertEquals("A94A8FE5CCB19BA61C4C0873D391E987982FBBD3", algorithm.hash("test"));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testSetAlgorithmNameWithInvalidName() {
        new HashAlgorithm().setAlgorithmName("Foo");
    }

}