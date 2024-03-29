/**
 * Copyright 2015 AnjLab
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anjlab.tapestry5.config;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ConfigHelperTest
{
    @Test
    public void testFromFile() throws IOException
    {
        ConfigHelper helper = ConfigHelper.fromFile("src/test/resources/base-config.properties");
        Assert.assertEquals("Base Property 1", helper.getRaw("prop1"));
        Assert.assertEquals("Base Property 2", helper.getRaw("prop2"));
    }

    @Test
    public void testFileConfigExtendFrom() throws IOException
    {
        ConfigHelper helper = ConfigHelper.fromFile("src/test/resources/extending-config.properties");
        Assert.assertEquals("Overridden Property 1", helper.getRaw("prop1"));
        Assert.assertEquals("Base Property 2", helper.getRaw("prop2"));
    }

    @Test
    public void testFromClasspath() throws IOException
    {
        ConfigHelper helper = ConfigHelper.fromClasspathResource("base-config.properties");
        Assert.assertEquals("Base Property 1", helper.getRaw("prop1"));
        Assert.assertEquals("Base Property 2", helper.getRaw("prop2"));
    }

    @Test
    public void testClasspathConfigExtendFrom() throws IOException
    {
        ConfigHelper helper = ConfigHelper.fromClasspathResource("extending-config.properties");
        Assert.assertEquals("Overridden Property 1", helper.getRaw("prop1"));
        Assert.assertEquals("Base Property 2", helper.getRaw("prop2"));
    }

    @Test
    public void testClasspathConfigExtendFromWithPrefix() throws IOException
    {
        ConfigHelper helper = ConfigHelper.fromClasspathResource("extend-with-prefix.properties");
        Assert.assertEquals("Overridden Property 1", helper.getRaw("namespace.prop1"));
        Assert.assertEquals("Base Property 2", helper.getRaw("namespace.prop2"));
    }

    @Test
    public void testMultiExtension() throws IOException
    {
        ConfigHelper helper = ConfigHelper.fromClasspathResource("multi-extending-config.properties");
        Assert.assertEquals("Base Property 1", helper.getRaw("prop1"));
        Assert.assertEquals("Base Property 2", helper.getRaw("prop2"));
    }
}
