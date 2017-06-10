/**
 * Copyright 2014 AnjLab
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
package com.anjlab.tapestry5.config.services;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class ConfigHelper
{
    private static final Logger logger = LoggerFactory.getLogger(ConfigHelper.class);

    public static final String EXTEND = "extend";

    private File configFile;

    private String resourceName;

    private Properties properties;

    private final Set<String> referenced = new HashSet<String>();

    /**
     * Creates an instance of {@link ConfigHelper} and load properties from resource on classpath.
     *
     * @see ClassLoader#getResourceAsStream(String)
     */
    public static ConfigHelper fromClasspathResource(String resourceName) throws IOException
    {
        logger.info("Reading config from classpath: {}", resourceName);
        InputStream input = ConfigHelper.class.getClassLoader().getResourceAsStream(resourceName);
        if (input == null)
        {
            throw new IOException("Classpath resource not found: " + resourceName);
        }
        return new ConfigHelper(new AutoCloseInputStream(input), resourceName);
    }

    /**
     * Creates an instance of {@link ConfigHelper} and load properties from file with filename specified by system property.
     */
    public static ConfigHelper fromSystemProperty(String property) throws IOException
    {
        String filename = System.getProperty(property);
        logger.info("Reading config from system property {}={}", property, filename);
        return new ConfigHelper(filename);
    }

    /**
     * Creates an instance of {@link ConfigHelper} and load properties from file with given name.
     */
    public static ConfigHelper fromFile(String file) throws IOException
    {
        return new ConfigHelper(file);
    }

    /**
     * Creates an instance of {@link ConfigHelper} and load properties from given {@link File}.
     */
    public static ConfigHelper fromFile(File file) throws IOException
    {
        return new ConfigHelper(file);
    }

    /**
     * Creates an instance of {@link ConfigHelper} and load properties from given {@link InputStream}.
     */
    public static ConfigHelper fromStream(InputStream input) throws IOException
    {
        return new ConfigHelper(input, null);
    }

    /**
     * Creates an empty instance of {@link ConfigHelper}.
     */
    public ConfigHelper()
    {
        this.properties = new Properties();
    }

    /**
     * Constructor for Tapestry IoC service.
     * <p>
     * {@link ConfigHelper}s from given <code>configuration</code> will be copied into new instance
     * using <code>this.{@link #copyFrom(ConfigHelper)}</code>.
     */
    public ConfigHelper(List<ConfigHelper> configuration)
    {
        this();

        for (ConfigHelper source : configuration)
        {
            copyFrom(source);
        }
    }

    private ConfigHelper(String configFilename) throws IOException
    {
        this(new File(configFilename));
    }

    private ConfigHelper(File configFile) throws IOException
    {
        logger.info("Reading config from file: {}", configFile.getAbsoluteFile());
        this.configFile = configFile;
        readProperties(configFile);
    }

    private ConfigHelper(InputStream input, String resourceName) throws IOException
    {
        this.resourceName = resourceName;
        readProperties(input);
    }

    protected void readProperties(File configFile) throws IOException
    {
        if (!configFile.exists())
        {
            throw new IllegalStateException("Configuration file not found: "
                    + configFile.getAbsolutePath());
        }

        FileInputStream input = null;
        try
        {
            input = new FileInputStream(configFile);

            readProperties(input);
        }
        finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (IOException e)
                {
                }
            }
        }
    }

    protected void readProperties(InputStream input) throws IOException
    {
        properties = new Properties();
        properties.load(input);

        List<ConfigHelper> extensions = new ArrayList<ConfigHelper>();

        if (properties.containsKey(EXTEND))
        {
            extensions.add(fromExtend(properties.getProperty(EXTEND)));
        }

        //  Support multiple ordered extensions
        boolean canContinue = true;

        for (int i = 0; canContinue; i++)
        {
            String key = EXTEND + "." + i;

            if (properties.containsKey(key))
            {
                extensions.add(fromExtend(properties.getProperty(key)));
            }
            else
            {
                //  Support 0 and 1 based offsets
                canContinue = i < 1;
            }
        }

        extendFrom(new ConfigHelper(extensions));
    }

    private ConfigHelper fromExtend(String relativePath) throws IOException
    {
        if (configFile != null)
        {
            return ConfigHelper.fromFile(FileUtils.getFile(configFile.getParentFile(), relativePath));
        }
        else if (resourceName != null)
        {
            File parentFile = new File(resourceName).getParentFile();
            String basePath = new File(parentFile, relativePath).getPath();
            return ConfigHelper.fromClasspathResource(basePath);
        }
        else
        {
            throw new RuntimeException("Unable to resolve relative path: " + relativePath);
        }
    }

    /**
     * Adds property with name <code>propertyName</code> and value from this {@link ConfigHelper} into given <code>configuration</code>
     * only if it exists in this {@link ConfigHelper}.
     *
     * @see MappedConfiguration#add(Object, Object)
     */
    public void addIfExists(String propertyName, MappedConfiguration<String, Object> configuration)
    {
        if (properties.containsKey(propertyName))
        {
            configuration.add(propertyName, properties.get(propertyName));
            referenced.add(propertyName);
        }
    }

    /**
     * Overrides property with name <code>propertyName</code> and value from this {@link ConfigHelper} in given <code>configuration</code>
     * only if it exists in this {@link ConfigHelper}.
     *
     * @see MappedConfiguration#override(Object, Object)
     */
    public void overrideIfExists(String propertyName, MappedConfiguration<String, Object> configuration)
    {
        if (properties.containsKey(propertyName))
        {
            configuration.override(propertyName, properties.get(propertyName));
            referenced.add(propertyName);
        }
    }

    /**
     * Adds property with name <code>propertyName</code> and value from this {@link ConfigHelper} into given <code>configuration</code>.
     *
     * @throws IllegalStateException if property with given name doesn't exist in this {@link ConfigHelper}.
     * @see MappedConfiguration#add(Object, Object)
     */
    public void add(String propertyName, MappedConfiguration<String, Object> configuration) throws IllegalStateException
    {
        assertPropertyDefined(propertyName, properties);

        configuration.add(propertyName, properties.get(propertyName));
        referenced.add(propertyName);
    }

    /**
     * Overrides property with name <code>propertyName</code> and value from this {@link ConfigHelper} in given <code>configuration</code>.
     *
     * @throws IllegalStateException if property with given name doesn't exist in this {@link ConfigHelper}.
     * @see MappedConfiguration#override(Object, Object)
     */
    public void override(String propertyName, MappedConfiguration<String, Object> configuration) throws IllegalStateException
    {
        assertPropertyDefined(propertyName, properties);

        configuration.override(propertyName, properties.get(propertyName));
        referenced.add(propertyName);
    }

    /**
     * @return Names of all properties from this {@link ConfigHelper}.
     */
    public Set<String> names()
    {
        Set<String> names = new HashSet<String>();
        for (Object key : properties.keySet())
        {
            names.add((String) key);
        }
        return names;
    }

    /**
     * @return Names of all properties from this {@link ConfigHelper} that were referenced using one of the following methods:
     * <ul>
     * <li>{@link #add(String, MappedConfiguration)}</li>
     * <li>{@link #addIfExists(String, MappedConfiguration)}</li>
     * <li>{@link #override(String, MappedConfiguration)}</li>
     * <li>{@link #overrideIfExists(String, MappedConfiguration)}</li>
     * <li>{@link #get(String)}</li>
     * </ul>
     * <p>
     * All properties not referenced using one of the above methods will be considered unused and will be reported
     * by the {@link UnreferencedPropertiesValidator}.
     */
    public Set<String> getReferenced()
    {
        return Collections.unmodifiableSet(referenced);
    }

    /**
     * @return Raw property value by its name or <code>null</code> if property doesn't exist.
     */
    public String get(String propertyName)
    {
        referenced.add(propertyName);

        return properties.getProperty(propertyName);
    }

    /**
     * Copy properties from given {@link ConfigHelper} into this instance.
     */
    public void copyFrom(ConfigHelper source)
    {
        for (String name : source.names())
        {
            properties.put(name, source.get(name));
        }
    }

    private void extendFrom(ConfigHelper source)
    {
        for (String name : source.names())
        {
            if (!properties.containsKey(name))
            {
                properties.put(name, source.get(name));
            }
        }
    }

    private static void assertPropertyDefined(String propertyName, Properties properties)
    {
        if (!properties.containsKey(propertyName))
        {
            throw new IllegalStateException("Required property not defined: " + propertyName);
        }
    }
}