/**
 * Copyright 2014 AnjLab
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anjlab.tapestry5.services;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.apache.commons.io.FileUtils.getFile;

public class ConfigHelper
{
    private static final Logger logger = LoggerFactory.getLogger(ConfigHelper.class);

    public static final String EXTEND = "extend";

    private File configFile;

    private String resourceName;

    private Properties properties;
    
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
    
    public static ConfigHelper fromSystemProperty(String property) throws IOException
    {
        String filename = System.getProperty(property);
        logger.info("Reading config from system property {}={}", property, filename);
        return new ConfigHelper(filename);
    }
    
    public static ConfigHelper fromFile(String file) throws IOException
    {
        return new ConfigHelper(file);
    }
    
    public static ConfigHelper fromFile(File file) throws IOException
    {
        return new ConfigHelper(file);
    }
    
    public ConfigHelper()
    {
        this.properties = new Properties();
    }
    
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
    
    public static ConfigHelper fromStream(InputStream input) throws IOException
    {
        return new ConfigHelper(input, null);
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
            return ConfigHelper.fromFile(getFile(configFile.getParentFile(), relativePath));
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

    public void addIfExists(String propertyName, MappedConfiguration<String, Object> configuration)
    {
        if (properties.containsKey(propertyName))
        {
            configuration.add(propertyName, properties.get(propertyName));
        }
    }
    
    public void overrideIfExists(String propertyName, MappedConfiguration<String, Object> configuration)
    {
        if (properties.containsKey(propertyName))
        {
            configuration.override(propertyName, properties.get(propertyName));
        }
    }
    
    public void override(String propertyName, MappedConfiguration<String, Object> configuration)
    {
        assertPropertyDefined(propertyName, properties);

        configuration.override(propertyName, properties.get(propertyName));
    }
    
    public void add(String propertyName, MappedConfiguration<String, Object> configuration)
    {
        assertPropertyDefined(propertyName, properties);

        configuration.add(propertyName, properties.get(propertyName));
    }
    
    public Set<String> names()
    {
        Set<String> names = new HashSet<String>();
        for (Object key : properties.keySet())
        {
            names.add((String) key);
        }
        return names;
    }
    
    public String get(String propertyName)
    {
        return properties.getProperty(propertyName);
    }
    
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