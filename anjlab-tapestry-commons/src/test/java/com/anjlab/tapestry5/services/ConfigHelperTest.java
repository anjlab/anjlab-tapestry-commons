package com.anjlab.tapestry5.services;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ConfigHelperTest
{
    @Test
    public void testFromFile() throws IOException
    {
        ConfigHelper helper = ConfigHelper.fromFile("src/test/resources/base-config.properties");
        Assert.assertEquals("Base Property 1", helper.get("prop1"));
        Assert.assertEquals("Base Property 2", helper.get("prop2"));
    }

    @Test
    public void testFileConfigExtendFrom() throws IOException
    {
        ConfigHelper helper = ConfigHelper.fromFile("src/test/resources/extending-config.properties");
        Assert.assertEquals("Overridden Property 1", helper.get("prop1"));
        Assert.assertEquals("Base Property 2", helper.get("prop2"));
    }

    @Test
    public void testFromClasspath() throws IOException
    {
        ConfigHelper helper = ConfigHelper.fromClasspathResource("base-config.properties");
        Assert.assertEquals("Base Property 1", helper.get("prop1"));
        Assert.assertEquals("Base Property 2", helper.get("prop2"));
    }

    @Test
    public void testClasspathConfigExtendFrom() throws IOException
    {
        ConfigHelper helper = ConfigHelper.fromClasspathResource("extending-config.properties");
        Assert.assertEquals("Overridden Property 1", helper.get("prop1"));
        Assert.assertEquals("Base Property 2", helper.get("prop2"));
    }

    @Test
    public void testMultiExtension() throws IOException
    {
        ConfigHelper helper = ConfigHelper.fromClasspathResource("multi-extending-config.properties");
        Assert.assertEquals("Base Property 1", helper.get("prop1"));
        Assert.assertEquals("Base Property 2", helper.get("prop2"));
    }
}
