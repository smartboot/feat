package tech.smartboot.feat.cloud.aot.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BoolSerializerTest {
    @Test
    public void shouldKeepExistingIsPrefix() {
        assertEquals("isPrivate", BoolSerializer.getterName("isPrivate"));
        assertEquals("isEnabled", BoolSerializer.getterName("isEnabled"));
    }

    @Test
    public void shouldAddIsPrefixForRegularBooleanFields() {
        assertEquals("isActive", BoolSerializer.getterName("active"));
        assertEquals("isCanPush", BoolSerializer.getterName("canPush"));
    }

    @Test
    public void shouldNotTreatLowercaseIsPrefixAsBooleanAccessorPrefix() {
        assertEquals("isLand", BoolSerializer.getterName("land"));
        assertEquals("isIsland", BoolSerializer.getterName("island"));
    }
}
