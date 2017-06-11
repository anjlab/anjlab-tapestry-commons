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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class ConfigHelper
{
    private static final Logger logger = LoggerFactory.getLogger(ConfigHelper.class);

    public static final String EXTEND = "extend";

    private File configFile;

    private String resourceName;

    private Properties properties;

    private final Set<String> referenced;
    private final Map<String, Class<?>> propertyTypes;

    /**
     * Creates an instance of {@link ConfigHelper} and load properties from resource on classpath.
     *
     * @param resourceName Path to classpath resource with properties.
     * @return New instance of {@link ConfigHelper} with values loaded from given <code>resourceName</code>.
     * @throws IOException on resource read errors.
     * @see ClassLoader#getResourceAsStream(String)
     * @see Properties
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
     *
     * @param property Name of a system property that holds path of a file with properties.
     * @return New instance of {@link ConfigHelper} with values loaded from a file.
     * @throws IOException on file read errors.
     * @see Properties
     * @see System#getProperty(String)
     */
    public static ConfigHelper fromSystemProperty(String property) throws IOException
    {
        String filename = System.getProperty(property);
        logger.info("Reading config from system property {}={}", property, filename);
        return new ConfigHelper(filename);
    }

    /**
     * Creates an instance of {@link ConfigHelper} and load properties from file with given name.
     *
     * @param file Name of file with properties.
     * @return New instance of {@link ConfigHelper} with properties loaded from <code>file</code>.
     * @throws IOException on <code>file</code> read errors.
     * @see Properties
     */
    public static ConfigHelper fromFile(String file) throws IOException
    {
        return new ConfigHelper(file);
    }

    /**
     * Creates an instance of {@link ConfigHelper} and load properties from given {@link File}.
     *
     * @param file File with properties.
     * @return New instance of {@link ConfigHelper} with properties loaded from given <code>file</code>.
     * @throws IOException on <code>file</code> read errors.
     * @see Properties
     */
    public static ConfigHelper fromFile(File file) throws IOException
    {
        return new ConfigHelper(file);
    }

    /**
     * Creates an instance of {@link ConfigHelper} and load properties from given {@link InputStream}.
     *
     * @param input Input stream with properties.
     * @return New instance of {@link ConfigHelper} with properties loaded from given <code>input</code>.
     * @throws IOException on <code>input</code> read errors.
     * @see Properties
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
        this.referenced = new HashSet<>();
        this.propertyTypes = new HashMap<>();
    }

    /**
     * Creates an instance of {@link ConfigHelper} and from given {@link Properties}.
     *
     * @param properties Properties to use for this {@link ConfigHelper}.
     */
    public ConfigHelper(Properties properties)
    {
        this.properties = new Properties();
        this.properties.putAll(properties);

        this.referenced = new HashSet<>(properties.size());
        this.propertyTypes = new HashMap<>(properties.size());
    }

    /**
     * Constructor for Tapestry IoC service.
     * <p>
     * {@link ConfigHelper}s from given <code>configuration</code> will be copied into new instance
     * using <code>this.{@link #copyFrom(ConfigHelper)}</code>.
     *
     * @param configuration List of source {@link ConfigHelper}s to copy properties from.
     *                      The value is usually created by the Tapestry IoC.
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
        referenced = new HashSet<>(properties.size());
        propertyTypes = new HashMap<>(properties.size());
    }

    private ConfigHelper(InputStream input, String resourceName) throws IOException
    {
        this.resourceName = resourceName;
        readProperties(input);
        referenced = new HashSet<>(properties.size());
        propertyTypes = new HashMap<>(properties.size());
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
     * Adds property with name <code>propertyName</code> and value from this {@link ConfigHelper} into given <code>configuration</code>.
     *
     * @param propertyName  Name of the property.
     * @param configuration Configuration of a {@link org.apache.tapestry5.ioc.services.SymbolProvider}.
     * @throws IllegalStateException if property with given name doesn't exist in this {@link ConfigHelper}.
     * @see MappedConfiguration#add(Object, Object)
     */
    public void add(String propertyName, MappedConfiguration<String, Object> configuration) throws IllegalStateException
    {
        add(Object.class, propertyName, configuration);
    }

    /**
     * Adds property with name <code>propertyName</code> and value from this {@link ConfigHelper} into given <code>configuration</code>.
     * <p>
     * {@link PropertyTypeValidator} will validate value of the property (after symbol expansion)
     * to be coercible to <code>propertyType</code>.
     *
     * @param propertyName  Name of the property.
     * @param propertyType  Desired type of the property after symbol expansion.
     * @param configuration Configuration of a {@link org.apache.tapestry5.ioc.services.SymbolProvider}.
     * @throws IllegalStateException if property with given name doesn't exist in this {@link ConfigHelper}.
     * @see MappedConfiguration#add(Object, Object)
     * @see org.apache.tapestry5.ioc.services.TypeCoercer
     */
    public void add(Class<?> propertyType, String propertyName, MappedConfiguration<String, Object> configuration)
            throws IllegalStateException
    {
        assertNotNull(propertyType);
        assertPropertyDefined(propertyName, properties);

        configuration.add(propertyName, properties.get(propertyName));
        propertyTypes.put(propertyName, propertyType);
        referenced.add(propertyName);
    }

    /**
     * Adds property with name <code>propertyName</code> and value from this {@link ConfigHelper} into given <code>configuration</code>
     * only if it exists in this {@link ConfigHelper}.
     *
     * @param propertyName  Name of the property.
     * @param configuration Configuration of a {@link org.apache.tapestry5.ioc.services.SymbolProvider}.
     * @see MappedConfiguration#add(Object, Object)
     */
    public void addIfExists(String propertyName, MappedConfiguration<String, Object> configuration)
    {
        addIfExists(Object.class, propertyName, configuration);
    }

    /**
     * Adds property with name <code>propertyName</code> and value from this {@link ConfigHelper} into given <code>configuration</code>
     * only if it exists in this {@link ConfigHelper}.
     * <p>
     * {@link PropertyTypeValidator} will validate value of the property (after symbol expansion)
     * to be coercible to <code>propertyType</code>.
     *
     * @param propertyName  Name of the property.
     * @param propertyType  Desired type of the property after symbol expansion.
     * @param configuration Configuration of a {@link org.apache.tapestry5.ioc.services.SymbolProvider}.
     * @see MappedConfiguration#add(Object, Object)
     * @see org.apache.tapestry5.ioc.services.TypeCoercer
     */
    public void addIfExists(Class<?> propertyType, String propertyName, MappedConfiguration<String, Object> configuration)
    {
        if (properties.containsKey(propertyName))
        {
            add(propertyType, propertyName, configuration);
        }
    }

    /**
     * Overrides property with name <code>propertyName</code> and value from this {@link ConfigHelper} in given <code>configuration</code>.
     *
     * @param propertyName  Name of the property.
     * @param configuration Configuration of a {@link org.apache.tapestry5.ioc.services.SymbolProvider}.
     * @throws IllegalStateException if property with given name doesn't exist in this {@link ConfigHelper}.
     * @see MappedConfiguration#override(Object, Object)
     */
    public void override(String propertyName, MappedConfiguration<String, Object> configuration) throws IllegalStateException
    {
        override(Object.class, propertyName, configuration);
    }

    /**
     * Overrides property with name <code>propertyName</code> and value from this {@link ConfigHelper} in given <code>configuration</code>.
     * <p>
     * {@link PropertyTypeValidator} will validate value of the property (after symbol expansion)
     * to be coercible to <code>propertyType</code>.
     *
     * @param propertyName  Name of the property.
     * @param propertyType  Desired type of the property after symbol expansion.
     * @param configuration Configuration of a {@link org.apache.tapestry5.ioc.services.SymbolProvider}.
     * @throws IllegalStateException if property with given name doesn't exist in this {@link ConfigHelper}.
     * @see MappedConfiguration#override(Object, Object)
     * @see org.apache.tapestry5.ioc.services.TypeCoercer
     */
    public void override(Class<?> propertyType, String propertyName, MappedConfiguration<String, Object> configuration)
            throws IllegalStateException
    {
        assertNotNull(propertyType);
        assertPropertyDefined(propertyName, properties);

        configuration.override(propertyName, properties.get(propertyName));
        propertyTypes.put(propertyName, propertyType);
        referenced.add(propertyName);
    }

    /**
     * Overrides property with name <code>propertyName</code> and value from this {@link ConfigHelper} in given <code>configuration</code>
     * only if it exists in this {@link ConfigHelper}.
     *
     * @param propertyName  Name of the property.
     * @param configuration Configuration of a {@link org.apache.tapestry5.ioc.services.SymbolProvider}.
     * @see MappedConfiguration#override(Object, Object)
     */
    public void overrideIfExists(String propertyName, MappedConfiguration<String, Object> configuration)
    {
        overrideIfExists(Object.class, propertyName, configuration);
    }

    /**
     * Overrides property with name <code>propertyName</code> and value from this {@link ConfigHelper} in given <code>configuration</code>
     * only if it exists in this {@link ConfigHelper}.
     * <p>
     * {@link PropertyTypeValidator} will validate value of the property (after symbol expansion)
     * to be coercible to <code>propertyType</code>.
     *
     * @param propertyName  Name of the property.
     * @param propertyType  Desired type of the property after symbol expansion.
     * @param configuration Configuration of a {@link org.apache.tapestry5.ioc.services.SymbolProvider}.
     * @see MappedConfiguration#override(Object, Object)
     * @see org.apache.tapestry5.ioc.services.TypeCoercer
     */
    public void overrideIfExists(Class<?> propertyType, String propertyName, MappedConfiguration<String, Object> configuration)
    {
        if (properties.containsKey(propertyName))
        {
            override(propertyType, propertyName, configuration);
        }
    }

    /**
     * @return Names of all properties from this {@link ConfigHelper}.
     */
    public Set<String> getPropertyNames()
    {
        Set<String> names = new HashSet<String>();
        for (Object key : properties.keySet())
        {
            names.add((String) key);
        }
        return names;
    }

    /**
     * @return Names of all properties from this {@link ConfigHelper} that were referenced using one of the <code>add...</code>,
     * <code>override...</code>, or {@link #getRaw(String)} methods.
     * <p>
     * All properties not referenced using one of the above methods will be considered unused and will be reported
     * by the {@link UnreferencedPropertiesValidator}.
     */
    public Set<String> getReferenced()
    {
        return Collections.unmodifiableSet(referenced);
    }

    /**
     * @param propertyName Name of the property.
     * @return Desired type of the given <code>propertyName</code> as specified by {@link #add(Class, String, MappedConfiguration)}
     * or {@link #override(Class, String, MappedConfiguration)}.
     * If not explicitly specified default type is {@link Object}.
     * Returns <code>null</code> if <code>propertyName</code> is not known to this {@link ConfigHelper}.
     */
    public Class<?> getPropertyType(String propertyName)
    {
        return propertyTypes.get(propertyName);
    }

    /**
     * @param propertyName Name of the property.
     * @return Raw property value by its name or <code>null</code> if property doesn't exist.
     */
    public String getRaw(String propertyName)
    {
        referenced.add(propertyName);

        return properties.getProperty(propertyName);
    }

    /**
     * Copy properties from given {@link ConfigHelper} into this instance.
     *
     * @param source Source {@link ConfigHelper} to read properties from.
     */
    public void copyFrom(ConfigHelper source)
    {
        for (String name : source.getPropertyNames())
        {
            properties.put(name, source.getRaw(name));
        }
    }

    private void extendFrom(ConfigHelper source)
    {
        for (String name : source.getPropertyNames())
        {
            if (!properties.containsKey(name))
            {
                properties.put(name, source.getRaw(name));
            }
        }
    }

    private static void assertNotNull(Object object)
    {
        if (object == null)
        {
            throw new NullPointerException();
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